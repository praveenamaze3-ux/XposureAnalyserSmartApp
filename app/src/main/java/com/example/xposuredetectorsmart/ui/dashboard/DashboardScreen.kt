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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.xposuredetectorsmart.database.entities.DoseLog
import com.example.xposuredetectorsmart.ui.components.AlertBanner
import com.example.xposuredetectorsmart.ui.components.AppHeader
import com.example.xposuredetectorsmart.ui.components.GlassCard
import com.example.xposuredetectorsmart.ui.components.HudButtonLabel
import com.example.xposuredetectorsmart.ui.components.SyncStatusChip
import com.example.xposuredetectorsmart.ui.theme.HudLabelStyle
import com.example.xposuredetectorsmart.ui.theme.HudNumberStyle
import com.example.xposuredetectorsmart.ui.theme.StatusCritical
import com.example.xposuredetectorsmart.ui.theme.StatusWarning
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

    com.example.xposuredetectorsmart.ui.components.HudBackground {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        AppHeader(title = "Dashboard", icon = Icons.Filled.Sensors)
        Spacer(Modifier.height(16.dp))

        if (active == null) {
            Text("No active shift. Scan a QR code to begin.", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onSwitchWorkerRequested,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = MaterialTheme.shapes.extraLarge,
            ) { HudButtonLabel("Scan QR") }
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

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("Shift: $workerId | $shiftDate | ${active.context.locationCode}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            SyncStatusChip(isOnline = isOnline)
            if (isAlertActive) {
                Spacer(Modifier.height(12.dp))
                AlertBanner(
                    message = "ALERT: cumulative exposure ${"%.1f".format(cumulative)} ppm exceeds ${Constants.ALERT_THRESHOLD_PPM.toInt()} ppm",
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("EXPOSURE TREND", style = HudLabelStyle.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize))
            Spacer(Modifier.height(8.dp))
            ExposureTrendChart(logs = logs, modifier = Modifier.fillMaxWidth().height(220.dp))
        }

        Spacer(Modifier.height(16.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("STATISTICS", style = HudLabelStyle.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize))
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatTile(
                    icon = Icons.Filled.Bolt,
                    value = "%.1f".format(cumulative),
                    label = "ppm dose",
                )
                StatTile(
                    icon = Icons.Filled.PhotoCamera,
                    value = "${logs.size}",
                    label = "captures",
                )
                StatTile(
                    icon = Icons.Filled.Verified,
                    value = "${(dashboardViewModel.averageConfidence(logs) * 100).toInt()}%",
                    label = "confidence",
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onNewCapture,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = MaterialTheme.shapes.extraLarge,
            ) { HudButtonLabel("New Capture") }
            OutlinedButton(onClick = onExportPdf, shape = MaterialTheme.shapes.extraLarge) { HudButtonLabel("Export PDF") }
            OutlinedButton(onClick = onSettings, shape = MaterialTheme.shapes.extraLarge) { HudButtonLabel("Settings") }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { dashboardViewModel.requestManualSync() },
            shape = MaterialTheme.shapes.extraLarge,
        ) { HudButtonLabel("Sync now") }
    }
    }
}

@Composable
private fun StatTile(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = HudNumberStyle.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize),
        )
        Text(
            text = label.uppercase(),
            style = HudLabelStyle.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ExposureTrendChart(logs: List<DoseLog>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary.toArgb()
    val labelColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f).toArgb()
    val warningColor = StatusWarning.toArgb()
    val criticalColor = StatusCritical.toArgb()

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
                mode = LineDataSet.Mode.CUBIC_BEZIER
                setDrawFilled(true)
                fillColor = lineColor
                fillAlpha = 60
            }

            chart.data = LineData(dataSet)
            chart.axisRight.isEnabled = false
            chart.axisLeft.textColor = labelColor
            chart.axisLeft.gridColor = gridColor
            chart.xAxis.textColor = labelColor
            chart.xAxis.setDrawGridLines(false)
            chart.legend.textColor = labelColor
            chart.description.isEnabled = false

            chart.axisLeft.removeAllLimitLines()
            chart.axisLeft.addLimitLine(
                LimitLine(Constants.OSHA_PEL_8HR.toFloat(), "PEL").apply {
                    setLineColor(warningColor)
                    lineWidth = 1.5f
                    enableDashedLine(12f, 8f, 0f)
                    textColor = warningColor
                },
            )
            chart.axisLeft.addLimitLine(
                LimitLine(Constants.IDLH_PPM.toFloat(), "IDLH").apply {
                    setLineColor(criticalColor)
                    lineWidth = 1.5f
                    enableDashedLine(12f, 8f, 0f)
                    textColor = criticalColor
                },
            )

            chart.invalidate()
        },
    )
}
