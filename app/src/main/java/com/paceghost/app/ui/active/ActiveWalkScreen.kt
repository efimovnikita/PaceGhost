package com.paceghost.app.ui.active

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paceghost.app.ui.active.components.DemoControlsOverlay
import com.paceghost.app.ui.active.components.HapticBadge
import com.paceghost.app.ui.active.components.OsmMapView
import com.paceghost.app.ui.active.components.WalkHudCard
import com.paceghost.app.ui.summary.WalkSummaryDialog
import com.paceghost.app.ui.theme.CyanGhost
import com.paceghost.app.ui.theme.DarkBackground
import com.paceghost.app.ui.theme.DarkSurfaceVariant
import com.paceghost.app.ui.theme.NeonGreenRunner
import com.paceghost.app.ui.theme.TextDisabled
import com.paceghost.app.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWalkScreen(
    ghostWalkId: Long,
    isDemo: Boolean,
    onNavigateBack: () -> Unit,
    viewModel: ActiveWalkViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val walkState by viewModel.walkState.collectAsState()
    val showSummaryDialog by viewModel.showSummaryDialog.collectAsState()
    var isOffRouteSimulated by remember { mutableStateOf(false) }

    // Start tracking and bind service on launch
    LaunchedEffect(ghostWalkId, isDemo) {
        viewModel.start(ghostWalkId, isDemo)
    }

    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                ActiveWalkNavigationEvent.NavigateBackToHome -> onNavigateBack()
            }
        }
    }

    // Intercept back button to confirm finish/exit
    BackHandler {
        viewModel.requestFinish()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // 1. Full-screen OpenStreetMap
        OsmMapView(
            userPoints = walkState.currentPoints,
            ghostPoints = walkState.ghostPoints,
            ghostLat = walkState.ghostComparison.ghostLat,
            ghostLon = walkState.ghostComparison.ghostLon,
            autoCenter = true,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Top bar and Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (walkState.isDemoMode) "PaceGhost: Demo" else "PaceGhost: Walk",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = TextPrimary
                        )
                        if (walkState.isDemoMode) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SIMULATOR",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp
                                ),
                                color = CyanGhost,
                                modifier = Modifier
                                    .background(DarkSurfaceVariant, CircleShape)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.requestFinish() }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleVoiceGuidance() }
                    ) {
                        Icon(
                            imageVector = if (walkState.isVoiceGuidanceEnabled) {
                                Icons.AutoMirrored.Filled.VolumeUp
                            } else {
                                Icons.AutoMirrored.Filled.VolumeOff
                            },
                            contentDescription = if (walkState.isVoiceGuidanceEnabled) "Отключить голосовые подсказки" else "Включить голосовые подсказки",
                            tint = if (walkState.isVoiceGuidanceEnabled) CyanGhost else TextDisabled
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground.copy(alpha = 0.85f)
                )
            )

            // Demo controls overlay if demo mode active
            if (walkState.isDemoMode) {
                DemoControlsOverlay(
                    currentSpeedKmh = walkState.currentSpeedKmh,
                    isOffRoute = isOffRouteSimulated,
                    onSetSpeed = { speed ->
                        viewModel.setDemoSpeed(speed)
                    },
                    onToggleOffRoute = { offRoute ->
                        isOffRouteSimulated = offRoute
                        viewModel.toggleDemoOffRoute(offRoute)
                    }
                )
            }

            // Voice & Haptic feedback badge
            HapticBadge(
                message = walkState.recentVibrationMessage,
                voiceMessage = walkState.recentVoiceMessage,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }

        // 3. Bottom HUD card
        WalkHudCard(
            walkState = walkState,
            onPauseResume = { viewModel.togglePauseResume() },
            onFinish = { viewModel.requestFinish() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )

        // 4. Summary Dialog
        if (showSummaryDialog) {
            WalkSummaryDialog(
                distanceMeters = walkState.distanceMeters,
                durationSeconds = walkState.elapsedSeconds,
                stepCount = walkState.stepCount,
                averagePaceSecondsPerKm = walkState.currentPaceSecondsPerKm,
                onSave = { title -> viewModel.saveWalk(title) },
                onDiscard = { viewModel.discardWalk() }
            )
        }
    }
}
