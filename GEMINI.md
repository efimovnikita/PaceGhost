# PaceGhost Project Steering Guide

Minimalist Android activity tracker featuring Ghost Pacing, OLED dark theme, and TTS audio guidance.

## Architecture & Navigation
- Domain: [`GhostEngine.kt`](file:///C:/Users/--/source/repos/PaceGhost/app/src/main/java/com/paceghost/app/domain/engine/GhostEngine.kt), [`VoiceSettings.kt`](file:///C:/Users/--/source/repos/PaceGhost/app/src/main/java/com/paceghost/app/domain/model/VoiceSettings.kt), [`SmartRouteNamingEngine.kt`](file:///C:/Users/--/source/repos/PaceGhost/app/src/main/java/com/paceghost/app/domain/engine/SmartRouteNamingEngine.kt)
- Services: [`WalkTrackingService.kt`](file:///C:/Users/--/source/repos/PaceGhost/app/src/main/java/com/paceghost/app/service/WalkTrackingService.kt), [`VoiceGuidanceManager.kt`](file:///C:/Users/--/source/repos/PaceGhost/app/src/main/java/com/paceghost/app/service/VoiceGuidanceManager.kt), [`StepSensorManager.kt`](file:///C:/Users/--/source/repos/PaceGhost/app/src/main/java/com/paceghost/app/service/StepSensorManager.kt), [`WalkGeocoder.kt`](file:///C:/Users/--/source/repos/PaceGhost/app/src/main/java/com/paceghost/app/service/geocode/WalkGeocoder.kt)
- UI (Jetpack Compose): [`HomeScreen.kt`](file:///C:/Users/--/source/repos/PaceGhost/app/src/main/java/com/paceghost/app/ui/home/HomeScreen.kt), [`ActiveWalkScreen.kt`](file:///C:/Users/--/source/repos/PaceGhost/app/src/main/java/com/paceghost/app/ui/active/ActiveWalkScreen.kt), [`VoiceSettingsDialog.kt`](file:///C:/Users/--/source/repos/PaceGhost/app/src/main/java/com/paceghost/app/ui/home/components/VoiceSettingsDialog.kt)
- Resources & Strings: [`strings.xml`](file:///C:/Users/--/source/repos/PaceGhost/app/src/main/res/values/strings.xml)
- Local Storage: [`VoiceSettingsPreferences.kt`](file:///C:/Users/--/source/repos/PaceGhost/app/src/main/java/com/paceghost/app/data/local/VoiceSettingsPreferences.kt), Room DB in `data/local/`

## Core Standards
1. **Strictly English UI & Audio**:
   - All user-facing UI labels, dialogs, button texts, accessibility `contentDescription`, and snackbars MUST be in English.
   - All TTS speech strings and engine initialization MUST use `Locale.US` with `Locale.ENGLISH` and `Locale.getDefault()` fallbacks.
2. **OLED Dark Theme**:
   - Background `#000000` / `#121212`, accent colors `NeonGreenRunner` (`#00FF66`) and `CyanGhost` (`#00E5FF`).
3. **TopAppBar Action Density**:
   - Keep actions in `TopAppBar` compact (use small chips or icon buttons) to prevent the `title` ("PaceGhost") from wrapping.
4. **Android Platform & Runtime Guardrails**:
   - **ViewModels**: Always declare `@JvmOverloads constructor(...)` on any `AndroidViewModel` with default parameter values so `ViewModelProvider` reflection finds `(Application)`.
   - **Regex on Android ICU**: Never use `(?U)` flag (unsupported in Android ICU and throws `PatternSyntaxException`). Use `[\p{L}\p{N}]` boundaries for Unicode text.
   - **Crash Telemetry**: Retain `setupCrashHandler` in [`AppLogger.kt`](file:///C:/Users/--/source/repos/PaceGhost/app/src/main/java/com/paceghost/app/util/AppLogger.kt) to flush uncaught exceptions to `paceghost.log`.

## Build & Verification Commands (PowerShell)
- Localization check: `pwsh scripts/check_localization.ps1`
- Android Compatibility check: `pwsh scripts/check_android_compat.ps1` (or `./gradlew checkAndroidCompatibility`)
- Unit Tests: `.\gradlew.bat testDebugUnitTest`
- Debug APK: `.\gradlew.bat assembleDebug`
- Signed Release APK: `.\gradlew.bat assembleRelease` (outputs to `app/build/outputs/apk/release/app-release.apk`)
- CI/CD: Automated release pipeline in `.github/workflows/release.yml` triggers on tag push `v*` and manual dispatch to build, test, and publish release APKs with auto-generated changelogs.

## Release & Versioning Workflow (PowerShell)
1. Bump `versionCode` and `versionName` in [`build.gradle.kts`](file:///C:/Users/--/source/repos/PaceGhost/app/build.gradle.kts).
2. Commit release changes: `git commit -am "chore(release): bump version to X.Y.Z"`
3. Tag and push to trigger automated release build:
   `git tag -a vX.Y.Z -m "Release vX.Y.Z" ; git push origin main ; git push origin vX.Y.Z`
4. The CI/CD pipeline will automatically verify localization, run unit tests, compile the signed APK, and publish the release on GitHub.

## Emulator & ADB Guidelines
- ADB Path: `C:\Users\--\AppData\Local\Android\Sdk\platform-tools\adb.exe`
- Always prefix ADB commands with device wait: `& "C:\Users\--\AppData\Local\Android\Sdk\platform-tools\adb.exe" wait-for-device ; ...`
- When executing complex shell commands with pipes, loops, or inner quotes via ADB in PowerShell, wrap the command in single quotes to avoid PowerShell interpolation: `& $adb shell 'while [ ... ]; do ...; done'`
- After starting an activity (`am start`), allow 2–3 seconds before taking screenshots so Compose finishes rendering.
- When capturing screenshots for the user, immediately embed them into an artifact with `UserFacing: true`.
- Target `rg` searches specifically inside `app/src/` to avoid scanning heavy `build/` and `.gradle/` directories.
