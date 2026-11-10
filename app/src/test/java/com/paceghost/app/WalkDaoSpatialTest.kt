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
class WalkDaoSpatialTest {

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

    private fun createTrackPoint(
        walkId: Long,
        id: Long = 0,
        lat: Double,
        lon: Double,
        timestamp: Long = 1000L
    ): TrackPointEntity {
        return TrackPointEntity(
            id = id,
            walkId = walkId,
            latitude = lat,
            longitude = lon,
            timestamp = timestamp,
            elapsedSeconds = 10,
            cumulativeDistance = 100.0,
            speed = 1.4f,
            stepCountAtPoint = 50
        )
    }

    @Test
    fun getPointsInBoundingBox_filtersOutPointsOutsideBoundingBox_andSortsByWalkIdAndIdAsc() = runTest {
        // Arrange
        val walk1 = WalkEntity(
            id = 1,
            title = "Walk 1",
            startTime = 1000L,
            endTime = 2000L,
            durationSeconds = 1000L,
            distanceMeters = 1000.0,
            stepCount = 1000,
            averagePaceSecondsPerKm = 600.0
        )
        val walk2 = WalkEntity(
            id = 2,
            title = "Walk 2",
            startTime = 3000L,
            endTime = 4000L,
            durationSeconds = 1000L,
            distanceMeters = 1000.0,
            stepCount = 1000,
            averagePaceSecondsPerKm = 600.0
        )
        walkDao.insertWalk(walk1)
        walkDao.insertWalk(walk2)

        // Bounding box: lat in [55.0, 56.0], lon in [37.0, 38.0]
        val points = listOf(
            // Walk 1 points
            createTrackPoint(walkId = 1, id = 1, lat = 55.1, lon = 37.1), // INSIDE
            createTrackPoint(walkId = 1, id = 2, lat = 54.9, lon = 37.5), // OUTSIDE (lat too low)
            createTrackPoint(walkId = 1, id = 3, lat = 55.5, lon = 38.1), // OUTSIDE (lon too high)
            createTrackPoint(walkId = 1, id = 4, lat = 55.8, lon = 37.9), // INSIDE

            // Walk 2 points
            createTrackPoint(walkId = 2, id = 5, lat = 55.2, lon = 37.2), // INSIDE
            createTrackPoint(walkId = 2, id = 6, lat = 56.5, lon = 37.5), // OUTSIDE (lat too high)
            createTrackPoint(walkId = 2, id = 7, lat = 55.3, lon = 36.9), // OUTSIDE (lon too low)
            createTrackPoint(walkId = 2, id = 8, lat = 55.9, lon = 37.8)  // INSIDE
        )
        walkDao.insertTrackPoints(points)

        // Act
        val result = walkDao.getPointsInBoundingBox(
            minLat = 55.0,
            maxLat = 56.0,
            minLon = 37.0,
            maxLon = 38.0
        )

        // Assert
        assertEquals(4, result.size)
        // Check order: walkId 1 (id 1, id 4), then walkId 2 (id 5, id 8)
        assertEquals(1L, result[0].walkId)
        assertEquals(1L, result[0].id)

        assertEquals(1L, result[1].walkId)
        assertEquals(4L, result[1].id)

        assertEquals(2L, result[2].walkId)
        assertEquals(5L, result[2].id)

        assertEquals(2L, result[3].walkId)
        assertEquals(8L, result[3].id)
    }

    @Test
    fun getPointsInBoundingBox_includesPointsOnExactBoundaries() = runTest {
        val walk = WalkEntity(
            id = 10,
            title = "Boundary Walk",
            startTime = 1000L,
            endTime = 2000L,
            durationSeconds = 1000L,
            distanceMeters = 1000.0,
            stepCount = 1000,
            averagePaceSecondsPerKm = 600.0
        )
        walkDao.insertWalk(walk)

        val points = listOf(
            createTrackPoint(walkId = 10, id = 101, lat = 50.0, lon = 30.0), // minLat, minLon
            createTrackPoint(walkId = 10, id = 102, lat = 60.0, lon = 40.0), // maxLat, maxLon
            createTrackPoint(walkId = 10, id = 103, lat = 50.0, lon = 40.0), // minLat, maxLon
            createTrackPoint(walkId = 10, id = 104, lat = 60.0, lon = 30.0), // maxLat, minLon
            createTrackPoint(walkId = 10, id = 105, lat = 49.99999, lon = 35.0), // Just outside
            createTrackPoint(walkId = 10, id = 106, lat = 60.00001, lon = 35.0)  // Just outside
        )
        walkDao.insertTrackPoints(points)

        val result = walkDao.getPointsInBoundingBox(50.0, 60.0, 30.0, 40.0)

        assertEquals(4, result.size)
        val ids = result.map { it.id }
        assertEquals(listOf(101L, 102L, 103L, 104L), ids)
    }

    @Test
    fun databaseSchema_hasCompositeSpatialIndexOnLatitudeAndLongitude() {
        val cursor = database.openHelper.readableDatabase.query("PRAGMA index_list('track_points')")
        var foundIndex = false
        while (cursor.moveToNext()) {
            val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
            if (name == "index_track_points_latitude_longitude") {
                foundIndex = true
                break
            }
        }
        cursor.close()
        assertTrue("Index index_track_points_latitude_longitude must exist on track_points table", foundIndex)
    }

    @Test
    fun migration1To2_executesCleanly() {
        val db = database.openHelper.writableDatabase
        AppDatabase.MIGRATION_1_2.migrate(db)
        // Verify migration doesn't throw and index is valid
        val cursor = db.query("PRAGMA index_info('index_track_points_latitude_longitude')")
        val indexedColumns = mutableListOf<String>()
        while (cursor.moveToNext()) {
            indexedColumns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
        }
        cursor.close()
        assertEquals(listOf("latitude", "longitude"), indexedColumns)
    }
}
