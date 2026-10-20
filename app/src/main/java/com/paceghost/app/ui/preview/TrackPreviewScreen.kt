package com.paceghost.app.ui.preview

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paceghost.app.data.local.entity.WalkEntity
import com.paceghost.app.data.local.entity.averageSpeedKmh
import com.paceghost.app.domain.engine.PaceCalculator
import com.paceghost.app.domain.model.TrackPoint
import com.paceghost.app.ui.preview.components.TrackPreviewMapView
import com.paceghost.app.ui.theme.CyanGhost
import com.paceghost.app.ui.theme.DarkBackground
import com.paceghost.app.ui.theme.DarkSurface
import com.paceghost.app.ui.theme.DarkSurfaceVariant
import com.paceghost.app.ui.theme.ErrorRed
import com.paceghost.app.ui.theme.NeonGreenRunner
import com.paceghost.app.ui.theme.TextPrimary
import com.paceghost.app.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackPreviewScreen(
    walkId: Long,
    onNavigateBack: () -> Unit,
    onStartGhostWalk: (Long) -> Unit,
    viewModel: TrackPreviewViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    LaunchedEffect(walkId) {
        viewModel.loadWalk(walkId)
    }

    BackHandler {
        onNavigateBack()
    }

    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        when (val state = uiState) {
            is TrackPreviewUiState.Loading -> {
                TopAppBar(
                    title = {
                        Text(
                            text = "Track Preview",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = TextPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DarkSurface.copy(alpha = 0.9f)
                    ),
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = NeonGreenRunner,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            is TrackPreviewUiState.Error -> {
                TopAppBar(
                    title = {
                        Text(
                            text = "Track Preview",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = TextPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = TextPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DarkSurface.copy(alpha = 0.9f)
                    ),
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = ErrorRed
                    )
                }
            }

            is TrackPreviewUiState.Success -> {
                TrackPreviewContent(
                    walk = state.walk,
                    trackPoints = state.trackPoints,
                    onNavigateBack = onNavigateBack,
                    onStartGhostWalk = onStartGhostWalk
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackPreviewContent(
    walk: WalkEntity,
    trackPoints: List<TrackPoint>,
    onNavigateBack: () -> Unit,
    onStartGhostWalk: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var fitRouteTrigger by remember { mutableIntStateOf(0) }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Full-screen map view
        TrackPreviewMapView(
            trackPoints = trackPoints,
            fitRouteTrigger = fitRouteTrigger,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Minimalist TopAppBar
        TopAppBar(
            title = {
                Text(
                    text = walk.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = DarkSurface.copy(alpha = 0.85f)
            ),
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // 3. FAB to re-center/fit route & Bottom HUD card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.End
        ) {
            FloatingActionButton(
                onClick = { fitRouteTrigger++ },
                modifier = Modifier.padding(end = 16.dp, bottom = 12.dp),
                containerColor = DarkSurfaceVariant,
                contentColor = CyanGhost,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Filled.CenterFocusStrong,
                    contentDescription = "Re-center route"
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    // Row 1: Steps, Distance, Time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricItem(
                            label = "👟 Steps",
                            value = String.format(Locale.US, "%,d", walk.stepCount).replace(',', ' '),
                            valueColor = TextPrimary
                        )
                        MetricItem(
                            label = "📏 Distance",
                            value = PaceCalculator.formatDistance(walk.distanceMeters),
                            valueColor = NeonGreenRunner
                        )
                        MetricItem(
                            label = "⏱️ Time",
                            value = PaceCalculator.formatDuration(walk.durationSeconds),
                            valueColor = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Row 2: Avg Pace, Avg Speed
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricItem(
                            label = "⚡ Avg Pace",
                            value = PaceCalculator.formatPaceMinSec(walk.averagePaceSecondsPerKm),
                            valueColor = CyanGhost
                        )
                        MetricItem(
                            label = "🚀 Avg Speed",
                            value = String.format(Locale.US, "%.1f km/h", walk.averageSpeedKmh),
                            valueColor = CyanGhost
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // "Walk with Ghost" Button
                    Button(
                        onClick = { onStartGhostWalk(walk.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanGhost,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Walk with Ghost",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
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
