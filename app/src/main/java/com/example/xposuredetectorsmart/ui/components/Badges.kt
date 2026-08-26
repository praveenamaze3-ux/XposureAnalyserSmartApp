package com.example.xposuredetectorsmart.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.xposuredetectorsmart.ui.theme.HudNumberStyle
import com.example.xposuredetectorsmart.ui.theme.StatusCritical
import com.example.xposuredetectorsmart.ui.theme.StatusSafe
import com.example.xposuredetectorsmart.ui.theme.StatusWarning
import com.example.xposuredetectorsmart.utils.Constants

fun ppmStatusColor(ppm: Double): Color = when {
    ppm >= Constants.IDLH_PPM -> StatusCritical
    ppm >= Constants.OSHA_PEL_8HR * 5 -> StatusWarning
    else -> StatusSafe
}

fun confidenceStatusColor(confidence: Float): Color =
    if (confidence < Constants.MIN_CONFIDENCE_WARNING) StatusWarning else StatusSafe

/** Compact ppm pill for inline/list contexts (the hero Results reading uses [CircularGauge] instead). */
@Composable
fun PpmBadge(ppm: Double, modifier: Modifier = Modifier) {
    val color = ppmStatusColor(ppm)
    Text(
        text = "%.1f ppm".format(ppm),
        color = Color.White,
        style = HudNumberStyle.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize),
        modifier = modifier
            .background(color, RoundedCornerShape(3.dp))
            .padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

@Composable
fun ConfidenceBadge(confidence: Float, modifier: Modifier = Modifier) {
    val color = confidenceStatusColor(confidence)
    Text(
        text = "CONFIDENCE %.0f%%".format(confidence * 100),
        color = Color.White,
        style = com.example.xposuredetectorsmart.ui.theme.HudLabelStyle.copy(fontSize = MaterialTheme.typography.labelLarge.fontSize),
        modifier = modifier
            .background(color, RoundedCornerShape(3.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}
