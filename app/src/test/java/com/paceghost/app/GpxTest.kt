package com.paceghost.app

import com.paceghost.app.data.gpx.GpxExporter
import com.paceghost.app.data.gpx.GpxImporter
import com.paceghost.app.data.local.DemoDataGenerator
import com.paceghost.app.data.local.entity.TrackPointEntity
import com.paceghost.app.data.local.entity.WalkEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GpxTest {

    @Test
    fun `export and import GPX roundtrip preserves walk metadata and points`() {
        val (originalWalk, originalPoints) = DemoDataGenerator.createDemoWalkData(
            walkId = 1L,
            startTimeMs = 1729340000000L
        )

        // 1. Export to GPX XML
        val gpxXml = GpxExporter.exportToGpx(originalWalk, originalPoints)
        assertNotNull(gpxXml)
        assertTrue(gpxXml.contains("<gpx version=\"1.1\""))
        assertTrue(gpxXml.contains(originalWalk.title))
        assertTrue(gpxXml.contains("<trkpt"))

        // 2. Import back
        val (importedWalk, importedPoints) = GpxImporter.importFromGpx(gpxXml)

        // 3. Verify walk entity
        assertEquals(originalWalk.title, importedWalk.title)
        assertEquals(originalPoints.size, importedPoints.size)
        assertEquals(originalWalk.durationSeconds, importedWalk.durationSeconds)
        assertEquals(originalWalk.distanceMeters, importedWalk.distanceMeters, 5.0)
        assertEquals(originalWalk.stepCount, importedWalk.stepCount)

        // 4. Verify points fidelity
        for (i in originalPoints.indices) {
            val orig = originalPoints[i]
            val imp = importedPoints[i]

            assertEquals("Lat mismatch at $i", orig.latitude, imp.latitude, 0.00001)
            assertEquals("Lon mismatch at $i", orig.longitude, imp.longitude, 0.00001)
            assertEquals("Alt mismatch at $i", orig.altitude, imp.altitude, 0.2)
            assertEquals("Timestamp mismatch at $i", orig.timestamp, imp.timestamp)
            assertEquals("ElapsedSec mismatch at $i", orig.elapsedSeconds, imp.elapsedSeconds)
            assertEquals("CumulativeDist mismatch at $i", orig.cumulativeDistance, imp.cumulativeDistance, 2.0)
            assertEquals("StepCount mismatch at $i", orig.stepCountAtPoint, imp.stepCountAtPoint)
        }
    }

    @Test
    fun `export and import handles special XML characters in title`() {
        val walk = WalkEntity(
            id = 2L,
            title = "Walk & Pace <Test> \"Quotes\" 'Apos'",
            startTime = 1729340000000L,
            endTime = 1729340100000L,
            durationSeconds = 100L,
            distanceMeters = 200.0,
            stepCount = 250,
            averagePaceSecondsPerKm = 500.0
        )
        val points = listOf(
            TrackPointEntity(
                id = 1,
                walkId = 2,
                latitude = 55.731,
                longitude = 37.505,
                altitude = 150.0,
                timestamp = 1729340000000L,
                elapsedSeconds = 0,
                cumulativeDistance = 0.0,
                speed = 2.0f,
                stepCountAtPoint = 0
            ),
            TrackPointEntity(
                id = 2,
                walkId = 2,
                latitude = 55.732,
                longitude = 37.505,
                altitude = 150.0,
                timestamp = 1729340100000L,
                elapsedSeconds = 100,
                cumulativeDistance = 111.0,
                speed = 1.11f,
                stepCountAtPoint = 250
            )
        )

        val xml = GpxExporter.exportToGpx(walk, points)
        assertTrue(xml.contains("&amp;"))
        assertTrue(xml.contains("&lt;"))
        assertTrue(xml.contains("&gt;"))

        val (importedWalk, importedPoints) = GpxImporter.importFromGpx(xml)
        assertEquals(walk.title, importedWalk.title)
        assertEquals(2, importedPoints.size)
    }
}
