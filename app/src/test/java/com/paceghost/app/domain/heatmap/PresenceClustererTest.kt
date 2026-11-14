package com.paceghost.app.domain.heatmap

import com.paceghost.app.domain.model.WalkAnchor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PresenceClustererTest {

    private fun anchor(id: Long, lat: Double, lon: Double) =
        WalkAnchor(id, lat, lon, lat - 0.001, lat + 0.001, lon - 0.001, lon + 0.001)

    private val cell = 64f

    @Test
    fun closeWalks_lowZoom_mergeIntoOneCluster_highZoom_split() {
        val a = anchor(1, 50.0, 37.6)
        val b = anchor(2, 50.1, 37.8)
        val low = PresenceClusterer.cluster(listOf(a, b), 2.0, cell)
        assertEquals(1, low.size)
        assertEquals(2, low[0].count)
        assertEquals(setOf(1L, 2L), low[0].walkIds.toSet())
        val high = PresenceClusterer.cluster(listOf(a, b), 9.0, cell)
        assertEquals(2, high.size)
        assertTrue(high.all { it.count == 1 })
    }

    @Test
    fun distantCities_areDifferentClusters() {
        val moscow = anchor(1, 55.75, 37.62)
        val berlin = anchor(2, 52.52, 13.40)
        val result = PresenceClusterer.cluster(listOf(moscow, berlin), 3.0, cell)
        assertEquals(2, result.size)
    }

    @Test
    fun zoomAtOrAboveCutoff_returnsEmpty() {
        val a = anchor(1, 55.75, 37.62)
        assertTrue(PresenceClusterer.cluster(listOf(a), 10.0, cell).isEmpty())
        assertTrue(PresenceClusterer.cluster(listOf(a), 12.5, cell).isEmpty())
    }

    @Test
    fun emptyInput_returnsEmpty() {
        assertTrue(PresenceClusterer.cluster(emptyList(), 3.0, cell).isEmpty())
    }

    @Test
    fun clusterBounds_unionOfWalkBounds() {
        val a = anchor(1, 55.75, 37.60)
        val b = anchor(2, 55.76, 37.62)
        val c = PresenceClusterer.cluster(listOf(a, b), 3.0, cell).single()
        assertEquals(a.minLat, c.minLat, 1e-9)
        assertEquals(b.maxLat, c.maxLat, 1e-9)
        assertEquals(a.minLon, c.minLon, 1e-9)
        assertEquals(b.maxLon, c.maxLon, 1e-9)
    }

    @Test
    fun markerRadius_singleIsEight_monotonic_clampedAtFourteen() {
        assertEquals(8f, PresenceClusterer.markerRadiusDp(1), 1e-4f)
        var prev = 8f
        for (n in listOf(2, 5, 10, 50, 100, 1000)) {
            val r = PresenceClusterer.markerRadiusDp(n)
            assertTrue(r >= prev)
            assertTrue(r <= 14f)
            prev = r
        }
        assertEquals(14f, PresenceClusterer.markerRadiusDp(1_000_000), 1e-4f)
    }

    @Test
    fun badgeText_capsAt99Plus() {
        assertEquals("1", PresenceClusterer.badgeText(1))
        assertEquals("99", PresenceClusterer.badgeText(99))
        assertEquals("99+", PresenceClusterer.badgeText(100))
    }

    @Test
    fun cutoffSharedWithHeatmapTileProvider() {
        assertEquals(HeatmapTileProvider.MIN_HEATMAP_ZOOM.toDouble(), PresenceClusterer.CUTOFF_ZOOM, 0.0)
    }
}
