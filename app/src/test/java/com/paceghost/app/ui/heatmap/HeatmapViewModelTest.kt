package com.paceghost.app.ui.heatmap

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.paceghost.app.data.local.entity.WalkEntity
import com.paceghost.app.data.repository.WalkRepository
import com.paceghost.app.domain.model.TrackPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class HeatmapViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `HeatmapViewModel must have single Application constructor for ViewModelProvider`() {
        val constructor = HeatmapViewModel::class.java.getConstructor(Application::class.java)
        assertNotNull(constructor)
    }

    @Test
    fun `HeatmapViewModel must have constructor with WalkRepository for testing`() {
        val constructor = HeatmapViewModel::class.java.getConstructor(
            Application::class.java,
            WalkRepository::class.java
        )
        assertNotNull(constructor)
    }

    @Test
    fun `loadRecentBounds with no walks sets null bounds and hasWalks false`() = runTest {
        val mockRepo = object : FakeWalkRepository() {
            override fun getAllWalks(): Flow<List<WalkEntity>> = flowOf(emptyList())
        }

        val viewModel = HeatmapViewModel(application, mockRepo)
        viewModel.loadRecentBounds()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.hasWalks)
        assertNull(state.recentWalkBounds)
    }

    @Test
    fun `loadRecentBounds with walks calculates correct BoundingBox from latest walk`() = runTest {
        val walk1 = WalkEntity(
            id = 10L,
            title = "Walk 1",
            startTime = 5000L,
            endTime = 6000L,
            durationSeconds = 1000L,
            distanceMeters = 500.0,
            stepCount = 600,
            averagePaceSecondsPerKm = 300.0
        )
        val walk2 = WalkEntity(
            id = 5L,
            title = "Walk 2",
            startTime = 1000L,
            endTime = 2000L,
            durationSeconds = 1000L,
            distanceMeters = 500.0,
            stepCount = 600,
            averagePaceSecondsPerKm = 300.0
        )

        val points = listOf(
            TrackPoint(lat = 55.750, lon = 37.610),
            TrackPoint(lat = 55.760, lon = 37.630),
            TrackPoint(lat = 55.740, lon = 37.620)
        )

        val mockRepo = object : FakeWalkRepository() {
            override fun getAllWalks(): Flow<List<WalkEntity>> = flowOf(listOf(walk1, walk2))
            override suspend fun getTrackPointsForWalk(walkId: Long): List<TrackPoint> {
                return if (walkId == 10L) points else emptyList()
            }
        }

        val viewModel = HeatmapViewModel(application, mockRepo)
        viewModel.loadRecentBounds()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.hasWalks)
        assertNotNull(state.recentWalkBounds)
        val bbox = state.recentWalkBounds!!
        assertEquals(55.760, bbox.latNorth, 0.0001)
        assertEquals(55.740, bbox.latSouth, 0.0001)
        assertEquals(37.630, bbox.lonEast, 0.0001)
        assertEquals(37.610, bbox.lonWest, 0.0001)
    }
}

open class FakeWalkRepository : WalkRepository {
    override fun getAllWalks(): Flow<List<WalkEntity>> = flowOf(emptyList())
    override suspend fun getWalkById(id: Long): WalkEntity? = null
    override suspend fun getPointsForWalk(walkId: Long): List<com.paceghost.app.data.local.entity.TrackPointEntity> = emptyList()
    override suspend fun getTrackPointsForWalk(walkId: Long): List<TrackPoint> = emptyList()
    override suspend fun saveWalk(walk: WalkEntity, points: List<com.paceghost.app.data.local.entity.TrackPointEntity>): Long = 0L
    override suspend fun updateWalk(walk: WalkEntity) {}
    override suspend fun deleteWalk(id: Long) {}
    override suspend fun setFavorite(id: Long, isFavorite: Boolean) {}
    override suspend fun exportWalkToGpx(walkId: Long): String? = null
    override suspend fun importWalkFromGpx(gpxContent: String, titleOverride: String?): Long = 0L
}
