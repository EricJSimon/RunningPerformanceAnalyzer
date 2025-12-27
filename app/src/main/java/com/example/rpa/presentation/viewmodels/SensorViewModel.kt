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
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateListOf
import com.example.rpa.data.model.MeasurementData
import com.example.rpa.data.sensor.SensorDataProvider
import com.example.rpa.domain.model.Algorithm
import com.example.rpa.domain.processing.EwmaFilterProcessor
import com.example.rpa.domain.processing.SensorFusionProcessor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "SensorViewModel"

class SensorViewModel(application: Application) : AndroidViewModel(application) {

    private val sensorDataProvider = SensorDataProvider(application)
    private val ewmaProcessor = EwmaFilterProcessor()
    private val sensorFusionProcessor = SensorFusionProcessor()

    private val _isMeasuring = mutableStateOf(false)
    val isMeasuring: State<Boolean> = _isMeasuring

    private val _armElevation = mutableFloatStateOf(0f)
    val armElevation: State<Float> = _armElevation

    private val _linearAccelerometerData = mutableStateOf(floatArrayOf(0f, 0f, 0f))
    val linearAccelerometerData: State<FloatArray> = _linearAccelerometerData

    private val _gyroscopeData = mutableStateOf(floatArrayOf(0f, 0f, 0f))
    val gyroscopeData: State<FloatArray> = _gyroscopeData

    private val _currentAlgorithm = mutableStateOf(Algorithm.EWMA_FILTER)
    val currentAlgorithm: State<Algorithm> = _currentAlgorithm

    private val _elevationHistory = mutableStateListOf<Float>()
    val elevationHistory: List<Float> = _elevationHistory

    private var sensorJob: Job? = null
    private val measurementHistory = mutableListOf<MeasurementData>()
    private var sessionStartTimestamp: Long = 0L
    private var lastTimestamp: Long = 0L
    private val accReading = FloatArray(3)

    fun setAlgorithm(algorithm: Algorithm) {
        if (!_isMeasuring.value) {
            _currentAlgorithm.value = algorithm
            Log.d(TAG,"Algorithm set to: $algorithm")
        } else {
            Log.w(TAG, "Cannot change algorithm while measuring")
        }
    }


    fun startMeasurement() {
        if (_isMeasuring.value) {
            Log.w(TAG,"startMeasurement called but already measuring.")
            return
        }
        _isMeasuring.value = true
        Log.i(TAG, "Starting measurement with algorithm: ${_currentAlgorithm.value}")

        resetAllState()

        sensorJob = sensorDataProvider.getSensorData(_currentAlgorithm.value)
            .onEach { event -> processSensorEvent(event) }
            .launchIn(viewModelScope)
    }

    fun stopMeasurement() {
        if (!this._isMeasuring.value) {
            Log.w(TAG,"stopMeasurement called but not measuring.")
            return
        }
            _isMeasuring.value = false
        Log.i(TAG, "Stopping measurement.")
        sensorJob?.cancel()
        sensorJob = null
    }

    private fun processSensorEvent(event: SensorEvent) {
        if (sessionStartTimestamp == 0L) {
            Log.d(TAG, "First sensor event received. Type ${event.sensor.name} Setting start timestamps.")
            sessionStartTimestamp = event.timestamp
            lastTimestamp = event.timestamp
            return
        }

        val relativeTimestamp = event.timestamp - sessionStartTimestamp

        when (event.sensor.type) {
            Sensor.TYPE_GRAVITY -> {
                val newAngle = ewmaProcessor.calculate(event.values[1], event.values[2])
                Log.v(TAG, "EWMA Calculated Angle: $newAngle")
                _armElevation.floatValue = newAngle
                addMeasurementToHistory(relativeTimestamp, newAngle, "EWMA")
            }

            Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_LINEAR_ACCELERATION -> {
                _linearAccelerometerData.value = event.values.clone()

                if(event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    System.arraycopy(event.values,0,accReading,0,3)
                }
            }

            Sensor.TYPE_GYROSCOPE -> {
                val dt = (event.timestamp - lastTimestamp) * 1.0f / 1_000_000_000.0f

                if (dt > 0) {
                    val newAngle = sensorFusionProcessor.calculate(
                        accY = accReading[1],
                        accZ = accReading[2],
                        gyroRate = event.values[0],
                        dt = dt
                    )
                    _armElevation.floatValue = newAngle
                    Log.v(TAG,"Fusion Calculated Angle: $newAngle, dt: $dt")
                    addMeasurementToHistory(relativeTimestamp, newAngle, "Sensor Fusion")
                } else {
                    Log.w(TAG, "Gyroscope event skipped due to non-positive dt: $dt")
                }
                lastTimestamp = event.timestamp
                _gyroscopeData.value = event.values.clone()
            }
        }
    }

    private fun addMeasurementToHistory(timestamp: Long,value: Float, algorithmName: String) {
        measurementHistory.add(MeasurementData(timestamp,value,algorithmName))
        _elevationHistory.add(value)
    }

    private fun resetAllState() {
        Log.d(TAG, "Resetting all states.")
        _elevationHistory.clear()
        measurementHistory.clear()
        sessionStartTimestamp = 0L
        lastTimestamp = 0L
        ewmaProcessor.reset()
        sensorFusionProcessor.reset()
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
            "Timestamp (HH:mm:ss.ms)${delimiter}Arm Elevation (degrees)${delimiter}Algorithm: ${_currentAlgorithm.value}\n"

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
            put(MediaStore.MediaColumns.DISPLAY_NAME, "ArmElevationData_$currentDateTimeString.csv")
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/SensorApp")
        }

        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)

        if (uri != null) {
            try {
                resolver.openOutputStream(uri).use { outputStream ->
                    if (outputStream != null) {
                        outputStream.write(fullCsv.toByteArray(Charsets.UTF_8))
                        Toast.makeText(
                            getApplication(),
                            "Data exported to Downloads/SensorApp",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.i(TAG,"CSV export successful to $uri")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG,"Error exporting file",e)
                Toast.makeText(
                    getApplication(),
                    "Error exporting file ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            Log.e(TAG,"Error creating file URI for CSV.")
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

        return String.format(
            Locale.US,
            "%02d:%02d:%02d.%03d",
            hours,
            minutes,
            seconds,
            millis
        )
    }

    private fun resetUiState() {
        _armElevation.floatValue = 0f
        _linearAccelerometerData.value = floatArrayOf(0f, 0f, 0f)
        _gyroscopeData.value = floatArrayOf(0f, 0f, 0f)
    }

    override fun onCleared() {
        super.onCleared()
        stopMeasurement()
    }
}