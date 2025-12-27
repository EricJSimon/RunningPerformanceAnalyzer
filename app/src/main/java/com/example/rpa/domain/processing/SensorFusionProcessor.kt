package com.example.rpa.domain.processing

import kotlin.math.atan2

class SensorFusionProcessor {
    private val filterAlpha = 0.98f
    private var fusedAngle: Float = 0f

    fun reset() {
        fusedAngle = 0f
    }

    fun calculate(accY: Float, accZ: Float, gyroRate: Float, dt: Float): Float {
        val accAngle = Math.toDegrees(atan2(-accY.toDouble(), accZ.toDouble())).toFloat()
        val gyroAngle = fusedAngle + gyroRate * dt
        fusedAngle = filterAlpha * gyroAngle + (1.0f - filterAlpha) * accAngle
        return fusedAngle
    }
}