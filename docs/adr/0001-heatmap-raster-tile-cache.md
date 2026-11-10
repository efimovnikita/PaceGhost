# Heatmap Raster Tile Cache for Offline Performance

To display cumulative historical routes on the map without UI stutter or memory exhaustion, we render Personal Heatmap traces into an offline raster tile pyramid (Mercator projection) served via Osmdroid `TilesOverlay`, rather than drawing raw vector polylines dynamically on every map movement.

## Considered Options

- **Dynamic Vector Polylines (Rejected)**: Passing hundreds of thousands of `TrackPoint` coordinates to the map canvas drops frame rates below 10 FPS and risks out-of-memory errors on mid-range Android devices.
- **Dynamic Bounding-Box Polling with Simplification (Rejected)**: Querying SQLite and running Douglas-Peucker simplification on visible viewport segments during pan/pinch still introduces perceptible jank during rapid gesture tracking.
- **Offline Raster Tile Cache (Selected)**: Traces are rasterized into 256x256 transparent PNG tiles indexed by zoom/x/y. Osmdroid loads and caches them asynchronously using native tile handling, ensuring consistent 60 FPS scrolling and low memory footprint.

## Consequences

- Requires a background tile generation pipeline and cache invalidation strategy when new walks are recorded or deleted.
- Requires tile coordinate math (Web Mercator projection to pixel coordinates within tile boundaries).
