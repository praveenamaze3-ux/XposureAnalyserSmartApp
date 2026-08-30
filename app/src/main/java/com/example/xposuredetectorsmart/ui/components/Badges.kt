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
import com.example.xposuredetectorsmart.imageprocessing.H2SRiskLevel
import com.example.xposuredetectorsmart.ui.theme.HudNumberStyle
import com.example.xposuredetectorsmart.ui.theme.SignalCyan
import com.example.xposuredetectorsmart.utils.Constants

fun riskLevelFor(shiftAveragePpm: Double): H2SRiskLevel = when {
    shiftAveragePpm < Constants.RISK_MODERATE_MIN_PPM -> H2SRiskLevel.SAFE
    shiftAveragePpm <= Constants.RISK_HIGH_MIN_PPM -> H2SRiskLevel.MODERATE
    shiftAveragePpm <= Constants.RISK_DANGEROUS_MIN_PPM -> H2SRiskLevel.HIGH
    else -> H2SRiskLevel.DANGEROUS
}

/** [shiftAveragePpm] is a concentration (ppm), not a raw dose (ppm·hr). */
fun ppmStatusColor(shiftAveragePpm: Double): Color = Color(android.graphics.Color.parseColor(riskLevelFor(shiftAveragePpm).colorHex))

/** Compact ppm pill for inline/list contexts (the hero Results reading uses [CircularGauge] instead). */
@Composable
fun PpmBadge(shiftAveragePpm: Double, modifier: Modifier = Modifier) {
    val color = ppmStatusColor(shiftAveragePpm)
    Text(
        text = "%.1f ppm".format(shiftAveragePpm),
        color = Color.White,
        style = HudNumberStyle.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize),
        modifier = modifier
            .background(color, RoundedCornerShape(3.dp))
            .padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

@Composable
fun ConfidenceBadge(confidence: Float, modifier: Modifier = Modifier) {
    Text(
        text = "CONFIDENCE %.0f%%".format(confidence * 100),
        color = Color.White,
        style = com.example.xposuredetectorsmart.ui.theme.HudLabelStyle.copy(fontSize = MaterialTheme.typography.labelLarge.fontSize),
        modifier = modifier
            .background(SignalCyan, RoundedCornerShape(3.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}
