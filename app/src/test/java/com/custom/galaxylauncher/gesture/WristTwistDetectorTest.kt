package com.custom.galaxylauncher.gesture

import com.custom.galaxylauncher.model.GestureSettings
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class WristTwistDetectorTest {

    private var triggerCount = 0
    private lateinit var detector: WristTwistDetector

    @Before
    fun setUp() {
        triggerCount = 0
        detector = WristTwistDetector {
            triggerCount++
        }
    }

    private fun simulateTwistArc(
        startNs: Long,
        durationMs: Long = 200L,
        angularVelocityRadPerSec: Float,
        settings: GestureSettings
    ): Long {
        var currentTimeNs = startNs
        val steps = (durationMs / 20).toInt()
        for (i in 0 until steps) {
            currentTimeNs += 20_000_000L // 20ms step
            detector.onSensorData(
                gyroX = 0f, gyroY = angularVelocityRadPerSec, gyroZ = 0f,
                settings = settings,
                timestampNs = currentTimeNs
            )
        }
        return currentTimeNs
    }

    @Test
    fun minorTilt_doesNotTrigger() {
        val settings = GestureSettings(
            twistEnabled = true,
            twistMinArcDegrees = 60.0f
        )

        var timeNs = 1_000_000_000L

        // Small angular movement in pocket: 2.0 rad/s for 100ms -> Arc ≈ 11.4 degrees (< 60°)
        timeNs = simulateTwistArc(startNs = timeNs, durationMs = 100L, angularVelocityRadPerSec = 2.0f, settings = settings)
        timeNs = simulateTwistArc(startNs = timeNs, durationMs = 100L, angularVelocityRadPerSec = -2.0f, settings = settings)

        assertEquals(0, triggerCount)
    }

    @Test
    fun singleTwist_60deg_doesNotTrigger() {
        val settings = GestureSettings(
            twistEnabled = true,
            twistMinArcDegrees = 60.0f
        )

        // 6.0 rad/s for 200ms -> Arc ≈ 68.75 degrees (>= 60°), but no reversal
        simulateTwistArc(
            startNs = 1_000_000_000L,
            durationMs = 200L,
            angularVelocityRadPerSec = 6.0f,
            settings = settings
        )

        assertEquals(0, triggerCount)
    }

    @Test
    fun doubleTwist_60deg_triggersCamera() {
        val settings = GestureSettings(
            twistEnabled = true,
            twistMinArcDegrees = 60.0f
        )

        var timeNs = 1_000_000_000L

        // Step 1: Forward flick -> 6.0 rad/s for 200ms -> Arc ≈ 68.75 degrees (>= 60°)
        timeNs = simulateTwistArc(
            startNs = timeNs,
            durationMs = 200L,
            angularVelocityRadPerSec = 6.0f,
            settings = settings
        )
        assertEquals(0, triggerCount)

        // Step 2: Immediate reversal flick -> -6.0 rad/s for 200ms -> Arc ≈ 68.75 degrees (>= 60°)
        simulateTwistArc(
            startNs = timeNs,
            durationMs = 200L,
            angularVelocityRadPerSec = -6.0f,
            settings = settings
        )

        assertEquals(1, triggerCount)
    }

    @Test
    fun doubleTwist_insufficientArc_doesNotTrigger() {
        val settings = GestureSettings(
            twistEnabled = true,
            twistMinArcDegrees = 60.0f
        )

        var timeNs = 1_000_000_000L

        // 3.5 rad/s for 140ms -> Arc ≈ 28.0 degrees (< 60°)
        timeNs = simulateTwistArc(
            startNs = timeNs,
            durationMs = 140L,
            angularVelocityRadPerSec = 3.5f,
            settings = settings
        )

        // Reversal also insufficient (< 60°)
        simulateTwistArc(
            startNs = timeNs,
            durationMs = 140L,
            angularVelocityRadPerSec = -3.5f,
            settings = settings
        )

        assertEquals(0, triggerCount)
    }

    @Test
    fun disabledTwist_doesNotTrigger() {
        val settings = GestureSettings(twistEnabled = false)

        var timeNs = 1_000_000_000L
        timeNs = simulateTwistArc(startNs = timeNs, durationMs = 200L, angularVelocityRadPerSec = 8.0f, settings = settings)
        simulateTwistArc(startNs = timeNs, durationMs = 200L, angularVelocityRadPerSec = -8.0f, settings = settings)

        assertEquals(0, triggerCount)
    }
}
