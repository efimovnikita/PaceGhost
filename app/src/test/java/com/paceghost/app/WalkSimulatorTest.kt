package com.paceghost.app

import com.paceghost.app.domain.engine.GeoMath
import com.paceghost.app.domain.engine.GhostEngine
import com.paceghost.app.domain.model.GhostStatus
import com.paceghost.app.domain.simulator.WalkSimulator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WalkSimulatorTest {

    @Test
    fun `walk simulator advances distance and steps on tick`() {
        val sim = WalkSimulator()
        sim.setSpeedKmh(5.0) // 5 km/h ~ 1.3889 m/s, ~1.9167 steps/s

        // 60 seconds tick
        val pt = sim.tick(60.0)

        // At 5 km/h for 60s, distance ~ 83.33 meters
        assertEquals(83.33, pt.cumulativeDistance, 2.0)
        assertEquals(60L, pt.elapsedSeconds)
        // Steps at 115 steps/min for 1 min ~ 115 steps
        assertTrue("Steps should be ~115, was ${pt.stepCount}", pt.stepCount in 113..117)
    }

    @Test
    fun `setting speed to zero stops progression`() {
        val sim = WalkSimulator()
        sim.tick(10.0)
        val distBefore = sim.tick(1.0).cumulativeDistance

        sim.setSpeedKmh(0.0)
        val ptStopped = sim.tick(10.0)

        assertEquals(distBefore, ptStopped.cumulativeDistance, 0.001)
        assertEquals(0f, ptStopped.speed, 0.001f)
    }

    @Test
    fun `setting offRoute shifts coordinates by more than 60m`() {
        val sim = WalkSimulator()
        val refTrack = sim.getReferenceTrack()
        val ghostEngine = GhostEngine(refTrack)

        // On route
        val onRoutePt = sim.tick(10.0)
        val r1 = ghostEngine.compare(onRoutePt)
        assertEquals(GhostStatus.ON_PACE, r1.status)

        // Off route
        sim.setOffRoute(true)
        val offRoutePt = sim.tick(1.0)
        val r2 = ghostEngine.compare(offRoutePt)
        assertEquals(GhostStatus.OFF_ROUTE, r2.status)
    }
}
