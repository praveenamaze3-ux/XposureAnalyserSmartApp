package com.example.xposuredetectorsmart.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.example.xposuredetectorsmart.ui.theme.HudGridLine

/**
 * Faint technical grid drawn behind screen content — the instrument-panel backdrop
 * used on every non-camera screen so the app reads as a HUD, not a generic form.
 */
@Composable
fun HudBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 28.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawLine(HudGridLine.copy(alpha = 0.035f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(HudGridLine.copy(alpha = 0.035f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                y += step
            }
        }
        content()
    }
}
