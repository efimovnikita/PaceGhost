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
The minimum zoom threshold (level 10) below which the personal heatmap tile layer stops rendering, preventing resource-heavy database queries and preserving full map exploration without visual clutter.
_Avoid_: Hard map clamp, unbounded layer rendering

**Lazy Cache Flush**:
A deferred invalidation process that wipes obsolete tile cache on demand upon screen entry rather than blocking application startup.
_Avoid_: Startup wipe, synchronous cold-boot purge
