package com.paceghost.app.domain.heatmap

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.paceghost.app.data.local.AppDatabase
import com.paceghost.app.data.local.WalkDao
import com.paceghost.app.data.local.entity.TrackPointEntity
import com.paceghost.app.data.local.entity.WalkEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.osmdroid.util.MapTileIndex
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class HeatmapTileProviderTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var walkDao: WalkDao
    private lateinit var cacheDir: File
    private lateinit var tileModuleProvider: HeatmapTileModuleProvider

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        walkDao = database.walkDao()
        cacheDir = tempFolder.newFolder("heatmap_tiles")
        tileModuleProvider = HeatmapTileModuleProvider(
            context = context,
            walkDao = walkDao,
            rasterizer = HeatmapTileRasterizer(),
            cacheDir = cacheDir
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createPoint(
        walkId: Long,
        id: Long = 0,
        lat: Double,
        lon: Double
    ): TrackPointEntity = TrackPointEntity(
        id = id,
        walkId = walkId,
        latitude = lat,
        longitude = lon,
        altitude = 0.0,
        timestamp = 1000L + id * 1000,
        elapsedSeconds = id,
        cumulativeDistance = id * 10.0,
        speed = 1.4f,
        stepCountAtPoint = id.toInt()
    )

    @Test
    fun loadTile_noPointsInTile_returnsNullAndDoesNotWriteDiskCache() {
        // Tile at zoom 15 around Moscow Red Square (lat ~55.7539, lon ~37.6208)
        val zoom = 15
        val (tileX, tileY) = WebMercatorProjection.latLonToTile(55.7539, 37.6208, zoom)
        val tileIndex = MapTileIndex.getTileIndex(zoom, tileX, tileY)

        val tileLoader = tileModuleProvider.tileLoader
        val drawable = tileLoader.loadTile(tileIndex)

        assertNull("Empty tile must return null", drawable)
        val cachedFile = File(cacheDir, "$zoom/$tileX/$tileY.png")
        assertTrue("Disk cache file must not be created for empty tiles", !cachedFile.exists())
    }

    @Test
    fun loadTile_withPoints_rasterizesAndWritesDiskCacheAndReturnsDrawable() = runBlocking {
        val zoom = 15
        val (tileX, tileY) = WebMercatorProjection.latLonToTile(55.7539, 37.6208, zoom)
        val tileIndex = MapTileIndex.getTileIndex(zoom, tileX, tileY)

        // Insert walk and points inside this tile
        val walk = WalkEntity(
            title = "Test Walk",
            startTime = 1000L,
            endTime = 5000L,
            durationSeconds = 4L,
            distanceMeters = 200.0,
            stepCount = 200,
            averagePaceSecondsPerKm = 300.0
        )
        val walkId = walkDao.insertWalk(walk)

        val points = listOf(
            createPoint(walkId, 1, 55.7539, 37.6208),
            createPoint(walkId, 2, 55.7545, 37.6215),
            createPoint(walkId, 3, 55.7550, 37.6220)
        )
        walkDao.insertTrackPoints(points)

        val tileLoader = tileModuleProvider.tileLoader
        val drawable = tileLoader.loadTile(tileIndex)

        assertNotNull("Tile with route points must produce a non-null Drawable", drawable)
        assertTrue("Produced drawable must be a BitmapDrawable", drawable is BitmapDrawable)

        val cachedFile = File(cacheDir, "$zoom/$tileX/$tileY.png")
        assertTrue("Cached PNG file must exist on disk", cachedFile.exists())
        assertTrue("Cached PNG file size must be > 0", cachedFile.length() > 0)
    }

    @Test
    fun loadTile_cachedFileAlreadyExists_readsFromDiskWithoutDbQuery() = runBlocking {
        val zoom = 15
        val tileX = 19825
        val tileY = 10245
        val tileIndex = MapTileIndex.getTileIndex(zoom, tileX, tileY)

        // Pre-create disk cache file with a dummy bitmap
        val file = File(cacheDir, "$zoom/$tileX/$tileY.png")
        file.parentFile?.mkdirs()
        val dummyBitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        file.outputStream().use { fos ->
            dummyBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        assertTrue("Test file pre-created", file.exists())

        // Call tileLoader without any database records
        val tileLoader = tileModuleProvider.tileLoader
        val drawable = tileLoader.loadTile(tileIndex)

        assertNotNull("Should successfully load pre-cached tile from disk", drawable)
        assertTrue("Loaded drawable must be BitmapDrawable", drawable is BitmapDrawable)
        val bitmapDrawable = drawable as BitmapDrawable
        assertEquals(256, bitmapDrawable.bitmap.width)
        assertEquals(256, bitmapDrawable.bitmap.height)
    }

    private class TrackingWalkDao(private val delegate: WalkDao) : WalkDao by delegate {
        var getPointsInBoundingBoxCallCount: Int = 0
            private set

        override suspend fun getPointsInBoundingBox(
            minLat: Double,
            maxLat: Double,
            minLon: Double,
            maxLon: Double
        ): List<TrackPointEntity> {
            getPointsInBoundingBoxCallCount++
            return delegate.getPointsInBoundingBox(minLat, maxLat, minLon, maxLon)
        }
    }

    @Test
    fun minimumZoomLevel_isEnforcedToTen() {
        assertEquals(10, HeatmapTileProvider.MIN_HEATMAP_ZOOM)
        assertEquals(10, HeatmapTileModuleProvider.MIN_HEATMAP_ZOOM)
        assertEquals(10, tileModuleProvider.minimumZoomLevel)

        val provider = HeatmapTileProvider(
            context = context,
            walkDao = walkDao,
            rasterizer = HeatmapTileRasterizer(),
            cacheDir = cacheDir
        )
        assertEquals(10, provider.minimumZoomLevel)
        assertEquals(10, provider.tileSource.minimumZoomLevel)
    }

    @Test
    fun getOrRenderTile_belowMinimumZoom_returnsNullWithoutQueryingDao() = runBlocking {
        val trackingDao = TrackingWalkDao(walkDao)
        val moduleProvider = HeatmapTileModuleProvider(
            context = context,
            walkDao = trackingDao,
            rasterizer = HeatmapTileRasterizer(),
            cacheDir = cacheDir
        )

        val belowMinZooms = listOf(0, 5, 8, 9)
        for (zoom in belowMinZooms) {
            val result = moduleProvider.getOrRenderTile(zoom = zoom, tileX = 0, tileY = 0)
            assertNull("Tile rendered below MIN_HEATMAP_ZOOM ($zoom) must immediately return null", result)
        }

        // Verify tileLoader.loadTile below min zoom also returns null without DB access
        val tileIndexZoom9 = MapTileIndex.getTileIndex(9, 100, 100)
        val drawable = moduleProvider.tileLoader.loadTile(tileIndexZoom9)
        assertNull("loadTile at zoom 9 must return null", drawable)

        assertEquals(
            "SQLite database must not be queried for tiles with zoom < MIN_HEATMAP_ZOOM",
            0,
            trackingDao.getPointsInBoundingBoxCallCount
        )
    }

    @Test
    fun heatmapTileProvider_createsTilesOverlayCompatibleProvider() {
        val provider = HeatmapTileProvider(
            context = context,
            walkDao = walkDao,
            rasterizer = HeatmapTileRasterizer(),
            cacheDir = cacheDir
        )

        assertEquals("Heatmap", provider.tileSource.name())
        assertEquals(HeatmapTileProvider.MIN_HEATMAP_ZOOM, provider.minimumZoomLevel)
        assertEquals(HeatmapTileProvider.MIN_HEATMAP_ZOOM, provider.tileSource.minimumZoomLevel)
        assertEquals(20, provider.maximumZoomLevel)
        assertNotNull("Tile cache should be present", provider.tileCache)
    }
}
