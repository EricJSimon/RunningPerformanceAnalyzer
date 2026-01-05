package com.example.rpa.presentation.views

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.rpa.presentation.viewmodels.SensorViewModel
import com.example.rpa.ui.theme.SensorAppTheme

class MainActivity : ComponentActivity() {
    private val viewModel: SensorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            SensorAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val isMeasuring by viewModel.isMeasuring
                    val stepCount by viewModel.stepCount
                    val currentCadence by viewModel.currentCadence

                    val lowImpactCount by viewModel.lowImpactSteps
                    val mediumImpactCount by viewModel.mediumImpactSteps
                    val highImpactCount by viewModel.highImpactSteps

                    val linearAccelerometerData by viewModel.linearAccelerometerData
                    val gyroscopeData by viewModel.gyroscopeData
                    val currentAlgorithm by viewModel.currentAlgorithm
                    val strideHistory = viewModel.cadenceHistory

                    HomeScreen(
                        modifier = Modifier.padding(innerPadding),
                        isMeasuring = isMeasuring,
                        stepCount = stepCount,
                        currentCadence = currentCadence,
                        lowImpactCount = lowImpactCount,
                        mediumImpactCount = mediumImpactCount,
                        highImpactCount = highImpactCount,
                        linearAccelerometerData = linearAccelerometerData,
                        gyroscopeData = gyroscopeData,
                        currentAlgorithm = currentAlgorithm,
                        strideHistory = strideHistory,
                        onAlgorithmChange = { algorithm ->
                            viewModel.setAlgorithm(algorithm)
                        },
                        onButtonClick = {
                            if (isMeasuring) {
                                viewModel.stopMeasurement()
                            } else {
                                viewModel.startMeasurement()
                            }
                        },
                        onExportClick = {
                            viewModel.exportDataToCsv()
                        },
                    )
                }
            }
        }
    }
}