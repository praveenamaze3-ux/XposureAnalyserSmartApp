package com.example.xposuredetectorsmart.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xposuredetectorsmart.ui.theme.HudLabelStyle
import com.example.xposuredetectorsmart.ui.theme.HudNumberStyle
import kotlin.math.cos
import kotlin.math.sin

private const val GAUGE_START_ANGLE = 135f
private const val GAUGE_SWEEP_ANGLE = 270f
private const val TICK_COUNT = 12

/**
 * A circular HUD-style dial. Draws an outer bezel, radial tick marks, a background
 * track over [GAUGE_SWEEP_ANGLE] degrees, and a colored value arc proportional to
 * `value / maxValue`, with the reading in monospace digits at the center.
 */
@Composable
fun CircularGauge(
    value: Float,
    maxValue: Float,
    valueText: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    valueTextSize: TextUnit = 28.sp,
) {
    val fraction = (value / maxValue).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 700),
        label = "gauge-fraction",
    )
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidth = this.size.minDimension * 0.08f
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            val arcSize = Size(
                this.size.width - strokeWidth,
                this.size.height - strokeWidth,
            )
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            val center = Offset(this.size.width / 2, this.size.height / 2)
            val bezelRadius = this.size.minDimension / 2 - 1.dp.toPx()

            // Outer bezel ring — reads as an instrument housing.
            drawCircle(
                color = trackColor,
                radius = bezelRadius,
                center = center,
                style = Stroke(width = 1.dp.toPx()),
            )

            // Radial tick marks around the sweep.
            for (i in 0..TICK_COUNT) {
                val angleDeg = GAUGE_START_ANGLE + GAUGE_SWEEP_ANGLE * i / TICK_COUNT
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val isMajor = i % 3 == 0
                val outer = bezelRadius - 2.dp.toPx()
                val inner = outer - (if (isMajor) 8.dp.toPx() else 4.dp.toPx())
                val tickColor = if (angleDeg <= GAUGE_START_ANGLE + GAUGE_SWEEP_ANGLE * animatedFraction) color else trackColor
                drawLine(
                    color = tickColor,
                    start = Offset(center.x + cos(angleRad).toFloat() * inner, center.y + sin(angleRad).toFloat() * inner),
                    end = Offset(center.x + cos(angleRad).toFloat() * outer, center.y + sin(angleRad).toFloat() * outer),
                    strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx(),
                )
            }

            drawArc(
                color = trackColor,
                startAngle = GAUGE_START_ANGLE,
                sweepAngle = GAUGE_SWEEP_ANGLE,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
            // Soft glow: a wider, low-alpha pass beneath the crisp value arc.
            drawArc(
                color = color.copy(alpha = 0.25f),
                startAngle = GAUGE_START_ANGLE,
                sweepAngle = GAUGE_SWEEP_ANGLE * animatedFraction,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth * 1.8f, cap = StrokeCap.Round),
            )
            drawArc(
                color = color,
                startAngle = GAUGE_START_ANGLE,
                sweepAngle = GAUGE_SWEEP_ANGLE * animatedFraction,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
            Text(
                text = valueText,
                style = HudNumberStyle.copy(fontSize = valueTextSize, color = color),
                textAlign = TextAlign.Center,
            )
            Text(
                text = label.uppercase(),
                style = HudLabelStyle.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
