package com.example.xposuredetectorsmart.ui.settings

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.xposuredetectorsmart.repository.ThemeMode
import com.example.xposuredetectorsmart.ui.components.AppHeader
import com.example.xposuredetectorsmart.ui.components.GlassCard
import com.example.xposuredetectorsmart.ui.theme.HudLabelStyle
import com.example.xposuredetectorsmart.utils.DateUtils
import com.example.xposuredetectorsmart.viewmodel.ShiftState
import com.example.xposuredetectorsmart.viewmodel.SharedShiftViewModel

@Composable
fun SettingsScreen(
    sharedShiftViewModel: SharedShiftViewModel,
    onViewAuditTrail: () -> Unit,
    onViewDoseLogHistory: () -> Unit,
    onRegisterWorker: () -> Unit,
    onBackToScanner: () -> Unit,
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

    com.example.xposuredetectorsmart.ui.components.HudBackground {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        AppHeader(
            title = "Settings",
            icon = Icons.Filled.Settings,
            trailing = {
                IconButton(onClick = onBackToScanner) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to scanner",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            },
        )
        Spacer(Modifier.height(20.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("APPEARANCE", style = HudLabelStyle.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize))
            Spacer(Modifier.height(12.dp))
            ThemeSegmentedControl(selected = themeMode, onSelect = viewModel::setThemeMode)
        }

        Spacer(Modifier.height(16.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("BIOMETRIC LOCK", style = HudLabelStyle.copy(fontSize = MaterialTheme.typography.titleMedium.fontSize))
                    Text("Require fingerprint to view dose data", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = biometricEnabled, onCheckedChange = { viewModel.setBiometricEnabled(it) })
            }
        }

        Spacer(Modifier.height(16.dp))

        val activeShift = shiftState as? ShiftState.Active
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            SettingsActionRow(
                icon = Icons.Filled.PictureAsPdf,
                label = if (exportState is ExportState.Exporting) "Exporting..." else "Export Shift Report (PDF)",
                enabled = activeShift != null && exportState !is ExportState.Exporting,
                onClick = {
                    activeShift?.let {
                        val shiftDurationHours = DateUtils.elapsedHours(it.context.shiftStartedAt)
                        viewModel.exportShiftReport(
                            workerId = it.context.workerId,
                            department = "Field Operations",
                            shiftDate = it.context.shiftDate,
                            location = it.context.locationCode,
                            shiftDurationHours = shiftDurationHours,
                        )
                    }
                },
            )
            if (exportState is ExportState.Error) {
                Text(
                    (exportState as ExportState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SettingsActionRow(icon = Icons.Filled.History, label = "View Audit Trail", onClick = onViewAuditTrail)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SettingsActionRow(icon = Icons.Filled.Assessment, label = "View Dose Log History", onClick = onViewDoseLogHistory)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SettingsActionRow(icon = Icons.Filled.PersonAdd, label = "Register Worker", onClick = onRegisterWorker)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SettingsActionRow(
                icon = Icons.AutoMirrored.Filled.Logout,
                label = "Logout",
                onClick = { sharedShiftViewModel.clearShift(); onLoggedOut() },
                tint = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.height(24.dp))
        Text("Version ${viewModel.appVersion}", style = MaterialTheme.typography.labelSmall)
        Text("Device: ${Build.MANUFACTURER} ${Build.MODEL}", style = MaterialTheme.typography.labelSmall)
    }
    }
}

@Composable
private fun ThemeSegmentedControl(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ThemeMode.entries.forEach { mode ->
            val isSelected = mode == selected
            Text(
                text = mode.name.uppercase(),
                style = HudLabelStyle.copy(fontSize = MaterialTheme.typography.labelLarge.fontSize),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(mode) }
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent,
                        RoundedCornerShape(3.dp),
                    )
                    .padding(vertical = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) tint else MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = label.uppercase(),
            style = HudLabelStyle.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize),
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).padding(start = 12.dp),
        )
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
