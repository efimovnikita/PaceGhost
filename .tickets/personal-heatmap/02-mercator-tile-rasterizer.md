# 02: Web Mercator Projection and Two-Pass Glow Tile Rasterizer

**What to build:** An offline tile generation engine that calculates Web Mercator tile boundaries `(z, x, y)`, projects GPS track segments onto a 256x256 pixel grid, and paints glowing neon trails using a two-pass technique (wide low-alpha aura + narrow saturated core) to output transparent PNG tiles formatted in the Cyber Neon Ramp color spectrum.

**Blocked by:** 01: Room Spatial Index and Bounding Box Query

**Status:** complete

- [x] Web Mercator coordinate conversion functions correctly map tile `(z, x, y)` to bounding box `[minLat, minLon, maxLat, maxLon]` with edge padding to prevent border clipping
- [x] GPS coordinates project accurately to tile pixel coordinates `(0..255, 0..255)`
- [x] Two-pass glow painter renders route segments with wide aura (`CyanGhost` tinted) and thin core (`NeonGreenRunner` tinted)
- [x] Multiple overlapping routes increase opacity and brightness via additive/alpha blending into white-hot neon
- [x] Unit tests verify tile coordinate math and ensure empty bounding boxes produce completely transparent tiles without allocations
