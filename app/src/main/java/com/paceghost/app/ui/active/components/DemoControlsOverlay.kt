package com.paceghost.app.ui.active.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paceghost.app.ui.theme.CyanGhost
import com.paceghost.app.ui.theme.DarkSurfaceTranslucent
import com.paceghost.app.ui.theme.DarkSurfaceVariant
import com.paceghost.app.ui.theme.ErrorRed
import com.paceghost.app.ui.theme.NeonGreenRunner
import com.paceghost.app.ui.theme.TextPrimary
import com.paceghost.app.ui.theme.TextSecondary
import com.paceghost.app.ui.theme.TrafficLightBlue
import com.paceghost.app.ui.theme.WarningAmber

@Composable
fun DemoControlsOverlay(
    currentSpeedKmh: Double,
    isOffRoute: Boolean,
    onSetSpeed: (Double) -> Unit,
    onToggleOffRoute: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurfaceTranslucent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ДЕМО-УПРАВЛЕНИЕ СИМУЛЯЦИЕЙ",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanGhost,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format("%.1f км/ч", currentSpeedKmh),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 3.0 km/h
                FilterChip(
                    selected = (currentSpeedKmh in 2.8..3.2),
                    onClick = { onSetSpeed(3.0) },
                    label = { Text("🐢 3.0 км/ч", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = WarningAmber.copy(alpha = 0.25f),
                        selectedLabelColor = WarningAmber,
                        containerColor = DarkSurfaceVariant,
                        labelColor = TextPrimary
                    )
                )

                // 5.0 km/h (Normal)
                FilterChip(
                    selected = (currentSpeedKmh in 4.8..5.2),
                    onClick = { onSetSpeed(5.0) },
                    label = { Text("🚶 5.0 км/ч", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonGreenRunner.copy(alpha = 0.25f),
                        selectedLabelColor = NeonGreenRunner,
                        containerColor = DarkSurfaceVariant,
                        labelColor = TextPrimary
                    )
                )

                // 6.5 km/h (Fast)
                FilterChip(
                    selected = (currentSpeedKmh in 6.3..6.7),
                    onClick = { onSetSpeed(6.5) },
                    label = { Text("🏃 6.5 км/ч", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanGhost.copy(alpha = 0.25f),
                        selectedLabelColor = CyanGhost,
                        containerColor = DarkSurfaceVariant,
                        labelColor = TextPrimary
                    )
                )

                // 0.0 km/h (Traffic light)
                FilterChip(
                    selected = (currentSpeedKmh == 0.0),
                    onClick = { onSetSpeed(0.0) },
                    label = { Text("🚦 Светофор", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TrafficLightBlue.copy(alpha = 0.25f),
                        selectedLabelColor = TrafficLightBlue,
                        containerColor = DarkSurfaceVariant,
                        labelColor = TextPrimary
                    )
                )

                // Off route toggle
                FilterChip(
                    selected = isOffRoute,
                    onClick = { onToggleOffRoute(!isOffRoute) },
                    label = { Text("↩️ Свернуть", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ErrorRed.copy(alpha = 0.25f),
                        selectedLabelColor = ErrorRed,
                        containerColor = DarkSurfaceVariant,
                        labelColor = TextPrimary
                    )
                )
            }
        }
    }
}
