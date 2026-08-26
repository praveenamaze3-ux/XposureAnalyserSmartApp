package com.example.xposuredetectorsmart.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "worker_context")
data class WorkerContext(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val workerId: String,
    val shiftDate: String, // ISO-8601 LocalDate
    val locationCode: String,
    val shiftType: String,
    val phoneModel: String,
    val appVersion: String,
    val scanTimestamp: Long,
)
