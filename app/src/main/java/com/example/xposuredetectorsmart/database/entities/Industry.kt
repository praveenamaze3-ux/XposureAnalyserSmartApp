package com.example.xposuredetectorsmart.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Local cache of a Firestore `industries/{industryId}` doc, synced read-only from the app. */
@Entity(tableName = "industries")
data class Industry(
    @PrimaryKey val industryId: String,
    val name: String,
    val pinHash: String,
    val shiftDurationHours: Long,
    val lastSyncedAt: Long,
)
