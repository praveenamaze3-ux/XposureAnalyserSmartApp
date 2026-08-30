package com.example.xposuredetectorsmart.ui.results

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.xposuredetectorsmart.ui.components.CircularGauge
import com.example.xposuredetectorsmart.ui.components.GlassCard
import com.example.xposuredetectorsmart.ui.components.HudBackground
import com.example.xposuredetectorsmart.ui.components.ppmStatusColor
import com.example.xposuredetectorsmart.ui.theme.HudLabelStyle
import com.example.xposuredetectorsmart.ui.theme.SignalCyan
import com.example.xposuredetectorsmart.utils.Constants
import com.example.xposuredetectorsmart.utils.DateUtils
import com.example.xposuredetectorsmart.utils.RgbColor
import com.example.xposuredetectorsmart.viewmodel.DoseAnalysisViewModel

private fun RgbColor.toComposeColor(): Color = Color(
    red = (r / 255.0).coerceIn(0.0, 1.0).toFloat(),
    green = (g / 255.0).coerceIn(0.0, 1.0).toFloat(),
    blue = (b / 255.0).coerceIn(0.0, 1.0).toFloat(),
)

@Composable
fun ResultsScreen(
    doseAnalysisViewModel: DoseAnalysisViewModel,
    onDone: () -> Unit,
    onRetry: () -> Unit,
) {
    val state by doseAnalysisViewModel.uiState.collectAsState()

    HudBackground {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (val s = state) {
            is DoseAnalysisViewModel.UiState.Loading -> {
                Spacer(Modifier.height(64.dp))
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Analyzing strip...", style = MaterialTheme.typography.bodyLarge)
            }

            is DoseAnalysisViewModel.UiState.Success -> {
                Text("DOSE RESULT", style = HudLabelStyle.copy(fontSize = MaterialTheme.typography.headlineSmall.fontSize))
                Spacer(Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularGauge(
                        value = s.dose.shiftAveragePpm.toFloat(),
                        maxValue = Constants.MAX_EXPECTED_CONCENTRATION_PPM.toFloat(),
                        valueText = "%.1f".format(s.dose.shiftAveragePpm),
                        label = "ppm avg",
                        color = ppmStatusColor(s.dose.shiftAveragePpm),
                        size = 180.dp,
                    )
                    CircularGauge(
                        value = s.confidence,
                        maxValue = 1f,
                        valueText = "${(s.confidence * 100).toInt()}%",
                        label = "confidence",
                        color = SignalCyan,
                        size = 110.dp,
                        valueTextSize = MaterialTheme.typography.titleLarge.fontSize,
                    )
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = s.dose.riskLevel.description,
                    style = MaterialTheme.typography.titleMedium,
                    color = ppmStatusColor(s.dose.shiftAveragePpm),
                )

                s.dose.warningMessage?.let { warning ->
                    Spacer(Modifier.height(8.dp))
                    Text(warning, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    "Measured at ${DateUtils.formatTimestamp(s.timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(24.dp))
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "EXPOSURE BREAKDOWN",
                        style = HudLabelStyle.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize),
                    )
                    Text(
                        "Optical density: %.3f".format(s.dose.opticalDensity),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        "Total dose: %.2f ppm·hr".format(s.dose.totalDosePpmHours),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "8-hr TWA: %.2f ppm".format(s.dose.eightHourTwaPpm),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    "BLANK / SAMPLE COLOR",
                    style = HudLabelStyle.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    ColorSwatchCard(
                        label = "BLANK (WHITE REF)",
                        color = s.blankColor.toComposeColor(),
                        rgbText = "RGB(${s.blankColor.r.toInt()}, ${s.blankColor.g.toInt()}, ${s.blankColor.b.toInt()})",
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(12.dp))
                    ColorSwatchCard(
                        label = "SAMPLE (STRIP)",
                        color = s.sampleColor.toComposeColor(),
                        rgbText = "RGB(${s.sampleColor.r.toInt()}, ${s.sampleColor.g.toInt()}, ${s.sampleColor.b.toInt()})",
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(24.dp))
                Text(
                    "* Estimated value based on colorimetric approximation. Not for medical diagnosis.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { doseAnalysisViewModel.reset(); onRetry() },
                        shape = MaterialTheme.shapes.extraLarge,
                    ) { com.example.xposuredetectorsmart.ui.components.HudButtonLabel("Retry") }
                    Button(
                        onClick = { doseAnalysisViewModel.reset(); onDone() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = MaterialTheme.shapes.extraLarge,
                    ) { com.example.xposuredetectorsmart.ui.components.HudButtonLabel("Done") }
                }
            }

            is DoseAnalysisViewModel.UiState.Error -> {
                Spacer(Modifier.height(64.dp))
                Text("Could not read strip", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(s.message, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { doseAnalysisViewModel.reset(); onRetry() },
                    shape = MaterialTheme.shapes.extraLarge,
                ) { com.example.xposuredetectorsmart.ui.components.HudButtonLabel("Retry") }
            }

            is DoseAnalysisViewModel.UiState.Idle -> {
                // Nothing to show; ResultsScreen should only be reached after a capture starts.
            }
        }
    }
    }
}

@Composable
private fun ColorSwatchCard(label: String, color: Color, rgbText: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        GlassCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(color, RoundedCornerShape(4.dp)),
            )
        }
        Text(
            label,
            style = HudLabelStyle.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(rgbText, style = MaterialTheme.typography.labelSmall)
    }
}
