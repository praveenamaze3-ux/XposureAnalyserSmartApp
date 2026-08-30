package com.example.xposuredetectorsmart.ui.doselog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xposuredetectorsmart.database.entities.DoseLog
import com.example.xposuredetectorsmart.repository.DoseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DoseLogHistoryViewModel @Inject constructor(
    doseRepository: DoseRepository,
) : ViewModel() {

    val logs: StateFlow<List<DoseLog>> = doseRepository.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
