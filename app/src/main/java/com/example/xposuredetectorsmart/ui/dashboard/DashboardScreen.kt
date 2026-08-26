package com.example.xposuredetectorsmart.ui.dashboard

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.xposuredetectorsmart.database.entities.DoseLog
import com.example.xposuredetectorsmart.utils.Constants
import com.example.xposuredetectorsmart.utils.DateUtils
import com.example.xposuredetectorsmart.viewmodel.AlertViewModel
import com.example.xposuredetectorsmart.viewmodel.ShiftState
import com.example.xposuredetectorsmart.viewmodel.SharedShiftViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

@Composable
fun DashboardScreen(
    sharedShiftViewModel: SharedShiftViewModel,
    onNewCapture: () -> Unit,
    onExportPdf: () -> Unit,
    onSettings: () -> Unit,
    onSwitchWorkerRequested: () -> Unit,
    dashboardViewModel: DashboardViewModel = hiltViewModel(),
    alertViewModel: AlertViewModel = hiltViewModel(),
) {
    val shiftState by sharedShiftViewModel.shiftState.collectAsState()
    val isOnline by dashboardViewModel.isOnline.collectAsState()
    val isAlertActive by alertViewModel.isAlertActive.collectAsState()

    val active = shiftState as? ShiftState.Active

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        if (active == null) {
            Text("No active shift. Scan a QR code to begin.", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onSwitchWorkerRequested) { Text("Scan QR") }
            return@Column
        }

        val workerId = active.context.workerId
        val shiftDate = active.context.shiftDate

        LaunchedEffect(workerId, shiftDate) {
            alertViewModel.watch(workerId, shiftDate)
        }

        val logs by remember(workerId, shiftDate) { dashboardViewModel.shiftLogs(workerId, shiftDate) }
            .collectAsState(initial = emptyList())
        val cumulative by remember(workerId, shiftDate) { dashboardViewModel.cumulativeDose(workerId, shiftDate) }
            .collectAsState(initial = 0.0)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Shift: $workerId | $shiftDate | ${active.context.locationCode}", style = MaterialTheme.typography.titleMedium)
                Text(if (isOnline) "Online - syncing automatically" else "Offline - captures queued locally", style = MaterialTheme.typography.bodySmall)
                if (isAlertActive) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "ALERT: cumulative exposure ${"%.1f".format(cumulative)} ppm exceeds ${Constants.ALERT_THRESHOLD_PPM.toInt()} ppm",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Exposure trend", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                ExposureTrendChart(logs = logs, modifier = Modifier.fillMaxWidth().height(220.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Statistics", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Text("Cumulative dose: %.2f ppm".format(cumulative))
                Text("Captures this shift: ${logs.size}")
                Text("Average confidence: %.0f%%".format(dashboardViewModel.averageConfidence(logs) * 100))
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onNewCapture) { Text("New Capture") }
            OutlinedButton(onClick = onExportPdf) { Text("Export PDF") }
            OutlinedButton(onClick = onSettings) { Text("Settings") }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { dashboardViewModel.requestManualSync() }) { Text("Sync now") }
    }
}

@Composable
private fun ExposureTrendChart(logs: List<DoseLog>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary.toArgb()
    val labelColor = MaterialTheme.colorScheme.onSurface.toArgb()

    AndroidView(
        modifier = modifier,
        factory = { context -> LineChart(context) },
        update = { chart ->
            var running = 0.0
            val entries = logs.mapIndexed { index, log ->
                running += log.dosePpm
                Entry(DateUtils.hoursSinceMidnight(log.timestamp).toFloat(), running.toFloat())
            }

            val dataSet = LineDataSet(entries, "Cumulative PPM").apply {
                color = lineColor
                setCircleColor(lineColor)
                valueTextColor = labelColor
                lineWidth = 2.5f
                circleRadius = 3f
                setDrawValues(false)
            }

            chart.data = LineData(dataSet)
            chart.axisRight.isEnabled = false
            chart.axisLeft.textColor = labelColor
            chart.xAxis.textColor = labelColor
            chart.legend.textColor = labelColor
            chart.description.isEnabled = false

            chart.axisLeft.removeAllLimitLines()
            chart.axisLeft.addLimitLine(
                LimitLine(Constants.OSHA_PEL_8HR.toFloat(), "PEL").apply { setLineColor(AndroidColor.rgb(230, 160, 0)) },
            )
            chart.axisLeft.addLimitLine(
                LimitLine(Constants.IDLH_PPM.toFloat(), "IDLH").apply { setLineColor(AndroidColor.RED) },
            )

            chart.invalidate()
        },
    )
}
