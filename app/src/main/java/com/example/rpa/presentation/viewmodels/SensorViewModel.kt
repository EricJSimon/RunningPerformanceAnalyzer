package com.example.rpa.presentation.viewmodels

import android.app.Application
import android.content.ContentValues
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rpa.data.model.MeasurementData
import com.example.rpa.data.sensor.SensorDataProvider
import com.example.rpa.domain.model.Algorithm
import com.example.rpa.domain.processing.StepDetector
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

private const val TAG = "SensorViewModel"

class SensorViewModel(application: Application) : AndroidViewModel(application) {

    private val sensorDataProvider = SensorDataProvider(application)

    private val stepDetector = StepDetector()

    private val _isMeasuring = mutableStateOf(false)
    val isMeasuring: State<Boolean> = _isMeasuring

    private val _currentCadence = mutableFloatStateOf(0f)
    val currentCadence: State<Float> = _currentCadence

    private val _stepCount = mutableIntStateOf(0)
    val stepCount: State<Int> = _stepCount

    private val _linearAccelerometerData = mutableStateOf(floatArrayOf(0f, 0f, 0f))
    val linearAccelerometerData: State<FloatArray> = _linearAccelerometerData

    private val _gyroscopeData = mutableStateOf(floatArrayOf(0f, 0f, 0f))
    val gyroscopeData: State<FloatArray> = _gyroscopeData

    private val _currentAlgorithm = mutableStateOf(Algorithm.CUSTOM_ALGORITHM)
    val currentAlgorithm: State<Algorithm> = _currentAlgorithm

    private val _cadenceHistory = mutableStateListOf<Float>()
    val cadenceHistory: List<Float> = _cadenceHistory

    private var sensorJob: Job? = null
    private val measurementHistory = mutableListOf<MeasurementData>()

    private var sessionStartTimestamp: Long = 0L
    private var lastTimestamp: Long = 0L

    private var latestGyro = floatArrayOf(0f, 0f, 0f)
    private var latestAcc = floatArrayOf(0f, 0f, 0f)

    private val _lowImpactSteps = mutableStateOf(0)
    var lowImpactSteps = _lowImpactSteps

    private val _mediumImpactSteps = mutableStateOf(0)
    var mediumImpactSteps = _mediumImpactSteps

    private val _highImpactSteps = mutableStateOf(0)
    var highImpactSteps = _highImpactSteps

    fun setAlgorithm(algorithm: Algorithm) {
        if (!_isMeasuring.value) {
            _currentAlgorithm.value = algorithm
            Log.d(TAG, "Algorithm set to: $algorithm")
        } else {
            Log.w(TAG, "Cannot change algorithm while measuring")
        }
    }

    fun startMeasurement() {
        if (_isMeasuring.value) {
            Log.w(TAG, "startMeasurement called but already measuring.")
            return
        }
        _isMeasuring.value = true
        Log.i(TAG, "Starting measurement.")

        resetAllState()

        sensorJob = sensorDataProvider.getSensorData(_currentAlgorithm.value)
            .onEach { event -> processSensorEvent(event) }
            .launchIn(viewModelScope)
    }

    fun stopMeasurement() {
        if (!this._isMeasuring.value) {
            Log.w(TAG, "stopMeasurement called but not measuring.")
            return
        }
        _isMeasuring.value = false
        Log.i(TAG, "Stopping measurement.")
        sensorJob?.cancel()
        sensorJob = null
    }

    private fun processSensorEvent(event: SensorEvent) {
        if (sessionStartTimestamp == 0L) {
            sessionStartTimestamp = event.timestamp
            lastTimestamp = event.timestamp
            return
        }

        val relativeTimestamp = event.timestamp - sessionStartTimestamp

        when (event.sensor.type) {
            Sensor.TYPE_STEP_DETECTOR -> {
                if (_currentAlgorithm.value == Algorithm.HARDWARE_STEP_DETECTOR) {
                    handleStepDetected(event.timestamp, relativeTimestamp, "Hardware")
                }
            }

            Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_LINEAR_ACCELERATION -> {
                _linearAccelerometerData.value = event.values.clone()
                System.arraycopy(event.values, 0, latestAcc, 0, 3)

                val magnitude = sqrt(
                    latestAcc[0] * latestAcc[0] +
                            latestAcc[1] * latestAcc[1] +
                            latestAcc[2] * latestAcc[2]
                )

                if (_currentAlgorithm.value == Algorithm.CUSTOM_ALGORITHM) {
                    val stepForce = stepDetector.detectStep(latestAcc, latestGyro, event.timestamp)

                    if (stepForce > 0f) {
                        handleStepDetected(event.timestamp, relativeTimestamp, "Custom")

                        when {
                            stepForce < 13.0f -> {
                                lowImpactSteps.value++
                                Log.d(TAG, "Impact: LOW ($stepForce)")
                            }

                            stepForce < 16.0f -> {
                                mediumImpactSteps.value++
                                Log.d(TAG, "Impact: MEDIUM ($stepForce)")
                            }

                            else -> {
                                highImpactSteps.value++
                                Log.d(TAG, "Impact: HIGH ($stepForce)")
                            }
                        }

                        addMeasurementToHistory(
                            relativeTimestamp,
                            magnitude,
                            "Step (Force: $stepForce)"
                        )
                    } else {
                        addMeasurementToHistory(relativeTimestamp, magnitude, "Raw")
                    }
                } else {
                    addMeasurementToHistory(relativeTimestamp, magnitude, "Raw")
                }
            }

            Sensor.TYPE_GYROSCOPE -> {
                _gyroscopeData.value = event.values.clone()
                System.arraycopy(event.values, 0, latestGyro, 0, 3)
            }
        }
    }

