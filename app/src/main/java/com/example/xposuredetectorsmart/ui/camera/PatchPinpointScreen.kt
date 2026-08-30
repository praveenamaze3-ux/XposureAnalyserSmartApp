package com.example.xposuredetectorsmart.ui.camera

import android.graphics.Bitmap
import android.graphics.Point
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.xposuredetectorsmart.imageprocessing.ManualPatchPoints
import com.example.xposuredetectorsmart.imageprocessing.PatchType
import com.example.xposuredetectorsmart.ui.components.HudButtonLabel
import com.example.xposuredetectorsmart.ui.theme.HudLabelStyle

private data class TappedPoint(val screenOffset: Offset, val bitmapPoint: Point, val previewColor: Color)

// Matches the reference implementation's pin colors exactly: White/Grey/Strip.
private val pinpointSteps = listOf(
    Triple(PatchType.WHITE, "Tap the WHITE reference patch", Color(0xFF2563EB)),
    Triple(PatchType.GREY, "Tap the GREY reference patch", Color(0xFF8B5CF6)),
    Triple(PatchType.INK, "Tap the strip's exposed color", Color(0xFFEA580C)),
)

/**
 * Shown after a strip photo is captured. The worker taps the White Ref, Grey Ref, and exposed
 * strip-color patches in sequence directly on the photo (10x10px average per tap), replacing
 * automatic patch detection.
 */
@Composable
fun PatchPinpointScreen(
    bitmap: Bitmap,
    isAnalyzing: Boolean,
    onRetake: () -> Unit,
    onConfirm: (ManualPatchPoints) -> Unit,
) {
    var points by remember { mutableStateOf<List<Pair<PatchType, TappedPoint>>>(emptyList()) }
    var imageSizePx by remember { mutableStateOf(IntSize.Zero) }

    val allPlaced = points.size == pinpointSteps.size
    val currentStepIndex = points.size.coerceAtMost(pinpointSteps.size - 1)

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(color = Color.Black.copy(alpha = 0.6f)) {
                Text(
                    text = if (allPlaced) "All patches marked" else pinpointSteps[currentStepIndex].second,
                    color = Color.White,
                    style = HudLabelStyle.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.4f)).padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                pinpointSteps.forEachIndexed { index, (type, _, color) ->
                    val tapped = points.getOrNull(index)?.second
                    ReadoutChip(label = type.name, color = color, tappedColor = tapped?.previewColor)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
                    .onSizeChanged { imageSizePx = it }
                    .pointerInput(bitmap, points.size) {
                        detectTapGestures { offset ->
                            if (points.size >= pinpointSteps.size) return@detectTapGestures
                            val size = imageSizePx
                            if (size.width == 0 || size.height == 0) return@detectTapGestures
                            val bx = (offset.x / size.width * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
                            val by = (offset.y / size.height * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
                            val previewColor = Color(bitmap.getPixel(bx, by))
                            val type = pinpointSteps[points.size].first
                            points = points + (type to TappedPoint(offset, Point(bx, by), previewColor))
                        }
                    },
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Captured strip photo",
                    modifier = Modifier.fillMaxSize(),
                )

                points.forEachIndexed { index, (type, tapped) ->
                    val pinColor = pinpointSteps.first { it.first == type }.third
                    MarkerDot(offset = tapped.screenOffset, label = "${index + 1}", color = pinColor)
                }
            }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(color = Color.White)
                Text("Analyzing...", color = Color.White, style = MaterialTheme.typography.bodySmall)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onRetake, shape = MaterialTheme.shapes.extraLarge) {
                        HudButtonLabel("Retake")
                    }
                    if (points.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { points = points.dropLast(1) },
                            shape = MaterialTheme.shapes.extraLarge,
                        ) { HudButtonLabel("Undo") }
                    }
                    Button(
                        onClick = {
                            val byType = points.toMap()
                            val white = byType[PatchType.WHITE]?.bitmapPoint ?: return@Button
                            val grey = byType[PatchType.GREY]?.bitmapPoint ?: return@Button
                            val ink = byType[PatchType.INK]?.bitmapPoint ?: return@Button
                            onConfirm(ManualPatchPoints(white = white, grey = grey, ink = ink))
                        },
                        enabled = allPlaced,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = MaterialTheme.shapes.extraLarge,
                    ) { HudButtonLabel("Analyze") }
                }
            }
        }
    }
}

@Composable
private fun ReadoutChip(label: String, color: Color, tappedColor: Color?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color),
        )
        Column(modifier = Modifier.padding(start = 6.dp)) {
            Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall)
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 12.dp)
                    .background(tappedColor ?: Color.DarkGray, RoundedCornerShape(2.dp)),
            )
        }
    }
}

@Composable
private fun MarkerDot(offset: Offset, label: String, color: Color) {
    Box(
        modifier = Modifier
            .offset { IntOffset(offset.x.toInt() - 14, offset.y.toInt() - 14) }
            .size(28.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, style = MaterialTheme.typography.labelMedium)
    }
}
