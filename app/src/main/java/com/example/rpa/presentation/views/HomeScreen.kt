package com.example.rpa.presentation.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rpa.domain.model.Algorithm
import java.util.Locale

@Composable
fun HomeScreen(
    isMeasuring: Boolean,
    armElevation: Float,
    linearAccelerometerData: FloatArray,
    gyroscopeData: FloatArray,
    currentAlgorithm: Algorithm,
    onAlgorithmChange: (Algorithm) -> Unit,
    elevationHistory: List<Float>,
    onButtonClick: () -> Unit,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Arm Elevation Monitor",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(40.dp))

        Text(
            "Algorithm",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { onAlgorithmChange(Algorithm.EWMA_FILTER) },
                enabled = !isMeasuring && currentAlgorithm != Algorithm.EWMA_FILTER
            ) {
                Text("EWMA Filter")
            }
            Button(
                onClick = { onAlgorithmChange(Algorithm.SENSOR_FUSION) },
                enabled = !isMeasuring && currentAlgorithm != Algorithm.SENSOR_FUSION
            ) {
                Text("Sensor Fusion")
            }
        }
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Arm Elevation:",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "${armElevation.toInt()}°",
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "Status: ${if (isMeasuring) "Measuring" else "Idle"}",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onButtonClick) {
            Text(text = if (isMeasuring) "Stop Measurement" else "Start Measurement")
        }
        Spacer(modifier = Modifier.height(32.dp))
        if(elevationHistory.isNotEmpty()) {
            ElevationGraph(dataPoints = elevationHistory)
        }
        Button(
            onClick = onExportClick,
            enabled = !isMeasuring
        ) {
            Text("Export Data to CSV")
        }
        Spacer(modifier = Modifier.height(60.dp))

        Text(
            text = "Raw Sensor Data",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Accelerometer: ${linearAccelerometerData.contentToString()}")
        Text(text = "Gyroscope: ${gyroscopeData.contentToString()}")
    }
}

private fun FloatArray.contentToString(): String {
    return this.joinToString(separator = ", ") { "%.2f".format(Locale.US, it) }
}