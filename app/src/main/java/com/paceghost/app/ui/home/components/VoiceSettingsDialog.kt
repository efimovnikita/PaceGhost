package com.paceghost.app.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paceghost.app.domain.model.VoiceSettings
import com.paceghost.app.ui.theme.CyanGhost
import com.paceghost.app.ui.theme.DarkSurface
import com.paceghost.app.ui.theme.DarkSurfaceVariant
import com.paceghost.app.ui.theme.NeonGreenRunner
import com.paceghost.app.ui.theme.TextDisabled
import com.paceghost.app.ui.theme.TextPrimary
import com.paceghost.app.ui.theme.TextSecondary
import kotlin.math.roundToLong

@Composable
fun VoiceSettingsDialog(
    initialSettings: VoiceSettings,
    onSave: (VoiceSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var behindSeconds by remember { mutableLongStateOf(initialSettings.behindCooldownSeconds) }
    var isAheadEnabled by remember { mutableStateOf(initialSettings.isAheadVoiceEnabled) }
    var aheadSeconds by remember { mutableLongStateOf(initialSettings.aheadCooldownSeconds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.RecordVoiceOver,
                        contentDescription = null,
                        tint = CyanGhost,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Pace Alerts",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Voice & vibration alerts",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Section 1: Behind ghost cooldown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "When Behind Ghost",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = VoiceSettings.formatSeconds(behindSeconds),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = CyanGhost,
                        modifier = Modifier
                            .background(DarkSurfaceVariant, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Alert frequency (voice & vibration): \"Catch up, lagging behind ghost\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Slider(
                    value = behindSeconds.toFloat(),
                    onValueChange = { raw ->
                        val step = VoiceSettings.BEHIND_COOLDOWN_STEP_SECONDS.toFloat()
                        val snapped = ((raw / step).roundToLong() * VoiceSettings.BEHIND_COOLDOWN_STEP_SECONDS)
                        behindSeconds = snapped.coerceIn(
                            VoiceSettings.MIN_BEHIND_COOLDOWN_SECONDS,
                            VoiceSettings.MAX_BEHIND_COOLDOWN_SECONDS
                        )
                    },
                    valueRange = VoiceSettings.MIN_BEHIND_COOLDOWN_SECONDS.toFloat()..VoiceSettings.MAX_BEHIND_COOLDOWN_SECONDS.toFloat(),
                    steps = 18,
                    colors = SliderDefaults.colors(
                        thumbColor = CyanGhost,
                        activeTrackColor = CyanGhost,
                        inactiveTrackColor = DarkSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "15 sec",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "5 min",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                HorizontalDivider(
                    color = DarkSurfaceVariant,
                    modifier = Modifier.padding(vertical = 14.dp)
                )

                // Section 2: Ahead of ghost switch and slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "When Ahead of Ghost",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Voice & vibration alerts for overtaking and pace",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = isAheadEnabled,
                        onCheckedChange = { isAheadEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonGreenRunner,
                            checkedTrackColor = NeonGreenRunner.copy(alpha = 0.35f),
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = DarkSurfaceVariant
                        )
                    )
                }

                if (isAheadEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Repeat for \"Fast pace, keep it up!\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Text(
                            text = VoiceSettings.formatSeconds(aheadSeconds),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = NeonGreenRunner,
                            modifier = Modifier
                                .background(DarkSurfaceVariant, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    Slider(
                        value = aheadSeconds.toFloat(),
                        onValueChange = { raw ->
                            val step = VoiceSettings.AHEAD_COOLDOWN_STEP_SECONDS.toFloat()
                            val snapped = ((raw / step).roundToLong() * VoiceSettings.AHEAD_COOLDOWN_STEP_SECONDS)
                            aheadSeconds = snapped.coerceIn(
                                VoiceSettings.MIN_AHEAD_COOLDOWN_SECONDS,
                                VoiceSettings.MAX_AHEAD_COOLDOWN_SECONDS
                            )
                        },
                        valueRange = VoiceSettings.MIN_AHEAD_COOLDOWN_SECONDS.toFloat()..VoiceSettings.MAX_AHEAD_COOLDOWN_SECONDS.toFloat(),
                        steps = 18,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonGreenRunner,
                            activeTrackColor = NeonGreenRunner,
                            inactiveTrackColor = DarkSurfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "30 sec",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "10 min",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Alerts disabled (silent when ahead)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDisabled
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        VoiceSettings(
                            behindCooldownSeconds = behindSeconds,
                            isAheadVoiceEnabled = isAheadEnabled,
                            aheadCooldownSeconds = aheadSeconds
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanGhost,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Save",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = "Cancel",
                    color = TextSecondary
                )
            }
        }
    )
}
