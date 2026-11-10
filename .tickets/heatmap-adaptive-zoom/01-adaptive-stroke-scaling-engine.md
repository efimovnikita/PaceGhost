# 01: Adaptive Stroke Scaling Engine

**What to build:** Enable the heatmap rasterizer to scale polyline diffusion stroke widths dynamically according to map zoom level down to a strict legibility floor, preventing walking routes from blooming into oversized blobs when zooming out while maintaining full Cyber Neon vibrancy at street levels.

**Blocked by:** None (can start immediately)

**Status:** closed

- [x] Implement `getDensityTiersForZoom(zoom: Int): List<HeatmapDensityTier>` in `HeatmapTileRasterizer`.
- [x] Scale factor follows $k = 2^{(\text{zoom} - 16) \times 0.6}$ for $\text{zoom} < 16$, and clamped to $k = 1.0$ for $\text{zoom} \ge 16$.
- [x] Enforce legibility floor clamps: core stroke width $\ge 2.0\text{f}$ and outer aura stroke width $\ge 4.5\text{f}$ on any zoom level.
- [x] Pass `zoom` into `drawDensityHeatmap` so tile paths and point circles use the zoom-adaptive tiers.
- [x] Add unit tests in `HeatmapTileRasterizerTest` verifying tier stroke widths at zoom levels 16, 12, 10, and 18.
- [x] Verify test suite passes via `.\gradlew.bat testDebugUnitTest`.
