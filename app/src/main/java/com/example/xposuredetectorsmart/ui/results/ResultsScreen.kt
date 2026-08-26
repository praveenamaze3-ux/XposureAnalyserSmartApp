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
import com.example.xposuredetectorsmart.ui.components.ConfidenceBadge
import com.example.xposuredetectorsmart.ui.components.PpmBadge
import com.example.xposuredetectorsmart.viewmodel.DoseAnalysisViewModel

@Composable
fun ResultsScreen(
    doseAnalysisViewModel: DoseAnalysisViewModel,
    onDone: () -> Unit,
    onRetry: () -> Unit,
) {
    val state by doseAnalysisViewModel.uiState.collectAsState()

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
                Text("Dose Result", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(16.dp))
                PpmBadge(ppm = s.ppm)
                Spacer(Modifier.height(8.dp))
                ConfidenceBadge(confidence = s.confidence)

                if (s.confidence < com.example.xposuredetectorsmart.utils.Constants.MIN_CONFIDENCE_WARNING) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Low confidence reading - consider retrying with better lighting/focus.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Spacer(Modifier.height(24.dp))
                Text("Reference patches detected: ${s.patches.size}", style = MaterialTheme.typography.titleSmall)
                s.patches.forEach { patch ->
                    Text(
                        "- ${patch.type}: saturation %.2f".format(patch.saturation),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Spacer(Modifier.height(24.dp))
                Text("Before / After correction", style = MaterialTheme.typography.titleSmall)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            bitmap = s.originalBitmap.asImageBitmap(),
                            contentDescription = "Original capture",
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                        )
                        Text("Original", style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            bitmap = s.correctedBitmap.asImageBitmap(),
                            contentDescription = "Color-corrected capture",
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                        )
                        Text("Corrected", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(Modifier.height(32.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { doseAnalysisViewModel.reset(); onRetry() }) { Text("Retry") }
                    Button(onClick = { doseAnalysisViewModel.reset(); onDone() }) { Text("Done") }
                }
            }

            is DoseAnalysisViewModel.UiState.Error -> {
                Spacer(Modifier.height(64.dp))
                Text("Could not read strip", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(s.message, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(24.dp))
                Button(onClick = { doseAnalysisViewModel.reset(); onRetry() }) { Text("Retry") }
            }

            is DoseAnalysisViewModel.UiState.Idle -> {
                // Nothing to show; ResultsScreen should only be reached after a capture starts.
            }
        }
    }
}
