package com.example.rpa.presentation.views

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.example.rpa.presentation.viewmodels.SensorViewModel
import com.example.rpa.ui.theme.SensorAppTheme

class MainActivity : ComponentActivity() {
    private val viewModel: SensorViewModel by viewModels()

    private val showPermissionDialog = mutableStateOf(false)

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                showPermissionDialog.value = true
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissionLauncher.launch(Manifest.permission.BODY_SENSORS)

        enableEdgeToEdge()
        setContent {
            SensorAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val isMeasuring by viewModel.isMeasuring
                    val armElevation by viewModel.armElevation
                    val linearAccelerometerData by viewModel.linearAccelerometerData
                    val gyroscopeData by viewModel.gyroscopeData
                    val currentAlgorithm by viewModel.currentAlgorithm
                    val elevationHistory = viewModel.elevationHistory

                    if (showPermissionDialog.value) {
                        AlertDialog(
                            onDismissRequest = {
                                showPermissionDialog.value = false
                            },
                            title = { Text("Permission Required") },
                            text = { Text("This app needs access to body sensors to measure arm elevation. Please grant the permission in the app settings.") },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showPermissionDialog.value = false
                                        val intent =
                                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                        val uri = Uri.fromParts("package", packageName, null)
                                        intent.data = uri
                                        startActivity(intent)
                                    }
                                ) {
                                    Text("Open Settings")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showPermissionDialog.value = false }) {
                                    Text("Dismiss")
                                }
                            }
                        )
                    }

                    HomeScreen(
                        modifier = Modifier.padding(innerPadding),
                        isMeasuring = isMeasuring,
                        armElevation = armElevation,
                        linearAccelerometerData = linearAccelerometerData,
                        gyroscopeData = gyroscopeData,
                        currentAlgorithm = currentAlgorithm,
                        elevationHistory = elevationHistory,
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