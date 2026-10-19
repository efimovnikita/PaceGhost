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
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.paceghost.app.ui.active.ActiveWalkScreen
import com.paceghost.app.ui.home.HomeScreen
import com.paceghost.app.ui.theme.DarkBackground
import com.paceghost.app.ui.theme.PaceGhostTheme

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
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

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
    ) {
        // Permissions handled
    }

    // Request permissions on first launch
    LaunchedEffect(Unit) {
        permissionLauncher.launch(permissionsToRequest)
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
                        if (!isDemo) {
                            permissionLauncher.launch(permissionsToRequest)
                        }
                        currentScreen = Screen.ActiveWalk(ghostWalkId = ghostWalkId, isDemo = isDemo)
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
