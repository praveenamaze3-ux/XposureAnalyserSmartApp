package com.example.xposuredetectorsmart.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Machine-vision style alignment guide: pulsing L-shaped corner brackets,
 * rangefinder tick marks at each edge midpoint, a static center crosshair,
 * and an optional scanning-line sweep — replacing a plain bordered box.
 */
@Composable
fun ScanFrame(
    accentColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 260.dp,
    cornerLength: Dp = 32.dp,
    showScanLine: Boolean = true,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan-frame")
    val sweep by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scan-line-sweep",
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bracket-pulse",
    )

    Canvas(modifier = modifier.size(size)) {
        val strokeWidth = 6f
        val corner = cornerLength.toPx()
        val w = this.size.width
        val h = this.size.height
        val bracketColor = accentColor.copy(alpha = pulse)

        fun bracket(origin: Offset, dx: Float, dy: Float) {
            drawLine(bracketColor, origin, Offset(origin.x + dx, origin.y), strokeWidth, StrokeCap.Round)
            drawLine(bracketColor, origin, Offset(origin.x, origin.y + dy), strokeWidth, StrokeCap.Round)
        }

        bracket(Offset(0f, 0f), corner, corner)
        bracket(Offset(w, 0f), -corner, corner)
        bracket(Offset(0f, h), corner, -corner)
        bracket(Offset(w, h), -corner, -corner)

        // Rangefinder tick marks at each edge midpoint.
        val tick = 10.dp.toPx()
        drawLine(accentColor.copy(alpha = 0.7f), Offset(w / 2, 0f), Offset(w / 2, tick), strokeWidth / 2)
        drawLine(accentColor.copy(alpha = 0.7f), Offset(w / 2, h), Offset(w / 2, h - tick), strokeWidth / 2)
        drawLine(accentColor.copy(alpha = 0.7f), Offset(0f, h / 2), Offset(tick, h / 2), strokeWidth / 2)
        drawLine(accentColor.copy(alpha = 0.7f), Offset(w, h / 2), Offset(w - tick, h / 2), strokeWidth / 2)

        // Static center crosshair.
        val crosshair = 14.dp.toPx()
        val cx = w / 2
        val cy = h / 2
        drawLine(accentColor.copy(alpha = 0.5f), Offset(cx - crosshair, cy), Offset(cx + crosshair, cy), strokeWidth / 3)
        drawLine(accentColor.copy(alpha = 0.5f), Offset(cx, cy - crosshair), Offset(cx, cy + crosshair), strokeWidth / 3)

        if (showScanLine) {
            val y = h * sweep
            drawLine(
                color = accentColor.copy(alpha = 0.8f),
                start = Offset(strokeWidth, y),
                end = Offset(w - strokeWidth, y),
                strokeWidth = strokeWidth / 2,
            )
        }
    }
}
