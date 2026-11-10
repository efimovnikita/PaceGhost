# 05: Release Version Bump, Git Push, and Release Publication

**What to build:** Finalize the release by incrementing the version in `app/build.gradle.kts`, executing localization and compatibility verifications, committing changes, creating an annotated git tag `v1.12.0`, and pushing to origin to trigger the automated GitHub Actions CI/CD release pipeline.

**Blocked by:** 04: End-to-End Emulator Verification

**Status:** ready-for-agent

- [ ] Bump `versionCode = 19` and `versionName = "1.12.0"` in `app/build.gradle.kts`.
- [ ] Run localization check: `pwsh scripts/check_localization.ps1`.
- [ ] Run compatibility check: `pwsh scripts/check_android_compat.ps1`.
- [ ] Run full unit test suite: `.\gradlew.bat testDebugUnitTest`.
- [ ] Commit all changes with message: `chore(release): bump version to 1.12.0 for adaptive heatmap zoom`.
- [ ] Create annotated git tag: `git tag -a v1.12.0 -m "Release v1.12.0"`.
- [ ] Push main branch and release tag: `git push origin main ; git push origin v1.12.0`.
- [ ] Verify GitHub Actions release workflow initiates build and publication of signed APK.
