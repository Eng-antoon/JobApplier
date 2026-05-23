# Usage Limit Per Account — Design Spec

**Date:** 2026-05-22
**Status:** Approved
**Scope:** Edge function enforcement, DB schema, Android UI

---

## Problem

App has no usage enforcement. Any authenticated user can make unlimited AI calls. Need minimal quota for testing, per-user adjustable limits, weekly reset, and proper UI when limits hit.

---

## Requirements

### Quota Rules

| Action Type | Limit | Reset | Notes |
|-------------|-------|-------|-------|
| `parse_resume` | 2 total (lifetime) | Never | Warning on 2nd call. After exhausted, must request from owner. |
| All other AI actions | 15 per week | Weekly (cron) | 60/month effective. Resets every Monday 00:00 UTC. |

### Extra Quota

- Owner grants +10 AI requests per approved request
- User can only request extra when quota is fully exhausted (0 remaining)
- User cannot request if a pending request already exists
- Owner approves by setting `status = 'approved'` in `quota_requests` table
- Approval triggers `extra_quota_remaining += 10` on `user_quotas`

### Resume Parse Warning

- 1st parse: normal, no warning
- 2nd parse: before executing, show warning dialog: "This is your final free resume parse. After this, you'll need to request additional quota from the app owner."
- After 2nd: block with quota exceeded dialog

---

## Database Schema

### Table: `user_quotas`

```sql
CREATE TABLE user_quotas (
  user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  weekly_ai_limit INTEGER NOT NULL DEFAULT 15,
  resume_parse_limit INTEGER NOT NULL DEFAULT 2,
  extra_quota_remaining INTEGER NOT NULL DEFAULT 0,
  week_start TIMESTAMPTZ NOT NULL DEFAULT now(),
  weekly_usage_count INTEGER NOT NULL DEFAULT 0,
  resume_parse_count INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- RLS: users can read own row, only service_role can write
ALTER TABLE user_quotas ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can read own quota"
  ON user_quotas FOR SELECT
  USING (auth.uid() = user_id);

-- No INSERT/UPDATE/DELETE policies for users — edge function uses service_role
```

### Enums

```sql
CREATE TYPE quota_request_status AS ENUM ('pending', 'approved', 'denied');
CREATE TYPE quota_request_type AS ENUM ('weekly', 'resume');
```

### Table: `quota_requests`

```sql
CREATE TABLE quota_requests (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  status quota_request_status NOT NULL DEFAULT 'pending',
  request_type quota_request_type NOT NULL DEFAULT 'weekly',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  resolved_at TIMESTAMPTZ
);

ALTER TABLE quota_requests ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can read own requests"
  ON quota_requests FOR SELECT
  USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own requests"
  ON quota_requests FOR INSERT
  WITH CHECK (auth.uid() = user_id);
```

### Auto-create quota row on signup (trigger)

```sql
CREATE OR REPLACE FUNCTION create_user_quota()
RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO user_quotas (user_id) VALUES (NEW.id);
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER on_auth_user_created_quota
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION create_user_quota();
```

### Weekly reset (pg_cron)

```sql
SELECT cron.schedule(
  'reset-weekly-quotas',
  '0 0 * * 1',  -- Every Monday at 00:00 UTC
  $$
    UPDATE user_quotas
    SET weekly_usage_count = 0,
        week_start = now(),
        updated_at = now();
  $$
);
```

### Approval trigger (grant extra quota)

```sql
CREATE OR REPLACE FUNCTION on_quota_request_approved()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.status = 'approved' AND OLD.status = 'pending' THEN
    UPDATE user_quotas
    SET extra_quota_remaining = extra_quota_remaining + 10,
        updated_at = now()
    WHERE user_id = NEW.user_id;
    NEW.resolved_at = now();
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER on_quota_approved
  BEFORE UPDATE ON quota_requests
  FOR EACH ROW EXECUTE FUNCTION on_quota_request_approved();
```

---

## Edge Function Changes (`ai-proxy/index.ts`)

### New: Quota check before every AI call

```typescript
async function checkAndIncrementQuota(
  serviceClient: SupabaseClient,
  userId: string,
  action: string
): Promise<{ allowed: boolean; quotaInfo?: QuotaError }> {
  // 1. Get or create user_quotas row
  // 2. For parse_resume: check resume_parse_count < resume_parse_limit
  // 3. For others: check (weekly_usage_count < weekly_ai_limit + extra_quota_remaining)
  // 4. If allowed: increment counter, return { allowed: true }
  // 5. If denied: return structured error with reset info
}
```

### Response on quota exceeded (HTTP 429)

