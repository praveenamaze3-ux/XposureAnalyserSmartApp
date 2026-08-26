package com.example.xposuredetectorsmart.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dose_logs")
data class DoseLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workerId: String,
    val shiftDate: String, // ISO-8601 LocalDate, e.g. 2026-08-25
    val dosePpm: Double,
    val confidence: Float,
    val timestamp: Long,
    val deviceModel: String,
    val imageHash: String,
    val correctionApplied: String, // JSON-encoded correction matrix/scale factors
    val location: String,
    val stripSerial: String, // identifies the specific disposable strip this reading came from
    val isSynced: Boolean = false,
    val syncTimestamp: Long? = null,
)
