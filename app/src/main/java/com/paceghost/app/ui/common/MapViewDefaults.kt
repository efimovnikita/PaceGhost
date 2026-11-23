package com.paceghost.app.ui.common

import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView

/**
 * Applies PaceGhost's baseline osmdroid MapView configuration:
 * standard MAPNIK tiles, pinch-to-zoom multi-touch, DPI-scaled tiles,
 * disabled world repetition, and hidden built-in zoom buttons.
 */
fun MapView.applyPaceGhostDefaults() {
    setTileSource(TileSourceFactory.MAPNIK)
    setMultiTouchControls(true)
    setTilesScaledToDpi(true)
    isHorizontalMapRepetitionEnabled = false
    isVerticalMapRepetitionEnabled = false
    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
}
