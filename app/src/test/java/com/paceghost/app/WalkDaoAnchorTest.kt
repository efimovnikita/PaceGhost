package com.paceghost.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.paceghost.app.data.local.AppDatabase
import com.paceghost.app.data.local.WalkDao
import com.paceghost.app.data.local.entity.TrackPointEntity
import com.paceghost.app.data.local.entity.WalkEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class WalkDaoAnchorTest {

    private lateinit var database: AppDatabase
    private lateinit var walkDao: WalkDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        walkDao = database.walkDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun walk(id: Long) = WalkEntity(
        id = id, title = "Walk $id", startTime = id * 1000L, endTime = id * 1000L + 500L,
        durationSeconds = 500L, distanceMeters = 1000.0, stepCount = 1000,
        averagePaceSecondsPerKm = 600.0
    )

    private fun point(walkId: Long, lat: Double, lon: Double) = TrackPointEntity(
        walkId = walkId, latitude = lat, longitude = lon, timestamp = 1000L,
        elapsedSeconds = 10, cumulativeDistance = 100.0, speed = 1.4f, stepCountAtPoint = 50
    )

    @Test
    fun getWalkAnchors_returnsBboxCenterAndBounds_skippingWalksWithFewerThanTwoPoints() = runTest {
        listOf(1L, 2L, 3L).forEach { walkDao.insertWalk(walk(it)) }
        walkDao.insertTrackPoints(
            listOf(
                point(1, 55.0, 37.0), point(1, 56.0, 38.0), point(1, 55.2, 37.9),
                point(2, 10.0, 20.0), // single point -> skipped
                // walk 3 has none -> skipped
            )
        )

        val anchors = walkDao.getWalkAnchors()

        assertEquals(1, anchors.size)
        val a = anchors.single()
        assertEquals(1L, a.walkId)
        assertEquals(55.5, a.centerLat, 1e-9)
        assertEquals(37.5, a.centerLon, 1e-9)
        assertEquals(55.0, a.minLat, 1e-9)
        assertEquals(56.0, a.maxLat, 1e-9)
        assertEquals(37.0, a.minLon, 1e-9)
        assertEquals(38.0, a.maxLon, 1e-9)
    }

    @Test
    fun getWalkAnchors_emptyDatabase_returnsEmptyList() = runTest {
        assertTrue(walkDao.getWalkAnchors().isEmpty())
    }
}
