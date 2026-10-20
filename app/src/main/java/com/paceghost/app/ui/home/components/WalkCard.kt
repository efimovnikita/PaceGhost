package com.paceghost.app.ui.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paceghost.app.data.local.entity.WalkEntity
import com.paceghost.app.domain.engine.PaceCalculator
import com.paceghost.app.ui.theme.CyanGhost
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
fun WalkCard(
    walk: WalkEntity,
    onChallengeGhost: (Long) -> Unit,
    onExportGpx: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onPreviewTrack: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val formattedDate = remember(walk.startTime) {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        sdf.format(Date(walk.startTime))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header: Title & Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = walk.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats 4-column summary
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = "Distance",
                    value = PaceCalculator.formatDistance(walk.distanceMeters),
                    valueColor = NeonGreenRunner
                )
                StatItem(
                    label = "Time",
                    value = PaceCalculator.formatDuration(walk.durationSeconds),
                    valueColor = TextPrimary
                )
                StatItem(
                    label = "Steps",
                    value = String.format("%,d", walk.stepCount).replace(',', ' '),
                    valueColor = TextPrimary
                )
                StatItem(
                    label = "Pace",
                    value = PaceCalculator.formatPaceMinSec(walk.averagePaceSecondsPerKm),
                    valueColor = CyanGhost
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action buttons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Group of primary/route actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Challenge Ghost button (prominent primary action with running icon only)
                    FilledIconButton(
                        onClick = { onChallengeGhost(walk.id) },
                        modifier = Modifier.size(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = CyanGhost,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                            contentDescription = "Walk with Ghost",
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // View track on map button
                    OutlinedIconButton(
                        onClick = { onPreviewTrack(walk.id) },
                        modifier = Modifier.size(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CyanGhost.copy(alpha = 0.5f)),
                        colors = IconButtonDefaults.outlinedIconButtonColors(
                            contentColor = CyanGhost
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Map,
                            contentDescription = "View track on map",
                            tint = CyanGhost,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // GPX Export button
                    OutlinedIconButton(
                        onClick = { onExportGpx(walk.id) },
                        modifier = Modifier.size(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, DarkSurfaceVariant),
                        colors = IconButtonDefaults.outlinedIconButtonColors(
                            contentColor = TextPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Export GPX",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Delete button (separated on the far right)
                IconButton(
                    onClick = { onDelete(walk.id) },
                    modifier = Modifier.size(42.dp),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = ErrorRed.copy(alpha = 0.8f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete walk",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    valueColor: Color
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            ),
            color = valueColor
        )
    }
}
