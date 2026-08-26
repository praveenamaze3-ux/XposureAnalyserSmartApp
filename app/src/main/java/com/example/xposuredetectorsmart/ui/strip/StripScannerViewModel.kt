package com.example.xposuredetectorsmart.ui.strip

import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xposuredetectorsmart.scanner.QRCodeScanner
import com.example.xposuredetectorsmart.scanner.QRParseException
import com.example.xposuredetectorsmart.scanner.QRParser
import com.example.xposuredetectorsmart.scanner.StripData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class StripScanUiState {
    object Scanning : StripScanUiState()
    data class Scanned(val data: StripData) : StripScanUiState()
    data class Invalid(val message: String) : StripScanUiState()
}

@HiltViewModel
class StripScannerViewModel @Inject constructor(
    private val qrCodeScanner: QRCodeScanner,
    private val qrParser: QRParser,
) : ViewModel() {

    private val _uiState = MutableStateFlow<StripScanUiState>(StripScanUiState.Scanning)
    val uiState: StateFlow<StripScanUiState> = _uiState.asStateFlow()

    private var isProcessing = false

    fun onFrame(imageProxy: ImageProxy) {
        if (isProcessing || _uiState.value is StripScanUiState.Scanned) {
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
                val data = qrParser.parseStrip(raw)
                _uiState.value = StripScanUiState.Scanned(data)
            } catch (e: QRParseException) {
                _uiState.value = StripScanUiState.Invalid(e.message ?: "Invalid strip QR code")
            }
        }
    }

    fun resetToScanning() {
        _uiState.value = StripScanUiState.Scanning
    }

    override fun onCleared() {
        super.onCleared()
        qrCodeScanner.close()
    }
}
