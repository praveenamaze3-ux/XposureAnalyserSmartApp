package com.example.xposuredetectorsmart.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xposuredetectorsmart.BuildConfig
import com.example.xposuredetectorsmart.database.entities.AuditAction
import com.example.xposuredetectorsmart.export.PdfGenerator
import com.example.xposuredetectorsmart.repository.AuditRepository
import com.example.xposuredetectorsmart.repository.DoseRepository
import com.example.xposuredetectorsmart.repository.SettingsRepository
import com.example.xposuredetectorsmart.repository.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed class ExportState {
    object Idle : ExportState()
    object Exporting : ExportState()
    data class Success(val file: File) : ExportState()
    data class Error(val message: String) : ExportState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val doseRepository: DoseRepository,
    private val auditRepository: AuditRepository,
    private val pdfGenerator: PdfGenerator,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val isBiometricEnabled: StateFlow<Boolean> = settingsRepository.isBiometricEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    val appVersion: String = BuildConfig.VERSION_NAME

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setBiometricEnabled(enabled) }
    }

    fun exportShiftReport(workerId: String, department: String, shiftDate: String, location: String, shiftDurationHours: Double) {
        _exportState.value = ExportState.Exporting
        viewModelScope.launch {
            try {
                val logs = doseRepository.getDoseLogsForShift(workerId, shiftDate).first()
                val file = pdfGenerator.generate(workerId, department, shiftDate, location, logs, shiftDurationHours)
                auditRepository.log(AuditAction.EXPORT_PDF, workerId, mapOf("file" to file.name, "shiftDate" to shiftDate))
                _exportState.value = ExportState.Success(file)
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Export failed")
            }
        }
    }

    fun resetExportState() {
        _exportState.value = ExportState.Idle
    }
}
