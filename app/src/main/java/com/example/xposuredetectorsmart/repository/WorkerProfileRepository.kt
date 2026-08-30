package com.example.xposuredetectorsmart.repository

import com.example.xposuredetectorsmart.database.dao.WorkerProfileDao
import com.example.xposuredetectorsmart.database.entities.WorkerProfile
import com.example.xposuredetectorsmart.sync.NetworkMonitor
import com.example.xposuredetectorsmart.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

/**
 * Offline-first read/write for permanent worker identities. Registration requires connectivity
 * (a new workerId must be reserved before another device might scan it), but once a profile has
 * been synced to this device at least once, scanning that worker's QR works fully offline.
 */
class WorkerProfileRepository @Inject constructor(
    private val workerProfileDao: WorkerProfileDao,
    private val firestore: FirebaseFirestore,
    private val networkMonitor: NetworkMonitor,
) {
    suspend fun getProfile(workerId: String): WorkerProfile? {
        if (networkMonitor.isCurrentlyOnline()) {
            runCatching { refreshFromFirestore(workerId) }
                .onFailure { Timber.w(it, "Failed to refresh worker profile %s from Firestore", workerId) }
        }
        return workerProfileDao.getById(workerId)
    }

    suspend fun registerWorker(industryId: String, name: String, employeeCode: String?): WorkerProfile {
        val docRef = firestore.collection(Constants.COLLECTION_WORKER_PROFILES).document()
        val workerId = docRef.id
        val qrPayload = "${Constants.QR_WORKER_PREFIX_V2}$industryId:$workerId"
        val now = System.currentTimeMillis()

        val data = mapOf(
            "workerId" to workerId,
            "industryId" to industryId,
            "name" to name,
            "employeeCode" to employeeCode,
            "status" to "ACTIVE",
            "createdAt" to now,
            "qrPayload" to qrPayload,
        )
        docRef.set(data).await()

        val profile = WorkerProfile(
            workerId = workerId,
            industryId = industryId,
            name = name,
            employeeCode = employeeCode,
            status = "ACTIVE",
            qrPayload = qrPayload,
            lastSyncedAt = now,
        )
        workerProfileDao.upsert(profile)
        return profile
    }

    private suspend fun refreshFromFirestore(workerId: String) {
        val snapshot = firestore.collection(Constants.COLLECTION_WORKER_PROFILES).document(workerId).get().await()
        if (!snapshot.exists()) return

        workerProfileDao.upsert(
            WorkerProfile(
                workerId = workerId,
                industryId = snapshot.getString("industryId") ?: return,
                name = snapshot.getString("name") ?: "",
                employeeCode = snapshot.getString("employeeCode"),
                status = snapshot.getString("status") ?: "ACTIVE",
                qrPayload = snapshot.getString("qrPayload") ?: "",
                lastSyncedAt = System.currentTimeMillis(),
            ),
        )
    }
}
