# Plan: Fix release-only onboarding redirect (R8 strips serialization metadata)

## Bug
Signed-in existing user (`890890fb...`, `is_onboarded=true`, `full_name` set) routed to setup page in **release build only**.

## Root Cause (confirmed)
- `proguard-rules.pro` has **NO** keep rules for kotlinx.serialization / Hilt / Supabase / DTOs.
- R8 minify strips `$serializer` companion metadata for `@Serializable` DTOs (e.g. `ProfileDto`).
- `ProfileRepositoryImpl.getProfile` → `decodeSingle<ProfileDto>()` throws (serializer missing).
- `AuthViewModel.checkOnboardingStatus.onFailure` → `_isOnboarded = false` → `AppState.Onboarding` → setup page.
- DB verified: `is_onboarded=true`. So failure is client-side decode, not data.

## Fix
1. Add ProGuard keep rules to `app/proguard-rules.pro`:
   - kotlinx.serialization core (Companion + serializer lookups).
   - All `@Serializable` DTOs under `com.aplicator.jobapplier.**` + their `$$serializer`.
   - Supabase + Ktor keeps (kotlinx-serialization based).
   - Keep SourceFile/LineNumberTable for stack traces.
2. Harden `AuthViewModel.checkOnboardingStatus`: on failure keep prior `isOnboarded` value (don't force false) — but primary fix is keep rules.
3. Rebuild release, reinstall via adb, verify existing user lands on Main.

## Verification
- adb install release.
- Sign in as `890890fb...`.
- Confirm lands on Dashboard (Main), NOT setup.
- Check logcat for no serialization errors.
