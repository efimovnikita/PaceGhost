# 05: Android Emulator Verification and User Screenshot Sign-off

**What to build:** Comprehensive end-to-end testing of the Personal Heatmap screen running on a live Android emulator, including generating overlapping demo routes, capturing device screenshots via ADB, embedding them in an artifact, and presenting them to the user for visual inspection and approval prior to committing code.

**Blocked by:** 04: Repository Integration and Heatmap Tile Cache Invalidation

**Status:** ready-for-agent

- [ ] Debug APK builds cleanly via `.\gradlew.bat assembleDebug` and installs on the running Android emulator
- [ ] App launches via ADB without crashes or runtime exceptions
- [ ] At least two overlapping demo walks are populated in the database
- [ ] User navigates to `HeatmapScreen` and map tiles render correctly with glowing neon traces over inverted dark streets
- [ ] High-resolution screenshots captured via ADB (`screencap -p /sdcard/...`) and pulled to disk
- [ ] Screenshots embedded in a session artifact (`UserFacing: true`) and presented to the user for formal approval before any git commit
