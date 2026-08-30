package com.example.xposuredetectorsmart.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.xposuredetectorsmart.ui.theme.HudNumberStyle
import com.example.xposuredetectorsmart.ui.theme.SignalCyan
import kotlinx.coroutines.delay

/** Live-ticking "how long has this shift been running" readout, meant for a screen corner. */
@Composable
fun ShiftClockBadge(shiftStartedAt: Long, modifier: Modifier = Modifier) {
    val nowMillis by produceState(initialValue = System.currentTimeMillis(), shiftStartedAt) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1000)
        }
    }

    val elapsed = (nowMillis - shiftStartedAt).coerceAtLeast(0)
    val hours = elapsed / 3_600_000
    val minutes = (elapsed % 3_600_000) / 60_000
    val seconds = (elapsed % 60_000) / 1000

    Text(
        text = "%02d:%02d:%02d".format(hours, minutes, seconds),
        color = SignalCyan,
        style = HudNumberStyle.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize),
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
