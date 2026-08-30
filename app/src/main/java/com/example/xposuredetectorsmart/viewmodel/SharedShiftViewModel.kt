package com.example.xposuredetectorsmart.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xposuredetectorsmart.database.entities.AuditAction
import com.example.xposuredetectorsmart.database.entities.WorkerContext
import com.example.xposuredetectorsmart.repository.AuditRepository
import com.example.xposuredetectorsmart.repository.WorkerRepository
import com.example.xposuredetectorsmart.scanner.ShiftSessionValidator
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

/** A physical strip currently being worn/monitored - identifies which readings belong together. */
data class StripSession(val serial: String, val issuedAt: Long)

/**
 * Holds the active worker/shift across all screens (nav-graph scoped) so a batch of captures,
 * or a switch between multi-user profiles, doesn't require re-scanning the QR code each time.
 */
@HiltViewModel
class SharedShiftViewModel @Inject constructor(
    private val workerRepository: WorkerRepository,
    private val auditRepository: AuditRepository,
    private val shiftSessionValidator: ShiftSessionValidator,
) : ViewModel() {

    private val _shiftState = MutableStateFlow<ShiftState>(ShiftState.NoShift)
    val shiftState: StateFlow<ShiftState> = _shiftState.asStateFlow()

    private var batchCaptureCount = 0
    private val _batchCount = MutableStateFlow(0)
    val batchCount: StateFlow<Int> = _batchCount.asStateFlow()

    // The strip currently being monitored. A re-check of this same strip must not add its
    // reading on top of the previous one - only issuing a new strip starts a fresh session.
    private val _currentStrip = MutableStateFlow<StripSession?>(null)
    val currentStrip: StateFlow<StripSession?> = _currentStrip.asStateFlow()

    val knownWorkerIds: StateFlow<List<String>> = workerRepository.observeKnownWorkerIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Starts a shift session immediately once the wristband QR resolves to a valid worker. */
    fun startShift(context: WorkerContext) {
        _shiftState.value = ShiftState.Active(context)
        batchCaptureCount = 0
        _batchCount.value = 0
        _currentStrip.value = null
        viewModelScope.launch {
            workerRepository.saveContext(context)
            auditRepository.log(
                AuditAction.SCAN_QR,
                context.workerId,
                mapOf("industryId" to context.industryId, "shiftExpiresAt" to context.shiftExpiresAt.toString()),
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
                _currentStrip.value = null
                auditRepository.log(AuditAction.WORKER_SWITCH, workerId, mapOf("switchedTo" to workerId))
            }
            onResult(context != null)
        }
    }

    fun registerBatchCapture() {
        batchCaptureCount += 1
        _batchCount.value = batchCaptureCount
    }

    /** Declares a freshly issued strip, replacing whatever strip was previously being monitored. */
    fun startNewStrip(): StripSession {
        val session = StripSession(serial = "STRIP_${System.currentTimeMillis()}", issuedAt = System.currentTimeMillis())
        _currentStrip.value = session
        return session
    }

    /** True once the active shift's fixed duration has elapsed; the worker must re-scan. */
    fun isShiftExpired(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val active = _shiftState.value as? ShiftState.Active ?: return false
        return shiftSessionValidator.isExpired(active.context, nowMillis)
    }

    fun clearShift() {
        _shiftState.value = ShiftState.NoShift
        batchCaptureCount = 0
        _batchCount.value = 0
        _currentStrip.value = null
    }

    /** Worker confirmed (via the QR re-scan prompt) that they're finishing their shift. */
    fun endShift() {
        val active = _shiftState.value as? ShiftState.Active ?: return
        viewModelScope.launch {
            auditRepository.log(
                AuditAction.SHIFT_END,
                active.context.workerId,
                mapOf(
                    "shiftStartedAt" to active.context.shiftStartedAt.toString(),
                    "endedAt" to System.currentTimeMillis().toString(),
                ),
            )
        }
        clearShift()
    }
}
