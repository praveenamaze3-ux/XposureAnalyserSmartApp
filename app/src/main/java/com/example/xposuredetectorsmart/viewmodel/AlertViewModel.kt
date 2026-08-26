package com.example.xposuredetectorsmart.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xposuredetectorsmart.database.entities.AuditAction
import com.example.xposuredetectorsmart.notification.AlertNotificationManager
import com.example.xposuredetectorsmart.repository.AuditRepository
import com.example.xposuredetectorsmart.repository.DoseRepository
import com.example.xposuredetectorsmart.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Watches cumulative shift exposure and fires a local alert the moment it crosses the threshold. */
@HiltViewModel
class AlertViewModel @Inject constructor(
    private val doseRepository: DoseRepository,
    private val notificationManager: AlertNotificationManager,
    private val auditRepository: AuditRepository,
) : ViewModel() {

    private val _isAlertActive = MutableStateFlow(false)
    val isAlertActive: StateFlow<Boolean> = _isAlertActive.asStateFlow()

    private var alertedForShiftKey: String? = null

    fun watch(workerId: String, shiftDate: String) {
        viewModelScope.launch {
            doseRepository.observeCumulativeDose(workerId, shiftDate).collect { cumulative ->
                val exceeded = cumulative >= Constants.ALERT_THRESHOLD_PPM
                _isAlertActive.value = exceeded

                val key = "$workerId|$shiftDate"
                if (exceeded && alertedForShiftKey != key) {
                    alertedForShiftKey = key
                    notificationManager.showExposureAlert(workerId, cumulative)
                    auditRepository.log(
                        AuditAction.ALERT_TRIGGERED,
                        workerId,
                        mapOf("cumulativePpm" to cumulative, "shiftDate" to shiftDate),
                    )
                } else if (!exceeded && alertedForShiftKey == key) {
                    alertedForShiftKey = null
                }
            }
        }
    }
}
