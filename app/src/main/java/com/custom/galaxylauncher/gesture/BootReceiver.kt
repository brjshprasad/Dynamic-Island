package com.custom.galaxylauncher.gesture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.custom.galaxylauncher.repository.GestureSettingsRepository

class BootReceiver : BroadcastReceiver {
    constructor() : super()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val settingsRepo = GestureSettingsRepository.getInstance(context)
            val settings = settingsRepo.settings.value
            if (settings.chopEnabled || settings.twistEnabled) {
                MotionGestureService.start(context)
            }
        }
    }
}
