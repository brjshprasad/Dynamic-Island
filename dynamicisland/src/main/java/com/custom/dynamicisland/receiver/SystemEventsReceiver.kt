package com.custom.dynamicisland.receiver

import android.app.KeyguardManager
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.BatteryManager
import com.custom.dynamicisland.model.IslandState
import com.custom.dynamicisland.repository.IslandSettingsRepository
import com.custom.dynamicisland.service.IslandController
import com.custom.dynamicisland.ui.LockScreenIslandActivity

class SystemEventsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        intent ?: return

        val settingsRepo = IslandSettingsRepository.getInstance(context)
        val settings = settingsRepo.settings.value
        if (!settings.isEnabled) return

        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                if (!settings.enableCharging) return
                val batteryStatus = context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                val pct = if (level >= 0 && scale > 0) (level * 100) / scale else 80

                IslandController.showState(
                    IslandState.Battery(level = pct, isCharging = true, isPlugged = true),
                    autoDismissMs = 3800L
                )
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                if (!settings.enableCharging) return
                IslandController.dismiss()
            }
            Intent.ACTION_SCREEN_ON -> {
                if (settings.showOnLockScreen) {
                    val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                    if (km != null && km.isKeyguardLocked) {
                        LockScreenIslandActivity.show(context)
                    }
                }
            }
            Intent.ACTION_USER_PRESENT -> {
                // Device successfully unlocked: dismiss lockscreen activity and let overlay service handle
                LockScreenIslandActivity.dismiss()
            }
            AudioManager.RINGER_MODE_CHANGED_ACTION -> {
                if (!settings.enableRinger) return
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                val mode = when (audioManager?.ringerMode) {
                    AudioManager.RINGER_MODE_SILENT -> "Silent"
                    AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
                    AudioManager.RINGER_MODE_NORMAL -> "Ring"
                    else -> "Sound"
                }
                IslandController.showState(
                    IslandState.Ringer(mode = mode),
                    autoDismissMs = 2800L
                )
            }
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                if (!settings.enableBluetooth) return
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val deviceName = try {
                    device?.name ?: "Bluetooth Device"
                } catch (_: SecurityException) {
                    "Bluetooth Audio"
                }
                IslandController.showState(
                    IslandState.Bluetooth(deviceName = deviceName, isConnected = true),
                    autoDismissMs = 3500L
                )
            }
        }
    }
}
