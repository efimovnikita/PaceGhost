# 02: Heatmap Visibility Cutoff

**What to build:** Enforce a minimum visibility threshold for the personal heatmap overlay at zoom level 10 (metropolitan scale), immediately returning transparent tiles without querying the SQLite track points database when zoomed further out into regional or country scale.

**Blocked by:** 01: Adaptive Stroke Scaling Engine

**Status:** completed

- [x] Declare `MIN_HEATMAP_ZOOM = 10` in `HeatmapTileProvider` / `HeatmapTileModuleProvider`.
- [x] Set `HeatmapTileModuleProvider.getMinimumZoomLevel()` to `MIN_HEATMAP_ZOOM`.
- [x] In `HeatmapTileModuleProvider.getOrRenderTile`, immediately return `null` if `zoom < MIN_HEATMAP_ZOOM`, bypassing Bounding Box calculations and SQLite queries.
- [x] Verify `HeatmapTileProviderTest` confirms 0 calls to `WalkDao.getPointsInBoundingBox` when `zoom < 10`.
- [x] Verify test suite passes via `.\gradlew.bat testDebugUnitTest`.
