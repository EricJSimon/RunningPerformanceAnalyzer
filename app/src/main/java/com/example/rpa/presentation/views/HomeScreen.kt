package com.example.rpa.presentation.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.rpa.domain.model.Algorithm
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.util.Locale

@Composable
fun HomeScreen(
    isMeasuring: Boolean,
    stepCount: Int,
    currentCadence: Float,
    lowImpactCount: Int,
    mediumImpactCount: Int,
    highImpactCount: Int,
    linearAccelerometerData: FloatArray,
    gyroscopeData: FloatArray,
    currentAlgorithm: Algorithm,
    onAlgorithmChange: (Algorithm) -> Unit,
    strideHistory: List<Float>,
    onButtonClick: () -> Unit,
    onExportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Running Analyzer",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                title = "Total Steps",
                value = "$stepCount",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Cadence (SPM)",
                value = "%.0f".format(Locale.US, currentCadence),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Status: ${if (isMeasuring) "Measuring..." else "Idle"}",
            style = MaterialTheme.typography.bodyLarge,
            color = if (isMeasuring) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onButtonClick,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isMeasuring) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (isMeasuring) "STOP RUN" else "START RUN",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Stride Impact Pattern",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        // We check if history is not empty to display the graph
        if (strideHistory.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().height(250.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                // The graph composable handles the AndroidView updates
                StridePatternGraph(dataPoints = strideHistory)
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth().height(150.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Start running to see stride data", color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (currentAlgorithm == Algorithm.CUSTOM_ALGORITHM) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Impact Force Analysis", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    val total = (lowImpactCount + mediumImpactCount + highImpactCount).toFloat()
                    if (total > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                        ) {
                            if (lowImpactCount > 0) Box(Modifier.weight(lowImpactCount.toFloat()).fillMaxHeight().background(Color(0xFF4CAF50)))
                            if (mediumImpactCount > 0) Box(Modifier.weight(mediumImpactCount.toFloat()).fillMaxHeight().background(Color(0xFFFFC107)))
                            if (highImpactCount > 0) Box(Modifier.weight(highImpactCount.toFloat()).fillMaxHeight().background(Color(0xFFF44336)))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Soft: $lowImpactCount", color = Color(0xFF4CAF50), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text("Med: $mediumImpactCount", color = Color(0xFFFFA000), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text("Hard: $highImpactCount", color = Color(0xFFF44336), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text("No impact data yet.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Impact Force Analysis", style = MaterialTheme.typography.titleMedium)
                    Text("Switch to 'Custom' algorithm to view impact classification.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onExportClick,
            enabled = !isMeasuring,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Export Data to CSV")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Raw Sensor Data",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "Acc (Mag): ${linearAccelerometerData.contentToString()}", style = MaterialTheme.typography.bodySmall)
        Text(text = "Gyro (Rot): ${gyroscopeData.contentToString()}", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(16.dp))

        Text("Processing Algorithm:", style = MaterialTheme.typography.labelSmall)
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { onAlgorithmChange(Algorithm.HARDWARE_STEP_DETECTOR) },
                enabled = !isMeasuring && currentAlgorithm != Algorithm.HARDWARE_STEP_DETECTOR,
                modifier = Modifier.weight(1f)
            ) {
                Text("Step Counter")
            }
            Button(
                onClick = { onAlgorithmChange(Algorithm.CUSTOM_ALGORITHM) },
                enabled = !isMeasuring && currentAlgorithm != Algorithm.CUSTOM_ALGORITHM,
                modifier = Modifier.weight(1f)
            ) {
                Text("Custom")
            }
        }
    }
}

@Composable
fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, style = MaterialTheme.typography.labelMedium)
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun StridePatternGraph(dataPoints: List<Float>) {
    AndroidView(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                axisRight.isEnabled = false
                setTouchEnabled(false)
                setDrawGridBackground(false)
                xAxis.isEnabled = false
                axisLeft.axisMinimum = 0f
                legend.isEnabled = false
            }
        },
        update = { chart ->
            val entries = dataPoints.mapIndexed { index, value ->
                Entry(index.toFloat(), value)
            }

            if (chart.data != null && chart.data.dataSetCount > 0) {
                val set = chart.data.getDataSetByIndex(0) as LineDataSet
                set.values = entries
                set.notifyDataSetChanged()
                chart.data.notifyDataChanged()
                chart.notifyDataSetChanged()
            } else {
                val dataSet = LineDataSet(entries, "Impact").apply {
                    color = android.graphics.Color.BLUE
                    lineWidth = 2f
                    setDrawCircles(false)
                    setDrawValues(false)
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                    setDrawFilled(true)
                    fillColor = android.graphics.Color.CYAN
                    fillAlpha = 50
                }
                chart.data = LineData(dataSet)
            }
            chart.invalidate()
        }
    )
}

private fun FloatArray.contentToString(): String {
    return this.joinToString(separator = ", ") { "%.2f".format(Locale.US, it) }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(
        isMeasuring = true,
        stepCount = 1245,
        currentCadence = 165f,
        lowImpactCount = 800,
        mediumImpactCount = 300,
        highImpactCount = 145,
        linearAccelerometerData = floatArrayOf(0.1f, 9.8f, 0.2f),
        gyroscopeData = floatArrayOf(0.01f, 0.02f, 0.01f),
        currentAlgorithm = Algorithm.CUSTOM_ALGORITHM,
        onAlgorithmChange = {},
        strideHistory = listOf(10f, 12f, 8f, 15f, 11f, 13f, 9f, 14f),
        onButtonClick = {},
        onExportClick = {}
    )
}