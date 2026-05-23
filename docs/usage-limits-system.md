# Usage Limit Per Account — System Documentation

## Goal

Control AI usage costs by enforcing per-user quotas:
- **Resume parsing**: 2 total (lifetime) per user
- **All other AI actions**: 15 per week, resetting every Monday 00:00 UTC
- **Extra quota**: Owner approvals are type-specific: +10 weekly AI actions or +2 resume parses
- **User visibility**: Quota usage indicator + dialog when limit hit
- **Sign-in visibility**: Login and signup explain usage limits before the user enters the app

---

## Architecture Overview

```
┌──────────────────────────────────────────────────────────────┐
│                     Android App                              │
│                                                              │
│  Screen → ViewModel → Repository → Supabase Edge Function   │
│                                                              │
│  QuotaDialogs ← QuotaExceededException ← 429 Response       │
│  QuotaIndicator ← QuotaRepository ← user_quotas table       │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│                   Supabase Backend                           │
│                                                              │
│  ai-proxy Edge Function                                      │
│    ├── checkAndIncrementQuota() — atomic quota enforcement   │
│    ├── Returns 429 + JSON on exceeded                        │
│    └── Returns quota_warning on last resume parse            │
│                                                              │
│  PostgreSQL                                                  │
│    ├── user_quotas — per-user counters and limits            │
│    ├── quota_requests — extra quota request workflow         │
│    ├── quota_request_status ENUM (pending/approved/denied)   │
│    ├── quota_request_type ENUM (weekly/resume)               │
│    ├── create_user_quota() trigger — auto-creates on signup  │
│    ├── on_quota_request_approved() trigger — type grants     │
│    └── pg_cron — weekly reset every Monday                   │
└──────────────────────────────────────────────────────────────┘
```

---

## Database Schema

### `user_quotas` Table
| Column | Type | Default | Description |
|--------|------|---------|-------------|
| user_id | UUID (PK, FK → auth.users) | — | User reference |
| weekly_ai_limit | INT | 15 | Max weekly AI requests (adjustable per user) |
| resume_parse_limit | INT | 2 | Max lifetime resume parses (adjustable per user) |
| extra_quota_remaining | INT | 0 | Approved extra weekly AI actions |
| extra_resume_parse_remaining | INT | 0 | Approved extra resume parses |
| week_start | TIMESTAMPTZ | now() | Start of current week period |
| weekly_usage_count | INT | 0 | Requests used this week |
| resume_parse_count | INT | 0 | Resume parses used (lifetime) |

### `quota_requests` Table
| Column | Type | Default | Description |
|--------|------|---------|-------------|
| id | UUID (PK) | gen_random_uuid() | Request ID |
| user_id | UUID (FK → auth.users) | — | Requesting user |
| status | quota_request_status ENUM | 'pending' | pending/approved/denied |
| request_type | quota_request_type ENUM | 'weekly' | weekly/resume |
| created_at | TIMESTAMPTZ | now() | Request timestamp |

### RLS Policies
- Users can SELECT own rows only
- All writes go through service_role (edge function)

### Triggers
1. **`create_user_quota()`** — Fires on `auth.users` INSERT → creates `user_quotas` row with defaults
2. **`on_quota_request_approved()`** — Fires on `quota_requests` UPDATE where status changes from `pending` to `approved`; weekly requests add 10 to `extra_quota_remaining`, resume requests add 2 to `extra_resume_parse_remaining`. Approved or denied requests set `resolved_at`.

### Cron
- **`reset-weekly-quotas`** — Every Monday 00:00 UTC: resets `weekly_usage_count = 0` and updates `week_start`

---

## Edge Function Logic (`ai-proxy/index.ts`)

### Quota Check Flow (every AI call)
```
1. Get user's quota row from user_quotas
2. If action = parse_resume:
   - Use included resume parses first: resume_parse_count < resume_parse_limit
   - If included parses are exhausted, consume from extra_resume_parse_remaining
   - If both are exhausted → return 429
   - Atomic update uses the current counter value in the WHERE clause
3. If action = any other:
   - Use included weekly actions first: weekly_usage_count < weekly_ai_limit
   - If weekly actions are exhausted, consume from extra_quota_remaining
   - If over limit AND no extra → return 429
   - Weekly extras decrement without increasing weekly_usage_count, so +10 approval grants the full 10 actions
4. Proceed with AI call
```

