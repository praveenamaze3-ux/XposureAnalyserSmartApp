package com.example.xposuredetectorsmart.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xposuredetectorsmart.database.entities.DoseLog
import com.example.xposuredetectorsmart.repository.DoseRepository
import com.example.xposuredetectorsmart.sync.NetworkMonitor
import com.example.xposuredetectorsmart.sync.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import android.content.Context
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val doseRepository: DoseRepository,
    private val networkMonitor: NetworkMonitor,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), networkMonitor.isCurrentlyOnline())

    fun shiftLogs(workerId: String, shiftDate: String): Flow<List<DoseLog>> =
        doseRepository.getDoseLogsForShift(workerId, shiftDate)

    fun cumulativeDose(workerId: String, shiftDate: String): Flow<Double> =
        doseRepository.observeCumulativeDose(workerId, shiftDate)

    fun requestManualSync() {
        SyncWorker.enqueueImmediate(context)
    }

    fun averageConfidence(logs: List<DoseLog>): Float =
        if (logs.isEmpty()) 0f else logs.map { it.confidence }.average().toFloat()
}
