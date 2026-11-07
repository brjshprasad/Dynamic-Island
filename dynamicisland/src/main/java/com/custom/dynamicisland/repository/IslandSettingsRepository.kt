package com.custom.dynamicisland.repository

import android.content.Context
import android.content.SharedPreferences
import com.custom.dynamicisland.model.IslandSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class IslandSettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<IslandSettings> = _settings.asStateFlow()

    private fun loadSettings(): IslandSettings {
        return IslandSettings(
            isEnabled = prefs.getBoolean(KEY_ISLAND_ENABLED, true),
            offsetY = prefs.getInt(KEY_OFFSET_Y, 12),
            offsetX = prefs.getInt(KEY_OFFSET_X, 0),
            baseWidth = prefs.getInt(KEY_BASE_WIDTH, 115),
            baseHeight = prefs.getInt(KEY_BASE_HEIGHT, 34),
            cornerRadius = prefs.getInt(KEY_CORNER_RADIUS, 36),
            backgroundOpacity = prefs.getFloat(KEY_BG_OPACITY, 0.78f),
            showOnLockScreen = prefs.getBoolean(KEY_SHOW_ON_LOCK, true),
            hideWhenIdle = prefs.getBoolean(KEY_HIDE_WHEN_IDLE, true),
            enableMedia = prefs.getBoolean(KEY_ENABLE_MEDIA, true),
            enableNavigation = prefs.getBoolean(KEY_ENABLE_NAVIGATION, true),
            enableNotifications = prefs.getBoolean(KEY_ENABLE_NOTIFICATIONS, true),
            enableCharging = prefs.getBoolean(KEY_ENABLE_CHARGING, true),
            enableRinger = prefs.getBoolean(KEY_ENABLE_RINGER, true),
            enableBluetooth = prefs.getBoolean(KEY_ENABLE_BLUETOOTH, true),
            enableHaptics = prefs.getBoolean(KEY_ENABLE_HAPTICS, true)
        )
    }

    fun updateSettings(newSettings: IslandSettings) {
        prefs.edit()
            .putBoolean(KEY_ISLAND_ENABLED, newSettings.isEnabled)
            .putInt(KEY_OFFSET_Y, newSettings.offsetY)
            .putInt(KEY_OFFSET_X, newSettings.offsetX)
            .putInt(KEY_BASE_WIDTH, newSettings.baseWidth)
            .putInt(KEY_BASE_HEIGHT, newSettings.baseHeight)
            .putInt(KEY_CORNER_RADIUS, newSettings.cornerRadius)
            .putFloat(KEY_BG_OPACITY, newSettings.backgroundOpacity)
            .putBoolean(KEY_SHOW_ON_LOCK, newSettings.showOnLockScreen)
            .putBoolean(KEY_HIDE_WHEN_IDLE, newSettings.hideWhenIdle)
            .putBoolean(KEY_ENABLE_MEDIA, newSettings.enableMedia)
            .putBoolean(KEY_ENABLE_NAVIGATION, newSettings.enableNavigation)
            .putBoolean(KEY_ENABLE_NOTIFICATIONS, newSettings.enableNotifications)
            .putBoolean(KEY_ENABLE_CHARGING, newSettings.enableCharging)
            .putBoolean(KEY_ENABLE_RINGER, newSettings.enableRinger)
            .putBoolean(KEY_ENABLE_BLUETOOTH, newSettings.enableBluetooth)
            .putBoolean(KEY_ENABLE_HAPTICS, newSettings.enableHaptics)
            .apply()
        _settings.value = newSettings
    }

    companion object {
        private const val PREFS_NAME = "dynamic_island_pro_clean_prefs"
        private const val KEY_ISLAND_ENABLED = "island_enabled"
        private const val KEY_OFFSET_Y = "island_offset_y"
        private const val KEY_OFFSET_X = "island_offset_x"
        private const val KEY_BASE_WIDTH = "island_base_width"
        private const val KEY_BASE_HEIGHT = "island_base_height"
        private const val KEY_CORNER_RADIUS = "island_corner_radius"
        private const val KEY_BG_OPACITY = "island_bg_opacity"
        private const val KEY_SHOW_ON_LOCK = "island_show_on_lock"
        private const val KEY_HIDE_WHEN_IDLE = "island_hide_when_idle"
        private const val KEY_ENABLE_MEDIA = "enable_media"
        private const val KEY_ENABLE_NAVIGATION = "enable_navigation"
        private const val KEY_ENABLE_NOTIFICATIONS = "enable_notifications"
        private const val KEY_ENABLE_CHARGING = "enable_charging"
        private const val KEY_ENABLE_RINGER = "enable_ringer"
        private const val KEY_ENABLE_BLUETOOTH = "enable_bluetooth"
        private const val KEY_ENABLE_HAPTICS = "enable_haptics"

        @Volatile
        private var instance: IslandSettingsRepository? = null

        fun getInstance(context: Context): IslandSettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: IslandSettingsRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
