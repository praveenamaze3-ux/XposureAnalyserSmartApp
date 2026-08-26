package com.example.xposuredetectorsmart.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AuditAction {
    SCAN_QR,
    CAPTURE_IMAGE,
    CALCULATE_DOSE,
    EXPORT_PDF,
    ALERT_TRIGGERED,
    WORKER_SWITCH,
    BIOMETRIC_UNLOCK,
}

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val action: String, // AuditAction.name
    val workerId: String,
    val timestamp: Long,
    val details: String, // JSON-encoded free-form payload
    val signature: String, // HMAC-SHA256 hex digest over (id-less) fields, for tamper detection
)