    private fun handleStepDetected(eventTimestamp: Long, relativeTimestamp: Long, source: String) {
        Log.i(TAG, "$source Step Detected! Count: ${_stepCount.intValue + 1}")

        _stepCount.intValue += 1

        val timeDiffSec = (eventTimestamp - lastTimestamp) / 1_000_000_000f

        if (timeDiffSec > 0.25f) {
            val spm = 60f / timeDiffSec
            Log.d(TAG, "Calculated SPM: $spm")

            val current = _currentCadence.floatValue
            _currentCadence.floatValue = if (current == 0f) spm else (current * 0.5f) + (spm * 0.5f)

            lastTimestamp = eventTimestamp
        }
    }

    private fun addMeasurementToHistory(timestamp: Long, value: Float, label: String) {
        measurementHistory.add(MeasurementData(timestamp, value, label))

        if (_cadenceHistory.size < 200) {
            _cadenceHistory.add(value)
        } else {
            _cadenceHistory.removeAt(0)
            _cadenceHistory.add(value)
        }

        if (_cadenceHistory.size % 100 == 0) {
            Log.v(TAG, "Graph history updated. Current val: $value")
        }
    }

    private fun resetAllState() {
        Log.d(TAG, "Resetting all states.")
        _cadenceHistory.clear()
        measurementHistory.clear()
        sessionStartTimestamp = 0L
        lastTimestamp = 0L

        stepDetector.reset()
        _stepCount.intValue = 0

        resetUiState()
    }

    fun exportDataToCsv() {
        Log.d(TAG, "Exporting data to CSV. ${measurementHistory.size} records.")
        if (measurementHistory.isEmpty()) {
            Toast.makeText(getApplication(), "No data to export.", Toast.LENGTH_SHORT).show()
            return
        }

        val delimiter = ";"
        val csvHeader =
            "Timestamp (HH:mm:ss.ms)${delimiter}Value (Mag/Cadence)${delimiter}Metric Type\n"

        val csvData = measurementHistory.joinToString(separator = "\n") { dataPoint ->
            val formattedTime = formatNanosToTimeString(dataPoint.timestamp)
            val formattedValue =
                String.format(Locale.forLanguageTag("sv-SE"), "%.4f", dataPoint.value)
            "$formattedTime$delimiter$formattedValue"
        }
        val fullCsv = csvHeader + csvData

        val timeFormatter = SimpleDateFormat("HH-mm-ss", Locale.getDefault())
        val currentDateTimeString = timeFormatter.format(Date())

        val resolver = getApplication<Application>().contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "RunningData_$currentDateTimeString.csv")
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/RunningPerformance")
        }

        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)

        if (uri != null) {
            try {
                resolver.openOutputStream(uri).use { outputStream ->
                    if (outputStream != null) {
                        outputStream.write(fullCsv.toByteArray(Charsets.UTF_8))
                        Toast.makeText(
                            getApplication(),
                            "Data exported to Downloads/RunningPerformance",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.i(TAG, "CSV export successful to $uri")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error exporting file", e)
                Toast.makeText(
                    getApplication(),
                    "Error exporting file ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            Log.e(TAG, "Error creating file URI for CSV.")
            Toast.makeText(getApplication(), "Error creating file URI.", Toast.LENGTH_LONG).show()
        }
        measurementHistory.clear()
    }

    private fun formatNanosToTimeString(nanos: Long): String {
        if (nanos < 0) return "00:00:00.000"
        val totalMillis = nanos / 1_000_000
        val hours = totalMillis / (1000 * 60 * 60)
        val minutes = (totalMillis % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = (totalMillis % (1000 * 60)) / 1000
        val millis = totalMillis % 1000

        return String.format(Locale.US, "%02d:%02d:%02d.%03d", hours, minutes, seconds, millis)
    }

    private fun resetUiState() {
        _currentCadence.floatValue = 0f
        _linearAccelerometerData.value = floatArrayOf(0f, 0f, 0f)
        _gyroscopeData.value = floatArrayOf(0f, 0f, 0f)
    }

    override fun onCleared() {
        super.onCleared()
        stopMeasurement()
    }
}