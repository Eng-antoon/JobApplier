# Plan: Production Release APK (lightweight, for friend testing)

## Goal
Generate signed, minified production APK to distribute to friends for testing. Small size priority.

## Decisions
- Generate new dedicated release keystore (reusable).
- Single universal APK (one file, installs any device).
- Release build already has `isMinifyEnabled=true` + `isShrinkResources=true`.

## Steps
1. Generate keystore via `keytool` → `keystore/jobapplier-release.jks`.
2. Create `keystore/key.properties` (gitignored) holding store/key passwords + aliases.
3. Add `keystore/` + `*.jks` + `key.properties` to `.gitignore`. (DONE)
4. Add `signingConfigs.release` block in `app/build.gradle.kts`, wire into `buildTypes.release.signingConfig`.
5. Run `./gradlew :app:assembleRelease`.
6. Verify output APK exists + size in `app/build/outputs/apk/release/`.
7. Run `coderabbit review --plain` on the build config change.
8. Update status.md + DECISIONS.md + Obsidian.

## Out of scope
- Play Store upload / .aab (user chose direct sideload).
- Removing pdfbox/poi (needed for resume feature).

## Size levers already applied
- R8 minify, resource shrinking.
- Universal single ABI — fine, app mostly JVM.
