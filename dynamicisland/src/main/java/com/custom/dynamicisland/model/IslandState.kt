package com.custom.dynamicisland.model

import android.app.PendingIntent

sealed class IslandState {
    // Resting state framing the camera hole
    data object Idle : IslandState()

    // Music / Media Playback
    data class Media(
        val title: String,
        val artist: String,
        val isPlaying: Boolean,
        val packageName: String,
        val durationMs: Long = 0L,
        val positionMs: Long = 0L,
        val isExpanded: Boolean = false
    ) : IslandState()

    // Incoming Notifications (WhatsApp, Messages, Telegram, etc.)
    data class Notification(
        val appName: String,
        val packageName: String,
        val title: String,
        val message: String,
        val notificationKey: String = "",
        val contentIntent: PendingIntent? = null
    ) : IslandState()

    // Incoming or Active Phone Call
    data class Call(
        val callerName: String,
        val isIncoming: Boolean = true
    ) : IslandState()

    // Turn-by-Turn Navigation (Google Maps, Mapple, Waze, OsmAnd, etc.)
    data class Navigation(
        val appName: String,
        val packageName: String,
        val instruction: String,
        val distance: String,
        val etaInfo: String = "",
        val maneuverType: ManeuverType = ManeuverType.STRAIGHT,
        val isEnlarged: Boolean = false,
        val contentIntent: PendingIntent? = null
    ) : IslandState()

    // Battery / Charging Status
    data class Battery(
        val level: Int,
        val isCharging: Boolean,
        val isPlugged: Boolean
    ) : IslandState()

    // Ringer & Audio Mode (Silent, Vibrate, Normal)
    data class Ringer(
        val mode: String
    ) : IslandState()

    // Bluetooth Audio Device Connected
    data class Bluetooth(
        val deviceName: String,
        val isConnected: Boolean
    ) : IslandState()

    // Expanded Quick Controls Dashboard
    data class QuickControls(
        val title: String = "Dynamic Island PRO",
        val subtitle: String = "Quick Controls"
    ) : IslandState()
}

enum class ManeuverType {
    TURN_LEFT,
    TURN_RIGHT,
    SLIGHT_LEFT,
    SLIGHT_RIGHT,
    SHARP_LEFT,
    SHARP_RIGHT,
    U_TURN,
    FLYOVER,        // Flyover / Elevated roadway ramp
    BIFURCATION,    // Bifurcation / Fork / Split
    ROUNDABOUT,
    STRAIGHT,
    DESTINATION
}
