package com.example.xposuredetectorsmart.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xposuredetectorsmart.database.entities.AuditAction
import com.example.xposuredetectorsmart.notification.AlertNotificationManager
import com.example.xposuredetectorsmart.repository.AuditRepository
import com.example.xposuredetectorsmart.repository.DoseRepository
import com.example.xposuredetectorsmart.utils.Constants
import com.example.xposuredetectorsmart.utils.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Watches cumulative shift exposure and fires a local alert once the shift-average concentration reaches DANGEROUS. */
@HiltViewModel
class AlertViewModel @Inject constructor(
    private val doseRepository: DoseRepository,
    private val notificationManager: AlertNotificationManager,
    private val auditRepository: AuditRepository,
) : ViewModel() {

    private val _isAlertActive = MutableStateFlow(false)
    val isAlertActive: StateFlow<Boolean> = _isAlertActive.asStateFlow()

    private var alertedForShiftKey: String? = null

    /**
     * [shiftStartedAt] anchors the shift-average calculation to the worker's actual elapsed
     * work time, recomputed fresh on every dose update - not a duration frozen at watch() call
     * time, or the danger check would go stale as the shift goes on.
     */
    fun watch(workerId: String, shiftDate: String, shiftStartedAt: Long) {
        viewModelScope.launch {
            doseRepository.observeCumulativeDose(workerId, shiftDate).collect { cumulative ->
                val shiftAveragePpm = cumulative / DateUtils.elapsedHours(shiftStartedAt)
                val exceeded = shiftAveragePpm >= Constants.RISK_DANGEROUS_MIN_PPM
                _isAlertActive.value = exceeded

                val key = "$workerId|$shiftDate"
                if (exceeded && alertedForShiftKey != key) {
                    alertedForShiftKey = key
                    notificationManager.showExposureAlert(workerId, shiftAveragePpm)
                    auditRepository.log(
                        AuditAction.ALERT_TRIGGERED,
                        workerId,
                        mapOf("shiftAveragePpm" to shiftAveragePpm, "cumulativeDosePpmHr" to cumulative, "shiftDate" to shiftDate),
                    )
                } else if (!exceeded && alertedForShiftKey == key) {
                    alertedForShiftKey = null
                }
            }
        }
    }
}
