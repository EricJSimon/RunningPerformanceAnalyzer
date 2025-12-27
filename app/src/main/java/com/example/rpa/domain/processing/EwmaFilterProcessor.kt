package com.example.rpa.domain.processing

import kotlin.math.atan2

class EwmaFilterProcessor {
    private val alpha = 0.2f
    private var lastFilteredValue = 0f

    fun reset() {
        lastFilteredValue = 0f
    }

    fun calculate(y: Float, z: Float): Float {
        val rawAngle = Math.toDegrees(atan2(-y.toDouble(), z.toDouble())).toFloat()
        lastFilteredValue = alpha * rawAngle + (1.0f - alpha) * lastFilteredValue
        return lastFilteredValue
    }
}