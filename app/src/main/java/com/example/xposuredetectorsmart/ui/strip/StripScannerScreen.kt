package com.example.xposuredetectorsmart.ui.strip

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.xposuredetectorsmart.ui.components.CameraHudOverlay
import com.example.xposuredetectorsmart.ui.components.CameraPermissionGate
import com.example.xposuredetectorsmart.ui.components.HudButtonLabel
import com.example.xposuredetectorsmart.ui.components.ScanFrame
import com.example.xposuredetectorsmart.ui.components.TelemetryText
import com.example.xposuredetectorsmart.ui.theme.HudLabelStyle
import com.example.xposuredetectorsmart.ui.theme.StatusWarning
import com.example.xposuredetectorsmart.viewmodel.SharedShiftViewModel
import java.util.concurrent.Executors

/**
 * Second step of shift setup: pairs the disposable chemical strip (issued fresh before the
 * shift) with the worker identified in [ui.qr.QRScannerScreen], by scanning the strip's own
 * serial QR. The shift doesn't actually start until this pairing completes.
 */
@Composable
fun StripScannerScreen(
    sharedShiftViewModel: SharedShiftViewModel,
    onPaired: () -> Unit,
    onNeedWorker: () -> Unit,
    viewModel: StripScannerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    // Guard against reaching this screen with nothing to pair (e.g. process death mid-flow
    // restored the back stack here). Checked once on entry only — pendingContext is
    // intentionally cleared by startShift() right after a successful pairing below, so a
    // reactive check here would misfire and race with onPaired().
    LaunchedEffect(Unit) {
        if (sharedShiftViewModel.pendingContext.value == null) onNeedWorker()
    }

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is StripScanUiState.Scanned) {
            val context = sharedShiftViewModel.pendingContext.value
            if (context == null) {
                onNeedWorker()
            } else {
                sharedShiftViewModel.startShift(context.copy(stripSerial = state.data.stripSerial))
                onPaired()
            }
        }
    }

    CameraPermissionGate {
        Box(modifier = Modifier.fillMaxSize()) {
            StripCameraPreview(onFrame = viewModel::onFrame)
            CameraHudOverlay(accentColor = StatusWarning, modifier = Modifier.align(Alignment.Center))

            ScanFrame(
                accentColor = StatusWarning,
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
                    border = BorderStroke(1.dp, StatusWarning.copy(alpha = 0.4f)),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "SCAN STRIP SERIAL",
                            color = Color.White,
                            style = HudLabelStyle.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize),
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.padding(top = 4.dp))
                        TelemetryText(text = "Pairing strip", color = StatusWarning)
                    }
                }

                val invalidState = uiState
                if (invalidState is StripScanUiState.Invalid) {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(4.dp)) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(invalidState.message, color = MaterialTheme.colorScheme.onErrorContainer)
                            Button(
                                onClick = { viewModel.resetToScanning() },
                                shape = MaterialTheme.shapes.extraLarge,
                            ) { HudButtonLabel("Try again") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StripCameraPreview(onFrame: (androidx.camera.core.ImageProxy) -> Unit) {
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
