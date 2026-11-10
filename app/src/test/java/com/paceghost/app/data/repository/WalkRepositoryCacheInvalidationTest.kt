package com.paceghost.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.paceghost.app.data.local.AppDatabase
import com.paceghost.app.data.local.WalkDao
import com.paceghost.app.data.local.entity.TrackPointEntity
import com.paceghost.app.data.local.entity.WalkEntity
import com.paceghost.app.domain.heatmap.HeatmapTileModuleProvider
import com.paceghost.app.domain.heatmap.HeatmapTileProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class WalkRepositoryCacheInvalidationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var walkDao: WalkDao
    private lateinit var testCacheDir: File
    private lateinit var repository: WalkRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        walkDao = database.walkDao()
        testCacheDir = tempFolder.newFolder("heatmap_tiles")

        repository = WalkRepositoryImpl(
            walkDao = walkDao,
            heatmapCacheDir = testCacheDir
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun populateDummyTileFile(fileName: String = "14/9600/5100.png"): File {
        val file = File(testCacheDir, fileName)
        file.parentFile?.mkdirs()
        file.writeText("dummy-tile-data")
        assertTrue("Precondition: dummy tile must exist", file.exists())
        return file
    }

    private fun sampleWalk(id: Long = 0L, title: String = "Test Walk") = WalkEntity(
        id = id,
        title = title,
        startTime = 1000L,
        endTime = 2000L,
        durationSeconds = 1000L,
        distanceMeters = 500.0,
        stepCount = 600,
        averagePaceSecondsPerKm = 600.0
    )

    private fun samplePoints(walkId: Long = 0L) = listOf(
        TrackPointEntity(
            id = 0L,
            walkId = walkId,
            latitude = 55.75,
            longitude = 37.61,
            timestamp = 1000L,
            elapsedSeconds = 0L,
            cumulativeDistance = 0.0,
            speed = 1.4f,
            stepCountAtPoint = 0
        ),
        TrackPointEntity(
            id = 0L,
            walkId = walkId,
            latitude = 55.76,
            longitude = 37.62,
            timestamp = 2000L,
            elapsedSeconds = 1000L,
            cumulativeDistance = 500.0,
            speed = 1.4f,
            stepCountAtPoint = 600
        )
    )

    @Test
    fun heatmapCacheDir_isLocalizedInContextCacheDirHeatmapTiles() {
        val expected = context.cacheDir.resolve("heatmap_tiles")
        assertEquals(expected, HeatmapTileProvider.getCacheDir(context))
    }

    @Test
    fun saveWalk_clearsHeatmapCacheDirectory() = runTest {
        val tileFile = populateDummyTileFile()

        repository.saveWalk(sampleWalk(), samplePoints())

        assertFalse("Tile file must be deleted after saveWalk", tileFile.exists())
    }

    @Test
    fun insertWalk_clearsHeatmapCacheDirectory() = runTest {
        val tileFile = populateDummyTileFile()

        repository.insertWalk(sampleWalk(), samplePoints())

        assertFalse("Tile file must be deleted after insertWalk", tileFile.exists())
    }

    @Test
    fun deleteWalk_clearsHeatmapCacheDirectory() = runTest {
        val walkId = repository.saveWalk(sampleWalk(), samplePoints())
        val tileFile = populateDummyTileFile()

        repository.deleteWalk(walkId)

        assertFalse("Tile file must be deleted after deleteWalk", tileFile.exists())
    }

    @Test
    fun clearAllWalks_purgesDatabaseAndClearsHeatmapCache() = runTest {
        repository.saveWalk(sampleWalk(id = 1), samplePoints())
        repository.saveWalk(sampleWalk(id = 2), samplePoints())
        assertEquals(2, repository.getAllWalks().first().size)

        val tileFile = populateDummyTileFile()

        repository.clearAllWalks()

        assertEquals(0, repository.getAllWalks().first().size)
        assertFalse("Tile file must be deleted after clearAllWalks", tileFile.exists())
    }

    @Test
    fun importWalkFromGpx_clearsHeatmapCacheDirectory() = runTest {
        val tileFile = populateDummyTileFile()

        val sampleGpx = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gpx version="1.1" creator="PaceGhost">
              <trk>
                <name>Morning Run</name>
                <trkseg>
                  <trkpt lat="55.751244" lon="37.618423">
                    <ele>150.0</ele>
                    <time>2026-11-10T08:00:00Z</time>
                  </trkpt>
                  <trkpt lat="55.752244" lon="37.619423">
                    <ele>151.0</ele>
                    <time>2026-11-10T08:05:00Z</time>
                  </trkpt>
                </trkseg>
              </trk>
            </gpx>
        """.trimIndent()

        val importedId = repository.importWalkFromGpx(sampleGpx, "Imported Morning Run")
        assertTrue(importedId > 0)
        assertFalse("Tile file must be deleted after GPX import", tileFile.exists())
    }

    @Test
    fun clearHeatmapCache_handlesNonExistentDirectoryGracefully() {
        val nonExistentDir = File(tempFolder.root, "non_existent_cache")
        val customRepo = WalkRepositoryImpl(walkDao, nonExistentDir)

        // Should not throw
        customRepo.clearHeatmapCache()
        assertFalse(nonExistentDir.exists())
    }

    @Test
    fun subsequentHeatmapView_reRasterizesTilesCleanlyOnDemandAfterCacheInvalidation() = runTest {
        val zoom = 15
        val (tileX, tileY) = com.paceghost.app.domain.heatmap.WebMercatorProjection.latLonToTile(
            55.7539,
            37.6208,
            zoom
        )

        val points = listOf(
            TrackPointEntity(
                id = 0L,
                walkId = 0L,
                latitude = 55.7539,
                longitude = 37.6208,
                timestamp = 1000L,
                elapsedSeconds = 0L,
                cumulativeDistance = 0.0,
                speed = 1.4f,
                stepCountAtPoint = 0
            ),
            TrackPointEntity(
                id = 0L,
                walkId = 0L,
                latitude = 55.7545,
                longitude = 37.6215,
                timestamp = 2000L,
                elapsedSeconds = 1000L,
                cumulativeDistance = 100.0,
                speed = 1.4f,
                stepCountAtPoint = 150
            )
        )

        // 1. Initial walk saved
        val walkId = repository.saveWalk(sampleWalk(), points)

        // 2. Render tile into cache directory
        val moduleProvider = HeatmapTileModuleProvider(
            context = context,
            walkDao = walkDao,
            cacheDir = testCacheDir
        )

        val tile = moduleProvider.getOrRenderTile(zoom, tileX, tileY)
        assertNotNull("Tile should be rendered", tile)

        val cachedFile = File(testCacheDir, "$zoom/$tileX/$tileY.png")
        assertTrue("Cached tile file should exist on disk", cachedFile.exists())

        // 3. Invalidate cache via repository (e.g. user deleted walk)
        repository.deleteWalk(walkId)
        assertFalse("Cache file must be purged after invalidation", cachedFile.exists())

        // 4. Subsequent view to Heatmap requests tile again
        val reRasterizedTile = moduleProvider.getOrRenderTile(zoom, tileX, tileY)
        // Database is now empty of points, so getOrRenderTile returns null
        // Verify disk cache remains clean without resurrecting deleted route
        assertFalse("Purged tile should not resurrect stale route", cachedFile.exists())
    }
}
