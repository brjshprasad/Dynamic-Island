package com.custom.galaxylauncher.repository

import android.content.Context
import android.content.SharedPreferences
import com.custom.galaxylauncher.model.GestureSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GestureSettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<GestureSettings> = _settings.asStateFlow()

    private fun loadSettings(): GestureSettings {
        return GestureSettings(
            chopEnabled = prefs.getBoolean(KEY_CHOP_ENABLED, true),
            twistEnabled = prefs.getBoolean(KEY_TWIST_ENABLED, true),
            gyroSensitivityX = prefs.getFloat(KEY_GYRO_X, 1.0f),
            gyroSensitivityY = prefs.getFloat(KEY_GYRO_Y, 1.0f),
            gyroSensitivityZ = prefs.getFloat(KEY_GYRO_Z, 1.0f),
            chopAccelThreshold = prefs.getFloat(KEY_CHOP_THRESH, 14.0f),
            chopMinArcDegrees = prefs.getFloat(KEY_CHOP_MIN_ARC, 45.0f),
            twistMinArcDegrees = prefs.getFloat(KEY_TWIST_MIN_ARC, 60.0f),
            twistAngularThreshold = prefs.getFloat(KEY_TWIST_THRESH, 3.5f),
            islandOffsetY = prefs.getInt(KEY_ISLAND_OFFSET_Y, 0),
            islandOffsetX = prefs.getInt(KEY_ISLAND_OFFSET_X, 0),
            islandBaseWidth = prefs.getInt(KEY_ISLAND_BASE_WIDTH, 115),
            islandBaseHeight = prefs.getInt(KEY_ISLAND_BASE_HEIGHT, 34)
        )
    }

    fun updateSettings(newSettings: GestureSettings) {
        prefs.edit()
            .putBoolean(KEY_CHOP_ENABLED, newSettings.chopEnabled)
            .putBoolean(KEY_TWIST_ENABLED, newSettings.twistEnabled)
            .putFloat(KEY_GYRO_X, newSettings.gyroSensitivityX)
            .putFloat(KEY_GYRO_Y, newSettings.gyroSensitivityY)
            .putFloat(KEY_GYRO_Z, newSettings.gyroSensitivityZ)
            .putFloat(KEY_CHOP_THRESH, newSettings.chopAccelThreshold)
            .putFloat(KEY_CHOP_MIN_ARC, newSettings.chopMinArcDegrees)
            .putFloat(KEY_TWIST_MIN_ARC, newSettings.twistMinArcDegrees)
            .putFloat(KEY_TWIST_THRESH, newSettings.twistAngularThreshold)
            .putInt(KEY_ISLAND_OFFSET_Y, newSettings.islandOffsetY)
            .putInt(KEY_ISLAND_OFFSET_X, newSettings.islandOffsetX)
            .putInt(KEY_ISLAND_BASE_WIDTH, newSettings.islandBaseWidth)
            .putInt(KEY_ISLAND_BASE_HEIGHT, newSettings.islandBaseHeight)
            .apply()
        _settings.value = newSettings
    }

    fun updateGyroSensitivities(x: Float, y: Float, z: Float) {
        val current = _settings.value
        updateSettings(current.copy(gyroSensitivityX = x, gyroSensitivityY = y, gyroSensitivityZ = z))
    }

    fun updateIslandOffsets(offsetX: Int, offsetY: Int) {
        val current = _settings.value
        updateSettings(current.copy(islandOffsetX = offsetX, islandOffsetY = offsetY))
    }

    companion object {
        private const val PREFS_NAME = "galaxy_launcher_prefs"
        private const val KEY_CHOP_ENABLED = "chop_enabled"
        private const val KEY_TWIST_ENABLED = "twist_enabled"
        private const val KEY_GYRO_X = "gyro_x"
        private const val KEY_GYRO_Y = "gyro_y"
        private const val KEY_GYRO_Z = "gyro_z"
        private const val KEY_CHOP_THRESH = "chop_threshold"
        private const val KEY_CHOP_MIN_ARC = "chop_min_arc_deg"
        private const val KEY_TWIST_MIN_ARC = "twist_min_arc_deg"
        private const val KEY_TWIST_THRESH = "twist_threshold"
        private const val KEY_ISLAND_OFFSET_Y = "island_offset_y"
        private const val KEY_ISLAND_OFFSET_X = "island_offset_x"
        private const val KEY_ISLAND_BASE_WIDTH = "island_base_width"
        private const val KEY_ISLAND_BASE_HEIGHT = "island_base_height"

        @Volatile
        private var instance: GestureSettingsRepository? = null

        fun getInstance(context: Context): GestureSettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: GestureSettingsRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
