package com.example.xposuredetectorsmart.ui.audit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.xposuredetectorsmart.repository.AuditEntry
import com.example.xposuredetectorsmart.ui.components.AppHeader
import com.example.xposuredetectorsmart.ui.components.GlassCard
import com.example.xposuredetectorsmart.ui.components.HudBackground
import com.example.xposuredetectorsmart.ui.components.SignatureStatusChip
import com.example.xposuredetectorsmart.ui.theme.HudLabelStyle
import com.example.xposuredetectorsmart.ui.theme.HudNumberStyle
import com.example.xposuredetectorsmart.utils.DateUtils

@Composable
fun AuditTrailScreen(viewModel: AuditTrailViewModel = hiltViewModel()) {
    val entries by viewModel.entries.collectAsState()

    HudBackground {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        AppHeader(title = "Audit Trail", icon = Icons.Filled.History)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Every scan, capture, dose calculation, and export is logged and HMAC-signed for tamper detection.",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.height(48.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No audit activity yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(entries, key = { it.id }) { entry -> AuditEntryRow(entry) }
            }
        }
    }
    }
}

private fun iconForAction(action: String): ImageVector {
    val a = action.lowercase()
    return when {
        "scan" in a -> Icons.Filled.QrCodeScanner
        "capture" in a -> Icons.Filled.PhotoCamera
        "export" in a -> Icons.Filled.FileUpload
        "alert" in a -> Icons.Filled.Warning
        else -> Icons.Filled.Description
    }
}

@Composable
private fun AuditEntryRow(entry: AuditEntry) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = iconForAction(entry.action),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp),
            )
            Column(modifier = Modifier.padding(start = 12.dp).fillMaxWidth()) {
                Text(
                    entry.action.uppercase(),
                    style = HudLabelStyle.copy(fontSize = MaterialTheme.typography.titleSmall.fontSize),
                )
                Text("Worker: ${entry.workerId}", style = MaterialTheme.typography.bodySmall)
                Text(
                    DateUtils.formatTimestamp(entry.timestamp),
                    style = HudNumberStyle.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                SignatureStatusChip(verified = entry.isTamperFree)
            }
        }
    }
}
