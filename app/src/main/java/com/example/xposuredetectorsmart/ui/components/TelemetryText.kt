package com.example.xposuredetectorsmart.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.xposuredetectorsmart.ui.theme.HudLabelStyle

/** Live system-status readout with a blinking cursor, e.g. "SEARCHING FOR TARGET_". */
@Composable
fun TelemetryText(text: String, color: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "telemetry-cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                1f at 0
                1f at 400
                0f at 401
                0f at 900
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "telemetry-cursor-alpha",
    )

    Row(modifier = modifier) {
        Text(text = text.uppercase(), style = HudLabelStyle.copy(fontSize = MaterialTheme.typography.labelMedium.fontSize), color = color)
        Text(text = "_", style = HudLabelStyle.copy(fontSize = MaterialTheme.typography.labelMedium.fontSize), color = color.copy(alpha = cursorAlpha))
    }
}
