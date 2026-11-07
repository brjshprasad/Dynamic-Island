package com.custom.galaxylauncher.gesture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.NotificationCompat
import com.custom.galaxylauncher.MainActivity
import com.custom.galaxylauncher.R
import com.custom.galaxylauncher.dynamicisland.DynamicIslandController
import com.custom.galaxylauncher.model.DynamicIslandState
import com.custom.galaxylauncher.repository.FlashlightManager
import com.custom.galaxylauncher.repository.GestureSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LiveSensorData(
    val accelX: Float = 0f, val accelY: Float = 0f, val accelZ: Float = 0f,
    val gyroX: Float = 0f, val gyroY: Float = 0f, val gyroZ: Float = 0f
)

class MotionGestureService : Service(), SensorEventListener {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private lateinit var sensorManager: SensorManager
    private var accelSensor: Sensor? = null
    private var gyroSensor: Sensor? = null

    private lateinit var settingsRepo: GestureSettingsRepository
    private lateinit var flashlightManager: FlashlightManager

    private var currentAccelX = 0f
    private var currentAccelY = 0f
    private var currentAccelZ = 0f

    private val chopDetector = ChopChopDetector {
        handleChopChop()
    }

    private val wristTwistDetector = WristTwistDetector {
        handleWristTwist()
    }

    override fun onCreate() {
        super.onCreate()
        settingsRepo = GestureSettingsRepository.getInstance(this)
        flashlightManager = FlashlightManager(this)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        startForegroundServiceNotification()
        registerSensors()
    }

    private fun startForegroundServiceNotification() {
        val channelId = "galaxy_gestures_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.motion_service_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.motion_service_notification_text)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.motion_service_notification_title))
            .setContentText(getString(R.string.motion_service_notification_text))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        startForeground(1001, notification)
    }

    private fun registerSensors() {
        accelSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        gyroSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                currentAccelX = event.values[0]
                currentAccelY = event.values[1]
                currentAccelZ = event.values[2]
            }
            Sensor.TYPE_GYROSCOPE -> {
                val gx = event.values[0]
                val gy = event.values[1]
                val gz = event.values[2]

                // Push to live sensor observer for settings screen fine-tuning
                _liveSensorData.value = LiveSensorData(
                    accelX = currentAccelX, accelY = currentAccelY, accelZ = currentAccelZ,
                    gyroX = gx, gyroY = gy, gyroZ = gz
                )

                val settings = settingsRepo.settings.value

                // Feed to chop-chop detector with event timestamp
                chopDetector.onSensorData(
                    currentAccelX, currentAccelY, currentAccelZ,
                    gx, gy, gz,
                    settings,
                    event.timestamp
                )

                // Feed to wrist twist detector with event timestamp
                wristTwistDetector.onSensorData(
                    gx, gy, gz,
                    settings,
                    event.timestamp
                )
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun handleChopChop() {
        serviceScope.launch {
            vibrate(longArrayOf(0, 70, 50, 70))
            flashlightManager.toggleTorch()
            val isOn = flashlightManager.isTorchOn.value
            DynamicIslandController.showState(DynamicIslandState.Flashlight(isOn), 3500L)
            Log.d("MotionGestureService", "Chop-Chop triggered! Flashlight toggled to: $isOn")
        }
    }

    private fun handleWristTwist() {
        serviceScope.launch {
            vibrate(longArrayOf(0, 100, 60, 120))
            DynamicIslandController.showState(DynamicIslandState.CameraLaunched(), 3500L)
            val cameraIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                startActivity(cameraIntent)
            } catch (e: Exception) {
                Log.e("MotionGestureService", "Failed to launch camera intent", e)
            }
            Log.d("MotionGestureService", "Double Wrist Twist triggered! Camera launched.")
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrate(pattern: LongArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    vibrator.vibrate(pattern, -1)
                }
            }
        } catch (e: Exception) {
            // Ignore if vibration fails
        }
    }

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private val _liveSensorData = MutableStateFlow(LiveSensorData())
        val liveSensorData = _liveSensorData.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, MotionGestureService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MotionGestureService::class.java))
        }
    }
}
