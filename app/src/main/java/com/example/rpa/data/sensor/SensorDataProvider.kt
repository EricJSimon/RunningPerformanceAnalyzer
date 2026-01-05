package com.example.rpa.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.example.rpa.domain.model.Algorithm
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

private const val TAG = "SensorDataProvider"

class SensorDataProvider(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    fun getSensorData(algorithm: Algorithm): Flow<SensorEvent> = callbackFlow {
        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    trySend(it)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            }
        }

        sensorManager.registerListener(
            sensorListener,
            accelerometer,
            SensorManager.SENSOR_DELAY_GAME
        )

        sensorManager.registerListener(
            sensorListener,
            gyroscope,
            SensorManager.SENSOR_DELAY_GAME
        )

        when (algorithm) {
            Algorithm.HARDWARE_STEP_DETECTOR -> {
                if (stepDetectorSensor != null) {
                    Log.d(TAG, "Registering Hardware Step Detector")
                    sensorManager.registerListener(
                        sensorListener,
                        stepDetectorSensor,
                        SensorManager.SENSOR_DELAY_FASTEST
                    )
                } else {
                    Log.e(TAG, "Hardware Step Detector sensor is NOT available on this device.")
                }
            }
            Algorithm.CUSTOM_ALGORITHM -> {
                Log.d(TAG, "Using Custom Algorithm (Accel+Gyro already registered)")
            }
        }

        awaitClose {
            Log.d(TAG, "Unregistering all sensors")
            sensorManager.unregisterListener(sensorListener)
        }
    }
}