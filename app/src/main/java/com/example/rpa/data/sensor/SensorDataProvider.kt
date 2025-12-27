package com.example.rpa.data.sensor

import android.content.Context

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.rpa.domain.model.Algorithm
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class SensorDataProvider(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val linearAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    fun getSensorData(algorithm: Algorithm): Flow<SensorEvent> = callbackFlow {
        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let { launch { send(it) } }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            }
        }

        when (algorithm) {
            Algorithm.EWMA_FILTER -> {
                sensorManager.registerListener(sensorListener, gravitySensor, SensorManager.SENSOR_DELAY_UI)
                sensorManager.registerListener(sensorListener, linearAccelerometer, SensorManager.SENSOR_DELAY_UI)
            }
            Algorithm.SENSOR_FUSION -> {
                sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
                sensorManager.registerListener(sensorListener, gyroscope, SensorManager.SENSOR_DELAY_UI)
            }
        }
        awaitClose {
            sensorManager.unregisterListener(sensorListener)
        }
    }
}