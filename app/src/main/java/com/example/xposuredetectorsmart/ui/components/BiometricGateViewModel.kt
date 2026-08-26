package com.example.xposuredetectorsmart.ui.components

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xposuredetectorsmart.database.entities.AuditAction
import com.example.xposuredetectorsmart.repository.AuditRepository
import com.example.xposuredetectorsmart.repository.SettingsRepository
import com.example.xposuredetectorsmart.security.BiometricAuthManager
import com.example.xposuredetectorsmart.security.BiometricResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LockState {
    object Checking : LockState()
    object Unlocked : LockState()
    object Locked : LockState()
}

@HiltViewModel
class BiometricGateViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val biometricAuthManager: BiometricAuthManager,
    private val auditRepository: AuditRepository,
) : ViewModel() {

    val isBiometricEnabled: StateFlow<Boolean> = settingsRepository.isBiometricEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _lockState = MutableStateFlow<LockState>(LockState.Checking)
    val lockState: StateFlow<LockState> = _lockState.asStateFlow()

    fun evaluate(biometricEnabled: Boolean) {
        if (!biometricEnabled) {
            _lockState.value = LockState.Unlocked
            return
        }
        viewModelScope.launch {
            val unlocked = settingsRepository.isSessionUnlocked(System.currentTimeMillis())
            _lockState.value = if (unlocked) LockState.Unlocked else LockState.Locked
        }
    }

    fun requestUnlock(activity: FragmentActivity, workerId: String) {
        viewModelScope.launch {
            when (biometricAuthManager.authenticate(activity)) {
                is BiometricResult.Success -> {
                    val now = System.currentTimeMillis()
                    settingsRepository.recordUnlock(now)
                    auditRepository.log(AuditAction.BIOMETRIC_UNLOCK, workerId)
                    _lockState.value = LockState.Unlocked
                }
                else -> _lockState.value = LockState.Locked
            }
        }
    }
}
