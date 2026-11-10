package com.paceghost.app.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.paceghost.app.domain.heatmap.HeatmapTileProvider
import com.paceghost.app.ui.heatmap.FakeWalkRepository
import com.paceghost.app.ui.heatmap.HeatmapViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class HeatmapCacheInvalidationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var heatmapPreferences: HeatmapPreferences
    private lateinit var testCacheDir: File

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        // Clear shared preferences before each test
        context.getSharedPreferences(HeatmapPreferences.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        heatmapPreferences = HeatmapPreferences(context)
        testCacheDir = tempFolder.newFolder("heatmap_tiles")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createDummyTile(fileName: String = "12/2048/1024.png"): File {
        val file = File(testCacheDir, fileName)
        file.parentFile?.mkdirs()
        file.writeText("dummy-raster-tile")
        assertTrue("Precondition: dummy tile file must exist", file.exists())
        return file
    }

    @Test
    fun `schema version constant is 2 in HeatmapPreferences and HeatmapTileProvider`() {
        assertEquals(2, HeatmapPreferences.HEATMAP_CACHE_SCHEMA_VERSION)
        assertEquals(2, HeatmapTileProvider.HEATMAP_CACHE_SCHEMA_VERSION)
    }

    @Test
    fun `purgeOutdatedCacheIfNeeded purges files and updates version to 2 when stored version is 0`() {
        val dummyTile = createDummyTile()
        assertEquals(0, heatmapPreferences.getCacheSchemaVersion())

        val purged = heatmapPreferences.purgeOutdatedCacheIfNeeded(testCacheDir)

        assertTrue("Cache should report purged", purged)
        assertFalse("Outdated tile should be deleted", dummyTile.exists())
        assertEquals(2, heatmapPreferences.getCacheSchemaVersion())
    }

    @Test
    fun `purgeOutdatedCacheIfNeeded purges files and updates version to 2 when stored version is 1`() {
        val dummyTile = createDummyTile()
        heatmapPreferences.setCacheSchemaVersion(1)
        assertEquals(1, heatmapPreferences.getCacheSchemaVersion())

        val purged = heatmapPreferences.purgeOutdatedCacheIfNeeded(testCacheDir)

        assertTrue("Cache should report purged", purged)
        assertFalse("Outdated tile should be deleted", dummyTile.exists())
        assertEquals(2, heatmapPreferences.getCacheSchemaVersion())
    }

    @Test
    fun `purgeOutdatedCacheIfNeeded is idempotent and does not purge files when version is already 2`() {
        heatmapPreferences.setCacheSchemaVersion(2)
        val dummyTile = createDummyTile()

        val purged = heatmapPreferences.purgeOutdatedCacheIfNeeded(testCacheDir)

        assertFalse("Cache should not be purged when version is already 2", purged)
        assertTrue("Tile should remain untouched", dummyTile.exists())
        assertEquals(2, heatmapPreferences.getCacheSchemaVersion())
    }

    @Test
    fun `HeatmapViewModel initialization purges outdated cache on startup`() = runTest(testDispatcher) {
        val dummyTile = createDummyTile()
        assertEquals(0, heatmapPreferences.getCacheSchemaVersion())

        val viewModel = HeatmapViewModel(
            application = ApplicationProvider.getApplicationContext(),
            repository = FakeWalkRepository(),
            heatmapPreferences = heatmapPreferences,
            heatmapCacheDir = testCacheDir,
            ioDispatcher = testDispatcher
        )

        advanceUntilIdle()

        assertFalse("Outdated tile should be purged by HeatmapViewModel init", dummyTile.exists())
        assertEquals(2, heatmapPreferences.getCacheSchemaVersion())
    }
}
