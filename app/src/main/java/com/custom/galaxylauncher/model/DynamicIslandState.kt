package com.custom.galaxylauncher.model

sealed class DynamicIslandState {
    // Standard resting state surrounding the punch-hole camera
    object Idle : DynamicIslandState()

    // Flashlight toggled via chop-chop
    data class Flashlight(val isOn: Boolean) : DynamicIslandState()

    // Camera launched via double wrist twist
    data class CameraLaunched(val message: String = "Camera Activated") : DynamicIslandState()

    // Charging connected state
    data class BatteryCharging(val level: Int, val isCharging: Boolean) : DynamicIslandState()

    // Ringer mode switch (Silent, Vibrate, Normal)
    data class RingerMode(val mode: String) : DynamicIslandState()

    // Media playback state
    data class Media(
        val title: String,
        val artist: String,
        val isPlaying: Boolean
    ) : DynamicIslandState()

    // User expanded state
    data class Expanded(
        val title: String,
        val subtitle: String,
        val primaryActionLabel: String? = null,
        val onPrimaryAction: (() -> Unit)? = null
    ) : DynamicIslandState()
}
