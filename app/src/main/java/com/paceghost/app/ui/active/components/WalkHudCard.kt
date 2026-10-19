package com.paceghost.app.ui.active.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paceghost.app.domain.engine.PaceCalculator
import com.paceghost.app.domain.model.GhostStatus
import com.paceghost.app.domain.model.WalkState
import com.paceghost.app.ui.theme.CyanGhost
import com.paceghost.app.ui.theme.DarkSurfaceTranslucent
import com.paceghost.app.ui.theme.DarkSurfaceVariant
import com.paceghost.app.ui.theme.ErrorRed
import com.paceghost.app.ui.theme.NeonGreenRunner
import com.paceghost.app.ui.theme.TextDisabled
import com.paceghost.app.ui.theme.TextPrimary
import com.paceghost.app.ui.theme.TextSecondary
import com.paceghost.app.ui.theme.TrafficLightBlue
import com.paceghost.app.ui.theme.WarningAmber
import kotlin.math.abs

@Composable
fun WalkHudCard(
    walkState: WalkState,
    onPauseResume: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurfaceTranslucent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Ghost Status Chip
            GhostStatusChip(walkState)

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Main Metrics (Pace & Steps)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pace
                Column {
                    Text(
                        text = "ТЕМП",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val paceFormatted = PaceCalculator.formatPaceMinSec(walkState.currentPaceSecondsPerKm)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = paceFormatted,
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 34.sp
                            ),
                            color = NeonGreenRunner
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "мин/км",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }

                // Steps
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "ШАГИ",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val formattedSteps = String.format("%,d", walkState.stepCount).replace(',', ' ')
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "🚶",
                            fontSize = 24.sp,
                            modifier = Modifier.padding(bottom = 4.dp, end = 4.dp)
                        )
                        Text(
                            text = formattedSteps,
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 34.sp
                            ),
                            color = TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Sub-metrics: Distance & Elapsed Time
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Дистанция: ${PaceCalculator.formatDistance(walkState.distanceMeters)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = "Время: ${PaceCalculator.formatDuration(walkState.elapsedSeconds)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Action Buttons (Pause / Resume & Finish)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pause / Resume Button
                val isPausedOrAuto = walkState.isPaused || walkState.isAutoPaused
                FilledIconButton(
                    onClick = onPauseResume,
                    modifier = Modifier.size(52.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isPausedOrAuto) NeonGreenRunner else DarkSurfaceVariant,
                        contentColor = if (isPausedOrAuto) Color.Black else TextPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (isPausedOrAuto) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = if (isPausedOrAuto) "Продолжить" else "Пауза",
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Finish Button
                Button(
                    onClick = onFinish,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2A1517),
                        contentColor = ErrorRed
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Завершить",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }
    }
}

@Composable
private fun GhostStatusChip(walkState: WalkState) {
    val ghost = walkState.ghostComparison

    val (chipText, chipColor, chipBg) = when {
        walkState.isAutoPaused -> Triple(
            "🚦 Светофор (Авто-пауза)",
            TrafficLightBlue,
            TrafficLightBlue.copy(alpha = 0.15f)
        )
        walkState.isPaused -> Triple(
            "⏸️ Пауза",
            TextSecondary,
            DarkSurfaceVariant
        )
        ghost.status == GhostStatus.AHEAD -> {
            val dist = "+${abs(ghost.distanceDeltaMeters).toInt()} м"
            val sec = "+${abs(ghost.timeDeltaSeconds)} с"
            Triple(
                "⚡ $dist / $sec (Впереди)",
                NeonGreenRunner,
                NeonGreenRunner.copy(alpha = 0.15f)
            )
        }
        ghost.status == GhostStatus.BEHIND -> {
            val dist = "-${abs(ghost.distanceDeltaMeters).toInt()} м"
            val sec = "-${abs(ghost.timeDeltaSeconds)} с"
            Triple(
                "⚠️ $dist / $sec (Отставание)",
                WarningAmber,
                WarningAmber.copy(alpha = 0.15f)
            )
        }
        ghost.status == GhostStatus.OFF_ROUTE -> Triple(
            "↩️ Вне маршрута (>60м)",
            ErrorRed,
            ErrorRed.copy(alpha = 0.15f)
        )
        ghost.status == GhostStatus.ON_PACE -> Triple(
            "🎯 В темпе призрака",
            CyanGhost,
            CyanGhost.copy(alpha = 0.15f)
        )
        else -> Triple(
            "👻 Свободная прогулка",
            TextDisabled,
            DarkSurfaceVariant.copy(alpha = 0.6f)
        )
    }

    val animatedColor by animateColorAsState(targetValue = chipColor, label = "chipColor")
    val animatedBg by animateColorAsState(targetValue = chipBg, label = "chipBg")

    Box(
        modifier = Modifier
            .background(animatedBg, CircleShape)
            .border(1.dp, animatedColor.copy(alpha = 0.4f), CircleShape)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = chipText,
            color = animatedColor,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        )
    }
}
