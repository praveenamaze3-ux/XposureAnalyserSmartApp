package com.example.xposuredetectorsmart.viewmodel

import android.graphics.Bitmap
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xposuredetectorsmart.database.entities.AuditAction
import com.example.xposuredetectorsmart.database.entities.DoseLog
import com.example.xposuredetectorsmart.imageprocessing.ColorPatch
import com.example.xposuredetectorsmart.imageprocessing.ImageProcessor
import com.example.xposuredetectorsmart.imageprocessing.ProcessingOutcome
import com.example.xposuredetectorsmart.repository.AuditRepository
import com.example.xposuredetectorsmart.repository.ColorProfileRepository
import com.example.xposuredetectorsmart.repository.DoseRepository
import com.example.xposuredetectorsmart.utils.BitmapUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject

/** Main capture-processing orchestrator: runs the full image pipeline and persists the result. */
@HiltViewModel
class DoseAnalysisViewModel @Inject constructor(
    private val imageProcessor: ImageProcessor,
    private val colorProfileRepository: ColorProfileRepository,
    private val doseRepository: DoseRepository,
    private val auditRepository: AuditRepository,
) : ViewModel() {

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(
            val doseLogId: Long,
            val ppm: Double,
            val confidence: Float,
            val correctedBitmap: Bitmap,
            val originalBitmap: Bitmap,
            val patches: List<ColorPatch>,
        ) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun processCapture(bitmap: Bitmap, workerId: String, shiftDate: String, location: String, stripSerial: String) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"

            val result = withContext(Dispatchers.Default) {
                val history = colorProfileRepository.getHistory(deviceModel, workerId)
                imageProcessor.process(bitmap, history) to history
            }
            val outcome = result.first

            when (outcome) {
                is ProcessingOutcome.Failure -> {
                    _uiState.value = UiState.Error(outcome.reason)
                }
                is ProcessingOutcome.Success -> {
                    val timestamp = System.currentTimeMillis()
                    val imageHash = withContext(Dispatchers.Default) { BitmapUtils.sha256Hash(bitmap) }

                    val correctionJson = JSONObject(
                        mapOf(
                            "scaleR" to outcome.correction.scale.r,
                            "scaleG" to outcome.correction.scale.g,
                            "scaleB" to outcome.correction.scale.b,
                            "meanSquareError" to outcome.correction.meanSquareError,
                        ),
                    ).toString()

                    val log = DoseLog(
                        workerId = workerId,
                        shiftDate = shiftDate,
                        dosePpm = outcome.ppm,
                        confidence = outcome.confidence,
                        timestamp = timestamp,
                        deviceModel = deviceModel,
                        imageHash = imageHash,
                        correctionApplied = correctionJson,
                        location = location,
                        stripSerial = stripSerial,
                    )

                    val id = doseRepository.saveDoseLog(log)

                    colorProfileRepository.recordCalibration(
                        deviceModel = deviceModel,
                        workerId = workerId,
                        patches = outcome.patches,
                        correction = outcome.correction,
                        timestamp = timestamp,
                    )

                    auditRepository.log(
                        AuditAction.CAPTURE_IMAGE,
                        workerId,
                        mapOf("imageHash" to imageHash, "stripSerial" to stripSerial),
                    )
                    auditRepository.log(
                        AuditAction.CALCULATE_DOSE,
                        workerId,
                        mapOf(
                            "ppm" to outcome.ppm,
                            "confidence" to outcome.confidence,
                            "doseLogId" to id,
                            "stripSerial" to stripSerial,
                        ),
                    )

                    _uiState.value = UiState.Success(
                        doseLogId = id,
                        ppm = outcome.ppm,
                        confidence = outcome.confidence,
                        correctedBitmap = outcome.correctedBitmap,
                        originalBitmap = bitmap,
                        patches = outcome.patches,
                    )
                }
            }
        }
    }

    fun reset() {
        _uiState.value = UiState.Idle
    }
}
