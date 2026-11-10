# 06: Release Version Bump, Git Commit, Tag, and Remote Publication

**What to build:** Formal release packaging and publication: bumping version numbers in `build.gradle.kts`, executing localization and compatibility guardrails, committing all feature work to git, creating an annotated release tag, and pushing both `main` and the release tag to the remote repository to trigger the automated GitHub Actions CI/CD release workflow.

**Blocked by:** 05: Android Emulator Verification and User Screenshot Sign-off

**Status:** ready-for-agent

- [ ] `versionCode` and `versionName` bumped in `app/build.gradle.kts`
- [ ] `pwsh scripts/check_localization.ps1` passes with 0 violations
- [ ] `pwsh scripts/check_android_compat.ps1` passes with 0 violations
- [ ] `.\gradlew.bat testDebugUnitTest` runs with all tests green
- [ ] All changes committed to git with a descriptive commit message
- [ ] Annotated git tag `vX.Y.Z` created
- [ ] `main` branch and `vX.Y.Z` tag pushed to remote (`origin`), triggering `.github/workflows/release.yml` for automated APK build and GitHub Release creation
