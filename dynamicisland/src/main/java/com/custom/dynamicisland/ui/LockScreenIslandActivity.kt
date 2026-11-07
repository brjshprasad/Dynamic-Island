package com.custom.dynamicisland.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.custom.dynamicisland.repository.IslandSettingsRepository
import com.custom.dynamicisland.service.IslandController
import com.custom.dynamicisland.ui.theme.DynamicIslandTheme

/**
 * Transparent Lock Screen Activity.
 *
 * Employs official Android API setShowWhenLocked(true) and setTurnScreenOn(true)
 * to render the Dynamic Island natively over Samsung One UI Keyguard/Lock Screen.
 * Automatically wakes screen and enlarges at navigation maneuvers (turns, flyovers, bifurcations).
 */
class LockScreenIslandActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        window.addFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        )

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val settingsRepo = IslandSettingsRepository.getInstance(this)

        setContent {
            val settings by settingsRepo.settings.collectAsState()

            DynamicIslandTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent),
                    contentAlignment = Alignment.TopCenter
                ) {
                    DynamicIslandOverlayView(
                        settings = settings,
                        modifier = Modifier.padding(
                            top = settings.offsetY.dp,
                            start = if (settings.offsetX > 0) settings.offsetX.dp else 0.dp,
                            end = if (settings.offsetX < 0) (-settings.offsetX).dp else 0.dp
                        )
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkUnlockedDismiss()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            checkUnlockedDismiss()
        }
    }

    private fun checkUnlockedDismiss() {
        val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (km != null && !km.isKeyguardLocked) {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) instance = null
    }

    companion object {
        var instance: LockScreenIslandActivity? = null
            private set

        fun show(context: Context) {
            val settingsRepo = IslandSettingsRepository.getInstance(context)
            if (!settingsRepo.settings.value.isEnabled || !settingsRepo.settings.value.showOnLockScreen) {
                return
            }

            val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            if (km != null && !km.isKeyguardLocked) {
                return // Device is already unlocked, overlay service handles it
            }

            try {
                val intent = Intent(context, LockScreenIslandActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    )
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }

        fun dismiss() {
            instance?.finish()
            instance = null
        }
    }
}
