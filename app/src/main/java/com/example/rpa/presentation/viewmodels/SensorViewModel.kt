package com.example.rpa.presentation.viewmodels

import android.app.Application
import android.content.ContentValues
import android.content.Intent
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rpa.data.repository.RunningRepository
import com.example.rpa.domain.model.Algorithm
import com.example.rpa.presentation.service.RunningService
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "SensorViewModel"

class SensorViewModel(application: Application) : AndroidViewModel(application) {

    // --- UI STATES ---
    val isMeasuring = mutableStateOf(false)
    val stepCount = mutableIntStateOf(0)
    val currentCadence = mutableFloatStateOf(0f)

    // Impact Analysis States
    val lowImpactSteps = mutableIntStateOf(0)
    val mediumImpactSteps = mutableIntStateOf(0)
    val highImpactSteps = mutableIntStateOf(0)

    // Raw Data for Debugging
    val linearAccelerometerData = mutableStateOf(floatArrayOf(0f, 0f, 0f))
    val gyroscopeData = mutableStateOf(floatArrayOf(0f, 0f, 0f))

    val currentAlgorithm = mutableStateOf(Algorithm.CUSTOM_ALGORITHM)

    // Graph Data
    val cadenceHistory = mutableStateListOf<Float>()

    init {

        RunningRepository.isMeasuring.onEach {
            isMeasuring.value = it
        }.launchIn(viewModelScope)

        RunningRepository.stepCount.onEach {
            stepCount.intValue = it

            refreshGraphData()
        }.launchIn(viewModelScope)

        RunningRepository.currentCadence.onEach {
            currentCadence.floatValue = it
        }.launchIn(viewModelScope)

        RunningRepository.currentAlgorithm.onEach {
            currentAlgorithm.value = it
        }.launchIn(viewModelScope)

        RunningRepository.accData.onEach {
            linearAccelerometerData.value = it
        }.launchIn(viewModelScope)

        RunningRepository.gyroData.onEach {
            gyroscopeData.value = it
        }.launchIn(viewModelScope)

        RunningRepository.impactStats.onEach { (low, med, high) ->
            lowImpactSteps.intValue = low
            mediumImpactSteps.intValue = med
            highImpactSteps.intValue = high
        }.launchIn(viewModelScope)
    }

    private fun refreshGraphData() {
        if (RunningRepository.cadenceHistory.isNotEmpty()) {
            cadenceHistory.clear()
            cadenceHistory.addAll(RunningRepository.cadenceHistory)
        }
    }

    fun setAlgorithm(algorithm: Algorithm) {
        RunningRepository.setAlgorithm(algorithm)
    }

    fun startMeasurement() {
        Log.i(TAG, "Requesting Service Start")
        val intent = Intent(getApplication(), RunningService::class.java)
        getApplication<Application>().startForegroundService(intent)
    }

    fun stopMeasurement() {
        Log.i(TAG, "Requesting Service Stop")
        val intent = Intent(getApplication(), RunningService::class.java)
        intent.action = "STOP_SERVICE"
        getApplication<Application>().startService(intent)
    }

    fun exportDataToCsv() {
        val history = RunningRepository.measurementHistory

        Log.d(TAG, "Exporting data to CSV. ${history.size} records.")
        if (history.isEmpty()) {
            Toast.makeText(getApplication(), "No data to export.", Toast.LENGTH_SHORT).show()
            return
        }

        val delimiter = ";"
        val csvHeader = "Timestamp (HH:mm:ss.ms)${delimiter}Value (Mag/Cadence)${delimiter}Metric Type\n"

        val csvData = history.joinToString(separator = "\n") { dataPoint ->
            val formattedTime = formatNanosToTimeString(dataPoint.timestamp)
            val formattedValue = String.format(Locale.forLanguageTag("sv-SE"), "%.4f", dataPoint.value)
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
                        Toast.makeText(getApplication(), "Data exported to Downloads/RunningPerformance", Toast.LENGTH_LONG).show()
                        Log.i(TAG, "CSV export successful to $uri")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error exporting file", e)
                Toast.makeText(getApplication(), "Error exporting file ${e.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Log.e(TAG, "Error creating file URI for CSV.")
            Toast.makeText(getApplication(), "Error creating file URI.", Toast.LENGTH_LONG).show()
        }
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
}