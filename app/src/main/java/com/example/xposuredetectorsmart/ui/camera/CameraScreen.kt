package com.example.xposuredetectorsmart.ui.camera

import android.graphics.BitmapFactory
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.xposuredetectorsmart.ui.components.CameraHudOverlay
import com.example.xposuredetectorsmart.ui.components.CameraPermissionGate
import com.example.xposuredetectorsmart.ui.components.ScanFrame
import com.example.xposuredetectorsmart.ui.components.TelemetryText
import com.example.xposuredetectorsmart.ui.theme.HudLabelStyle
import com.example.xposuredetectorsmart.ui.theme.StatusWarning
import com.example.xposuredetectorsmart.utils.BitmapUtils
import com.example.xposuredetectorsmart.utils.Constants
import com.example.xposuredetectorsmart.viewmodel.DoseAnalysisViewModel
import com.example.xposuredetectorsmart.viewmodel.ShiftState
import com.example.xposuredetectorsmart.viewmodel.SharedShiftViewModel
import java.util.concurrent.Executors

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
        if (shiftState is ShiftState.NoShift) onNeedQr()
    }

    LaunchedEffect(analysisState) {
        val state = analysisState
        if (state is DoseAnalysisViewModel.UiState.Success || state is DoseAnalysisViewModel.UiState.Error) {
            viewModel.onCaptureFinished()
            onCaptured()
        }
    }

    val activeShift = shiftState as? ShiftState.Active ?: return

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    CameraPermissionGate {
        Box(modifier = Modifier.fillMaxSize()) {
            CaptureCameraPreview(onImageCaptureReady = { imageCapture = it })
            CameraHudOverlay(accentColor = StatusWarning, modifier = Modifier.align(Alignment.Center))

            ScanFrame(
                accentColor = StatusWarning,
                modifier = Modifier.align(Alignment.Center),
                size = Constants.STRIP_FRAME_SIZE_DP.dp,
                showScanLine = false,
            )

            Surface(
                color = Color.Black.copy(alpha = 0.55f),
                shape = RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp),
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "WORKER ${activeShift.context.workerId} | ${activeShift.context.shiftType} | ${activeShift.context.locationCode}",
                        color = Color.White,
                        style = HudLabelStyle.copy(fontSize = MaterialTheme.typography.bodyMedium.fontSize),
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
                        val capture = imageCapture ?: return@ShutterButton
                        viewModel.onCaptureStarted()
                        capture.takePicture(
                            cameraExecutor,
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    val bitmap = image.toRotatedBitmap()
                                    image.close()
                                    sharedShiftViewModel.registerBatchCapture()
                                    doseAnalysisViewModel.processCapture(
                                        bitmap = bitmap,
                                        workerId = activeShift.context.workerId,
                                        shiftDate = activeShift.context.shiftDate,
                                        location = activeShift.context.locationCode,
                                        stripSerial = activeShift.context.stripSerial,
                                    )
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
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture,
                    )
                    onImageCaptureReady(imageCapture)
                } catch (_: Exception) {
                    // Transient binding failure during rapid navigation; nothing user-actionable.
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
    )
}
