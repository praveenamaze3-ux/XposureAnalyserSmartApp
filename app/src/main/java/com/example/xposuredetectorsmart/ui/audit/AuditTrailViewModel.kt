package com.example.xposuredetectorsmart.ui.audit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xposuredetectorsmart.repository.AuditEntry
import com.example.xposuredetectorsmart.repository.AuditRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AuditTrailViewModel @Inject constructor(
    auditRepository: AuditRepository,
) : ViewModel() {

    val entries: StateFlow<List<AuditEntry>> = auditRepository.observeAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
