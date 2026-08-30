package com.example.xposuredetectorsmart.ui.qr

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.xposuredetectorsmart.database.entities.WorkerContext
import com.example.xposuredetectorsmart.ui.components.CameraHudOverlay
import com.example.xposuredetectorsmart.ui.components.CameraPermissionGate
import com.example.xposuredetectorsmart.ui.components.ScanFrame
import com.example.xposuredetectorsmart.ui.components.TelemetryText
import com.example.xposuredetectorsmart.ui.theme.HudLabelStyle
import com.example.xposuredetectorsmart.ui.theme.SignalCyan
import com.example.xposuredetectorsmart.utils.DateUtils
import com.example.xposuredetectorsmart.viewmodel.ShiftState
import com.example.xposuredetectorsmart.viewmodel.SharedShiftViewModel
import java.util.concurrent.Executors

@Composable
fun QRScannerScreen(
    sharedShiftViewModel: SharedShiftViewModel,
    onWorkerIdentified: () -> Unit,
    onContinueExistingShift: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: QRScannerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var pendingRescan by remember { mutableStateOf<WorkerContext?>(null) }

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is QRScanUiState.Scanned) {
            // A worker's wristband QR is permanent and gets re-scanned mid-shift too (to check
            // their exposure info) - that must not silently restart their shift clock. Only
            // treat this as starting a fresh shift when there's no active shift for this same
            // worker already.
            val active = sharedShiftViewModel.shiftState.value as? ShiftState.Active
            if (active != null && active.context.workerId == state.context.workerId) {
                pendingRescan = active.context
            } else {
                sharedShiftViewModel.startShift(state.context)
                onWorkerIdentified()
            }
        }
    }

    pendingRescan?.let { activeContext ->
        ShiftEndConfirmationDialog(
            elapsedText = DateUtils.formatElapsedHm(activeContext.shiftStartedAt),
            onEndShift = {
                sharedShiftViewModel.endShift()
                pendingRescan = null
                viewModel.resetToScanning()
            },
            onContinueShift = {
                pendingRescan = null
                viewModel.resetToScanning()
                onContinueExistingShift()
            },
        )
    }

    CameraPermissionGate {
        Box(modifier = Modifier.fillMaxSize()) {
            QRCameraPreview(onFrame = viewModel::onFrame)
            CameraHudOverlay(accentColor = SignalCyan, modifier = Modifier.align(Alignment.Center))

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = SignalCyan,
                )
            }

            ScanFrame(
                accentColor = SignalCyan,
                modifier = Modifier.align(Alignment.Center),
                size = 260.dp,
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SignalCyan.copy(alpha = 0.4f)),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "SCAN WRISTBAND QR",
                            color = Color.White,
                            style = HudLabelStyle.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize),
                            textAlign = TextAlign.Center,
                        )
                        androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 4.dp))
                        TelemetryText(text = "Searching for worker", color = SignalCyan)
                    }
                }

                val invalidState = uiState
                if (invalidState is QRScanUiState.Invalid) {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(4.dp)) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(invalidState.message, color = MaterialTheme.colorScheme.onErrorContainer)
                            Button(
                                onClick = { viewModel.resetToScanning() },
                                shape = MaterialTheme.shapes.extraLarge,
                            ) { com.example.xposuredetectorsmart.ui.components.HudButtonLabel("Try again") }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Shown when the worker re-scans their own wristband mid-shift. Re-scanning is also how they
 * pull up their current exposure info, so this must not be silently treated as ending the
 * shift - it always asks first.
 */
@Composable
private fun ShiftEndConfirmationDialog(elapsedText: String, onEndShift: () -> Unit, onContinueShift: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* must resolve via one of the two actions */ },
        title = { Text("Active shift in progress") },
        text = { Text("Your shift has been running for $elapsedText. Are you ending your shift?") },
        confirmButton = {
            TextButton(onClick = onEndShift) { Text("End Shift") }
        },
        dismissButton = {
            TextButton(onClick = onContinueShift) { Text("Continue Shift") }
        },
    )
}

@Composable
private fun QRCameraPreview(onFrame: (androidx.camera.core.ImageProxy) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    AndroidView(
        modifier = Modifier.fillMaxSize().aspectRatio(9f / 16f),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(cameraExecutor, onFrame) }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis,
                    )
                } catch (_: Exception) {
                    // Camera binding can fail transiently during rapid navigation; nothing user-actionable to do.
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
    )
}
