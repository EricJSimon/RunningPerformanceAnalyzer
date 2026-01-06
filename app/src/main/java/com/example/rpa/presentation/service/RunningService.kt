package com.example.rpa.presentation.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.rpa.R
import com.example.rpa.data.model.MeasurementData
import com.example.rpa.data.repository.RunningRepository
import com.example.rpa.data.sensor.SensorDataProvider
import com.example.rpa.domain.model.Algorithm
import com.example.rpa.domain.processing.StepDetector
import com.example.rpa.presentation.views.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.math.sqrt

class RunningService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var sensorDataProvider: SensorDataProvider
    private var sensorJob: Job? = null

    // Logic Components
    private val stepDetector = StepDetector()
    private var sessionStartTimestamp: Long = 0L
    private var lastTimestamp: Long = 0L

    // Buffers
    private var latestGyro = floatArrayOf(0f, 0f, 0f)
    private var latestAcc = floatArrayOf(0f, 0f, 0f)

    // Local Counters
    private var currentStepCount = 0
    private var lowImpact = 0
    private var medImpact = 0
    private var highImpact = 0
    private var currentCadence = 0f

    override fun onCreate() {
        super.onCreate()
        sensorDataProvider = SensorDataProvider(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val action = intent?.action
            if (action == "STOP_SERVICE") {
                stopSelf()
                return START_NOT_STICKY
            }

            // 1. Build notification immediately to satisfy Foreground Service requirements
            val notification = buildNotification()
            startForeground(1, notification)

            // 2. Reset Logic
            RunningRepository.reset()
            currentStepCount = 0
            lowImpact = 0
            medImpact = 0
            highImpact = 0
            currentCadence = 0f
            sessionStartTimestamp = 0L
            stepDetector.reset()
            RunningRepository.setMeasuring(true)

            // 3. Start Sensors safely
            val algorithm = RunningRepository.currentAlgorithm.value
            Log.d("RunningService", "Starting sensors with algo: $algorithm")

            if (::sensorDataProvider.isInitialized) {
                sensorJob?.cancel() // Cancel previous job if exists
                sensorJob = sensorDataProvider.getSensorData(algorithm)
                    .onEach { event -> processSensorEvent(event) }
                    .launchIn(serviceScope)
            } else {
                Log.e("RunningService", "SensorDataProvider was not initialized")
                stopSelf()
                return START_NOT_STICKY
            }

            return START_STICKY

        } catch (e: Exception) {
            Log.e("RunningService", "Error starting service", e)
            e.printStackTrace()
            stopSelf()
            return START_NOT_STICKY
        }
    }

    private fun processSensorEvent(event: SensorEvent) {
        if (sessionStartTimestamp == 0L) {
            sessionStartTimestamp = event.timestamp
            lastTimestamp = event.timestamp
            return
        }
        val relativeTimestamp = event.timestamp - sessionStartTimestamp
        val algo = RunningRepository.currentAlgorithm.value

        when (event.sensor.type) {
            Sensor.TYPE_STEP_DETECTOR -> {
                if (algo == Algorithm.HARDWARE_STEP_DETECTOR) {
                    handleStepDetected(event.timestamp)
                }
            }
            Sensor.TYPE_ACCELEROMETER, Sensor.TYPE_LINEAR_ACCELERATION -> {
                System.arraycopy(event.values, 0, latestAcc, 0, 3)
                RunningRepository.setAccData(event.values)

                val magnitude = sqrt(latestAcc[0]*latestAcc[0] + latestAcc[1]*latestAcc[1] + latestAcc[2]*latestAcc[2])

                if (algo == Algorithm.CUSTOM_ALGORITHM) {
                    val stepForce = stepDetector.detectStep(latestAcc, latestGyro, event.timestamp)
                    if (stepForce > 0f) {
                        handleStepDetected(event.timestamp)

                        when {
                            stepForce < 13.0f -> lowImpact++
                            stepForce < 16.0f -> medImpact++
                            else -> highImpact++
                        }
                        RunningRepository.updateImpactStats(lowImpact, medImpact, highImpact)
                        addHistory(relativeTimestamp, magnitude, "Step ($stepForce)")
                    } else {
                        addHistory(relativeTimestamp, magnitude, "Raw")
                    }
                } else {
                    addHistory(relativeTimestamp, magnitude, "Raw")
                }
            }
            Sensor.TYPE_GYROSCOPE -> {
                System.arraycopy(event.values, 0, latestGyro, 0, 3)
                RunningRepository.setGyroData(event.values)
            }
        }
    }

    private fun handleStepDetected(eventTimestamp: Long) {
        currentStepCount++
        RunningRepository.setStepCount(currentStepCount)

        val timeDiffSec = (eventTimestamp - lastTimestamp) / 1_000_000_000f
        if (timeDiffSec > 0.25f) {
            val spm = 60f / timeDiffSec
            currentCadence = if(currentCadence == 0f) spm else (currentCadence * 0.5f) + (spm * 0.5f)
            RunningRepository.setCadence(currentCadence)
            lastTimestamp = eventTimestamp

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.notify(1, buildNotification())
        }
    }

    private fun addHistory(timestamp: Long, value: Float, label: String) {
        synchronized(RunningRepository) {
            RunningRepository.measurementHistory.add(MeasurementData(timestamp, value, label))
            if (RunningRepository.cadenceHistory.size < 200) {
                RunningRepository.cadenceHistory.add(value)
            } else {
                RunningRepository.cadenceHistory.removeAt(0)
                RunningRepository.cadenceHistory.add(value)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorJob?.cancel()
        serviceScope.cancel()
        RunningRepository.setMeasuring(false)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel("running_channel", "Running Tracking", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, "running_channel")
            .setContentTitle("Running Analyzer Active")
            .setContentText("Steps: $currentStepCount | SPM: ${currentCadence.toInt()}")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}