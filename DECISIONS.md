# Architecture Decisions

## 2026-05-23: Catch RestException instead of checking HttpResponse status for quota enforcement

**Context:** supabase-kt SDK v3.1.4 throws `RestException` on any non-2xx HTTP response before the caller receives the `HttpResponse` object. The `Functions.parseErrorResponse()` maps unknown status codes (including 429) to `UnauthorizedRestException` via an `else` branch.

**Decision:** Catch `RestException` in repository layer, check `e.statusCode == 429`, parse `e.error` (which contains the raw response body) as `QuotaExceededResponse`, and throw `QuotaExceededException`. This is the only reliable way to intercept 429 responses from edge functions when using supabase-kt.

**Alternatives considered:**
- Custom Ktor `HttpClient` plugin to intercept before SDK — too invasive, couples to SDK internals
- Disable `parseErrorResponse` on Functions — not supported by SDK API

**Why:** The SDK's error handling is opaque but `RestException` preserves both `statusCode` and raw body text, making it sufficient for our needs.

## 2026-05-22: PostgreSQL ENUMs for bounded-choice fields

**Context:** `quota_requests.status` and `quota_requests.request_type` are bounded to specific values.

**Decision:** Use PostgreSQL ENUM types (`quota_request_status`, `quota_request_type`) instead of `TEXT + CHECK` constraints.

**Why:** Tony's explicit requirement — enforces constraints at DB type level, clearer intent, better tooling support.

## 2026-05-22: Atomic increment for quota consumption

**Context:** Multiple concurrent AI requests could race and exceed quota limits.

**Decision:** Use optimistic locking via `UPDATE ... WHERE count = old_count`. If 0 rows affected, re-read and retry once.

**Why:** Simpler than `SELECT FOR UPDATE` (no explicit transaction needed), works well at this scale, and the retry-once pattern handles the rare race condition.
