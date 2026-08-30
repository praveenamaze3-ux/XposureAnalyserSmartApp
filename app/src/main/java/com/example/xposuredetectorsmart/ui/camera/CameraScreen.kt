package com.example.xposuredetectorsmart.ui.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.xposuredetectorsmart.ui.components.CameraHudOverlay
import com.example.xposuredetectorsmart.ui.components.CameraPermissionGate
import com.example.xposuredetectorsmart.ui.components.ScanFrame
import com.example.xposuredetectorsmart.ui.components.ShiftClockBadge
import com.example.xposuredetectorsmart.ui.components.TelemetryText
import com.example.xposuredetectorsmart.ui.theme.HudLabelStyle
import com.example.xposuredetectorsmart.ui.theme.StatusWarning
import com.example.xposuredetectorsmart.utils.BitmapUtils
import com.example.xposuredetectorsmart.utils.Constants
import com.example.xposuredetectorsmart.utils.DateUtils
import com.example.xposuredetectorsmart.viewmodel.DoseAnalysisViewModel
import com.example.xposuredetectorsmart.viewmodel.ShiftState
import com.example.xposuredetectorsmart.viewmodel.SharedShiftViewModel
import com.example.xposuredetectorsmart.viewmodel.StripSession
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Composable
fun CameraScreen(
    sharedShiftViewModel: SharedShiftViewModel,
    doseAnalysisViewModel: DoseAnalysisViewModel,
    onCaptured: () -> Unit,
    onNeedQr: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel(),
) {
    val shiftState by sharedShiftViewModel.shiftState.collectAsState()
    val batchCount by sharedShiftViewModel.batchCount.collectAsState()
    val analysisState by doseAnalysisViewModel.uiState.collectAsState()
    val isCapturing by viewModel.isCapturing.collectAsState()

    LaunchedEffect(shiftState) {
        val active = shiftState as? ShiftState.Active
        val expired = active != null && sharedShiftViewModel.isShiftExpired()
        if (shiftState is ShiftState.NoShift || expired) {
            if (expired) sharedShiftViewModel.clearShift()
            onNeedQr()
        }
    }

    LaunchedEffect(analysisState) {
        val state = analysisState
        if (state is DoseAnalysisViewModel.UiState.Success || state is DoseAnalysisViewModel.UiState.Error) {
            onCaptured()
        }
    }

    val activeShift = shiftState as? ShiftState.Active ?: return

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pendingStrip by remember { mutableStateOf<StripSession?>(null) }
    var awaitingStripAnswer by remember { mutableStateOf(false) }

    fun resetCapture() {
        capturedBitmap = null
        pendingStrip = null
        awaitingStripAnswer = false
    }

    CameraPermissionGate {
        val bitmap = capturedBitmap
        val strip = pendingStrip

        if (awaitingStripAnswer) {
            StripIdentityDialog(
                onNewStrip = {
                    pendingStrip = sharedShiftViewModel.startNewStrip()
                    awaitingStripAnswer = false
                },
                onSameStrip = {
                    pendingStrip = sharedShiftViewModel.currentStrip.value
                    awaitingStripAnswer = false
                },
            )
        }

        if (bitmap != null && strip != null) {
            PatchPinpointScreen(
                bitmap = bitmap,
                isAnalyzing = analysisState is DoseAnalysisViewModel.UiState.Loading,
                onRetake = { resetCapture() },
                onConfirm = { points ->
                    sharedShiftViewModel.registerBatchCapture()
                    val shiftDurationHours = DateUtils.elapsedHours(activeShift.context.shiftStartedAt)
                    doseAnalysisViewModel.processCapture(
                        bitmap = bitmap,
                        points = points,
                        workerId = activeShift.context.workerId,
                        shiftDate = activeShift.context.shiftDate,
                        location = activeShift.context.locationCode,
                        stripSerial = strip.serial,
                        shiftDurationHours = shiftDurationHours,
                    )
                },
            )
            return@CameraPermissionGate
        }

        Box(modifier = Modifier.fillMaxSize()) {
            CaptureCameraPreview(onImageCaptureReady = { imageCapture = it })
            CameraHudOverlay(accentColor = StatusWarning, modifier = Modifier.align(Alignment.Center))

            ShiftClockBadge(
                shiftStartedAt = activeShift.context.shiftStartedAt,
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
            )

            ScanFrame(
                accentColor = StatusWarning,
                modifier = Modifier.align(Alignment.Center),
                size = Constants.STRIP_FRAME_SIZE_DP.dp,
                showScanLine = false,
            )
            Text(
                "Align White/Grey/Strip in frame · hold steady ~15cm",
                color = Color.White,
                style = HudLabelStyle.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = (Constants.STRIP_FRAME_SIZE_DP / 2 + 16).dp),
            )

            Surface(
                color = Color.Black.copy(alpha = 0.55f),
                shape = RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp),
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "WORKER ${activeShift.context.workerId} | ${activeShift.context.locationCode}",
                        color = Color.White,
                        style = HudLabelStyle.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize),
                    )
                    Text(
                        DateUtils.formatShiftWindow(activeShift.context.shiftStartedAt, activeShift.context.shiftExpiresAt),
                        color = Color.White,
                        style = HudLabelStyle.copy(fontSize = MaterialTheme.typography.labelMedium.fontSize),
                    )
                    if (batchCount > 0) {
                        Text(
                            "BATCH CAPTURES THIS SHIFT: $batchCount",
                            color = Color.White,
                            style = HudLabelStyle.copy(fontSize = MaterialTheme.typography.labelMedium.fontSize),
                        )
                    }
                    androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 4.dp))
                    TelemetryText(text = "Live feed", color = StatusWarning)
                }
            }

            Column(
                modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (isCapturing) {
                    CircularProgressIndicator(color = Color.White)
                    TelemetryText(text = "Processing", color = Color.White)
                } else {
                    Text(
                        "CAPTURE STRIP",
                        color = Color.White,
                        style = HudLabelStyle.copy(fontSize = MaterialTheme.typography.labelLarge.fontSize),
                    )
                    ShutterButton(onClick = {
                        if (sharedShiftViewModel.isShiftExpired()) {
                            sharedShiftViewModel.clearShift()
                            onNeedQr()
                            return@ShutterButton
                        }
                        val capture = imageCapture ?: return@ShutterButton
                        viewModel.onCaptureStarted()
                        capture.takePicture(
                            cameraExecutor,
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    val bmp = image.toRotatedBitmap()
                                    image.close()
                                    viewModel.onCaptureFinished()
                                    capturedBitmap = bmp
                                    val existingStrip = sharedShiftViewModel.currentStrip.value
                                    if (existingStrip == null) {
                                        // Nothing to compare against yet - this shift's first capture is
                                        // necessarily a new strip.
                                        pendingStrip = sharedShiftViewModel.startNewStrip()
                                    } else {
                                        awaitingStripAnswer = true
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    viewModel.onCaptureFinished()
                                }
                            },
                        )
                    })
                }
            }
        }
    }
}

