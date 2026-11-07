package com.custom.galaxylauncher.gesture

import com.custom.galaxylauncher.model.GestureSettings
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ChopChopDetectorTest {

    private var triggerCount = 0
    private lateinit var detector: ChopChopDetector

    @Before
    fun setUp() {
        triggerCount = 0
        detector = ChopChopDetector {
            triggerCount++
        }
    }

    private fun simulateChop(
        startNs: Long,
        durationMs: Long = 200L,
        angularVelocityRadPerSec: Float = 4.5f,
        accelMagnitude: Float = 18.0f,
        settings: GestureSettings
    ): Long {
        var currentTimeNs = startNs
        val steps = (durationMs / 20).toInt()
        for (i in 0 until steps) {
            currentTimeNs += 20_000_000L // 20ms step
            detector.onSensorData(
                accelX = 0f, accelY = accelMagnitude, accelZ = 2f,
                gyroX = angularVelocityRadPerSec, gyroY = 0f, gyroZ = 0f,
                settings = settings,
                timestampNs = currentTimeNs
            )
        }
        // Finalizing sample: angular velocity decelerates to finish stroke
        currentTimeNs += 20_000_000L
        detector.onSensorData(
            accelX = 0f, accelY = 9.8f, accelZ = 0f,
            gyroX = 0.5f, gyroY = 0f, gyroZ = 0f,
            settings = settings,
            timestampNs = currentTimeNs
        )
        return currentTimeNs
    }

    @Test
    fun minorPocketMovement_doesNotTrigger() {
        val settings = GestureSettings(
            chopEnabled = true,
            chopMinArcDegrees = 45.0f,
            chopAccelThreshold = 14.0f
        )

        var timeNs = 1_000_000_000L

        // Simulate small jostles in pocket: 1.5 rad/s for 80ms -> Arc ≈ 6.8 degrees (< 45°)
        for (i in 0 until 5) {
            timeNs = simulateChop(
                startNs = timeNs,
                durationMs = 80L,
                angularVelocityRadPerSec = 1.5f,
                accelMagnitude = 11.0f,
                settings = settings
            )
            timeNs += 200_000_000L
        }

        assertEquals(0, triggerCount)
    }

    @Test
    fun singleChop_45deg_doesNotTrigger() {
        val settings = GestureSettings(
            chopEnabled = true,
            chopMinArcDegrees = 45.0f,
            chopAccelThreshold = 14.0f
        )

        // 4.5 rad/s for 200ms -> Arc ≈ 51.5 degrees (>= 45°)
        simulateChop(
            startNs = 1_000_000_000L,
            durationMs = 200L,
            angularVelocityRadPerSec = 4.5f,
            accelMagnitude = 18.0f,
            settings = settings
        )

        assertEquals(0, triggerCount)
    }

    @Test
    fun doubleChop_45deg_triggersFlashlight() {
        val settings = GestureSettings(
            chopEnabled = true,
            chopMinArcDegrees = 45.0f,
            chopAccelThreshold = 14.0f
        )

        var timeNs = 1_000_000_000L

        // First chop: Arc ≈ 51.5 degrees (>= 45°)
        timeNs = simulateChop(
            startNs = timeNs,
            durationMs = 200L,
            angularVelocityRadPerSec = 4.5f,
            accelMagnitude = 18.0f,
            settings = settings
        )
        assertEquals(0, triggerCount)

        // Quick succession pause: 250ms
        timeNs += 250_000_000L

        // Second chop: Arc ≈ 51.5 degrees (>= 45°)
        simulateChop(
            startNs = timeNs,
            durationMs = 200L,
            angularVelocityRadPerSec = 4.5f,
            accelMagnitude = 18.0f,
            settings = settings
        )

        assertEquals(1, triggerCount)
    }

    @Test
    fun doubleChop_insufficientArc_doesNotTrigger() {
        val settings = GestureSettings(
            chopEnabled = true,
            chopMinArcDegrees = 45.0f,
            chopAccelThreshold = 14.0f
        )

        var timeNs = 1_000_000_000L

        // 3.0 rad/s for 120ms -> Arc ≈ 20.6 degrees (< 45°)
        timeNs = simulateChop(
            startNs = timeNs,
            durationMs = 120L,
            angularVelocityRadPerSec = 3.0f,
            accelMagnitude = 16.0f,
            settings = settings
        )

        timeNs += 250_000_000L

        timeNs = simulateChop(
            startNs = timeNs,
            durationMs = 120L,
            angularVelocityRadPerSec = 3.0f,
            accelMagnitude = 16.0f,
            settings = settings
        )

        assertEquals(0, triggerCount)
    }

    @Test
    fun doubleChop_tooSlowWindow_doesNotTrigger() {
        val settings = GestureSettings(
            chopEnabled = true,
            chopMinArcDegrees = 45.0f,
            chopAccelThreshold = 14.0f
        )

        var timeNs = 1_000_000_000L

        // First chop
        timeNs = simulateChop(
            startNs = timeNs,
            durationMs = 200L,
            angularVelocityRadPerSec = 4.5f,
            accelMagnitude = 18.0f,
            settings = settings
        )

        // Pause 1200ms (> 850ms window)
        timeNs += 1_200_000_000L

        // Second chop
        simulateChop(
            startNs = timeNs,
            durationMs = 200L,
            angularVelocityRadPerSec = 4.5f,
            accelMagnitude = 18.0f,
            settings = settings
        )

        // Should not trigger because interval exceeded quick succession limit
        assertEquals(0, triggerCount)
    }

    @Test
    fun disabledChop_doesNotTrigger() {
        val settings = GestureSettings(chopEnabled = false)

        var timeNs = 1_000_000_000L
        timeNs = simulateChop(startNs = timeNs, settings = settings)
        timeNs += 250_000_000L
        simulateChop(startNs = timeNs, settings = settings)

        assertEquals(0, triggerCount)
    }
}
