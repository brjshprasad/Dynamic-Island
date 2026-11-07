package com.custom.dynamicisland.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.custom.dynamicisland.repository.IslandSettingsRepository
import com.custom.dynamicisland.service.IslandOverlayService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED || intent?.action == "android.intent.action.QUICKBOOT_POWERON") {
            val settings = IslandSettingsRepository.getInstance(context).settings.value
            if (settings.isEnabled) {
                IslandOverlayService.start(context)
            }
        }
    }
}
