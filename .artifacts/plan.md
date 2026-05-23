# Implementation Plan: Usage Limits Per Account

**Spec:** `docs/superpowers/specs/2026-05-22-usage-limits-design.md`
**Date:** 2026-05-22

---

## Phase 1: Database (Supabase Migration)

### Step 1.1 — Create enums + tables

Apply migration with:
- `quota_request_status` enum: `pending`, `approved`, `denied`
- `quota_request_type` enum: `weekly`, `resume`
- `user_quotas` table (columns per spec)
- `quota_requests` table (using enums)
- RLS policies on both tables

### Step 1.2 — Triggers

- `create_user_quota()` trigger on `auth.users` insert → auto-create quota row
- `on_quota_request_approved()` trigger on `quota_requests` update → grant +10 extra

### Step 1.3 — Weekly reset cron

- `pg_cron` job: every Monday 00:00 UTC → reset `weekly_usage_count = 0`, update `week_start`

### Step 1.4 — Backfill existing users

- Insert `user_quotas` row for all existing `auth.users` who don't have one yet

---

## Phase 2: Edge Function (`supabase/functions/ai-proxy/index.ts`)

### Step 2.1 — Add quota check function

Add `checkAndIncrementQuota(serviceClient, userId, action)`:
- Query `user_quotas` for user (create row if missing)
- For `parse_resume`: check `resume_parse_count < resume_parse_limit`
- For others: check `weekly_usage_count < weekly_ai_limit + extra_quota_remaining`
- Use atomic SQL: `UPDATE ... SET count = count + 1 WHERE count < limit RETURNING *`
- Return `{ allowed, quotaInfo?, isLastResumeParse? }`

### Step 2.2 — Integrate quota check into main handler

- Call `checkAndIncrementQuota` before `callGemini`
- If not allowed → return HTTP 429 with structured JSON body
- For `parse_resume` on last use → add `quota_warning: "last_resume_parse"` to response

### Step 2.3 — Fix `fetch_job_url` logging gap

- Move usage logging to happen for ALL actions including `fetch_job_url`
- `fetch_job_url` counts toward weekly quota

### Step 2.4 — Consumption order logic

- First consume from weekly limit (`weekly_usage_count++`)
- When weekly exhausted, consume from extra (`extra_quota_remaining--`)
- When both zero → 429

---

## Phase 3: Android Data Layer

### Step 3.1 — New models

**File:** `app/src/main/java/com/aplicator/jobapplier/data/remote/ai/QuotaModels.kt`
- `QuotaExceededResponse` (maps 429 body)
- `QuotaExceededException` (custom exception wrapping quota info)

### Step 3.2 — Modify `AiRepositoryImpl.callEdgeFunction()`

**File:** `app/src/main/java/com/aplicator/jobapplier/data/repository/AiRepositoryImpl.kt`
- Inspect HTTP status from edge function response
- If 429 → deserialize `QuotaExceededResponse` → throw `QuotaExceededException`
- If 200 + `quota_warning` field → include in result

### Step 3.3 — New `QuotaRepository`

**File:** `app/src/main/java/com/aplicator/jobapplier/data/repository/QuotaRepository.kt`
- `getQuotaStatus(): QuotaStatus` — read `user_quotas` via Postgrest
- `requestExtraQuota(type: String): Result<Unit>` — insert into `quota_requests`
- `hasPendingRequest(): Boolean` — check for existing pending

### Step 3.4 — DI registration

**File:** `app/src/main/java/com/aplicator/jobapplier/di/AppModule.kt`
- Register `QuotaRepository` in Hilt/Koin module

---

## Phase 4: Android ViewModel Layer

### Step 4.1 — Quota state handling

Add to each AI-calling ViewModel (`JobViewModel`, `AiSuggestionsViewModel`, `ResumeImportViewModel`):
- Catch `QuotaExceededException` specifically from `runCatching`
- Emit quota-exceeded state with parsed info
- Handle `quota_warning` for resume parse warning

### Step 4.2 — Request extra quota action

- Add `requestExtraQuota()` function in relevant ViewModels
- Validates: no remaining quota AND no pending request
- Calls `QuotaRepository.requestExtraQuota()`

---

## Phase 5: Android UI Layer

### Step 5.1 — `QuotaExceededDialog` composable

**File:** `app/src/main/java/com/aplicator/jobapplier/ui/components/QuotaExceededDialog.kt`
- Material3 AlertDialog
- Shows: quota type, usage stats, reset date (weekly) or "lifetime limit" (resume)
- "Request Extra Quota" button (disabled if can't request)
- "OK" dismiss button
- Clear user-friendly copy

### Step 5.2 — `ResumeParseWarningDialog` composable

**File:** `app/src/main/java/com/aplicator/jobapplier/ui/components/ResumeParseWarningDialog.kt`
- Warning before executing 2nd parse
- "This is your final free resume parse..."
- "Continue" / "Cancel" buttons

### Step 5.3 — Integrate dialogs into screens

- `JobDetailScreen.kt` — show `QuotaExceededDialog` on quota state
- `AiSuggestionsScreen.kt` — same
- `ResumeImportScreen.kt` — both dialogs (warning + exceeded)
- `BubbleOverlayService.kt` — toast "Quota exceeded" + link to open app

### Step 5.4 — Request confirmation UI

- After "Request Extra Quota" tapped → show success message "Request sent to app owner"
- If pending request exists → show "Request pending, please wait for approval"

---

## Phase 6: Verification

1. Deploy migration → verify tables/triggers exist
2. Deploy edge function → test 429 response format
3. Make 15 AI calls → verify 16th returns 429
4. Parse resume twice → verify warning on 2nd, block on 3rd
5. Request extra → approve in DB → verify +10 works
6. Test Android dialog renders on 429
7. Test "Request Extra" button creates DB row
8. Test weekly reset (manual trigger) → counter resets
9. Test concurrent requests → atomic increment prevents over-use

---

## Files Modified/Created

| File | Action |
|------|--------|
| `supabase/migrations/YYYYMMDD_usage_limits.sql` | Create |
| `supabase/functions/ai-proxy/index.ts` | Modify |
| `app/.../data/remote/ai/QuotaModels.kt` | Create |
| `app/.../data/repository/AiRepositoryImpl.kt` | Modify |
| `app/.../data/repository/QuotaRepository.kt` | Create |
| `app/.../di/AppModule.kt` | Modify |
| `app/.../ui/components/QuotaExceededDialog.kt` | Create |
| `app/.../ui/components/ResumeParseWarningDialog.kt` | Create |
| `app/.../ui/job/JobDetailScreen.kt` | Modify |
| `app/.../ui/job/JobViewModel.kt` | Modify |
| `app/.../ui/suggestions/AiSuggestionsScreen.kt` | Modify |
| `app/.../ui/suggestions/AiSuggestionsViewModel.kt` | Modify |
| `app/.../ui/resume/ResumeImportScreen.kt` | Modify |
| `app/.../ui/resume/ResumeImportViewModel.kt` | Modify |
| `app/.../service/BubbleOverlayService.kt` | Modify |
