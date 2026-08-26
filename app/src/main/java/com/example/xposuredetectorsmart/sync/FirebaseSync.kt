package com.example.xposuredetectorsmart.sync

import com.example.xposuredetectorsmart.database.entities.DoseLog
import com.example.xposuredetectorsmart.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

/** Thin wrapper around Firestore writes for dose logs and per-shift rollups. */
class FirebaseSync @Inject constructor(
    private val firestore: FirebaseFirestore,
) {

    suspend fun uploadDoseLog(log: DoseLog): Result<Unit> = runCatching {
        val docId = FirestoreMappers.doseLogDocId(log)
        val data = FirestoreMappers.doseLogData(log)
        firestore.collection(Constants.COLLECTION_DOSE_LOGS).document(docId).set(data).await()
        Timber.d("Synced dose log %s to Firestore", docId)
    }

    suspend fun uploadShiftReport(workerId: String, shiftDate: String, logs: List<DoseLog>): Result<Unit> = runCatching {
        val docId = FirestoreMappers.shiftReportDocId(workerId, shiftDate)
        val data = FirestoreMappers.shiftReportData(workerId, shiftDate, logs, System.currentTimeMillis())
        firestore.collection(Constants.COLLECTION_SHIFT_REPORTS).document(docId).set(data).await()
    }
}
