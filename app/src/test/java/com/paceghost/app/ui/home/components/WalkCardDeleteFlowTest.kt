package com.paceghost.app.ui.home.components

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.paceghost.app.data.local.entity.WalkEntity
import com.paceghost.app.domain.engine.PaceCalculator
import com.paceghost.app.ui.theme.PaceGhostTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WalkCardDeleteFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val sampleWalk = WalkEntity(
        id = 42L,
        title = "Morning River Loop",
        startTime = 1711958400000L, // 2024-04-01 08:00:00 UTC
        endTime = 1711960800000L,
        durationSeconds = 2400L,
        distanceMeters = 3450.0,
        stepCount = 4500,
        averagePaceSecondsPerKm = 695.0
    )

    private fun setUpDeleteDialogFlow(confirmedDeletes: MutableList<Long>) {
        composeRule.setContent {
            PaceGhostTheme {
                var pendingDelete by remember { mutableStateOf<WalkEntity?>(null) }

                WalkCard(
                    walk = sampleWalk,
                    onChallengeGhost = {},
                    onExportGpx = {},
                    onDelete = { pendingDelete = sampleWalk },
                    onPreviewTrack = {}
                )

                pendingDelete?.let { walk ->
                    DeleteWalkConfirmationDialog(
                        walk = walk,
                        onConfirm = {
                            confirmedDeletes.add(walk.id)
                            pendingDelete = null
                        },
                        onDismiss = { pendingDelete = null }
                    )
                }
            }
        }
    }

    private fun openDeleteDialogFromOverflow() {
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun `walkCard hides direct delete and export buttons and shows overflow menu and date`() {
        val expectedDate = formatWalkDate(sampleWalk.startTime)

        composeRule.setContent {
            PaceGhostTheme {
                WalkCard(
                    walk = sampleWalk,
                    onChallengeGhost = {},
                    onExportGpx = {},
                    onDelete = {},
                    onPreviewTrack = {}
                )
            }
        }

        composeRule.onNodeWithText(sampleWalk.title).assertIsDisplayed()
        composeRule.onNodeWithText(expectedDate).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Walk with Ghost").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("View track on map").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("More options").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Delete walk").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Export GPX").assertDoesNotExist()
    }

    @Test
    fun `tapping more options opens menu with export gpx and delete`() {
        composeRule.setContent {
            PaceGhostTheme {
                WalkCard(
                    walk = sampleWalk,
                    onChallengeGhost = {},
                    onExportGpx = {},
                    onDelete = {},
                    onPreviewTrack = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Export GPX").assertIsDisplayed()
        composeRule.onNodeWithText("Delete").assertIsDisplayed()
    }

    @Test
    fun `tapping export gpx closes menu and triggers export without delete`() {
        val exportedIds = mutableListOf<Long>()
        val deletedIds = mutableListOf<Long>()

        composeRule.setContent {
            PaceGhostTheme {
                WalkCard(
                    walk = sampleWalk,
                    onChallengeGhost = {},
                    onExportGpx = { exportedIds.add(it) },
                    onDelete = { deletedIds.add(it) },
                    onPreviewTrack = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Export GPX").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Export GPX").assertDoesNotExist()
        assertEquals(listOf(42L), exportedIds)
        assertEquals(emptyList<Long>(), deletedIds)
    }

    @Test
    fun `tapping delete in menu opens confirmation dialog without deleting yet`() {
        val confirmedDeletes = mutableListOf<Long>()
        val expectedDate = formatWalkDate(sampleWalk.startTime)
        val expectedDistance = PaceCalculator.formatDistance(sampleWalk.distanceMeters)

        setUpDeleteDialogFlow(confirmedDeletes)
        openDeleteDialogFromOverflow()

        composeRule.onNodeWithText("Export GPX").assertDoesNotExist()
        composeRule.onNodeWithText("Delete walk?").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Are you sure you want to delete \"${sampleWalk.title}\" ($expectedDate, $expectedDistance)? This action cannot be undone."
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").assertIsDisplayed()
        composeRule.onNodeWithText("Delete").assertIsDisplayed()
        assertEquals(emptyList<Long>(), confirmedDeletes)
    }

    @Test
    fun `tapping cancel in dialog closes dialog without deleting`() {
        val confirmedDeletes = mutableListOf<Long>()

        setUpDeleteDialogFlow(confirmedDeletes)
        openDeleteDialogFromOverflow()

        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Delete walk?").assertDoesNotExist()
        assertEquals(emptyList<Long>(), confirmedDeletes)
    }

    @Test
    fun `dismissing dialog with back press closes dialog without deleting`() {
        val confirmedDeletes = mutableListOf<Long>()

        setUpDeleteDialogFlow(confirmedDeletes)
        openDeleteDialogFromOverflow()

        composeRule.onNodeWithText("Delete walk?").assertIsDisplayed()
        composeRule.runOnUiThread {
            ShadowDialog.getLatestDialog().onBackPressed()
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Delete walk?").assertDoesNotExist()
        assertEquals(emptyList<Long>(), confirmedDeletes)
    }

    @Test
    fun `tapping delete in dialog closes dialog and confirms deletion once`() {
        val confirmedDeletes = mutableListOf<Long>()

        setUpDeleteDialogFlow(confirmedDeletes)
        openDeleteDialogFromOverflow()

        composeRule.onNodeWithText("Delete").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Delete walk?").assertDoesNotExist()
        assertEquals(listOf(42L), confirmedDeletes)
    }
}
