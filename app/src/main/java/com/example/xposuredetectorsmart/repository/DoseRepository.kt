package com.example.xposuredetectorsmart.repository

import com.example.xposuredetectorsmart.database.dao.DoseLogDao
import com.example.xposuredetectorsmart.database.entities.DoseLog
import com.example.xposuredetectorsmart.sync.FirebaseAuthBootstrapper
import com.example.xposuredetectorsmart.sync.FirebaseSync
import com.example.xposuredetectorsmart.sync.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject

class DoseRepository @Inject constructor(
    private val doseLogDao: DoseLogDao,
    private val firebaseSync: FirebaseSync,
    private val authBootstrapper: FirebaseAuthBootstrapper,
    private val networkMonitor: NetworkMonitor,
) {

    fun getDoseLogsForShift(workerId: String, shiftDate: String): Flow<List<DoseLog>> =
        doseLogDao.getDoseLogsForShift(workerId, shiftDate)

    fun observeCumulativeDose(workerId: String, shiftDate: String): Flow<Double> =
        doseLogDao.observeCumulativeDose(workerId, shiftDate)

    fun getAllLogs(): Flow<List<DoseLog>> = doseLogDao.getAllLogs()

    fun observeLog(id: Long): Flow<DoseLog?> = doseLogDao.observeById(id)

    suspend fun saveDoseLog(log: DoseLog): Long {
        val id = doseLogDao.insert(log)
        if (networkMonitor.isCurrentlyOnline()) {
            runCatching { syncSingle(id) }
                .onFailure { Timber.w(it, "Immediate sync failed for dose log %d, will retry via WorkManager", id) }
        }
        return id
    }

    private suspend fun syncSingle(id: Long) {
        val log = doseLogDao.getById(id) ?: return
        if (log.isSynced) return
        if (!authBootstrapper.ensureSignedIn()) return
        firebaseSync.uploadDoseLog(log).onSuccess {
            doseLogDao.markAsSynced(id, System.currentTimeMillis())
        }
    }

    /** Uploads every unsynced log plus a rollup per (worker, shift date) touched. Returns count synced. */
    suspend fun syncUnsyncedLogs(): Int {
        if (!authBootstrapper.ensureSignedIn()) return 0

        val unsynced = doseLogDao.getUnsyncedLogs()
        if (unsynced.isEmpty()) return 0

        var syncedCount = 0
        for (log in unsynced) {
            firebaseSync.uploadDoseLog(log).onSuccess {
                doseLogDao.markAsSynced(log.id, System.currentTimeMillis())
                syncedCount++
            }
        }

        unsynced.map { it.workerId to it.shiftDate }.distinct().forEach { (workerId, shiftDate) ->
            val shiftLogs = doseLogDao.getDoseLogsForShiftOnce(workerId, shiftDate)
            firebaseSync.uploadShiftReport(workerId, shiftDate, shiftLogs)
        }

        return syncedCount
    }
}
