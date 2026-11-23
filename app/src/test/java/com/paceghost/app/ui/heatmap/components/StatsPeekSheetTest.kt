package com.paceghost.app.ui.heatmap.components

import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import com.paceghost.app.domain.model.LifetimeStats
import com.paceghost.app.ui.theme.PaceGhostTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StatsPeekSheetTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val sampleStats = LifetimeStats(
        walkCount = 12,
        totalDistanceMeters = 45600.0,
        totalDurationSeconds = 36000L,
        totalSteps = 58400L,
        longestWalkMeters = 8400.0
    )

    @Test
    fun `collapsed handle expands on tap`() {
        var expanded = false
        composeRule.setContent {
            PaceGhostTheme {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    StatsPeekSheet(
                        stats = sampleStats,
                        isExpanded = false,
                        onExpandedChange = { expanded = it }
                    )
                }
            }
        }

        composeRule.onNodeWithText("Stats").assertIsDisplayed()
        composeRule.onNodeWithText("TOTAL DISTANCE").assertDoesNotExist()

        composeRule.onNodeWithText("Stats").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        assertTrue(expanded)
    }

    @Test
    fun `collapsed handle expands on swipe up`() {
        var expanded = false
        composeRule.setContent {
            PaceGhostTheme {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    StatsPeekSheet(
                        stats = sampleStats,
                        isExpanded = false,
                        onExpandedChange = { expanded = it }
                    )
                }
            }
        }

        composeRule.onNodeWithText("Stats").performTouchInput {
            swipeUp(startY = centerY, endY = centerY - 120f, durationMillis = 150)
        }
        composeRule.waitForIdle()
        assertTrue(expanded)
    }

    @Test
    fun `collapsed handle expands on quick short flick up`() {
        var expanded = false
        composeRule.setContent {
            PaceGhostTheme {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    StatsPeekSheet(
                        stats = sampleStats,
                        isExpanded = false,
                        onExpandedChange = { expanded = it }
                    )
                }
            }
        }

        composeRule.onNodeWithText("Stats").performTouchInput {
            swipeUp(startY = centerY, endY = centerY - 22f, durationMillis = 30)
        }
        composeRule.waitForIdle()
        assertTrue(expanded)
    }

    @Test
    fun `collapsed handle does not expand on swipe down`() {
        var expanded = false
        composeRule.setContent {
            PaceGhostTheme {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    StatsPeekSheet(
                        stats = sampleStats,
                        isExpanded = false,
                        onExpandedChange = { expanded = it }
                    )
                }
            }
        }

        composeRule.onNodeWithText("Stats").performTouchInput {
            swipeDown(startY = top, endY = bottom + 120f, durationMillis = 150)
        }
        composeRule.waitForIdle()
        assertFalse(expanded)
    }

    @Test
    fun `expanded card collapses on handle tap`() {
        var expanded = true
        composeRule.setContent {
            PaceGhostTheme {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    StatsPeekSheet(
                        stats = sampleStats,
                        isExpanded = true,
                        onExpandedChange = { expanded = it }
                    )
                }
            }
        }

        composeRule.onNodeWithText("TOTAL DISTANCE").assertIsDisplayed()
        composeRule.onNodeWithText("45.6 km").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Collapse Stats").performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()
        assertFalse(expanded)
    }

    @Test
    fun `expanded card collapses on swipe down`() {
        var expanded = true
        composeRule.setContent {
            PaceGhostTheme {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    StatsPeekSheet(
                        stats = sampleStats,
                        isExpanded = true,
                        onExpandedChange = { expanded = it }
                    )
                }
            }
        }

        composeRule.onNodeWithText("TOTAL DISTANCE").performTouchInput {
            swipeDown(startY = centerY, endY = centerY + 120f, durationMillis = 150)
        }
        composeRule.waitForIdle()
        assertFalse(expanded)
    }

    @Test
    fun `expanded card does not collapse on swipe up`() {
        var expanded = true
        composeRule.setContent {
            PaceGhostTheme {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    StatsPeekSheet(
                        stats = sampleStats,
                        isExpanded = true,
                        onExpandedChange = { expanded = it }
                    )
                }
            }
        }

        composeRule.onNodeWithText("TOTAL DISTANCE").performTouchInput {
            swipeUp(startY = centerY, endY = centerY - 120f, durationMillis = 150)
        }
        composeRule.waitForIdle()
        assertTrue(expanded)
    }

    @Test
    fun `back press collapses expanded sheet before navigating back`() {
        var navigateBackCount = 0
        lateinit var backDispatcher: OnBackPressedDispatcher

        composeRule.setContent {
            PaceGhostTheme {
                var isExpanded by remember { mutableStateOf(true) }
                backDispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher
                BackHandler {
                    if (isExpanded) {
                        isExpanded = false
                    } else {
                        navigateBackCount++
                    }
                }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    StatsPeekSheet(
                        stats = sampleStats,
                        isExpanded = isExpanded,
                        onExpandedChange = { isExpanded = it }
                    )
                }
            }
        }

        composeRule.onNodeWithText("TOTAL DISTANCE").assertIsDisplayed()

        composeRule.runOnUiThread { backDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Stats").assertIsDisplayed()
        assertEquals(0, navigateBackCount)

        composeRule.runOnUiThread { backDispatcher.onBackPressed() }
        composeRule.waitForIdle()

        assertEquals(1, navigateBackCount)
    }

    @Test
    fun `empty stats displays zeros and prompt when expanded`() {
        composeRule.setContent {
            PaceGhostTheme {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    StatsPeekSheet(
                        stats = LifetimeStats(),
                        isExpanded = true,
                        onExpandedChange = {}
                    )
                }
            }
        }

        composeRule.onNodeWithText("0.0 km").assertIsDisplayed()
        composeRule.onNodeWithText("Record your first walk to see lifetime stats").assertIsDisplayed()
    }
}
