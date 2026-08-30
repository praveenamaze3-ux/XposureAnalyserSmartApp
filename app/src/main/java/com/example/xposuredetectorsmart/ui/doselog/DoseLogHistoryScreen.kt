package com.example.xposuredetectorsmart.ui.doselog

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
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.xposuredetectorsmart.database.entities.DoseLog
import com.example.xposuredetectorsmart.ui.components.AppHeader
import com.example.xposuredetectorsmart.ui.components.ConfidenceBadge
import com.example.xposuredetectorsmart.ui.components.GlassCard
import com.example.xposuredetectorsmart.ui.components.HudBackground
import com.example.xposuredetectorsmart.ui.theme.HudLabelStyle
import com.example.xposuredetectorsmart.ui.theme.HudNumberStyle
import com.example.xposuredetectorsmart.utils.DateUtils

@Composable
fun DoseLogHistoryScreen(viewModel: DoseLogHistoryViewModel = hiltViewModel()) {
    val logs by viewModel.logs.collectAsState()

    HudBackground {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        AppHeader(title = "Dose Log History", icon = Icons.Filled.Assessment)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Every strip reading captured on this device, newest first.",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Assessment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.height(48.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No dose readings yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(logs, key = { it.id }) { log -> DoseLogRow(log) }
            }
        }
    }
    }
}

@Composable
private fun DoseLogRow(log: DoseLog) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "%.2f ppm·hr".format(log.dosePpm),
                    style = HudNumberStyle.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize),
                    modifier = Modifier.weight(1f),
                )
                ConfidenceBadge(confidence = log.confidence)
            }
            Spacer(Modifier.height(6.dp))
            Text("Worker: ${log.workerId}  ·  Strip: ${log.stripSerial}", style = MaterialTheme.typography.bodySmall)
            Text(
                DateUtils.formatTimestamp(log.timestamp),
                style = HudNumberStyle.copy(fontSize = MaterialTheme.typography.bodySmall.fontSize),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (log.isSynced) "SYNCED" else "PENDING SYNC",
                style = HudLabelStyle.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
