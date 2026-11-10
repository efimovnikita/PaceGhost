package com.paceghost.app.domain.heatmap

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sinh
import kotlin.math.tan

/**
 * Standard Web Mercator (EPSG:3857) projection calculations for mapping GPS coordinates
 * to Slippy Map tiles and pixel coordinates.
 */
object WebMercatorProjection {

    const val MAX_LATITUDE = 85.0511287798066
    const val MIN_LATITUDE = -85.0511287798066
    const val DEFAULT_TILE_SIZE = 256

    /**
     * Geographic bounding box in WGS84 degrees.
     */
    data class MercatorBoundingBox(
        val minLat: Double,
        val maxLat: Double,
        val minLon: Double,
        val maxLon: Double
    )

    /**
     * Converts Slippy Map tile coordinates `(zoom, tileX, tileY)` to a geographic bounding box.
     *
     * @param bufferFraction fractional expansion of the tile boundary (e.g. 0.10 for 10% edge padding)
     *        to prevent visual seam clipping across tile borders.
     */
    fun tileToBounds(
        zoom: Int,
        tileX: Int,
        tileY: Int,
        bufferFraction: Double = 0.0
    ): MercatorBoundingBox {
        val n = 2.0.pow(zoom)
        val xMin = tileX.toDouble() - bufferFraction
        val xMax = tileX.toDouble() + 1.0 + bufferFraction
        val yMin = tileY.toDouble() - bufferFraction
        val yMax = tileY.toDouble() + 1.0 + bufferFraction

        val minLon = (xMin / n * 360.0 - 180.0).coerceIn(-180.0, 180.0)
        val maxLon = (xMax / n * 360.0 - 180.0).coerceIn(-180.0, 180.0)

        val latNorth = Math.toDegrees(atan(sinh(PI * (1.0 - 2.0 * yMin / n)))).coerceIn(MIN_LATITUDE, MAX_LATITUDE)
        val latSouth = Math.toDegrees(atan(sinh(PI * (1.0 - 2.0 * yMax / n)))).coerceIn(MIN_LATITUDE, MAX_LATITUDE)

        return MercatorBoundingBox(
            minLat = minOf(latNorth, latSouth),
            maxLat = maxOf(latNorth, latSouth),
            minLon = minOf(minLon, maxLon),
            maxLon = maxOf(minLon, maxLon)
        )
    }

    /**
     * Projects a GPS coordinate (latitude, longitude) to pixel coordinates within a tile.
     * Note: Returned pixels can lie outside `[0, tileSize]` if the point falls within the buffer zone.
     */
    fun projectToTilePixel(
        lat: Double,
        lon: Double,
        zoom: Int,
        tileX: Int,
        tileY: Int,
        tileSize: Int = DEFAULT_TILE_SIZE
    ): Pair<Float, Float> {
        val n = 2.0.pow(zoom)
        val clampedLat = lat.coerceIn(MIN_LATITUDE, MAX_LATITUDE)
        val clampedLon = lon.coerceIn(-180.0, 180.0)

        val x = ((clampedLon + 180.0) / 360.0) * n
        val latRad = Math.toRadians(clampedLat)
        val y = (1.0 - ln(tan(PI / 4.0 + latRad / 2.0)) / PI) / 2.0 * n

        val px = ((x - tileX) * tileSize).toFloat()
        val py = ((y - tileY) * tileSize).toFloat()

        return Pair(px, py)
    }

    /**
     * Converts a GPS coordinate (latitude, longitude) to the enclosing tile index `(tileX, tileY)` at [zoom].
     */
    fun latLonToTile(
        lat: Double,
        lon: Double,
        zoom: Int
    ): Pair<Int, Int> {
        val n = 2.0.pow(zoom)
        val maxTileIndex = (n - 1.0).toInt().coerceAtLeast(0)

        val clampedLat = lat.coerceIn(MIN_LATITUDE, MAX_LATITUDE)
        val clampedLon = lon.coerceIn(-180.0, 180.0)

        val x = (((clampedLon + 180.0) / 360.0) * n).toInt().coerceIn(0, maxTileIndex)
        val latRad = Math.toRadians(clampedLat)
        val y = (((1.0 - ln(tan(PI / 4.0 + latRad / 2.0)) / PI) / 2.0) * n).toInt().coerceIn(0, maxTileIndex)

        return Pair(x, y)
    }
}
