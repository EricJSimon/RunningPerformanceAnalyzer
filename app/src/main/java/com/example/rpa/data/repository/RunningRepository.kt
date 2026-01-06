package com.example.rpa.data.repository

import com.example.rpa.data.model.MeasurementData
import com.example.rpa.domain.model.Algorithm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object RunningRepository {

    private val _isMeasuring = MutableStateFlow(false)
    val isMeasuring: StateFlow<Boolean> = _isMeasuring.asStateFlow()

    private val _currentAlgorithm = MutableStateFlow(Algorithm.CUSTOM_ALGORITHM)
    val currentAlgorithm: StateFlow<Algorithm> = _currentAlgorithm.asStateFlow()

    private val _stepCount = MutableStateFlow(0)
    val stepCount: StateFlow<Int> = _stepCount.asStateFlow()

    private val _currentCadence = MutableStateFlow(0f)
    val currentCadence: StateFlow<Float> = _currentCadence.asStateFlow()

    private val _impactStats = MutableStateFlow(Triple(0, 0, 0)) // Low, Med, High
    val impactStats: StateFlow<Triple<Int, Int, Int>> = _impactStats.asStateFlow()

    private val _accData = MutableStateFlow(floatArrayOf(0f, 0f, 0f))
    val accData: StateFlow<FloatArray> = _accData.asStateFlow()

    private val _gyroData = MutableStateFlow(floatArrayOf(0f, 0f, 0f))
    val gyroData: StateFlow<FloatArray> = _gyroData.asStateFlow()

    val cadenceHistory = mutableListOf<Float>()
    val measurementHistory = mutableListOf<MeasurementData>()

    fun setMeasuring(isMeasuring: Boolean) { _isMeasuring.value = isMeasuring }
    fun setAlgorithm(algo: Algorithm) { _currentAlgorithm.value = algo }
    fun setStepCount(count: Int) { _stepCount.value = count }
    fun setCadence(cadence: Float) { _currentCadence.value = cadence }
    fun setAccData(data: FloatArray) { _accData.value = data }
    fun setGyroData(data: FloatArray) { _gyroData.value = data }

    fun updateImpactStats(low: Int, med: Int, high: Int) {
        _impactStats.value = Triple(low, med, high)
    }

    fun reset() {
        _stepCount.value = 0
        _currentCadence.value = 0f
        _impactStats.value = Triple(0,0,0)
        _accData.value = floatArrayOf(0f,0f,0f)
        _gyroData.value = floatArrayOf(0f,0f,0f)
        cadenceHistory.clear()
        measurementHistory.clear()
    }
}