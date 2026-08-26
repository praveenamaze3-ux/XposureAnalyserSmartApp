package com.example.xposuredetectorsmart.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xposuredetectorsmart.ui.theme.HudLabelStyle
import com.example.xposuredetectorsmart.ui.theme.StatusCritical

/** Prominent banner for the active-shift exposure alert, with a soft pulsing glow. */
@Composable
fun AlertBanner(message: String, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "alert-glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.24f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alert-glow-alpha",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(StatusCritical.copy(alpha = glowAlpha), RoundedCornerShape(4.dp))
            .border(1.dp, StatusCritical.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = Icons.Filled.Warning, contentDescription = null, tint = StatusCritical)
        Text(
            text = message.uppercase(),
            style = HudLabelStyle.copy(fontSize = 13.sp),
            color = StatusCritical,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}
