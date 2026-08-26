package com.example.xposuredetectorsmart.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

/**
 * Angular HUD panel: sharp corners, a subtle brand-colored edge, and four
 * targeting-style corner brackets — the base container for grouped content
 * across every screen.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    Box {
        OutlinedCard(
            modifier = modifier,
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.22f)),
            elevation = CardDefaults.outlinedCardElevation(defaultElevation = 0.dp),
        ) {
            Column(modifier = Modifier.padding(contentPadding)) {
                content()
            }
        }
        CornerBrackets(color = accent, modifier = Modifier.matchParentSize())
    }
}

@Composable
private fun CornerBrackets(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val arm = 12.dp.toPx()
        val stroke = 2.dp.toPx()
        val w = size.width
        val h = size.height

        fun bracket(cx: Float, cy: Float, dx: Float, dy: Float) {
            drawLine(color, androidx.compose.ui.geometry.Offset(cx, cy), androidx.compose.ui.geometry.Offset(cx + dx, cy), stroke, StrokeCap.Round)
            drawLine(color, androidx.compose.ui.geometry.Offset(cx, cy), androidx.compose.ui.geometry.Offset(cx, cy + dy), stroke, StrokeCap.Round)
        }

        bracket(0f, 0f, arm, arm)
        bracket(w, 0f, -arm, arm)
        bracket(0f, h, arm, -arm)
        bracket(w, h, -arm, -arm)
    }
}
