package com.custom.galaxylauncher.dynamicisland

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.BatteryManager
import com.custom.galaxylauncher.model.DynamicIslandState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object DynamicIslandController {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var revertJob: Job? = null

    private val _currentState = MutableStateFlow<DynamicIslandState>(DynamicIslandState.Idle)
    val currentState: StateFlow<DynamicIslandState> = _currentState.asStateFlow()

    private var isReceiverRegistered = false

    fun initSystemListeners(context: Context) {
        if (isReceiverRegistered) return
        isReceiverRegistered = true

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
        }

        val receiver = object : BroadcastReceiver() {
            private var lastPlugged = false

            override fun onReceive(ctx: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_POWER_CONNECTED -> {
                        val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                        showState(DynamicIslandState.BatteryCharging(level, isCharging = true), 4000L)
                        lastPlugged = true
                    }
                    Intent.ACTION_BATTERY_CHANGED -> {
                        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                status == BatteryManager.BATTERY_STATUS_FULL
                        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                        val pct = if (scale > 0) (level * 100) / scale else level

                        if (isCharging && !lastPlugged) {
                            showState(DynamicIslandState.BatteryCharging(pct, isCharging = true), 4000L)
                            lastPlugged = true
                        } else if (!isCharging) {
                            lastPlugged = false
                        }
                    }
                    AudioManager.RINGER_MODE_CHANGED_ACTION -> {
                        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        val modeStr = when (am.ringerMode) {
                            AudioManager.RINGER_MODE_SILENT -> "Silent"
                            AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
                            else -> "Ring"
                        }
                        showState(DynamicIslandState.RingerMode(modeStr), 3000L)
                    }
                }
            }
        }

        try {
            context.applicationContext.registerReceiver(receiver, filter)
        } catch (_: Exception) {}
    }

    fun showState(state: DynamicIslandState, autoDismissMs: Long = 3000L) {
        revertJob?.cancel()
        _currentState.value = state

        if (autoDismissMs > 0L && state !is DynamicIslandState.Idle) {
            revertJob = scope.launch {
                delay(autoDismissMs)
                _currentState.value = DynamicIslandState.Idle
            }
        }
    }

    fun dismiss() {
        revertJob?.cancel()
        _currentState.value = DynamicIslandState.Idle
    }
}
