package com.custom.galaxylauncher.gesture

import com.custom.galaxylauncher.model.GestureSettings
import kotlin.math.sqrt

/**
 * High-precision chop-chop gesture detector.
 *
 * Requirements:
 * - Requires at least a 45-degree arc distance (or settings.chopMinArcDegrees).
 * - Must occur twice in quick succession.
 * - Minor movements in pocket (small angles, gentle jostles, or isolated motions) are ignored.
 */
class ChopChopDetector(
    private val onChopChopDetected: () -> Unit
) {
    private var lastTimestampNs: Long = 0L
    private var lastTriggerTime: Long = 0L

    // Current stroke state
    private var isInStroke: Boolean = false
    private var strokeStartTimeMs: Long = 0L
    private var strokeArcDegrees: Float = 0f
    private var strokePeakAccel: Float = 0f

    // Double chop sequence tracking
    private var firstChopCompletedTimeMs: Long = 0L

    // Timing parameters for ergonomics and pocket rejection
    private val minStrokeDurationMs = 60L       // Rapid intentional chop swing minimum duration
    private val maxStrokeDurationMs = 450L      // Maximum duration of a single chop swing
    private val minBetweenChopsMs = 120L        // Debounce interval between the two chops
    private val maxBetweenChopsMs = 850L        // Quick succession window for the second chop
    private val triggerCooldownMs = 1200L       // Cooldown after triggering gesture

    fun onSensorData(
        accelX: Float, accelY: Float, accelZ: Float,
        gyroX: Float, gyroY: Float, gyroZ: Float,
        settings: GestureSettings,
        timestampNs: Long = System.nanoTime()
    ) {
        if (!settings.chopEnabled) return

        val nowMs = timestampNs / 1_000_000L
        if (lastTriggerTime > 0L && nowMs - lastTriggerTime < triggerCooldownMs) {
            lastTimestampNs = timestampNs
            return
        }

        // Calculate dt in seconds (clamped for safety)
        val dtSec = if (lastTimestampNs > 0L) {
            val deltaNs = timestampNs - lastTimestampNs
            (deltaNs.toDouble() / 1_000_000_000.0).toFloat().coerceIn(0.001f, 0.05f)
        } else {
            0.02f
        }
        lastTimestampNs = timestampNs

        // Gyro angular velocity scaled with 3-axis sensitivities
        val gx = gyroX * settings.gyroSensitivityX
        val gy = gyroY * settings.gyroSensitivityY
        val gz = gyroZ * settings.gyroSensitivityZ
        val gyroMagnitude = sqrt(gx * gx + gy * gy + gz * gz)

        // Accelerometer magnitude
        val accelMagnitude = sqrt(accelX * accelX + accelY * accelY + accelZ * accelZ)

        // Expire first chop if window has passed
        if (firstChopCompletedTimeMs != 0L && (nowMs - firstChopCompletedTimeMs > maxBetweenChopsMs)) {
            firstChopCompletedTimeMs = 0L
        }

        val startStrokeVelocity = 2.0f // rad/s (~115 deg/s) - rapid deliberate swing
        val endStrokeVelocity = 1.2f   // rad/s - swing deceleration

        if (!isInStroke) {
            // Check if a new chop swing has begun (requires significant angular velocity + acceleration)
            if (gyroMagnitude >= startStrokeVelocity && accelMagnitude >= settings.chopAccelThreshold * 0.7f) {
                isInStroke = true
                strokeStartTimeMs = nowMs
                strokeArcDegrees = gyroMagnitude * dtSec * RAD_TO_DEG
                strokePeakAccel = accelMagnitude
            }
        } else {
            // Continuing stroke: accumulate angular arc and peak acceleration
            strokeArcDegrees += gyroMagnitude * dtSec * RAD_TO_DEG
            if (accelMagnitude > strokePeakAccel) {
                strokePeakAccel = accelMagnitude
            }

            val strokeDuration = nowMs - strokeStartTimeMs

            // Stroke finishes when motion decelerates or maximum stroke duration is reached
            val strokeFinished = gyroMagnitude < endStrokeVelocity || strokeDuration >= maxStrokeDurationMs

            if (strokeFinished) {
                isInStroke = false

                // Evaluate whether this completed stroke was a valid chop:
                // 1. Arc distance >= required degrees (at least 45° arc distance)
                // 2. Stroke duration within fast swing bounds (prevents slow gradual tilts)
                // 3. Peak acceleration meets threshold (ensures karate-chop momentum)
                val isValidChop = strokeArcDegrees >= settings.chopMinArcDegrees &&
                        strokeDuration in minStrokeDurationMs..maxStrokeDurationMs &&
                        strokePeakAccel >= settings.chopAccelThreshold

                if (isValidChop) {
                    if (firstChopCompletedTimeMs == 0L) {
                        // First valid chop recorded!
                        firstChopCompletedTimeMs = nowMs
                    } else {
                        val interval = nowMs - firstChopCompletedTimeMs
                        if (interval in minBetweenChopsMs..maxBetweenChopsMs) {
                            // Valid second chop in quick succession!
                            firstChopCompletedTimeMs = 0L
                            lastTriggerTime = nowMs
                            onChopChopDetected()
                        } else if (interval > maxBetweenChopsMs) {
                            // Window expired, set this as the new first chop
                            firstChopCompletedTimeMs = nowMs
                        }
                    }
                }
            }
        }
    }

    fun reset() {
        firstChopCompletedTimeMs = 0L
        isInStroke = false
        strokeArcDegrees = 0f
        strokePeakAccel = 0f
        lastTriggerTime = 0L
        lastTimestampNs = 0L
    }

    companion object {
        private const val RAD_TO_DEG = 57.2957795f
    }
}
