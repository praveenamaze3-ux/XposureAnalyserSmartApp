package com.example.xposuredetectorsmart.ui.qr

import android.os.Build
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xposuredetectorsmart.BuildConfig
import com.example.xposuredetectorsmart.database.entities.WorkerContext
import com.example.xposuredetectorsmart.scanner.QRCodeScanner
import com.example.xposuredetectorsmart.scanner.QRData
import com.example.xposuredetectorsmart.scanner.QRParseException
import com.example.xposuredetectorsmart.scanner.QRParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import javax.inject.Inject

sealed class QRScanUiState {
    object Scanning : QRScanUiState()
    data class Scanned(val data: QRData, val context: WorkerContext) : QRScanUiState()
    data class Invalid(val message: String) : QRScanUiState()
}

@HiltViewModel
class QRScannerViewModel @Inject constructor(
    private val qrCodeScanner: QRCodeScanner,
    private val qrParser: QRParser,
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
                val context = WorkerContext(
                    workerId = data.workerId,
                    shiftDate = data.date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    locationCode = data.location,
                    shiftType = data.shift,
                    phoneModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                    appVersion = BuildConfig.VERSION_NAME,
                    scanTimestamp = System.currentTimeMillis(),
                )
                _uiState.value = QRScanUiState.Scanned(data, context)
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
