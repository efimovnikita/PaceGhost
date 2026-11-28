package com.paceghost.app.ui.home.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontWeight
import com.paceghost.app.data.local.entity.WalkEntity
import com.paceghost.app.domain.engine.PaceCalculator
import com.paceghost.app.ui.theme.DarkSurface
import com.paceghost.app.ui.theme.ErrorRed
import com.paceghost.app.ui.theme.TextPrimary
import com.paceghost.app.ui.theme.TextSecondary

/**
 * OLED-themed confirmation dialog before permanently deleting a recorded walk.
 */
@Composable
fun DeleteWalkConfirmationDialog(
    walk: WalkEntity,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val formattedDate = remember(walk.startTime) { formatWalkDate(walk.startTime) }
    val formattedDistance = remember(walk.distanceMeters) {
        PaceCalculator.formatDistance(walk.distanceMeters)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text(
                text = "Delete walk?",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete \"${walk.title}\" ($formattedDate, $formattedDistance)? This action cannot be undone.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
            ) {
                Text(
                    text = "Delete",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
            ) {
                Text(text = "Cancel")
            }
        }
    )
}
