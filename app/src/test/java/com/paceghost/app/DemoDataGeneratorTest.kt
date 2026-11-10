package com.paceghost.app

import com.paceghost.app.data.local.DemoDataGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoDataGeneratorTest {

    @Test
    fun `createDemoWalkData and createSecondDemoWalkData generate overlapping demo routes`() {
        val (walk1, points1) = DemoDataGenerator.createDemoWalkData(walkId = 1L)
        val (walk2, points2) = DemoDataGenerator.createSecondDemoWalkData(walkId = 2L)

        assertEquals("Victory Park Demo Track", walk1.title)
        assertEquals("Victory Park Avenue Track", walk2.title)
        assertEquals(100, points1.size)
        assertEquals(100, points2.size)
        assertTrue(walk1.distanceMeters > 500.0)
        assertTrue(walk2.distanceMeters > 500.0)

        // Check overlapping coordinates: both walks visit the northern apex (lat ~ 55.73275, lon ~ 37.5050)
        val hasNorthernApex1 = points1.any { it.latitude > 55.7325 }
        val hasNorthernApex2 = points2.any { it.latitude > 55.7325 }
        assertTrue("Walk 1 should reach northern loop", hasNorthernApex1)
        assertTrue("Walk 2 should reach northern loop (overlap)", hasNorthernApex2)

        // Check center intersection: walk2 traverses center latitude 55.7310 and longitude 37.5050
        val traversesCenter = points2.any { Math.abs(it.latitude - DemoDataGenerator.CENTER_LAT) < 0.0001 && Math.abs(it.longitude - DemoDataGenerator.CENTER_LON) < 0.0005 }
        assertTrue("Walk 2 should traverse center alley", traversesCenter)
    }
}
