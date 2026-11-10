# Inverted Base Map Color Filter for OLED Theme

To preserve the OLED Dark Theme across map views without depending on external proprietary dark map tile servers or API keys, we apply a runtime `ColorMatrixColorFilter` (invert luminance + high contrast + dark tint) to default OpenStreetMap Mapnik tiles.

## Considered Options

- **External Dark Tile Providers (CartoDB Dark Matter, Stadia, Mapbox) (Rejected)**: Introduces third-party network dependencies, API keys, usage limits, and requires maintaining two separate tile caches when the user already caches standard Mapnik tiles.
- **Pure Black Canvas Without Map Features (Rejected)**: Completely omits street geometry, rivers, and landmarks, making navigation and spatial orientation difficult for the runner.
- **Runtime ColorMatrix Filter over OSM Mapnik (Selected)**: Applies an inverted color matrix to Mapnik tiles directly inside Osmdroid. Operates completely offline, requires no keys, and seamlessly converts road networks into subtle dark grey contours beneath the neon heatmap overlay.

## Consequences

- Low GPU/Canvas overhead when drawing base tiles.
- Map labels remain visible as faint grey text rather than bright white.
