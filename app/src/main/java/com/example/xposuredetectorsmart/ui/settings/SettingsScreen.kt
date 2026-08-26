package com.example.xposuredetectorsmart.ui.settings

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.xposuredetectorsmart.repository.ThemeMode
import com.example.xposuredetectorsmart.viewmodel.ShiftState
import com.example.xposuredetectorsmart.viewmodel.SharedShiftViewModel

@Composable
fun SettingsScreen(
    sharedShiftViewModel: SharedShiftViewModel,
    onViewAuditTrail: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val biometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val exportState by viewModel.exportState.collectAsState()
    val shiftState by sharedShiftViewModel.shiftState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(exportState) {
        val state = exportState
        if (state is ExportState.Success) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", state.file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share shift report"))
            viewModel.resetExportState()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))

        Text("Appearance", style = MaterialTheme.typography.titleMedium)
        ThemeMode.entries.forEach { mode ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                RadioButton(selected = themeMode == mode, onClick = { viewModel.setThemeMode(mode) })
                Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Biometric lock", style = MaterialTheme.typography.titleMedium)
                Text("Require fingerprint to view dose data", style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = biometricEnabled, onCheckedChange = { viewModel.setBiometricEnabled(it) })
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        val activeShift = shiftState as? ShiftState.Active
        Button(
            onClick = {
                activeShift?.let {
                    viewModel.exportShiftReport(
                        workerId = it.context.workerId,
                        department = "Field Operations",
                        shiftDate = it.context.shiftDate,
                        location = it.context.locationCode,
                    )
                }
            },
            enabled = activeShift != null && exportState !is ExportState.Exporting,
        ) {
            Text(if (exportState is ExportState.Exporting) "Exporting..." else "Export Shift Report (PDF)")
        }
        if (exportState is ExportState.Error) {
            Text((exportState as ExportState.Error).message, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onViewAuditTrail, modifier = Modifier.fillMaxWidth()) {
            Text("View Audit Trail")
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { sharedShiftViewModel.clearShift(); onLoggedOut() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Logout")
        }

        Spacer(Modifier.height(24.dp))
        Text("Version ${viewModel.appVersion}", style = MaterialTheme.typography.labelSmall)
        Text("Device: ${Build.MANUFACTURER} ${Build.MODEL}", style = MaterialTheme.typography.labelSmall)
    }
}
