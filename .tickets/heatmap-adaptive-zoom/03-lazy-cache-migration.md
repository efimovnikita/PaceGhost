# 03: Lazy Cache Migration on Heatmap Launch

**What to build:** Automatically purge outdated static-width raster tiles stored on disk when opening the heatmap screen after upgrading to the adaptive zoom engine, avoiding display of stale blurry blobs while leaving normal app cold startup unblocked.

**Blocked by:** 02: Heatmap Visibility Cutoff

**Status:** closed

- [x] Define `HEATMAP_CACHE_SCHEMA_VERSION = 2` in `HeatmapPreferences` or `HeatmapTileProvider`.
- [x] Implement lazy one-time cache purge upon entering `HeatmapScreen` / initializing `HeatmapViewModel`.
- [x] If stored cache version in `SharedPreferences` is less than 2, delete existing files in `context.cacheDir/heatmap_tiles` and update stored version to 2.
- [x] Add unit tests in `HeatmapCacheInvalidationTest` verifying one-time directory purge and subsequent idempotency.
- [x] Verify test suite passes via `.\gradlew.bat testDebugUnitTest`.
