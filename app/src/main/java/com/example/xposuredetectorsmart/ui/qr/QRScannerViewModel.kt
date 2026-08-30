package com.example.xposuredetectorsmart.ui.qr

import android.os.Build
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xposuredetectorsmart.BuildConfig
import com.example.xposuredetectorsmart.database.entities.WorkerContext
import com.example.xposuredetectorsmart.repository.IndustryRepository
import com.example.xposuredetectorsmart.repository.WorkerProfileRepository
import com.example.xposuredetectorsmart.scanner.QRCodeScanner
import com.example.xposuredetectorsmart.scanner.QRParseException
import com.example.xposuredetectorsmart.scanner.QRParser
import com.example.xposuredetectorsmart.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.inject.Inject

sealed class QRScanUiState {
    object Scanning : QRScanUiState()
    data class Scanned(val context: WorkerContext) : QRScanUiState()
    data class Invalid(val message: String) : QRScanUiState()
}

@HiltViewModel
class QRScannerViewModel @Inject constructor(
    private val qrCodeScanner: QRCodeScanner,
    private val qrParser: QRParser,
    private val workerProfileRepository: WorkerProfileRepository,
    private val industryRepository: IndustryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<QRScanUiState>(QRScanUiState.Scanning)
    val uiState: StateFlow<QRScanUiState> = _uiState.asStateFlow()

    private var isProcessing = false

    fun onFrame(imageProxy: ImageProxy) {
        if (isProcessing || _uiState.value is QRScanUiState.Scanned) {
            imageProxy.close()
            return
        }
        isProcessing = true
        qrCodeScanner.scanFrame(imageProxy) { raw ->
            isProcessing = false
            if (raw != null) handleRawValue(raw)
        }
    }

    private fun handleRawValue(raw: String) {
        viewModelScope.launch {
            try {
                val data = qrParser.parse(raw)

                val profile = workerProfileRepository.getProfile(data.workerId)
                if (profile == null) {
                    _uiState.value = QRScanUiState.Invalid("Worker not found — connect to sync profiles")
                    return@launch
                }
                if (profile.status != "ACTIVE") {
                    _uiState.value = QRScanUiState.Invalid("Worker inactive")
                    return@launch
                }

                val industry = industryRepository.getIndustry(data.industryId)
                val shiftDurationHours = industry?.shiftDurationHours ?: Constants.DEFAULT_SHIFT_DURATION_HOURS
                val now = System.currentTimeMillis()

                val context = WorkerContext(
                    workerId = profile.workerId,
                    industryId = data.industryId,
                    shiftDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                    locationCode = "",
                    shiftStartedAt = now,
                    shiftExpiresAt = now + TimeUnit.HOURS.toMillis(shiftDurationHours),
                    phoneModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                    appVersion = BuildConfig.VERSION_NAME,
                )
                _uiState.value = QRScanUiState.Scanned(context)
            } catch (e: QRParseException) {
                _uiState.value = QRScanUiState.Invalid(e.message ?: "Invalid QR code")
            }
        }
    }

    fun resetToScanning() {
        _uiState.value = QRScanUiState.Scanning
    }

    override fun onCleared() {
        super.onCleared()
        qrCodeScanner.close()
    }
}
