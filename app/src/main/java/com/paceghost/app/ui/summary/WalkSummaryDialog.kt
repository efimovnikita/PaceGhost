package com.paceghost.app.ui.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paceghost.app.domain.engine.PaceCalculator
import com.paceghost.app.ui.theme.DarkSurface
import com.paceghost.app.ui.theme.DarkSurfaceVariant
import com.paceghost.app.ui.theme.ErrorRed
import com.paceghost.app.ui.theme.NeonGreenRunner
import com.paceghost.app.ui.theme.TextPrimary
import com.paceghost.app.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WalkSummaryDialog(
    distanceMeters: Double,
    durationSeconds: Long,
    stepCount: Int,
    averagePaceSecondsPerKm: Double,
    onSave: (title: String) -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val defaultTitle = remember {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        "Прогулка ${dateFormat.format(Date())}"
    }

    var title by remember { mutableStateOf(defaultTitle) }

    AlertDialog(
        onDismissRequest = onDiscard,
        modifier = modifier,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Итоги прогулки 🎉",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                // Name textfield
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название маршрута") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreenRunner,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        focusedLabelColor = NeonGreenRunner,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Grid
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SummaryRow(
                            label = "Дистанция",
                            value = PaceCalculator.formatDistance(distanceMeters),
                            valueColor = NeonGreenRunner
                        )
                        SummaryRow(
                            label = "Время",
                            value = PaceCalculator.formatDuration(durationSeconds),
                            valueColor = TextPrimary
                        )
                        SummaryRow(
                            label = "Шаги",
                            value = "🚶 " + String.format("%,d", stepCount).replace(',', ' '),
                            valueColor = TextPrimary
                        )
                        SummaryRow(
                            label = "Средний темп",
                            value = PaceCalculator.formatPace(averagePaceSecondsPerKm),
                            valueColor = NeonGreenRunner
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title.ifBlank { defaultTitle }) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGreenRunner,
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = "Сохранить",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDiscard,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
            ) {
                Text("Сбросить")
            }
        }
    )
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = valueColor
        )
    }
}
