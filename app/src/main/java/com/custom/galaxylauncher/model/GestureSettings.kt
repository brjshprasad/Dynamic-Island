package com.custom.galaxylauncher.model

data class GestureSettings(
    val chopEnabled: Boolean = true,
    val twistEnabled: Boolean = true,
    // Gyroscope sensitivities in all three planes (multipliers between 0.2x and 3.0x)
    val gyroSensitivityX: Float = 1.0f, // Pitch axis (tilting forward/backward)
    val gyroSensitivityY: Float = 1.0f, // Roll axis (twisting side to side)
    val gyroSensitivityZ: Float = 1.0f, // Yaw axis (rotation flat)
    // Motion thresholds and arc sensitivity
    val chopAccelThreshold: Float = 14.0f,
    val chopMinArcDegrees: Float = 45.0f,   // Minimum arc distance (deg) twice in quick succession (default 45°)
    val twistMinArcDegrees: Float = 60.0f,  // Minimum arc distance (deg) for wrist twist (default 60°)
    val twistAngularThreshold: Float = 3.5f,
    // Dynamic Island Camera Alignment Offsets (in dp)
    val islandOffsetY: Int = 0,
    val islandOffsetX: Int = 0,
    val islandBaseWidth: Int = 115,
    val islandBaseHeight: Int = 34
)
