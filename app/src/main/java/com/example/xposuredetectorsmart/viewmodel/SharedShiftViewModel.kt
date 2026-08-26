package com.example.xposuredetectorsmart.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xposuredetectorsmart.database.entities.AuditAction
import com.example.xposuredetectorsmart.database.entities.WorkerContext
import com.example.xposuredetectorsmart.repository.AuditRepository
import com.example.xposuredetectorsmart.repository.WorkerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ShiftState {
    object NoShift : ShiftState()
    data class Active(val context: WorkerContext) : ShiftState()
}

/**
 * Holds the active worker/shift across all screens (nav-graph scoped) so a batch of captures,
 * or a switch between multi-user profiles, doesn't require re-scanning the QR code each time.
 */
@HiltViewModel
class SharedShiftViewModel @Inject constructor(
    private val workerRepository: WorkerRepository,
    private val auditRepository: AuditRepository,
) : ViewModel() {

    private val _shiftState = MutableStateFlow<ShiftState>(ShiftState.NoShift)
    val shiftState: StateFlow<ShiftState> = _shiftState.asStateFlow()

    // Worker identified via the wristband scan, awaiting the disposable-strip scan to pair
    // with before the shift actually starts.
    private val _pendingContext = MutableStateFlow<WorkerContext?>(null)
    val pendingContext: StateFlow<WorkerContext?> = _pendingContext.asStateFlow()

    private var batchCaptureCount = 0
    private val _batchCount = MutableStateFlow(0)
    val batchCount: StateFlow<Int> = _batchCount.asStateFlow()

    val knownWorkerIds: StateFlow<List<String>> = workerRepository.observeKnownWorkerIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Called once the wristband QR is scanned; the shift isn't active until a strip is paired via [startShift]. */
    fun identifyWorker(context: WorkerContext) {
        _pendingContext.value = context
    }

    fun startShift(context: WorkerContext) {
        _shiftState.value = ShiftState.Active(context)
        _pendingContext.value = null
        batchCaptureCount = 0
        _batchCount.value = 0
        viewModelScope.launch {
            workerRepository.saveContext(context)
            auditRepository.log(
                AuditAction.SCAN_QR,
                context.workerId,
                mapOf("location" to context.locationCode, "shift" to context.shiftType),
            )
            auditRepository.log(
                AuditAction.SCAN_STRIP,
                context.workerId,
                mapOf("stripSerial" to context.stripSerial),
            )
        }
    }

    fun switchWorker(workerId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val context = workerRepository.getLatestForWorker(workerId)
            if (context != null) {
                _shiftState.value = ShiftState.Active(context)
                batchCaptureCount = 0
                _batchCount.value = 0
                auditRepository.log(AuditAction.WORKER_SWITCH, workerId, mapOf("switchedTo" to workerId))
            }
            onResult(context != null)
        }
    }

    fun registerBatchCapture() {
        batchCaptureCount += 1
        _batchCount.value = batchCaptureCount
    }

    fun clearShift() {
        _shiftState.value = ShiftState.NoShift
        _pendingContext.value = null
        batchCaptureCount = 0
        _batchCount.value = 0
    }
}
