package com.example.rpa.domain.processing

import android.util.Log
import kotlin.math.sqrt

private const val TAG = "StepDetector"

class StepDetector {
    private var lastMagnitude = 0f
    private var lastStepTimeNs = 0L

    private val STEP_THRESHOLD = 11.0f
    private val MIN_TIME_BETWEEN_STEPS_NS = 250_000_000L

    private val ALPHA = 0.1f
    private var smoothedMagnitude = 9.8f

    fun detectStep(accValues: FloatArray, gyroValues: FloatArray, timestampNs: Long): Float {
        val x = accValues[0]
        val y = accValues[1]
        val z = accValues[2]
        val rawMagnitude = sqrt(x * x + y * y + z * z)
        var stepMagnitude = 0f

        smoothedMagnitude += ALPHA * (rawMagnitude - smoothedMagnitude)

        val gyroMagnitude = sqrt(
            gyroValues[0] * gyroValues[0] +
                    gyroValues[1] * gyroValues[1] +
                    gyroValues[2] * gyroValues[2]
        )
        val isDynamicMotion = gyroMagnitude > 0.5f

        if (rawMagnitude > 10.0f) {
            Log.d(TAG, "Peak Check -> RawMag: %.2f, SmoothMag: %.2f, Gyro: %.2f, Dynamic: $isDynamicMotion".format(rawMagnitude, smoothedMagnitude, gyroMagnitude))
        }
        if (isDynamicMotion && smoothedMagnitude > STEP_THRESHOLD && lastMagnitude <= STEP_THRESHOLD && (timestampNs - lastStepTimeNs) > MIN_TIME_BETWEEN_STEPS_NS) {
            lastStepTimeNs = timestampNs
            stepMagnitude = smoothedMagnitude
            Log.i(TAG, ">>> STEP DETECTED! <<< (Time diff: ${(timestampNs - lastStepTimeNs)/1_000_000}ms)")
        }
        lastMagnitude = smoothedMagnitude
        return stepMagnitude
    }

    fun reset() {
        Log.d(TAG, "Detector Reset")
        lastMagnitude = 0f
        lastStepTimeNs = 0L
        smoothedMagnitude = 9.8f
    }
}