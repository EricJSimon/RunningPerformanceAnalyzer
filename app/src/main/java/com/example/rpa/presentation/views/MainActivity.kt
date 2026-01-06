package com.example.rpa.presentation.views

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val activityRecognitionGranted = permissions[android.Manifest.permission.ACTIVITY_RECOGNITION] ?: false
        val notificationGranted = permissions[android.Manifest.permission.POST_NOTIFICATIONS] ?: false

        if (activityRecognitionGranted) {
            android.util.Log.d("MainActivity", "Activity Recognition granted.")
        }

        if (!notificationGranted) {
            android.widget.Toast.makeText(
                this,
                "Notifications disabled: You won't see run status in the status bar.",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val permissionsToRequest = mutableListOf<String>()

            permissionsToRequest.add(android.Manifest.permission.ACTIVITY_RECOGNITION)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                permissionsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }

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