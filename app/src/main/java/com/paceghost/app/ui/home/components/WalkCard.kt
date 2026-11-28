package com.paceghost.app.ui.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    val formattedDate = remember(walk.startTime) { formatWalkDate(walk.startTime) }
    var menuExpanded by remember { mutableStateOf(false) }

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
            // Header: Title + Date on the left, Overflow Menu on the right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = walk.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                Box {
                    IconButton(
                        onClick = { menuExpanded = true }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More options",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        containerColor = DarkSurfaceVariant
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Export GPX",
                                    color = TextPrimary
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = null,
                                    tint = TextPrimary
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onExportGpx(walk.id)
                            }
                        )
                        HorizontalDivider(color = TextSecondary.copy(alpha = 0.15f))
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Delete",
                                    color = ErrorRed
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = null,
                                    tint = ErrorRed
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onDelete(walk.id)
                            }
                        )
                    }
                }
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
            }
        }
    }
}

internal fun formatWalkDate(timestampMs: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US)
    return sdf.format(Date(timestampMs))
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
