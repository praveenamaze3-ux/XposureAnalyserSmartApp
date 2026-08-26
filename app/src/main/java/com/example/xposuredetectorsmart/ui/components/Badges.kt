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
import com.example.xposuredetectorsmart.utils.Constants

@Composable
fun PpmBadge(ppm: Double, modifier: Modifier = Modifier) {
    val color = when {
        ppm >= Constants.IDLH_PPM -> Color(0xFFD32F2F)
        ppm >= Constants.OSHA_PEL_8HR * 5 -> Color(0xFFF9A825)
        else -> Color(0xFF2E7D32)
    }
    Text(
        text = "%.1f ppm".format(ppm),
        color = Color.White,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier
            .background(color, RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

@Composable
fun ConfidenceBadge(confidence: Float, modifier: Modifier = Modifier) {
    val color = if (confidence < Constants.MIN_CONFIDENCE_WARNING) Color(0xFFF9A825) else Color(0xFF2E7D32)
    Text(
        text = "Confidence %.0f%%".format(confidence * 100),
        color = Color.White,
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier
            .background(color, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}
