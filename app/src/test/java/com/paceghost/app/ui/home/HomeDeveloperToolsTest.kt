package com.paceghost.app.ui.home

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.paceghost.app.BuildConfig
import com.paceghost.app.data.local.entity.TrackPointEntity
import com.paceghost.app.data.local.entity.WalkEntity
import com.paceghost.app.data.repository.WalkRepository
import com.paceghost.app.domain.model.TrackPoint
import com.paceghost.app.ui.home.components.DeveloperToolsDialog
import com.paceghost.app.ui.theme.PaceGhostTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeDeveloperToolsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val fakeRepository = object : WalkRepository {
        override fun getAllWalks(): Flow<List<WalkEntity>> = flowOf(emptyList())
        override suspend fun getWalkById(id: Long): WalkEntity? = null
        override suspend fun getPointsForWalk(walkId: Long): List<TrackPointEntity> = emptyList()
        override suspend fun getTrackPointsForWalk(walkId: Long): List<TrackPoint> = emptyList()
        override suspend fun saveWalk(walk: WalkEntity, points: List<TrackPointEntity>): Long = 1L
        override suspend fun deleteWalk(id: Long) {}
        override suspend fun updateWalk(walk: WalkEntity) {}
        override suspend fun setFavorite(id: Long, isFavorite: Boolean) {}
        override suspend fun exportWalkToGpx(walkId: Long): String? = null
        override suspend fun importWalkFromGpx(gpxContent: String, titleOverride: String?): Long = 1L
    }

    private fun createViewModel(isDebugBuild: Boolean): HomeViewModel {
        val app = ApplicationProvider.getApplicationContext<Application>()
        return HomeViewModel(
            application = app,
            isDebugBuild = isDebugBuild,
            repositoryOverride = fakeRepository,
            populateDemoWalkAction = {}
        )
    }

    @Test
    fun `homeViewModel has single Application constructor for ViewModelProvider`() {
        val constructor = HomeViewModel::class.java.getConstructor(Application::class.java)
        assertNotNull(constructor)
    }

    @Test
    fun `homeViewModel setDemoMode true is no-op when isDebugBuild is false`() {
        val viewModel = createViewModel(isDebugBuild = false)

        assertFalse(viewModel.isDemoMode.value)
        viewModel.setDemoMode(true)
        assertFalse(viewModel.isDemoMode.value)
    }

    @Test
    fun `homeViewModel setDemoMode toggles state when isDebugBuild is true`() {
        val viewModel = createViewModel(isDebugBuild = true)

        assertFalse(viewModel.isDemoMode.value)
        viewModel.setDemoMode(true)
        assertTrue(viewModel.isDemoMode.value)
        viewModel.setDemoMode(false)
        assertFalse(viewModel.isDemoMode.value)
    }

    @Test
    fun `topAppBar hides demo chip and export logs and keeps only user actions`() {
        val viewModel = createViewModel(isDebugBuild = true)

        composeRule.setContent {
            PaceGhostTheme {
                HomeScreen(
                    onStartWalk = { _, _ -> },
                    viewModel = viewModel,
                    isDebugBuild = true
                )
            }
        }

        composeRule.onNodeWithContentDescription("Personal Heatmap").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Import GPX").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Pace Alert Settings").assertIsDisplayed()

        composeRule.onNodeWithText("Demo").assertDoesNotExist()
        composeRule.onNodeWithText("DEMO").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Demo Mode").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Export Logs").assertDoesNotExist()
    }

    @Test
    fun `when demo mode is off banner is hidden and start button shows New Walk`() {
        val viewModel = createViewModel(isDebugBuild = true)

        composeRule.setContent {
            PaceGhostTheme {
                HomeScreen(
                    onStartWalk = { _, _ -> },
                    viewModel = viewModel,
                    isDebugBuild = true
                )
            }
        }

        composeRule.onNodeWithText("Demo Mode Active").assertDoesNotExist()
        composeRule.onNodeWithText("Exit Demo").assertDoesNotExist()
        composeRule.onNodeWithText("New Walk").assertIsDisplayed()
        composeRule.onNodeWithText("Start Walk").assertIsDisplayed()
    }

    @Test
    fun `long pressing PaceGhost logo opens Developer Tools dialog with version`() {
        val viewModel = createViewModel(isDebugBuild = true)
        val expectedVersion = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

        composeRule.setContent {
            PaceGhostTheme {
                HomeScreen(
                    onStartWalk = { _, _ -> },
                    viewModel = viewModel,
                    isDebugBuild = true
                )
            }
        }

        composeRule.onNode(hasText("Pace") or hasContentDescription("PaceGhost"))
            .performTouchInput { longClick() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Developer Tools").assertIsDisplayed()
        composeRule.onNodeWithText(expectedVersion).assertIsDisplayed()
    }

    @Test
    fun `debug build allows toggling demo mode from dialog and exiting via banner`() {
        val viewModel = createViewModel(isDebugBuild = true)

        composeRule.setContent {
            PaceGhostTheme {
                HomeScreen(
                    onStartWalk = { _, _ -> },
                    viewModel = viewModel,
                    isDebugBuild = true
                )
            }
        }

        // Open Developer Tools via long press on PaceGhost logo
        composeRule.onNodeWithText("Pace").performTouchInput { longClick() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Demo Mode").assertIsDisplayed()
        composeRule.onNodeWithText("Export Debug Logs").assertIsDisplayed()

        // Enable Demo Mode
        composeRule.onNodeWithText("Demo Mode").performClick()
        composeRule.waitForIdle()

        assertTrue(viewModel.isDemoMode.value)

        // Close dialog
        composeRule.onNodeWithText("Close").performClick()
        composeRule.waitForIdle()

        // Banner and Demo button labels appear
        composeRule.onNodeWithText("Demo Mode Active").assertIsDisplayed()
        composeRule.onNodeWithText("Exit Demo").assertIsDisplayed()
        composeRule.onNodeWithText("New Walk (Demo)").assertIsDisplayed()
        composeRule.onNodeWithText("Start Demo").performScrollTo().assertIsDisplayed()

        // One-tap Exit Demo
        composeRule.onNodeWithText("Exit Demo").performClick()
        composeRule.waitForIdle()

        assertFalse(viewModel.isDemoMode.value)
        composeRule.onNodeWithText("Demo Mode Active").assertDoesNotExist()
        composeRule.onNodeWithText("New Walk").assertIsDisplayed()
        composeRule.onNodeWithText("Start Walk").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `release build hides Demo Mode in Developer Tools and keeps Export Debug Logs`() {
        val viewModel = createViewModel(isDebugBuild = false)
        val expectedVersion = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

        composeRule.setContent {
            PaceGhostTheme {
                HomeScreen(
                    onStartWalk = { _, _ -> },
                    viewModel = viewModel,
                    isDebugBuild = false
                )
            }
        }

        composeRule.onNodeWithText("Pace").performTouchInput { longClick() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Developer Tools").assertIsDisplayed()
        composeRule.onNodeWithText(expectedVersion).assertIsDisplayed()
        composeRule.onNodeWithText("Export Debug Logs").assertIsDisplayed()
        composeRule.onNodeWithText("Demo Mode").assertDoesNotExist()
    }

    @Test
    fun `clicking Export Debug Logs in DeveloperToolsDialog triggers callback`() {
        var exportCount = 0
        var dismissCount = 0

        composeRule.setContent {
            PaceGhostTheme {
                DeveloperToolsDialog(
                    isDebugBuild = false,
                    isDemoMode = false,
                    versionLabel = "v1.15.1 (24)",
                    onToggleDemoMode = {},
                    onExportLogs = { exportCount++ },
                    onDismiss = { dismissCount++ }
                )
            }
        }

        composeRule.onNodeWithText("Export Debug Logs").performClick()
        composeRule.waitForIdle()

        assertEquals(1, exportCount)

        composeRule.onNodeWithText("Close").performClick()
        composeRule.waitForIdle()

        assertEquals(1, dismissCount)
    }
}
