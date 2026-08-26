package com.example.xposuredetectorsmart.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Machine-vision style overlay drawn on top of a live camera preview: a faint
 * technical grid plus an edge vignette, so the feed reads as an industrial
 * inspection camera rather than a plain phone photo.
 */
@Composable
fun CameraHudOverlay(accentColor: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val step = 32.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawLine(accentColor.copy(alpha = 0.06f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
            x += step
        }
        var y = 0f
        while (y < size.height) {
            drawLine(accentColor.copy(alpha = 0.06f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            y += step
        }

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)),
                center = Offset(size.width / 2, size.height / 2),
                radius = size.maxDimension * 0.75f,
            ),
        )
    }
}