/**
 * A passive colorimetric strip integrates exposure over its own wear period - re-photographing
 * the same strip must not add its new reading on top of the last one, or cumulative exposure
 * would be double-counted. This confirms which strip session a capture belongs to before it's
 * processed.
 */
@Composable
private fun StripIdentityDialog(onNewStrip: () -> Unit, onSameStrip: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* must resolve via one of the two actions */ },
        title = { Text("Confirm Strip Status") },
        text = {
            Text(
                "Cumulative exposure is tracked per physical strip. Is this capture from a " +
                    "newly issued strip, or a re-check of the strip already in use this shift?",
            )
        },
        confirmButton = {
            TextButton(onClick = onNewStrip) { Text("New Strip Issued") }
        },
        dismissButton = {
            TextButton(onClick = onSameStrip) { Text("Same Strip (Re-check)") }
        },
    )
}

@Composable
private fun ShutterButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(76.dp)
            .border(4.dp, Color.White, CircleShape)
            .padding(6.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White, CircleShape),
        )
    }
}

private fun ImageProxy.toRotatedBitmap(): android.graphics.Bitmap {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    return BitmapUtils.rotate(bitmap, imageInfo.rotationDegrees.toFloat())
}

@Composable
private fun CaptureCameraPreview(onImageCaptureReady: (ImageCapture) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .build()

                try {
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture,
                    )
                    // Force constant diffuse lighting and lock AE/AWB centered on the strip, so
                    // ambient lighting/exposure doesn't drift between the tapped patches.
                    camera.cameraControl.enableTorch(true)
                    val meteringPoint = SurfaceOrientedMeteringPointFactory(1f, 1f).createPoint(0.5f, 0.5f)
                    val meteringAction = FocusMeteringAction.Builder(
                        meteringPoint,
                        FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB,
                    ).setAutoCancelDuration(10, TimeUnit.SECONDS).build()
                    camera.cameraControl.startFocusAndMetering(meteringAction)

                    onImageCaptureReady(imageCapture)
                } catch (_: Exception) {
                    // Transient binding failure during rapid navigation; nothing user-actionable.
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
    )
}
