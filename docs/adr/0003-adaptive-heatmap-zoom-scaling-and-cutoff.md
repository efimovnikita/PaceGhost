# Adaptive Heatmap Trace Scaling and Zoom Cutoff

To prevent personal heatmap walking routes from blooming into multi-kilometer opaque blobs when zooming out, while maintaining complete pan and zoom freedom across the base map, we apply exponential stroke attenuation with a legibility floor for zoom levels down to 10, and truncate heatmap tile rendering entirely below zoom 10.

## Considered Options

- **Static Stroke Width with Hard Map Zoom Clamp (Rejected)**: Prevents the blob effect by physically restricting map view zoom (e.g. min zoom 11), but unnecessarily limits the user's ability to explore broader regional geography on the base map.
- **Strictly Metric Real-World Stroke Scaling (Rejected)**: Scaling stroke purely according to real-world ground meters causes lines to diminish into sub-pixel fractions at city scale, making user routes completely invisible.
- **Free Map Zoom with Static Stroke Width (Rejected)**: Legacy behavior where a fixed 56px aura expands to tens of kilometers at low zooms, smothering entire urban agglomerations in cyan haze.
- **Decoupled Map Zoom with Adaptive Trace Scaling and Visibility Cutoff (Selected)**: The base map allows unrestricted zooming, but the heatmap overlay ceases tile generation below zoom 10 (city scale). For zoom levels 10 to 16, stroke widths scale exponentially with zoom according to `2^((zoom - 16) * 0.6)` down to a 2px core floor, preserving crisp neon arterial lines across all zoom levels without database or rendering bloat.

## Consequences

- Requires zoom-aware tier calculation in `HeatmapTileRasterizer`.
- Restricts `HeatmapTileModuleProvider` minimum zoom to 10 to avoid unnecessary SQLite queries on large bounding boxes.
- Requires lazy cache versioning to flush legacy static-width tiles on first entry to the heatmap screen.
