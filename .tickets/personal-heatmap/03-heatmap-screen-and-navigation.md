# 03: Full-Screen Heatmap Screen, Inverted Base Map, and Navigation

**What to build:** A dedicated Compose screen (`HeatmapScreen`) displaying the runner's cumulative routes layered over OpenStreetMap Mapnik tiles darkened via runtime `ColorMatrixColorFilter`, with full pan and zoom at 60 FPS, a back button, a recenter button, and an entry icon in the `HomeScreen` TopAppBar.

**Blocked by:** 02: Web Mercator Projection and Two-Pass Glow Tile Rasterizer

**Status:** completed

- [x] `MainActivity` navigation supports `Screen.Heatmap`
- [x] `HomeScreen` TopAppBar features a compact `IconButton` with `Icons.Filled.Layers` and `contentDescription = "Personal Heatmap"` without breaking title layout or causing text wrap
- [x] Base OpenStreetMap tiles render in dark inverted mode using `ColorMatrixColorFilter` over a pure black `#000000` canvas
- [x] `TilesOverlay` loads generated heatmap tiles from local cache on-demand without freezing the UI thread
- [x] Screen includes top app bar with back navigation and a floating button to center the map on recent walk bounds
- [x] All user-facing UI strings and content descriptions strictly adhere to English
