package com.paceghost.app.ui.home

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.core.content.FileProvider
import com.paceghost.app.util.AppLogger
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paceghost.app.ui.home.components.VoiceSettingsDialog
import com.paceghost.app.ui.home.components.WalkCard
import com.paceghost.app.ui.theme.CyanGhost
import com.paceghost.app.ui.theme.DarkBackground
import com.paceghost.app.ui.theme.DarkSurface
import com.paceghost.app.ui.theme.DarkSurfaceVariant
import com.paceghost.app.ui.theme.NeonGreenRunner
import com.paceghost.app.ui.theme.TextPrimary
import com.paceghost.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartWalk: (ghostWalkId: Long, isDemo: Boolean) -> Unit,
    viewModel: HomeViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val walks by viewModel.walks.collectAsState()
    val isDemoMode by viewModel.isDemoMode.collectAsState()
    val voiceSettings by viewModel.voiceSettings.collectAsState()
    var showVoiceSettingsDialog by remember { mutableStateOf(false) }

    // Observe user message notifications
    LaunchedEffect(Unit) {
        viewModel.userMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // GPX File picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val content = inputStream?.bufferedReader()?.use { it.readText() }
                if (!content.isNullOrBlank()) {
                    viewModel.importGpxContent(content)
                }
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("Error reading file: ${e.message}")
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pace",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp
                            ),
                            color = NeonGreenRunner,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = "Ghost",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp
                            ),
                            color = CyanGhost,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                },
                actions = {
                    // Compact Demo Mode Toggle Chip
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isDemoMode) CyanGhost.copy(alpha = 0.25f) else DarkSurfaceVariant)
                            .clickable { viewModel.setDemoMode(!isDemoMode) }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Science,
                            contentDescription = "Demo Mode",
                            tint = if (isDemoMode) CyanGhost else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isDemoMode) "DEMO" else "Demo",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isDemoMode) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isDemoMode) CyanGhost else TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Import GPX Button
                    IconButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FileOpen,
                            contentDescription = "Import GPX",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Pace Alert Settings Button
                    IconButton(
                        onClick = { showVoiceSettingsDialog = true },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Pace Alert Settings",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Export Logs Button
                    IconButton(
                        onClick = {
                            val logFile = AppLogger.getLogFile(context)
                            if (logFile == null || logFile.length() == 0L) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Log file is empty or not created yet")
                                }
                            } else {
                                try {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        logFile
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_SUBJECT, "PaceGhost Debug Log")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    val chooser = Intent.createChooser(shareIntent, "Export PaceGhost Logs").apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(chooser)
                                } catch (e: Exception) {
                                    AppLogger.e("HomeScreen", "Failed to export logs", e)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Failed to export logs: ${e.message}")
                                    }
                                }
                            }
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BugReport,
                            contentDescription = "Export Logs",
                            tint = CyanGhost,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        bottomBar = {
            // Prominent "New Walk" Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBackground)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = { onStartWalk(-1L, isDemoMode) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonGreenRunner,
                        contentColor = Color.Black
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isDemoMode) "New Walk (Demo)" else "New Walk",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        if (walks.isEmpty()) {
            EmptyWalksView(
                isDemoMode = isDemoMode,
                onStart = { onStartWalk(-1L, isDemoMode) },
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(
                        text = "Saved Routes",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        ),
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                }

                items(walks, key = { it.id }) { walk ->
                    WalkCard(
                        walk = walk,
                        onChallengeGhost = { id -> onStartWalk(id, isDemoMode) },
                        onExportGpx = { id ->
                            scope.launch {
                                val gpx = viewModel.getGpxContent(id)
                                if (gpx != null) {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, gpx)
                                        type = "text/xml"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Export GPX: ${walk.title}")
                                    context.startActivity(shareIntent)
                                } else {
                                    snackbarHostState.showSnackbar("GPX export error")
                                }
                            }
                        },
                        onDelete = { id -> viewModel.deleteWalk(id) }
                    )
                }
            }
        }

        if (showVoiceSettingsDialog) {
            VoiceSettingsDialog(
                initialSettings = voiceSettings,
                onSave = { newSettings ->
                    viewModel.saveVoiceSettings(newSettings)
                    showVoiceSettingsDialog = false
                },
                onDismiss = { showVoiceSettingsDialog = false }
            )
        }
    }
}

@Composable
private fun EmptyWalksView(
    isDemoMode: Boolean,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "👻",
                    fontSize = 48.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Saved Routes",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Start your first walk to record your pace. Next time you can race against your ghost!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onStart,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanGhost,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isDemoMode) "Start Demo" else "Start Walk",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
