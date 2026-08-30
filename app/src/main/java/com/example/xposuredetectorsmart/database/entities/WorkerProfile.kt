package com.example.xposuredetectorsmart.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local cache of a Firestore `worker_profiles/{workerId}` doc - the permanent worker identity
 * behind the wristband QR, distinct from [WorkerContext] which is a per-scan shift session.
 */
@Entity(tableName = "worker_profiles")
data class WorkerProfile(
    @PrimaryKey val workerId: String,
    val industryId: String,
    val name: String,
    val employeeCode: String?,
    val status: String, // "ACTIVE" | "INACTIVE"
    val qrPayload: String,
    val lastSyncedAt: Long,
)
