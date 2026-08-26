package com.example.xposuredetectorsmart.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xposuredetectorsmart.ui.theme.HudLabelStyle
import com.example.xposuredetectorsmart.ui.theme.StatusSafe
import com.example.xposuredetectorsmart.ui.theme.StatusWarning

/** Small icon + tracked-uppercase label used for binary status signals (online/offline, verified/mismatch). */
@Composable
fun StatusChip(
    text: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(2.dp))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
        androidx.compose.material3.Text(
            text = text.uppercase(),
            style = HudLabelStyle.copy(fontSize = 11.sp),
            color = color,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
fun SyncStatusChip(isOnline: Boolean, modifier: Modifier = Modifier) {
    StatusChip(
        text = if (isOnline) "Online" else "Offline",
        icon = if (isOnline) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
        color = if (isOnline) StatusSafe else StatusWarning,
        modifier = modifier,
    )
}

@Composable
fun SignatureStatusChip(verified: Boolean, modifier: Modifier = Modifier) {
    StatusChip(
        text = if (verified) "Signature verified" else "SIGNATURE MISMATCH",
        icon = if (verified) Icons.Filled.CheckCircle else Icons.Filled.Error,
        color = if (verified) StatusSafe else com.example.xposuredetectorsmart.ui.theme.StatusCritical,
        modifier = modifier,
    )
}
