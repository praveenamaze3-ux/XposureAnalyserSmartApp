package com.example.xposuredetectorsmart.ui.audit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.xposuredetectorsmart.repository.AuditEntry
import com.example.xposuredetectorsmart.utils.DateUtils

@Composable
fun AuditTrailScreen(viewModel: AuditTrailViewModel = hiltViewModel()) {
    val entries by viewModel.entries.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Audit Trail", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Every scan, capture, dose calculation, and export is logged and HMAC-signed for tamper detection.",
            style = MaterialTheme.typography.bodySmall,
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 8.dp))

        LazyColumn {
            items(entries, key = { it.id }) { entry -> AuditEntryRow(entry) }
        }
    }
}

@Composable
private fun AuditEntryRow(entry: AuditEntry) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(entry.action, style = MaterialTheme.typography.titleSmall)
            Text("Worker: ${entry.workerId}", style = MaterialTheme.typography.bodySmall)
            Text(DateUtils.formatTimestamp(entry.timestamp), style = MaterialTheme.typography.bodySmall)
            Text(
                if (entry.isTamperFree) "Signature verified" else "SIGNATURE MISMATCH - possible tampering",
                style = MaterialTheme.typography.labelSmall,
                color = if (entry.isTamperFree) Color(0xFF2E7D32) else Color(0xFFD32F2F),
            )
        }
    }
}
