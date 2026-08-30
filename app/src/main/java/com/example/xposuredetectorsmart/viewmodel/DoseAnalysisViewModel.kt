package com.example.xposuredetectorsmart.viewmodel

import android.graphics.Bitmap
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xposuredetectorsmart.database.entities.AuditAction
import com.example.xposuredetectorsmart.database.entities.DoseLog
import com.example.xposuredetectorsmart.imageprocessing.ColorPatch
import com.example.xposuredetectorsmart.imageprocessing.DoseResult
import com.example.xposuredetectorsmart.imageprocessing.ImageProcessor
import com.example.xposuredetectorsmart.imageprocessing.ManualPatchPoints
import com.example.xposuredetectorsmart.imageprocessing.ProcessingOutcome
import com.example.xposuredetectorsmart.repository.AuditRepository
import com.example.xposuredetectorsmart.repository.DoseRepository
import com.example.xposuredetectorsmart.utils.BitmapUtils
import com.example.xposuredetectorsmart.utils.RgbColor
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
    private val doseRepository: DoseRepository,
    private val auditRepository: AuditRepository,
) : ViewModel() {

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(
            val doseLogId: Long,
            val dose: DoseResult,
            val confidence: Float,
            val sampleColor: RgbColor,
            val blankColor: RgbColor,
            val originalBitmap: Bitmap,
            val patches: List<ColorPatch>,
            val timestamp: Long,
        ) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /** [shiftDurationHours] is the worker's actual elapsed work time since shift start, not the industry's configured/scheduled shift length. */
    fun processCapture(
        bitmap: Bitmap,
        points: ManualPatchPoints,
        workerId: String,
        shiftDate: String,
        location: String,
        stripSerial: String,
        shiftDurationHours: Double,
    ) {
        _uiState.value = UiState.Loading
        viewModelScope.launch {
            val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"

            val outcome = withContext(Dispatchers.Default) {
                imageProcessor.process(bitmap, points, shiftDurationHours)
            }

            when (outcome) {
                is ProcessingOutcome.Failure -> {
                    _uiState.value = UiState.Error(outcome.reason)
                }
                is ProcessingOutcome.Success -> {
                    val timestamp = System.currentTimeMillis()
                    val imageHash = withContext(Dispatchers.Default) { BitmapUtils.sha256Hash(bitmap) }

                    val correctionJson = JSONObject(
                        mapOf(
                            "opticalDensity" to outcome.dose.opticalDensity,
                            "shiftAveragePpm" to outcome.dose.shiftAveragePpm,
                            "eightHourTwaPpm" to outcome.dose.eightHourTwaPpm,
                            "riskLevel" to outcome.dose.riskLevel.name,
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
                            "riskLevel" to outcome.dose.riskLevel.name,
                            "confidence" to outcome.confidence,
                            "doseLogId" to id,
                            "stripSerial" to stripSerial,
                        ),
                    )

                    _uiState.value = UiState.Success(
                        doseLogId = id,
                        dose = outcome.dose,
                        confidence = outcome.confidence,
                        sampleColor = outcome.sampleColor,
                        blankColor = outcome.blankColor,
                        originalBitmap = bitmap,
                        patches = outcome.patches,
                        timestamp = timestamp,
                    )
                }
            }
        }
    }

    fun reset() {
        _uiState.value = UiState.Idle
    }
}
