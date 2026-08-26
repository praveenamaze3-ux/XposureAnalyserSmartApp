package com.example.xposuredetectorsmart.ui.results

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.example.xposuredetectorsmart.ui.components.CircularGauge
import com.example.xposuredetectorsmart.ui.components.GlassCard
import com.example.xposuredetectorsmart.ui.components.HudBackground
import com.example.xposuredetectorsmart.ui.components.confidenceStatusColor
import com.example.xposuredetectorsmart.ui.components.ppmStatusColor
import com.example.xposuredetectorsmart.ui.theme.HudLabelStyle
import com.example.xposuredetectorsmart.utils.Constants
import com.example.xposuredetectorsmart.viewmodel.DoseAnalysisViewModel

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
                        value = s.ppm.toFloat(),
                        maxValue = (Constants.IDLH_PPM * 1.2).toFloat(),
                        valueText = "%.1f".format(s.ppm),
                        label = "ppm",
                        color = ppmStatusColor(s.ppm),
                        size = 180.dp,
                    )
                    CircularGauge(
                        value = s.confidence,
                        maxValue = 1f,
                        valueText = "${(s.confidence * 100).toInt()}%",
                        label = "confidence",
                        color = confidenceStatusColor(s.confidence),
                        size = 110.dp,
                        valueTextSize = MaterialTheme.typography.titleLarge.fontSize,
                    )
                }

                if (s.confidence < Constants.MIN_CONFIDENCE_WARNING) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Low confidence reading - consider retrying with better lighting/focus.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Spacer(Modifier.height(24.dp))
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "REFERENCE PATCHES DETECTED: ${s.patches.size}",
                        style = HudLabelStyle.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize),
                    )
                    s.patches.forEach { patch ->
                        Text(
                            "- ${patch.type}: saturation %.2f".format(patch.saturation),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    "BEFORE / AFTER CORRECTION",
                    style = HudLabelStyle.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        GlassCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                            Image(
                                bitmap = s.originalBitmap.asImageBitmap(),
                                contentDescription = "Original capture",
                                modifier = Modifier.fillMaxWidth().height(160.dp),
                            )
                        }
                        Text(
                            "ORIGINAL",
                            style = HudLabelStyle.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        GlassCard(contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                            Image(
                                bitmap = s.correctedBitmap.asImageBitmap(),
                                contentDescription = "Color-corrected capture",
                                modifier = Modifier.fillMaxWidth().height(160.dp),
                            )
                        }
                        Text(
                            "CORRECTED",
                            style = HudLabelStyle.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))
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
