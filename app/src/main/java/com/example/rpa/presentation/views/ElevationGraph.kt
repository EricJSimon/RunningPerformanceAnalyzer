package com.example.rpa.presentation.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

@Composable
fun ElevationGraph(
    dataPoints: List<Float>,
    modifier: Modifier = Modifier
) {
    if (dataPoints.isEmpty()) {
        return
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    Column(modifier = modifier) {
        Text(
            "Elevation History",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )

        @Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16 / 9f)
                .padding(horizontal = 16.dp),
            factory = { context ->
                LineChart(context).apply {
                    description.isEnabled = false
                    legend.isEnabled = false
                    isDragEnabled = true
                    isScaleXEnabled = true
                    isScaleYEnabled = false
                    setDrawGridBackground(false)

                    xAxis.position = XAxis.XAxisPosition.BOTTOM
                    xAxis.setDrawGridLines(true)
                    xAxis.textColor = Color.Black.toArgb()
                    xAxis.axisLineColor = Color.Black.toArgb()

                    axisLeft.setDrawGridLines(true)
                    axisLeft.textColor = Color.Black.toArgb()
                    axisLeft.axisLineColor = Color.Black.toArgb()

                    axisRight.isEnabled = false
                }
            },
            update = { lineChart ->
                val entries = dataPoints.mapIndexed { index, value ->
                    Entry(index.toFloat(), value)
                }

                val dataSet = LineDataSet(entries, "Elevation").apply {
                    color = primaryColor.toArgb()
                    valueTextColor = Color.Black.toArgb()
                    setDrawValues(false)
                    setDrawCircles(false)
                    lineWidth = 2f
                }

                lineChart.data = LineData(dataSet)

                lineChart.invalidate()
            }
        )
    }
}