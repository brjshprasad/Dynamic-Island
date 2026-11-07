package com.custom.galaxylauncher

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.custom.galaxylauncher.dynamicisland.DynamicIslandController
import com.custom.galaxylauncher.gesture.MotionGestureService
import com.custom.galaxylauncher.model.AppInfo
import com.custom.galaxylauncher.repository.AppRepository
import com.custom.galaxylauncher.repository.FlashlightManager
import com.custom.galaxylauncher.repository.GestureSettingsRepository
import com.custom.galaxylauncher.repository.UsageTracker
import com.custom.galaxylauncher.ui.screens.AllAppsDrawer
import com.custom.galaxylauncher.ui.screens.HomeScreen
import com.custom.galaxylauncher.ui.screens.LauncherSettingsScreen
import com.custom.galaxylauncher.ui.theme.GalaxyLauncherTheme

enum class Screen {
    HOME,
    ALL_APPS,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private lateinit var appRepository: AppRepository
    private lateinit var settingsRepository: GestureSettingsRepository
    private lateinit var flashlightManager: FlashlightManager
    private lateinit var usageTracker: UsageTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appRepository = AppRepository(this)
        settingsRepository = GestureSettingsRepository.getInstance(this)
        flashlightManager = FlashlightManager(this)
        usageTracker = UsageTracker(this)

        // Initialize Dynamic Island system event listeners (battery charging & ringer mode)
        DynamicIslandController.initSystemListeners(this)

        // Start background motion gesture service (Chop-Chop & Wrist Twist)
        MotionGestureService.start(this)

        setContent {
            GalaxyLauncherTheme {
                LauncherApp(
                    appRepository = appRepository,
                    settingsRepository = settingsRepository,
                    usageTracker = usageTracker,
                    onToggleTorch = { flashlightManager.toggleTorch() },
                    onLaunchCamera = { launchCameraIntent() }
                )
            }
        }
    }

    private fun launchCameraIntent() {
        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {}
    }
}

@Composable
fun LauncherApp(
    appRepository: AppRepository,
    settingsRepository: GestureSettingsRepository,
    usageTracker: UsageTracker,
    onToggleTorch: () -> Unit,
    onLaunchCamera: () -> Unit
) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var autoFocusSearch by remember { mutableStateOf(false) }
    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }

    // Load installed apps asynchronously
    LaunchedEffect(Unit) {
        installedApps = appRepository.getInstalledApps()
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screenTransition",
        modifier = Modifier.fillMaxSize()
    ) { screen ->
        when (screen) {
            Screen.HOME -> HomeScreen(
                apps = installedApps,
                settingsRepo = settingsRepository,
                onAppClick = { app ->
                    usageTracker.recordAppLaunch(app.packageName)
                    appRepository.launchApp(app)
                },
                onOpenAllApps = {
                    autoFocusSearch = false
                    currentScreen = Screen.ALL_APPS
                },
                onOpenSearch = {
                    autoFocusSearch = true
                    currentScreen = Screen.ALL_APPS
                },
                onOpenSettings = { currentScreen = Screen.SETTINGS },
                onToggleTorch = onToggleTorch,
                onLaunchCamera = onLaunchCamera
            )
            Screen.ALL_APPS -> AllAppsDrawer(
                apps = installedApps,
                usageTracker = usageTracker,
                autoFocusKeyboard = autoFocusSearch,
                onAppClick = { app ->
                    usageTracker.recordAppLaunch(app.packageName)
                    appRepository.launchApp(app)
                    currentScreen = Screen.HOME
                },
                onCloseDrawer = { currentScreen = Screen.HOME }
            )
            Screen.SETTINGS -> LauncherSettingsScreen(
                settingsRepo = settingsRepository,
                onBack = { currentScreen = Screen.HOME }
            )
        }
    }
}
