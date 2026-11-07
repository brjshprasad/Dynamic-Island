package com.custom.dynamicisland.service

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.custom.dynamicisland.MainActivity
import com.custom.dynamicisland.R
import com.custom.dynamicisland.model.IslandState
import com.custom.dynamicisland.receiver.SystemEventsReceiver
import com.custom.dynamicisland.repository.IslandSettingsRepository
import com.custom.dynamicisland.ui.DynamicIslandOverlayView
import com.custom.dynamicisland.ui.LockScreenIslandActivity
import com.custom.dynamicisland.ui.theme.DynamicIslandTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class IslandOverlayService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var windowManager: WindowManager
    private lateinit var settingsRepo: IslandSettingsRepository

    private var overlayView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var lifecycleOwner: ServiceLifecycleOwner? = null

    private var systemEventsReceiver: SystemEventsReceiver? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        settingsRepo = IslandSettingsRepository.getInstance(this)

        startForegroundNotification()
        registerSystemEvents()

        if (Settings.canDrawOverlays(this)) {
            setupOverlay()
        }

        // Listen for settings changes to re-position / re-size
        serviceScope.launch {
            settingsRepo.settings.collectLatest { settings ->
                if (!settings.isEnabled) {
                    removeOverlay()
                } else if (overlayView == null && Settings.canDrawOverlays(this@IslandOverlayService)) {
                    setupOverlay()
                } else {
                    updateWindowDimensions(isExpanded = false)
                }
            }
        }

        // Dynamically resize WindowManager window when Island expands or collapses
        IslandController.onLayoutExpansionChanged = { isExpanded ->
            updateWindowDimensions(isExpanded)
        }

        // Wake screen / display on lock screen when a turn/flyover/bifurcation maneuver warning triggers
        IslandController.onNavigationManeuverWarning = { _ ->
            val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            if (km != null && km.isKeyguardLocked && settingsRepo.settings.value.showOnLockScreen) {
                LockScreenIslandActivity.show(this@IslandOverlayService)
            }
        }
    }

    private fun startForegroundNotification() {
        val channelId = "dynamic_island_channel"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.island_service_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.island_service_desc)
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Dynamic Island PRO")
            .setContentText("Active over camera punch-hole")
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(2001, notification)
    }

    private fun setupOverlay() {
        if (overlayView != null) return

        val density = resources.displayMetrics.density
        val settings = settingsRepo.settings.value
        val state = IslandController.currentState.value

        val initialWidthDp = if (state is IslandState.Idle && settings.hideWhenIdle) 0 else settings.baseWidth
        val initialHeightDp = if (state is IslandState.Idle && settings.hideWhenIdle) 0 else settings.baseHeight

        val pillWidthPx = (initialWidthDp * density).toInt()
        val pillHeightPx = (initialHeightDp * density).toInt()
        val topOffsetPx = (settings.offsetY * density).toInt()
        val xOffsetPx = (settings.offsetX * density).toInt()

        val lockScreenFlag = if (settings.showOnLockScreen) {
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        } else {
            0
        }

        val params = WindowManager.LayoutParams(
            pillWidthPx,
            pillHeightPx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    lockScreenFlag,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = xOffsetPx
            y = topOffsetPx
        }
        layoutParams = params

        val owner = ServiceLifecycleOwner()
        lifecycleOwner = owner

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                val liveSettings by settingsRepo.settings.collectAsState()
                DynamicIslandTheme {
                    DynamicIslandOverlayView(settings = liveSettings)
                }
            }
        }

        try {
            windowManager.addView(composeView, params)
            overlayView = composeView
        } catch (_: Exception) {}
    }

    private fun updateWindowDimensions(isExpanded: Boolean) {
        val view = overlayView ?: return
        val params = layoutParams ?: return
        val density = resources.displayMetrics.density
        val settings = settingsRepo.settings.value

        val topOffsetPx = (settings.offsetY * density).toInt()
        val xOffsetPx = (settings.offsetX * density).toInt()

        val lockScreenFlag = if (settings.showOnLockScreen) {
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        } else {
            0
        }

        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                lockScreenFlag

        if (isExpanded) {
            params.width = (340 * density).toInt()
            params.height = (145 * density).toInt()
        } else {
            val state = IslandController.currentState.value
            val compactWidthDp = when (state) {
                is IslandState.Idle -> if (settings.hideWhenIdle) 0 else settings.baseWidth
                is IslandState.Media -> 230
                is IslandState.Navigation -> 230
                is IslandState.Notification -> 250
                is IslandState.Call -> 240
                is IslandState.Bluetooth -> 235
                else -> settings.baseWidth.coerceAtLeast(210)
            }
            val compactHeightDp = when (state) {
                is IslandState.Idle -> if (settings.hideWhenIdle) 0 else settings.baseHeight
                else -> settings.baseHeight.coerceAtLeast(36)
            }
            params.width = (compactWidthDp * density).toInt()
            params.height = (compactHeightDp * density).toInt()
        }
        params.x = xOffsetPx
        params.y = topOffsetPx

        try {
            windowManager.updateViewLayout(view, params)
        } catch (_: Exception) {}
    }

    private fun removeOverlay() {
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (_: Exception) {}
        }
        overlayView = null
        lifecycleOwner?.destroy()
        lifecycleOwner = null
    }

    private fun registerSystemEvents() {
        systemEventsReceiver = SystemEventsReceiver()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(systemEventsReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        systemEventsReceiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) {}
        }
        removeOverlay()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, IslandOverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, IslandOverlayService::class.java))
        }
    }
}

private class ServiceLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    fun destroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}
