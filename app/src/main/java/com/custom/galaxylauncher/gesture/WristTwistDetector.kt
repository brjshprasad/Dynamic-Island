package com.custom.galaxylauncher.gesture

import com.custom.galaxylauncher.model.GestureSettings
import kotlin.math.abs

/**
 * High-precision wrist-twist gesture detector.
 *
 * Requirements:
 * - Requires at least a 60-degree arc of rotation (or settings.twistMinArcDegrees).
 * - Detects rapid flick and reversal motion.
 * - Rejects pocket jostles, casual phone turns, and minor tilts.
 */
class WristTwistDetector(
    private val onWristTwistDetected: () -> Unit
) {
    private var lastTimestampNs: Long = 0L
    private var lastTriggerTime: Long = 0L

    // Twist state machine:
    // 0: IDLE
    // 1: FIRST_TWIST (accumulating >= twistMinArcDegrees in direction A)
    // 2: REVERSAL_TWIST (accumulating >= twistMinArcDegrees in opposite direction B)
    private var twistStage = 0
    private var initialDirection = 0
    private var stageStartTimeMs = 0L
    private var stageAccumulatedArcDeg = 0f

    private val stageTimeoutMs = 450L          // Maximum time to complete each twist arc
    private val triggerCooldownMs = 1200L      // Cooldown after triggering gesture
    private val minTwistAngularVelocity = 2.5f  // rad/s (~143 deg/s) required for deliberate flick

    fun onSensorData(
        gyroX: Float, gyroY: Float, gyroZ: Float,
        settings: GestureSettings,
        timestampNs: Long = System.nanoTime()
    ) {
        if (!settings.twistEnabled) return

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

        // 3-Axis weighted angular velocity:
        // Primarily along Y (roll) axis for phone twist, augmented by X and Z
        val weightedRoll = gyroY * settings.gyroSensitivityY
        val weightedPitch = gyroX * settings.gyroSensitivityX
        val weightedYaw = gyroZ * settings.gyroSensitivityZ

        // Effective dominant rotation vector
        val dominantAngularVelocity = if (abs(weightedRoll) >= abs(weightedPitch) && abs(weightedRoll) >= abs(weightedYaw)) {
            weightedRoll
        } else if (abs(weightedPitch) >= abs(weightedYaw)) {
            weightedPitch
        } else {
            weightedYaw
        }

        val speed = abs(dominantAngularVelocity)
        val dir = if (dominantAngularVelocity >= 0) 1 else -1

        when (twistStage) {
            0 -> {
                // Waiting for a fast initial twist
                if (speed >= minTwistAngularVelocity) {
                    twistStage = 1
                    initialDirection = dir
                    stageStartTimeMs = nowMs
                    stageAccumulatedArcDeg = speed * dtSec * RAD_TO_DEG
                }
            }
            1 -> {
                val elapsed = nowMs - stageStartTimeMs
                if (elapsed > stageTimeoutMs) {
                    // Timed out, reset
                    twistStage = 0
                    stageAccumulatedArcDeg = 0f
                } else if (dir == initialDirection) {
                    // Continuing in same initial direction
                    stageAccumulatedArcDeg += speed * dtSec * RAD_TO_DEG
                } else {
                    // Motion reversed direction!
                    // Check if initial direction completed at least the required arc distance (default 60°)
                    if (stageAccumulatedArcDeg >= settings.twistMinArcDegrees) {
                        // Successfully completed 1st twist with >= 60° arc!
                        // Now transition to stage 2: reversal twist in opposite direction
                        twistStage = 2
                        stageStartTimeMs = nowMs
                        stageAccumulatedArcDeg = speed * dtSec * RAD_TO_DEG
                    } else {
                        // Reversed too soon (< 60° arc) - reset or restart if current reversal is fast
                        if (speed >= minTwistAngularVelocity) {
                            twistStage = 1
                            initialDirection = dir
                            stageStartTimeMs = nowMs
                            stageAccumulatedArcDeg = speed * dtSec * RAD_TO_DEG
                        } else {
                            twistStage = 0
                            stageAccumulatedArcDeg = 0f
                        }
                    }
                }
            }
            2 -> {
                val elapsed = nowMs - stageStartTimeMs
                if (elapsed > stageTimeoutMs) {
                    twistStage = 0
                    stageAccumulatedArcDeg = 0f
                } else if (dir != initialDirection) {
                    // Continuing in reversal direction
                    stageAccumulatedArcDeg += speed * dtSec * RAD_TO_DEG

                    // Check if reversal has achieved >= required arc distance (at least 60° arc)
                    val requiredReversalArc = settings.twistMinArcDegrees * 0.9f
                    if (stageAccumulatedArcDeg >= requiredReversalArc) {
                        // Both initial twist and reversal reached >= 60° arc!
                        // TRIGGER CAMERA!
                        twistStage = 0
                        stageAccumulatedArcDeg = 0f
                        lastTriggerTime = nowMs
                        onWristTwistDetected()
                    }
                } else {
                    // Direction flipped again before achieving required arc
                    twistStage = 0
                    stageAccumulatedArcDeg = 0f
                }
            }
        }
    }

    fun reset() {
        twistStage = 0
        initialDirection = 0
        stageStartTimeMs = 0L
        stageAccumulatedArcDeg = 0f
        lastTriggerTime = 0L
        lastTimestampNs = 0L
    }

    companion object {
        private const val RAD_TO_DEG = 57.2957795f
    }
}
