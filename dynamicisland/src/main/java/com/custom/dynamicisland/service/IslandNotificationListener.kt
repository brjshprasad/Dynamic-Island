package com.custom.dynamicisland.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.custom.dynamicisland.model.IslandState
import com.custom.dynamicisland.model.ManeuverType
import com.custom.dynamicisland.repository.IslandSettingsRepository

class IslandNotificationListener : NotificationListenerService() {

    private lateinit var settingsRepo: IslandSettingsRepository
    private var mediaSessionManager: MediaSessionManager? = null
    private var activeMediaController: MediaController? = null

    private val mediaCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            updateMediaState()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            updateMediaState()
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        settingsRepo = IslandSettingsRepository.getInstance(this)
        setupMediaSessionListener()
    }

    override fun onDestroy() {
        super.onDestroy()
        activeMediaController?.unregisterCallback(mediaCallback)
        activeMediaController = null
        if (instance == this) instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val settings = settingsRepo.settings.value
        if (!settings.isEnabled || !settings.enableNotifications) return

        // Skip our own notifications
        val packageName = sbn.packageName
        if (packageName == applicationContext.packageName) return

        val notification = sbn.notification ?: return
        // Eliminate ghost notifications: ignore group summaries and silent sync updates
        if ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return

        val category = notification.category
        val isCall = (category == Notification.CATEGORY_CALL) ||
                packageName.contains("incallui") ||
                packageName.contains("dialer") ||
                packageName.contains("telecom") ||
                packageName.contains("phone")

        val isNavApp = isNavigationNotification(sbn, packageName)
        if (sbn.isOngoing && !isCall && !isNavApp) return

        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        if (title.isBlank() && text.isBlank() && bigText.isBlank()) return

        if (isCall) {
            val caller = title.ifBlank { text.ifBlank { "Incoming Call" } }
            IslandController.showState(
                IslandState.Call(callerName = caller, isIncoming = true),
                autoDismissMs = 6000L
            )
            return
        }

        val appName = try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            packageName
        }

        if (isNavApp && settings.enableNavigation) {
            handleNavigationNotification(sbn, appName, packageName, title, text, subText, bigText)
            return
        }

        val contentIntent = sbn.notification.contentIntent

        IslandController.showState(
            IslandState.Notification(
                appName = appName,
                packageName = packageName,
                title = title.ifBlank { appName },
                message = text.ifBlank { bigText },
                notificationKey = sbn.key,
                contentIntent = contentIntent
            ),
            autoDismissMs = 4500L
        )
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return
        val packageName = sbn.packageName
        val key = sbn.key

        if (isNavigationNotification(sbn, packageName)) {
            // Navigation ended or closed -> clear navigation imprint immediately!
            IslandController.clearNavigation()
        } else if (packageName == activeMediaController?.packageName) {
            // Media player notification swiped away / closed -> clear media imprint!
            IslandController.clearMedia()
        } else {
            val category = sbn.notification?.category
            val isCall = (category == Notification.CATEGORY_CALL) ||
                    packageName.contains("incallui") ||
                    packageName.contains("dialer") ||
                    packageName.contains("telecom") ||
                    packageName.contains("phone")
            if (isCall) {
                IslandController.dismissCall()
            } else {
                // Eliminate ghost notification: removed immediately from island!
                IslandController.dismissNotificationByKey(key, packageName)
            }
        }
    }

    private fun isNavigationNotification(sbn: StatusBarNotification, packageName: String): Boolean {
        val category = sbn.notification?.category
        val hasNavCategory = (category == Notification.CATEGORY_NAVIGATION)
        val isNavPackage = packageName == "com.google.android.apps.maps" ||
                packageName == "com.mmi.maps" ||
                packageName == "com.mapmyindia.move" ||
                packageName.contains("mapmyindia") ||
                packageName == "com.waze" ||
                packageName.contains("navigation") ||
                packageName.contains("osmand") ||
                packageName.contains("here.app.maps") ||
                packageName.contains("sygic") ||
                packageName.contains("mapple") ||
                packageName.contains("maps")
        return hasNavCategory || isNavPackage
    }

    private fun classifyManeuver(combined: String): ManeuverType {
        val lower = combined.lowercase()
        return when {
            lower.contains("flyover") || lower.contains("overpass") || lower.contains("elevated") || lower.contains("ramp") -> ManeuverType.FLYOVER
            lower.contains("bifurcation") || lower.contains("fork") || lower.contains("split") || lower.contains("bear left") || lower.contains("bear right") || lower.contains("stay on") -> ManeuverType.BIFURCATION
            lower.contains("u-turn") || lower.contains("uturn") -> ManeuverType.U_TURN
            lower.contains("roundabout") || lower.contains("rotary") || lower.contains("circle") -> ManeuverType.ROUNDABOUT
            lower.contains("sharp left") -> ManeuverType.SHARP_LEFT
            lower.contains("sharp right") -> ManeuverType.SHARP_RIGHT
            lower.contains("slight left") -> ManeuverType.SLIGHT_LEFT
            lower.contains("slight right") -> ManeuverType.SLIGHT_RIGHT
            lower.contains("turn left") || lower.contains("left") -> ManeuverType.TURN_LEFT
            lower.contains("turn right") || lower.contains("right") -> ManeuverType.TURN_RIGHT
            lower.contains("arrived") || lower.contains("destination") || lower.contains("reached") -> ManeuverType.DESTINATION
            else -> ManeuverType.STRAIGHT
        }
    }

    private fun handleNavigationNotification(
        sbn: StatusBarNotification,
        appName: String,
        packageName: String,
        title: String,
        text: String,
        subText: String,
        bigText: String
    ) {
        val combined = "$title $text $bigText $subText"

        val distRegex = Regex("""(?i)\b(?:in\s+)?(\d+(?:\.\d+)?\s*(?:m|km|mi|ft|meters|kilometers|miles|feet))\b""")
        val match = distRegex.find(combined)
        val distance = match?.groupValues?.get(1)?.trim() ?: "Now"

        val instruction = when {
            text.contains("turn", ignoreCase = true) ||
            text.contains("flyover", ignoreCase = true) ||
            text.contains("fork", ignoreCase = true) ||
            text.contains("bifurcation", ignoreCase = true) -> {
                text.replace(Regex("""(?i)^in\s+\d+(?:\.\d+)?\s*(?:m|km|mi|ft)\s*[-–—,:]?\s*"""), "").trim()
            }
            title.isNotBlank() -> {
                title.replace(Regex("""(?i)^in\s+\d+(?:\.\d+)?\s*(?:m|km|mi|ft)\s*[-–—,:]?\s*"""), "").trim()
            }
            bigText.isNotBlank() -> bigText
            else -> text
        }.ifBlank { "Proceed along route" }

        val maneuverType = classifyManeuver(combined)
        val etaInfo = subText.ifBlank {
            if (combined.contains("ETA") || combined.contains("min") || combined.contains("km")) {
                subText.ifBlank { if (text.contains("min") || text.contains("km")) text else "" }
            } else ""
        }

        val contentIntent = sbn.notification.contentIntent

        // Automatically enlarges for 4 seconds at every turn, flyover, bifurcation warning
        IslandController.updateNavigation(
            IslandState.Navigation(
                appName = if (appName.isNotBlank() && appName != packageName) appName else "Maps",
                packageName = packageName,
                instruction = instruction,
                distance = distance,
                etaInfo = etaInfo,
                maneuverType = maneuverType,
                isEnlarged = true,
                contentIntent = contentIntent
            ),
            autoShrinkMs = 4000L
        )
    }

    private fun setupMediaSessionListener() {
        try {
            mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val component = ComponentName(this, IslandNotificationListener::class.java)

            mediaSessionManager?.addOnActiveSessionsChangedListener({ controllers ->
                handleActiveSessions(controllers)
            }, component)

            val initialControllers = mediaSessionManager?.getActiveSessions(component)
            handleActiveSessions(initialControllers)
        } catch (_: Exception) {}
    }

    private fun handleActiveSessions(controllers: List<MediaController>?) {
        val settings = settingsRepo.settings.value
        if (!settings.enableMedia) return

        activeMediaController?.unregisterCallback(mediaCallback)
        activeMediaController = controllers?.firstOrNull()

        activeMediaController?.let { controller ->
            controller.registerCallback(mediaCallback)
            updateMediaState()
        } ?: run {
            IslandController.clearMedia()
        }
    }

    private fun updateMediaState() {
        val settings = settingsRepo.settings.value
        if (!settings.isEnabled || !settings.enableMedia) {
            IslandController.clearMedia()
            return
        }

        val controller = activeMediaController ?: run {
            IslandController.clearMedia()
            return
        }

        val metadata = controller.metadata
        val playbackState = controller.playbackState
        val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
        val isStopped = (playbackState == null) ||
                (playbackState.state == PlaybackState.STATE_STOPPED) ||
                (playbackState.state == PlaybackState.STATE_NONE) ||
                (playbackState.state == PlaybackState.STATE_ERROR)

        if (isStopped) {
            IslandController.clearMedia()
            return
        }

        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: ""

        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_AUTHOR)
            ?: ""

        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val position = playbackState.position

        if (title.isNotBlank() && isPlaying) {
            val currentState = IslandController.currentState.value
            val isExpanded = (currentState as? IslandState.Media)?.isExpanded ?: false

            IslandController.updateMedia(
                IslandState.Media(
                    title = title,
                    artist = artist.ifBlank { "Media Player" },
                    isPlaying = isPlaying,
                    packageName = controller.packageName ?: "",
                    durationMs = duration,
                    positionMs = position,
                    isExpanded = isExpanded
                )
            )
        } else if (!isPlaying) {
            // App closed or music stopped: clear imprint from island
            IslandController.clearMedia()
        }
    }

    fun sendMediaPlayPause() {
        val controller = activeMediaController ?: return
        val isPlaying = controller.playbackState?.state == PlaybackState.STATE_PLAYING
        if (isPlaying) {
            controller.transportControls?.pause()
        } else {
            controller.transportControls?.play()
        }
    }

    fun sendMediaNext() {
        activeMediaController?.transportControls?.skipToNext()
    }

    fun sendMediaPrevious() {
        activeMediaController?.transportControls?.skipToPrevious()
    }

    companion object {
        var instance: IslandNotificationListener? = null
            private set
    }
}
