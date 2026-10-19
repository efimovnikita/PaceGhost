package com.paceghost.app

import com.paceghost.app.domain.engine.GeoMath
import com.paceghost.app.domain.model.TrackPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoMathTest {

    @Test
    fun `haversineDistance between same coordinates is zero`() {
        val dist = GeoMath.haversineDistance(55.75, 37.61, 55.75, 37.61)
        assertEquals(0.0, dist, 0.001)
    }

    @Test
    fun `haversineDistance calculation on equator matches expected 1 degree distance`() {
        // 1 degree longitude on equator ~ 111.195 km = 111195 m
        val dist = GeoMath.haversineDistance(0.0, 0.0, 0.0, 1.0)
        assertEquals(111195.0, dist, 50.0)
    }

    @Test
    fun `haversineDistance between Moscow landmarks`() {
        // Red Square (55.7525, 37.6231) to Bolshoi Theatre (55.7602, 37.6186) ~ 900m
        val dist = GeoMath.haversineDistance(55.7525, 37.6231, 55.7602, 37.6186)
        assertTrue("Distance should be around 900m, was $dist", dist in 850.0..950.0)
    }

    @Test
    fun `projectPointOnSegment for point on line returns zero cross track distance`() {
        val start = TrackPoint(lat = 55.730, lon = 37.500, elapsedSeconds = 0, cumulativeDistance = 0.0)
        val end = TrackPoint(lat = 55.732, lon = 37.500, elapsedSeconds = 100, cumulativeDistance = 222.0)
        val probe = TrackPoint(lat = 55.731, lon = 37.500, elapsedSeconds = 50)

        val (proj, crossDist) = GeoMath.projectPointOnSegment(probe, start, end)

        assertEquals(0.0, crossDist, 1.0)
        assertEquals(55.731, proj.lat, 0.0001)
        assertEquals(37.500, proj.lon, 0.0001)
        assertEquals(50L, proj.elapsedSeconds)
        assertEquals(111.0, proj.cumulativeDistance, 5.0)
    }

    @Test
    fun `projectPointOnSegment for perpendicular offset measures cross track distance`() {
        val start = TrackPoint(lat = 55.7300, lon = 37.5000)
        val end = TrackPoint(lat = 55.7320, lon = 37.5000)
        // Offset ~30 meters east (+0.00048 lon at 55.73 lat is ~30m)
        val probe = TrackPoint(lat = 55.7310, lon = 37.5005)

        val (proj, crossDist) = GeoMath.projectPointOnSegment(probe, start, end)

        assertEquals(55.7310, proj.lat, 0.0001)
        assertEquals(37.5000, proj.lon, 0.0001)
        assertTrue("Cross track distance should be ~31 meters, was $crossDist", crossDist in 25.0..36.0)
    }

    @Test
    fun `projectPointOnSegment clamps before start and after end`() {
        val start = TrackPoint(lat = 55.7300, lon = 37.5000)
        val end = TrackPoint(lat = 55.7320, lon = 37.5000)

        // Probe before start
        val probeBefore = TrackPoint(lat = 55.7280, lon = 37.5000)
        val (projBefore, _) = GeoMath.projectPointOnSegment(probeBefore, start, end)
        assertEquals(start.lat, projBefore.lat, 0.00001)

        // Probe after end
        val probeAfter = TrackPoint(lat = 55.7350, lon = 37.5000)
        val (projAfter, _) = GeoMath.projectPointOnSegment(probeAfter, start, end)
        assertEquals(end.lat, projAfter.lat, 0.00001)
    }

    @Test
    fun `computeCumulativeDistances accumulates sequential distances`() {
        val p1 = TrackPoint(lat = 55.7300, lon = 37.5000)
        val p2 = TrackPoint(lat = 55.7310, lon = 37.5000)
        val p3 = TrackPoint(lat = 55.7320, lon = 37.5000)

        val result = GeoMath.computeCumulativeDistances(listOf(p1, p2, p3))

        assertEquals(3, result.size)
        assertEquals(0.0, result[0].cumulativeDistance, 0.001)
        val d1 = GeoMath.haversineDistance(p1, p2)
        val d2 = GeoMath.haversineDistance(p2, p3)
        assertEquals(d1, result[1].cumulativeDistance, 0.001)
        assertEquals(d1 + d2, result[2].cumulativeDistance, 0.001)
    }
}