### 429 Response Format
```json
{
  "error": "quota_exceeded",
  "quota_type": "weekly" | "resume",
  "used": 15,
  "limit": 15,
  "extra_remaining": 0,
  "weekly_extra_remaining": 0,
  "resume_extra_remaining": 0,
  "resets_at": "2026-05-26T00:00:00.000Z" | null,
  "can_request_extra": true | false
}
```

### Successful Response Warning
On last resume parse, adds to response: `"quota_warning": "last_resume_parse"`

### Atomic Increment Pattern
```sql
UPDATE user_quotas
SET weekly_usage_count = weekly_usage_count + 1
WHERE user_id = $userId AND weekly_usage_count = $currentCount
```
If 0 rows affected → concurrent request beat us → re-read and retry once. Prevents race conditions.

---

## Android App Architecture

### Data Layer
- **`QuotaModels.kt`** — `QuotaExceededResponse` (maps 429 JSON), `QuotaExceededException` (custom exception), `UserQuotaRow` (maps user_quotas), `QuotaRequestRow` (maps quota_requests)
- **`QuotaRepository`** — Interface: `getQuotaStatus()`, `requestExtraQuota()`, `hasPendingRequest()`
- **`QuotaRepositoryImpl`** — Supabase Postgrest implementation
- **`AiRepositoryImpl.callEdgeFunction()`** — Catches `RestException` on 429, throws `QuotaExceededException`
- **`ResumeImportRepositoryImpl.parseResume()`** — Same 429 handling

### UI Layer
- **`QuotaDialogs.kt`** — Three composables:
  - `QuotaExceededDialog` — Shows when limit hit (progress bar, request type-specific button, pending state)
  - `ResumeParseWarningDialog` — Warning before last parse
  - `QuotaRequestSuccessDialog` — Confirmation after requesting weekly or resume extras
- **All ViewModels** expose: `quotaExceeded`, `quotaRequestPending`, `quotaRequestSuccess`, `quotaRequestSuccessType` StateFlows
- **All Screens** render `QuotaExceededDialog` and `QuotaRequestSuccessDialog` when state is set
- **Auth screens** show an "AI usage process" note: 15 weekly actions, 2 resume parses, approval grants, and reset timing
- **BubbleOverlayService** — Shows Toast with descriptive message on quota error

### Error Flow
```
Edge Function returns 429
    ↓
Supabase SDK throws RestException (statusCode=429, error=JSON body)
    ↓
Repository catches RestException, checks statusCode == 429
    ↓
Parses e.error as QuotaExceededResponse
    ↓
Throws QuotaExceededException
    ↓
ViewModel catches in onFailure { if (error is QuotaExceededException) }
    ↓
Sets _quotaExceeded.value = error.quotaInfo
    ↓
Screen renders QuotaExceededDialog
```

---

## Owner Management (SQL)

```sql
-- View pending requests
SELECT qr.*, uq.weekly_usage_count, uq.resume_parse_count
FROM quota_requests qr
JOIN user_quotas uq ON uq.user_id = qr.user_id
WHERE qr.status = 'pending';

-- Approve request (weekly grants +10 actions, resume grants +2 parses)
UPDATE quota_requests SET status = 'approved' WHERE id = '<request-id>';

-- Deny request
UPDATE quota_requests SET status = 'denied' WHERE id = '<request-id>';

-- Adjust limits per user
UPDATE user_quotas SET weekly_ai_limit = 30 WHERE user_id = '<user-id>';
UPDATE user_quotas SET resume_parse_limit = 5 WHERE user_id = '<user-id>';

-- Manually grant extras if needed
UPDATE user_quotas SET extra_quota_remaining = extra_quota_remaining + 10 WHERE user_id = '<user-id>';
UPDATE user_quotas SET extra_resume_parse_remaining = extra_resume_parse_remaining + 2 WHERE user_id = '<user-id>';

-- Reset a user's weekly count manually
UPDATE user_quotas SET weekly_usage_count = 0 WHERE user_id = '<user-id>';
```

---

## Fixed Issues

1. **429 handling was broken** — SDK throws exception before response is visible. Fixed by catching `RestException` and checking `statusCode`.
2. **No proactive quota display** — Dashboard card shows weekly usage, weekly extras, resume usage, and resume extras.
3. **Resume approvals did not unblock parsing** — Resume approvals now grant and consume `extra_resume_parse_remaining`.
4. **Weekly approvals granted fewer than 10 usable actions** — Weekly extras now decrement independently from `weekly_usage_count`.
5. **Users did not see usage rules during auth** — Login and signup now show the AI usage process before sign-in.
