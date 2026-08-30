package com.example.xposuredetectorsmart.repository

import com.example.xposuredetectorsmart.database.dao.IndustryDao
import com.example.xposuredetectorsmart.database.entities.Industry
import com.example.xposuredetectorsmart.sync.NetworkMonitor
import com.example.xposuredetectorsmart.utils.Constants
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

/**
 * Offline-first read of an industry's config (PIN hash, shift duration). Room is read first;
 * a background refresh from Firestore keeps it current whenever the device is online, so a
 * previously-synced industry still resolves while offline.
 */
class IndustryRepository @Inject constructor(
    private val industryDao: IndustryDao,
    private val firestore: FirebaseFirestore,
    private val networkMonitor: NetworkMonitor,
) {
    suspend fun getIndustry(industryId: String): Industry? {
        if (networkMonitor.isCurrentlyOnline()) {
            runCatching { refreshFromFirestore(industryId) }
                .onFailure { Timber.w(it, "Failed to refresh industry %s from Firestore", industryId) }
        }
        return industryDao.getById(industryId)
    }

    private suspend fun refreshFromFirestore(industryId: String) {
        val snapshot = firestore.collection(Constants.COLLECTION_INDUSTRIES).document(industryId).get().await()
        if (!snapshot.exists()) return

        industryDao.upsert(
            Industry(
                industryId = industryId,
                name = snapshot.getString("name") ?: industryId,
                pinHash = snapshot.getString("pinHash") ?: "",
                shiftDurationHours = snapshot.getLong("shiftDurationHours") ?: Constants.DEFAULT_SHIFT_DURATION_HOURS,
                lastSyncedAt = System.currentTimeMillis(),
            ),
        )
    }
}
