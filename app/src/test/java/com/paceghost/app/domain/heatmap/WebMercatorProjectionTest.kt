package com.paceghost.app.domain.heatmap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class WebMercatorProjectionTest {

    @Test
    fun tileToBounds_zoom0_coversWorld() {
        val bounds = WebMercatorProjection.tileToBounds(zoom = 0, tileX = 0, tileY = 0)
        assertEquals(-180.0, bounds.minLon, 1e-4)
        assertEquals(180.0, bounds.maxLon, 1e-4)
        assertEquals(-85.051128, bounds.minLat, 1e-4)
        assertEquals(85.051128, bounds.maxLat, 1e-4)
    }

    @Test
    fun tileToBounds_withPadding_expandsBounds() {
        val standardBounds = WebMercatorProjection.tileToBounds(zoom = 10, tileX = 500, tileY = 300, bufferFraction = 0.0)
        val paddedBounds = WebMercatorProjection.tileToBounds(zoom = 10, tileX = 500, tileY = 300, bufferFraction = 0.1)

        assertTrue(paddedBounds.minLat < standardBounds.minLat)
        assertTrue(paddedBounds.maxLat > standardBounds.maxLat)
        assertTrue(paddedBounds.minLon < standardBounds.minLon)
        assertTrue(paddedBounds.maxLon > standardBounds.maxLon)
    }

    @Test
    fun projectToTilePixel_cornersMatchTileBoundaries() {
        val bounds = WebMercatorProjection.tileToBounds(zoom = 14, tileX = 9912, tileY = 5122)

        // North-West corner should project to (0, 0)
        val (nwX, nwY) = WebMercatorProjection.projectToTilePixel(
            lat = bounds.maxLat,
            lon = bounds.minLon,
            zoom = 14,
            tileX = 9912,
            tileY = 5122,
            tileSize = 256
        )
        assertEquals(0f, nwX, 0.1f)
        assertEquals(0f, nwY, 0.1f)

        // South-East corner should project to (256, 256)
        val (seX, seY) = WebMercatorProjection.projectToTilePixel(
            lat = bounds.minLat,
            lon = bounds.maxLon,
            zoom = 14,
            tileX = 9912,
            tileY = 5122,
            tileSize = 256
        )
        assertEquals(256f, seX, 0.1f)
        assertEquals(256f, seY, 0.1f)
    }

    @Test
    fun latLonToTile_mapsCenterOfMoscow() {
        // Red Square, Moscow: 55.7525° N, 37.6231° E
        val (tileX, tileY) = WebMercatorProjection.latLonToTile(
            lat = 55.7525,
            lon = 37.6231,
            zoom = 14
        )
        // Zoom 14 for Moscow Red Square is tileX = 9904, tileY = 5122
        val bounds = WebMercatorProjection.tileToBounds(zoom = 14, tileX = tileX, tileY = tileY)
        assertTrue("Latitude should be within bounds", 55.7525 in bounds.minLat..bounds.maxLat)
        assertTrue("Longitude should be within bounds", 37.6231 in bounds.minLon..bounds.maxLon)
    }

    @Test
    fun extremeLatitudes_clampedGracefully() {
        val (tileXHigh, tileYHigh) = WebMercatorProjection.latLonToTile(lat = 89.9, lon = 0.0, zoom = 5)
        assertEquals(0, tileYHigh)

        val (tileXLow, tileYLow) = WebMercatorProjection.latLonToTile(lat = -89.9, lon = 0.0, zoom = 5)
        assertEquals(31, tileYLow)

        val (px, py) = WebMercatorProjection.projectToTilePixel(lat = 90.0, lon = 0.0, zoom = 0, tileX = 0, tileY = 0)
        assertTrue(!px.isNaN() && !px.isInfinite())
        assertTrue(!py.isNaN() && !py.isInfinite())
    }
}
