# 05: Release Version Bump, Git Push, and Release Publication

**What to build:** Finalize the release by incrementing the version in `app/build.gradle.kts`, executing localization and compatibility verifications, committing changes, creating an annotated git tag `v1.12.0`, and pushing to origin to trigger the automated GitHub Actions CI/CD release pipeline.

**Blocked by:** 04: End-to-End Emulator Verification

**Status:** completed

- [x] Bump `versionCode = 19` and `versionName = "1.12.0"` in `app/build.gradle.kts`.
- [x] Run localization check: `pwsh scripts/check_localization.ps1`.
- [x] Run compatibility check: `pwsh scripts/check_android_compat.ps1`.
- [x] Run full unit test suite: `.\gradlew.bat testDebugUnitTest`.
- [x] Commit all changes with message: `chore(release): bump version to 1.12.0 for adaptive heatmap zoom`.
- [x] Create annotated git tag: `git tag -a v1.12.0 -m "Release v1.12.0"`.
- [x] Push main branch and release tag: `git push origin main ; git push origin v1.12.0`.
- [x] Verify GitHub Actions release workflow initiates build and publication of signed APK.
