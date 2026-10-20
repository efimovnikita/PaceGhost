package com.paceghost.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import com.paceghost.app.ui.active.ActiveWalkScreen
import com.paceghost.app.ui.home.HomeScreen
import com.paceghost.app.ui.theme.CyanGhost
import com.paceghost.app.ui.theme.DarkBackground
import com.paceghost.app.ui.theme.DarkSurface
import com.paceghost.app.ui.theme.NeonGreenRunner
import com.paceghost.app.ui.theme.PaceGhostTheme
import com.paceghost.app.ui.theme.TextPrimary
import com.paceghost.app.ui.theme.TextSecondary

sealed interface Screen {
    object Home : Screen
    data class ActiveWalk(val ghostWalkId: Long, val isDemo: Boolean) : Screen
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PaceGhostTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var pendingWalk by remember { mutableStateOf<Screen.ActiveWalk?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Prepare permission list
    val permissionsToRequest = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (fineLocationGranted && pendingWalk != null) {
            currentScreen = pendingWalk!!
        }
        pendingWalk = null
    }

    // Request permissions on first launch
    LaunchedEffect(Unit) {
        permissionLauncher.launch(permissionsToRequest)
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = {
                showPermissionDialog = false
                pendingWalk = null
            },
            containerColor = DarkSurface,
            title = {
                Text(
                    text = "Location Permission Required",
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = NeonGreenRunner
                )
            },
            text = {
                Text(
                    text = "PaceGhost needs precise location access to track your route, pace, and compare your progress with the ghost runner in real time.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        permissionLauncher.launch(permissionsToRequest)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanGhost,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Grant Permission", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        pendingWalk = null
                    }
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "ScreenTransition"
    ) { screen ->
        when (screen) {
            is Screen.Home -> {
                HomeScreen(
                    onStartWalk = { ghostWalkId, isDemo ->
                        if (isDemo) {
                            currentScreen = Screen.ActiveWalk(ghostWalkId = ghostWalkId, isDemo = true)
                        } else {
                            val fineLocationGranted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED

                            if (fineLocationGranted) {
                                currentScreen = Screen.ActiveWalk(ghostWalkId = ghostWalkId, isDemo = false)
                            } else {
                                pendingWalk = Screen.ActiveWalk(ghostWalkId = ghostWalkId, isDemo = false)
                                showPermissionDialog = true
                            }
                        }
                    }
                )
            }
            is Screen.ActiveWalk -> {
                ActiveWalkScreen(
                    ghostWalkId = screen.ghostWalkId,
                    isDemo = screen.isDemo,
                    onNavigateBack = {
                        currentScreen = Screen.Home
                    }
                )
            }
        }
    }
}
