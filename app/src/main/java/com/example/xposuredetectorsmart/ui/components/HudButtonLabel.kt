package com.example.xposuredetectorsmart.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.xposuredetectorsmart.ui.theme.HudLabelStyle

/** Tracked uppercase label for button content, matching the HUD type language. */
@Composable
fun HudButtonLabel(text: String) {
    Text(text.uppercase(), style = HudLabelStyle.copy(fontSize = MaterialTheme.typography.labelLarge.fontSize))
}
