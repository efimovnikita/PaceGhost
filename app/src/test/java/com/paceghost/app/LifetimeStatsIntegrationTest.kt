package com.paceghost.app

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.paceghost.app.data.local.AppDatabase
import com.paceghost.app.data.local.HeatmapPreferences
import com.paceghost.app.data.local.WalkDao
import com.paceghost.app.data.local.entity.WalkEntity
import com.paceghost.app.data.repository.WalkRepositoryImpl
import com.paceghost.app.domain.model.LifetimeStats
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class LifetimeStatsIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var database: AppDatabase
    private lateinit var walkDao: WalkDao
    private lateinit var repository: WalkRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(application, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        walkDao = database.walkDao()
        repository = WalkRepositoryImpl(walkDao = walkDao, heatmapCacheDir = null)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    private fun walk(
        id: Long,
        durationSeconds: Long,
        distanceMeters: Double,
        stepCount: Int = 100
    ) = WalkEntity(
        id = id,
        title = "Walk $id",
        startTime = id * 10_000L,
        endTime = id * 10_000L + durationSeconds * 1000L,
        durationSeconds = durationSeconds,
        distanceMeters = distanceMeters,
        stepCount = stepCount,
        averagePaceSecondsPerKm = 600.0
    )

    private fun createViewModel(): HeatmapViewModel {
        return HeatmapViewModel(
            application = application,
            repository = repository,
            heatmapPreferences = HeatmapPreferences(application),
            heatmapCacheDir = File(application.cacheDir, "test_heatmap_cache"),
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun `empty database yields zero lifetime stats in HeatmapViewModel`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val stats = viewModel.uiState.value.lifetimeStats
        assertEquals(0, stats.walkCount)
        assertEquals(0.0, stats.totalDistanceMeters, 1e-6)
        assertEquals(0L, stats.totalDurationSeconds)
        assertEquals(0L, stats.totalSteps)
        assertEquals(0.0, stats.longestWalkMeters, 1e-6)
        assertFalse(stats.hasQualifyingWalks)
        assertNull(stats.weightedAverageSpeedKmh)
    }

    @Test
    fun `qualifying walks are aggregated and micro walks are excluded`() = runTest {
        // Micro-walks (excluded)
        walkDao.insertWalk(walk(1L, durationSeconds = 59L, distanceMeters = 500.0, stepCount = 500))
        walkDao.insertWalk(walk(2L, durationSeconds = 300L, distanceMeters = 49.9, stepCount = 100))

        // Boundary qualifying walk (included)
        walkDao.insertWalk(walk(3L, durationSeconds = 60L, distanceMeters = 50.0, stepCount = 80))
        // Regular qualifying walks (included)
        walkDao.insertWalk(walk(4L, durationSeconds = 1800L, distanceMeters = 2500.0, stepCount = 3200))
        walkDao.insertWalk(walk(5L, durationSeconds = 3600L, distanceMeters = 5000.0, stepCount = 6500))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val stats = viewModel.uiState.value.lifetimeStats
        assertTrue(stats.hasQualifyingWalks)
        assertEquals(3, stats.walkCount)
        assertEquals(7550.0, stats.totalDistanceMeters, 1e-6)
        assertEquals(5460L, stats.totalDurationSeconds)
        assertEquals(9780L, stats.totalSteps)
        assertEquals(5000.0, stats.longestWalkMeters, 1e-6)
    }

    @Test
    fun `weighted average speed weights by duration rather than arithmetic mean of speeds`() = runTest {
        // Short fast walk: 60s, 300m -> 18.0 km/h
        walkDao.insertWalk(walk(1L, durationSeconds = 60L, distanceMeters = 300.0, stepCount = 200))
        // Long slow walk: 3540s, 3300m -> ~3.356 km/h
        walkDao.insertWalk(walk(2L, durationSeconds = 3540L, distanceMeters = 3300.0, stepCount = 4000))
        // Total: 3600s (1 hour), 3600m (3.6 km) -> weighted avg = 3.6 km/h (vs arithmetic mean ~10.68 km/h)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val stats = viewModel.uiState.value.lifetimeStats
        assertEquals(2, stats.walkCount)
        assertEquals(3.6, stats.weightedAverageSpeedKmh!!, 1e-6)
    }
}