```json
{
  "error": "quota_exceeded",
  "quota_type": "weekly | resume",
  "used": 15,
  "limit": 15,
  "extra_remaining": 0,
  "resets_at": "2026-05-26T00:00:00Z",
  "can_request_extra": true
}
```

### Response for resume parse warning (HTTP 200 + warning header)

When `resume_parse_count == 1` (about to use last one):
- Proceed with the parse (don't block)
- Add field to response: `"quota_warning": "last_resume_parse"`
- Android app shows warning after successful parse

### Consumption order for regular actions

1. Consume from `weekly_ai_limit` first (weekly_usage_count++)
2. When weekly limit exhausted, consume from `extra_quota_remaining` (extra_quota_remaining--)
3. When both exhausted → 429

---

## Android App Changes

### 1. Data Layer

**New model:** `QuotaInfo.kt`
```kotlin
data class QuotaInfo(
    val quotaType: String,  // "weekly" or "resume"
    val used: Int,
    val limit: Int,
    val extraRemaining: Int,
    val resetsAt: String?,
    val canRequestExtra: Boolean
)
```

**Modify:** `AiRepositoryImpl.callEdgeFunction()`
- Check HTTP response status code
- If 429 → parse body as `QuotaInfo` → throw `QuotaExceededException(quotaInfo)`
- If 200 with `quota_warning` field → include warning in result

**New:** `QuotaRepository.kt`
- `getQuotaStatus()` → reads `user_quotas` via Postgrest
- `requestExtraQuota(type: String)` → inserts into `quota_requests`
- `hasPendingRequest()` → checks for existing pending request

### 2. ViewModel Layer

**New sealed class:** `QuotaState`
- `Available(remaining: Int, total: Int)`
- `Warning(message: String)` — for 2nd resume parse
- `Exceeded(quotaInfo: QuotaInfo)`

Each AI-calling ViewModel gets a `quotaState` flow that triggers the dialog.

### 3. UI Layer

**New composable:** `QuotaExceededDialog`
- Modal overlay (Material3 AlertDialog)
- Shows: which limit hit, usage stats, reset date (weekly) or "lifetime" (resume)
- "Request Extra Quota" button — enabled only when `canRequestExtra == true`
- "OK" dismiss button
- Clean, informative copy

**New composable:** `ResumeParseWarningDialog`
- Shows before 2nd parse executes
- "This is your final free resume parse..."
- "Continue" and "Cancel" buttons

**Modify existing screens:**
- `JobDetailScreen` — observe `quotaState`, show dialog on `Exceeded`
- `AiSuggestionsScreen` — same
- `ResumeImportScreen` — same + warning dialog before 2nd parse
- `BubbleOverlayService` — show toast with "Quota exceeded" message + disable AI buttons

### 4. Error 429 Handling (currently missing)

The Supabase Kotlin SDK throws when edge function returns non-2xx. Current `runCatching` catches everything as generic error. Fix:

```kotlin
try {
    val response = supabaseClient.functions.invoke("ai-proxy", body = request)
    // success path
} catch (e: RestException) {
    if (e.statusCode == 429) {
        // parse quota info from error body
        throw QuotaExceededException(parseQuotaInfo(e.body))
    }
    throw e
}
```

---

## Owner Workflow (Tony)

To grant extra quota:
```sql
UPDATE quota_requests SET status = 'approved' WHERE id = '<request-id>';
-- Trigger auto-grants +10 to user
```

To manually adjust limits for specific user:
```sql
UPDATE user_quotas
SET weekly_ai_limit = 30, resume_parse_limit = 5
WHERE user_id = '<user-id>';
```

To check pending requests:
```sql
SELECT qr.*, p.full_name, p.email
FROM quota_requests qr
JOIN profiles p ON p.id = qr.user_id
WHERE qr.status = 'pending';
```

---

## Edge Cases

1. **New user, no quota row** → edge function creates one with defaults on first AI call
2. **Concurrent requests** → use `UPDATE ... SET count = count + 1 WHERE count < limit RETURNING *` for atomic check+increment
3. **Extra quota + weekly reset** → extra_quota_remaining does NOT reset weekly, only weekly_usage_count resets
4. **fetch_job_url action** → counts toward weekly limit (fix current gap where it's not logged)
5. **User already has pending request** → "Request Extra" button disabled, shows "Request pending"

---

## Verification Plan

1. Create tables + triggers via Supabase migration
2. Deploy updated edge function
3. Test: make 15 AI calls → verify 16th returns 429
4. Test: parse resume twice → verify warning on 2nd, block on 3rd
5. Test: request extra quota → approve → verify +10 works
6. Test: weekly reset cron → verify counter resets
7. Test: Android dialog appears on 429
8. Test: "Request Extra" button creates DB row
