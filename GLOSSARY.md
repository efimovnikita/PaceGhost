# PaceGhost

A minimalist Android activity tracker featuring ghost pacing, OLED dark theme, and TTS audio guidance.

## Language

**Personal Heatmap**:
A visual map overlay displaying all cumulative historical walking routes rendered with additive glow to highlight frequently traveled areas.
_Avoid_: Thermal map, breadcrumb cloud, frequency grid

**Heatmap Tile Cache**:
A localized, pyramid-structured raster tile repository on disk used to serve pre-rendered heatmap overlays to the map engine at 60 FPS.
_Avoid_: Vector point buffer, live line buffer

**Additive Trace**:
A semi-transparent polyline segment rendered using additive color blending so overlapping routes brighten progressively into a vivid neon glow.
_Avoid_: Opaque line, polyline cluster

**Base Map Dark Filter**:
A color matrix filter applied directly to standard OpenStreetMap tiles to invert and darken luminance, fitting the OLED Dark Theme without external map services.
_Avoid_: Dark tile server, inverted canvas wrapper

**Cyber Neon Ramp**:
A progression of glowing neon colors spanning from deep cyan through CyanGhost and NeonGreenRunner to white, representing increasing passage density.
_Avoid_: Heat spectrum, thermal palette, fire gradient

**Two-Pass Glow**:
A rendering method drawing a wide, semi-transparent aura beneath a thin, saturated core trace to simulate a glowing neon trail on a raster tile.
_Avoid_: Blur filter, raster glow effect

**Tile Bounding Box**:
The geographic coordinate envelope (minimum and maximum latitude and longitude) encompassing a single Web Mercator map tile.
_Avoid_: Tile envelope, map quadrangle

**Adaptive Heat Trace**:
A dynamic polyline rendering strategy where stroke width attenuates exponentially at lower zoom levels, keeping routes legible as crisp neon filaments rather than expanding into oversized blobs.
_Avoid_: Fixed-width stroke, static line buffer

**Heatmap Visibility Cutoff**:
The zoom threshold (level 10) separating the two representations of walk history: below it only Walk Presence Markers are shown, at or above it only the Personal Heatmap is shown, never both.
_Avoid_: Hard map clamp, unbounded layer rendering, overlap band

**Walk Anchor**:
The single geographic point representing a walk at world scale, defined as the center of the walk's route bounding box; walks with fewer than two track points have no anchor and are not represented.
_Avoid_: Start point, walk origin, route centroid

**Walk Presence Marker**:
A small glowing CyanGhost dot shown below the Heatmap Visibility Cutoff signalling that walks exist at a location, without revealing route detail.
_Avoid_: Pin, location marker, walk icon

**Presence Cluster**:
A Walk Presence Marker aggregating all Walk Anchors that fall into the same screen-space grid cell at the current zoom, always labelled with the number of walks it contains (including 1) and growing logarithmically in size with that count.
_Avoid_: City group, region bucket, marker stack

**Cluster Drill-Down**:
The animated zoom triggered by tapping a Presence Cluster that frames the combined bounds of its walks, never landing below the Heatmap Visibility Cutoff so the heatmap appears immediately.
_Avoid_: Marker click, expand cluster

**Lazy Cache Flush**:
A deferred invalidation process that wipes obsolete tile cache on demand upon screen entry rather than blocking application startup.
_Avoid_: Startup wipe, synchronous cold-boot purge

**Qualifying Walk**:
A recorded walk meeting the minimum validity threshold (at least 60 seconds in duration and at least 50 meters in distance), used for pace modeling and lifetime statistics to exclude accidental starts and micro-recordings.
_Avoid_: Valid walk, real walk, filtered walk

**Lifetime Stats**:
The cumulative all-time walking summary aggregated across all Qualifying Walks, reporting total distance, walk count, total duration, total steps, weighted average speed, and longest walk.
_Avoid_: Global telemetry, user totals, all-time summary

**Stats Peek Sheet**:
A non-modal bottom sheet on the Personal Heatmap screen that rests collapsed as a centered peek handle and expands via upward swipe or tap to display Lifetime Stats without dimming the map above.
_Avoid_: Stats drawer, metrics popup, analytics modal

**GPS Signal Score**:
A session-level 0–100% rating of GPS reception during a walk, weighted across active time spent in good, weak, and dead-reckoning (PDR) signal states, and summarized on the walk completion screen as Good, Fair, or Poor.
_Avoid_: Average accuracy, fix rate, GPS rating
