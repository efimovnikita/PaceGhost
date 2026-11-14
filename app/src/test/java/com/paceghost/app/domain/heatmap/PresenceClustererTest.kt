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

    @Test
    fun drillDownTarget_singleShortWalk_clampsZoomTo16() {
        val a = WalkAnchor(1L, 55.7500, 37.6200, 55.7495, 55.7505, 37.6195, 37.6205)
        val cluster = PresenceClusterer.cluster(listOf(a), 3.0, cell).single()
        val target = PresenceClusterer.drillDownTarget(cluster, viewportWidthPx = 1080, viewportHeightPx = 1920, paddingPx = 96)
        assertEquals(16.0, target.targetZoom, 1e-6)
        assertEquals(55.7495, target.minLat, 1e-9)
        assertEquals(55.7505, target.maxLat, 1e-9)
        assertEquals(37.6195, target.minLon, 1e-9)
        assertEquals(37.6205, target.maxLon, 1e-9)
    }

    @Test
    fun drillDownTarget_continentSizedCluster_clampsZoomTo10() {
        val a = WalkAnchor(1L, 48.0, 2.0, 47.9, 48.1, 1.9, 2.1)
        val b = WalkAnchor(2L, 55.0, 37.0, 54.9, 55.1, 36.9, 37.1)
        val cluster = PresenceCluster(
            centerLat = 51.5,
            centerLon = 19.5,
            count = 2,
            walkIds = listOf(1L, 2L),
            minLat = a.minLat,
            maxLat = b.maxLat,
            minLon = a.minLon,
            maxLon = b.maxLon
        )
        val target = PresenceClusterer.drillDownTarget(cluster, viewportWidthPx = 1080, viewportHeightPx = 1920, paddingPx = 96)
        assertEquals(10.0, target.targetZoom, 1e-6)
        assertEquals(47.9, target.minLat, 1e-9)
        assertEquals(55.1, target.maxLat, 1e-9)
        assertEquals(1.9, target.minLon, 1e-9)
        assertEquals(37.1, target.maxLon, 1e-9)
    }

    @Test
    fun drillDownTarget_mediumDistrictCluster_returnsUnclampedIntermediateZoom() {
        val cluster = PresenceCluster(
            centerLat = 55.75,
            centerLon = 37.62,
            count = 3,
            walkIds = listOf(1L, 2L, 3L),
            minLat = 55.70,
            maxLat = 55.80,
            minLon = 37.55,
            maxLon = 37.70
        )
        val target = PresenceClusterer.drillDownTarget(cluster, viewportWidthPx = 1080, viewportHeightPx = 1920, paddingPx = 96)
        assertTrue("Expected zoom strictly between 10 and 16, got ${target.targetZoom}", target.targetZoom > 10.0 && target.targetZoom < 16.0)
    }
}
