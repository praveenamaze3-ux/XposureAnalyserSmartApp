package com.example.xposuredetectorsmart.ui.registration

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xposuredetectorsmart.export.QrImageExporter
import com.example.xposuredetectorsmart.repository.WorkerProfileRepository
import com.example.xposuredetectorsmart.scanner.QRCodeGenerator
import com.example.xposuredetectorsmart.sync.FirebaseAuthBootstrapper
import com.example.xposuredetectorsmart.sync.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed class RegistrationState {
    object Idle : RegistrationState()
    object Saving : RegistrationState()
    data class Success(val workerId: String, val qrBitmap: Bitmap) : RegistrationState()
    data class Error(val message: String) : RegistrationState()
}

@HiltViewModel
class WorkerRegistrationViewModel @Inject constructor(
    private val workerProfileRepository: WorkerProfileRepository,
    private val qrCodeGenerator: QRCodeGenerator,
    private val qrImageExporter: QrImageExporter,
    private val networkMonitor: NetworkMonitor,
    private val authBootstrapper: FirebaseAuthBootstrapper,
) : ViewModel() {

    private val _state = MutableStateFlow<RegistrationState>(RegistrationState.Idle)
    val state: StateFlow<RegistrationState> = _state.asStateFlow()

    private val _exportedFile = MutableStateFlow<File?>(null)
    val exportedFile: StateFlow<File?> = _exportedFile.asStateFlow()

    fun register(industryId: String, name: String, employeeCode: String?) {
        if (name.isBlank()) {
            _state.value = RegistrationState.Error("Name is required")
            return
        }
        if (!networkMonitor.isCurrentlyOnline()) {
            _state.value = RegistrationState.Error("Registration requires an internet connection")
            return
        }

        _state.value = RegistrationState.Saving
        viewModelScope.launch {
            try {
                authBootstrapper.ensureSignedInOrThrow()
            } catch (e: Exception) {
                _state.value = RegistrationState.Error("Could not authenticate: ${e.message ?: e::class.simpleName}")
                return@launch
            }

            runCatching {
                workerProfileRepository.registerWorker(
                    industryId = industryId,
                    name = name.trim(),
                    employeeCode = employeeCode?.trim()?.takeIf { it.isNotBlank() },
                )
            }.onSuccess { profile ->
                val bitmap = qrCodeGenerator.generate(profile.qrPayload)
                _state.value = RegistrationState.Success(profile.workerId, bitmap)
            }.onFailure { e ->
                _state.value = RegistrationState.Error(e.message ?: "Registration failed")
            }
        }
    }

    fun shareQr(bitmap: Bitmap, workerId: String) {
        viewModelScope.launch {
            _exportedFile.value = qrImageExporter.saveAsPngFile(bitmap, workerId)
        }
    }

    fun consumeExportedFile() {
        _exportedFile.value = null
    }

    fun reset() {
        _state.value = RegistrationState.Idle
    }
}
