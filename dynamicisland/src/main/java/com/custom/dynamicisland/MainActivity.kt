package com.custom.dynamicisland

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationManagerCompat
import com.custom.dynamicisland.repository.IslandSettingsRepository
import com.custom.dynamicisland.service.IslandNotificationListener
import com.custom.dynamicisland.service.IslandOverlayService
import com.custom.dynamicisland.ui.SettingsScreen
import com.custom.dynamicisland.ui.theme.DynamicIslandTheme

class MainActivity : ComponentActivity() {

    private lateinit var settingsRepo: IslandSettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        settingsRepo = IslandSettingsRepository.getInstance(this)

        setContent {
            DynamicIslandTheme {
                MainSettingsApp(
                    settingsRepo = settingsRepo,
                    onRequestOverlayPermission = { requestOverlayPermission() },
                    onRequestNotificationPermission = { requestNotificationPermission() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // If enabled and permission granted, ensure background overlay is active
        val settings = settingsRepo.settings.value
        if (settings.isEnabled && Settings.canDrawOverlays(this)) {
            IslandOverlayService.start(this)
        }
    }

    private fun requestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun requestNotificationPermission() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
    }
}

@Composable
fun MainSettingsApp(
    settingsRepo: IslandSettingsRepository,
    onRequestOverlayPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    var hasOverlayPermission by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }
    var hasNotificationPermission by remember {
        mutableStateOf(
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
        )
    }

    // Refresh permission status
    LaunchedEffect(Unit) {
        hasOverlayPermission = Settings.canDrawOverlays(context)
        hasNotificationPermission = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }

    SettingsScreen(
        settingsRepo = settingsRepo,
        hasOverlayPermission = hasOverlayPermission,
        hasNotificationPermission = hasNotificationPermission,
        onRequestOverlayPermission = onRequestOverlayPermission,
        onRequestNotificationPermission = onRequestNotificationPermission,
        modifier = Modifier.fillMaxSize()
    )
}
