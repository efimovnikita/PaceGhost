# 04: End-to-End Emulator Verification

**What to build:** Validate adaptive heatmap zoom and visibility cutoff behavior end-to-end on an Android emulator with live rendered routes, capturing screenshots across street, city, and regional scales.

**Blocked by:** 03: Lazy Cache Migration on Heatmap Launch

**Status:** completed

- [x] Assemble and install debug APK onto Android emulator via ADB.
- [x] Ensure test walk data is populated and navigate to Personal Heatmap screen.
- [x] Inspect visual rendering at `zoom = 16` (street scale: rich glowing corridor).
- [x] Zoom out to `zoom = 12` (district scale: slim neon line).
- [x] Zoom out to `zoom = 10` (city scale: thin filament without bloating over the city).
- [x] Zoom out to `zoom = 8` (regional scale: heatmap gracefully cuts off, dark base map remains clean).
- [x] Capture screenshots via `adb shell screencap`, pull to host, and embed in an artifact with `UserFacing: true`.
