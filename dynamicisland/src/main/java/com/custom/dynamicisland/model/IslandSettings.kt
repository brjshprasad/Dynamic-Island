package com.custom.dynamicisland.model

data class IslandSettings(
    val isEnabled: Boolean = true,
    // Camera Cutout Alignment (defaults calibrated for Samsung Galaxy F62 center punch-hole)
    val offsetY: Int = 12,       // Vertical distance from top of screen in dp
    val offsetX: Int = 0,        // Horizontal offset from center in dp
    val baseWidth: Int = 115,    // Resting pill width in dp
    val baseHeight: Int = 34,    // Resting pill height in dp
    val cornerRadius: Int = 36,  // Pill capsule corner radius in dp
    val backgroundOpacity: Float = 0.78f, // Translucency for enlarged island (0.5 to 0.95, default 78%)
    val showOnLockScreen: Boolean = true, // Accessible on lock screen without unlocking
    val hideWhenIdle: Boolean = true,     // Island disappears when no eligible app is running, reappears when opened
    // Feature Toggles (All unlocked PRO features, 0 paywalls)
    val enableMedia: Boolean = true,
    val enableNavigation: Boolean = true,
    val enableNotifications: Boolean = true,
    val enableCharging: Boolean = true,
    val enableRinger: Boolean = true,
    val enableBluetooth: Boolean = true,
    val enableHaptics: Boolean = true
)
