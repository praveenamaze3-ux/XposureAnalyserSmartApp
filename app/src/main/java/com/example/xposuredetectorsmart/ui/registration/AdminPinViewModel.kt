package com.example.xposuredetectorsmart.ui.registration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xposuredetectorsmart.repository.IndustryRepository
import com.example.xposuredetectorsmart.repository.SettingsRepository
import com.example.xposuredetectorsmart.security.PinHasher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PinGateState {
    object CheckingIndustry : PinGateState()
    data class NeedsIndustrySetup(val error: String? = null, val isChecking: Boolean = false) : PinGateState()
    data class EnteringPin(val industryId: String, val error: String? = null) : PinGateState()
    data class Granted(val industryId: String) : PinGateState()
}

/**
 * Gates the worker-registration flow with a shared supervisor PIN (per industry), which is a
 * UX gate only - see the Firestore rules comment on worker_profiles for the accepted limitation
 * that this is not a cryptographic access boundary.
 */
@HiltViewModel
class AdminPinViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val industryRepository: IndustryRepository,
    private val pinHasher: PinHasher,
) : ViewModel() {

    private val _state = MutableStateFlow<PinGateState>(PinGateState.CheckingIndustry)
    val state: StateFlow<PinGateState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val industryId = settingsRepository.getCurrentIndustryId()
            _state.value = if (industryId != null) {
                PinGateState.EnteringPin(industryId)
            } else {
                PinGateState.NeedsIndustrySetup()
            }
        }
    }

    fun submitIndustryId(rawIndustryId: String) {
        val industryId = rawIndustryId.trim()
        if (industryId.isEmpty()) {
            _state.value = PinGateState.NeedsIndustrySetup(error = "Enter an industry id")
            return
        }
        _state.value = PinGateState.NeedsIndustrySetup(isChecking = true)
        viewModelScope.launch {
            val industry = industryRepository.getIndustry(industryId)
            if (industry == null) {
                _state.value = PinGateState.NeedsIndustrySetup(error = "Industry not found — check the id and your connection")
            } else {
                settingsRepository.setCurrentIndustryId(industry.industryId)
                _state.value = PinGateState.EnteringPin(industry.industryId)
            }
        }
    }

    fun submitPin(pin: String) {
        val current = _state.value as? PinGateState.EnteringPin ?: return
        viewModelScope.launch {
            val industry = industryRepository.getIndustry(current.industryId)
            val hashed = pinHasher.hash(pin, current.industryId)
            if (industry != null && industry.pinHash == hashed) {
                _state.value = PinGateState.Granted(current.industryId)
            } else {
                _state.value = current.copy(error = "Incorrect PIN")
            }
        }
    }
}
