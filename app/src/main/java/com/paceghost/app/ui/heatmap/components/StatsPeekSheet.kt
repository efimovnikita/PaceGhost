package com.paceghost.app.ui.heatmap.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paceghost.app.domain.model.LifetimeStats
import com.paceghost.app.domain.model.LifetimeStatsFormatter
import com.paceghost.app.ui.theme.CyanGhost
import com.paceghost.app.ui.theme.DarkSurface
import com.paceghost.app.ui.theme.DarkSurfaceTranslucent
import com.paceghost.app.ui.theme.DarkSurfaceVariant
import com.paceghost.app.ui.theme.NeonGreenRunner
import com.paceghost.app.ui.theme.TextPrimary
import com.paceghost.app.ui.theme.TextSecondary

@Composable
fun StatsPeekSheet(
    stats: LifetimeStats,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val dragThresholdPx = with(LocalDensity.current) { 40.dp.toPx() }

    AnimatedContent(
        targetState = isExpanded,
        contentAlignment = Alignment.BottomCenter,
        transitionSpec = {
            if (targetState) {
                (fadeIn(animationSpec = tween(220)) + slideInVertically(animationSpec = tween(220)) { it / 3 })
                    .togetherWith(
                        fadeOut(animationSpec = tween(150)) + slideOutVertically(animationSpec = tween(150)) { it / 2 }
                    )
            } else {
                (fadeIn(animationSpec = tween(200)) + slideInVertically(animationSpec = tween(200)) { it / 2 })
                    .togetherWith(
                        fadeOut(animationSpec = tween(180)) + slideOutVertically(animationSpec = tween(180)) { it / 3 }
                    )
            }.using(SizeTransform(clip = false))
        },
        modifier = modifier,
        label = "StatsPeekSheetTransition"
    ) { expanded ->
        if (!expanded) {
            CollapsedStatsHandle(
                dragThresholdPx = dragThresholdPx,
                onExpand = { onExpandedChange(true) }
            )
        } else {
            ExpandedStatsCard(
                stats = stats,
                dragThresholdPx = dragThresholdPx,
                onCollapse = { onExpandedChange(false) }
            )
        }
    }
}

@Composable
private fun CollapsedStatsHandle(
    dragThresholdPx: Float,
    onExpand: () -> Unit
) {
    Surface(
        onClick = onExpand,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = DarkSurfaceTranslucent,
        border = BorderStroke(1.dp, DarkSurfaceVariant),
        modifier = Modifier
            .semantics { contentDescription = "Expand Stats" }
            .verticalSwipeThreshold(
                thresholdPx = dragThresholdPx,
                swipeUp = true,
                onTriggered = onExpand
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(CyanGhost.copy(alpha = 0.7f))
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Stats",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    ),
                    color = TextPrimary
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowUp,
                    contentDescription = "Expand Stats",
                    tint = CyanGhost,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ExpandedStatsCard(
    stats: LifetimeStats,
    dragThresholdPx: Float,
    onCollapse: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
        color = DarkSurface.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, DarkSurfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .verticalSwipeThreshold(
                thresholdPx = dragThresholdPx,
                swipeUp = false,
                onTriggered = onCollapse
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag handle + collapse tap target
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClickLabel = "Collapse Stats", onClick = onCollapse)
                    .semantics { contentDescription = "Collapse Stats" }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(CyanGhost.copy(alpha = 0.7f))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Hero: Total Distance
            Text(
                text = "TOTAL DISTANCE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                    fontSize = 11.sp
                ),
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = LifetimeStatsFormatter.formatTotalDistance(stats.totalDistanceMeters),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                ),
                color = if (stats.hasQualifyingWalks) NeonGreenRunner else TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = DarkSurfaceVariant, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Row 1: Total Walks, Total Time, Total Steps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatCell(
                    label = "Total Walks",
                    value = LifetimeStatsFormatter.formatTotalWalks(stats.walkCount),
                    modifier = Modifier.weight(1f)
                )
                StatCell(
                    label = "Total Time",
                    value = LifetimeStatsFormatter.formatTotalTime(stats.totalDurationSeconds),
                    modifier = Modifier.weight(1f)
                )
                StatCell(
                    label = "Total Steps",
                    value = LifetimeStatsFormatter.formatTotalSteps(stats.totalSteps),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Row 2: Avg Speed, Longest Walk
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCell(
                    label = "Avg Speed",
                    value = LifetimeStatsFormatter.formatAvgSpeed(stats.weightedAverageSpeedKmh),
                    modifier = Modifier.weight(1f)
                )
                StatCell(
                    label = "Longest Walk",
                    value = LifetimeStatsFormatter.formatLongestWalk(
                        walkCount = stats.walkCount,
                        longestWalkMeters = stats.longestWalkMeters
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            if (!stats.hasQualifyingWalks) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Record your first walk to see lifetime stats",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            ),
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun Modifier.verticalSwipeThreshold(
    thresholdPx: Float,
    swipeUp: Boolean,
    onTriggered: () -> Unit
): Modifier {
    val currentOnTriggered by rememberUpdatedState(onTriggered)
    val density = LocalDensity.current
    val flingVelocityPx = with(density) { 400.dp.toPx() }
    val minFlingDistancePx = with(density) { 12.dp.toPx() }

    return this.pointerInput(thresholdPx, swipeUp, flingVelocityPx, minFlingDistancePx) {
        val touchSlop = viewConfiguration.touchSlop
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val velocityTracker = VelocityTracker()
            velocityTracker.addPosition(down.uptimeMillis, down.position)
            var totalDragY = 0f
            var totalDragX = 0f
            var pastTouchSlop = false
            var triggered = false

            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) {
                    if (pastTouchSlop && !triggered) {
                        val velocityY = velocityTracker.calculateVelocity().y
                        val flingTriggered = if (swipeUp) {
                            totalDragY <= -minFlingDistancePx && velocityY <= -flingVelocityPx
                        } else {
                            totalDragY >= minFlingDistancePx && velocityY >= flingVelocityPx
                        }
                        if (flingTriggered) {
                            currentOnTriggered()
                        }
                    }
                    break
                }

                val deltaY = change.position.y - change.previousPosition.y
                val deltaX = change.position.x - change.previousPosition.x
                totalDragY += deltaY
                totalDragX += deltaX
                velocityTracker.addPosition(change.uptimeMillis, change.position)

                if (!pastTouchSlop && kotlin.math.abs(totalDragY) > touchSlop &&
                    kotlin.math.abs(totalDragY) > kotlin.math.abs(totalDragX)
                ) {
                    pastTouchSlop = true
                }

                if (pastTouchSlop) {
                    change.consume()
                    if (!triggered) {
                        val distanceTriggered = if (swipeUp) {
                            totalDragY <= -thresholdPx
                        } else {
                            totalDragY >= thresholdPx
                        }
                        if (distanceTriggered) {
                            triggered = true
                            currentOnTriggered()
                        }
                    }
                }
            }
        }
    }
}
