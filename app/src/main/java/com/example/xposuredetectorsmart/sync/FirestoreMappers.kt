package com.example.xposuredetectorsmart.sync

import com.example.xposuredetectorsmart.database.entities.DoseLog
import com.example.xposuredetectorsmart.export.ReportFormatter

/**
 * Pure mapping from Room entities to Firestore document shapes, kept separate from [FirebaseSync]
 * so the mapping logic is unit-testable without touching the Firestore SDK's Task machinery.
 */
object FirestoreMappers {

    fun doseLogDocId(log: DoseLog): String = "${log.workerId}_${log.id}"

    fun doseLogData(log: DoseLog): Map<String, Any?> = mapOf(
        "workerID" to log.workerId,
        "shiftDate" to log.shiftDate,
        "dosePPM" to log.dosePpm,
        "confidence" to log.confidence,
        "timestamp" to log.timestamp,
        "location" to log.location,
        "device" to log.deviceModel,
        "imageHash" to log.imageHash,
        "correctionApplied" to log.correctionApplied,
    )

    fun shiftReportDocId(workerId: String, shiftDate: String): String = "${workerId}_$shiftDate"

    fun shiftReportData(workerId: String, shiftDate: String, logs: List<DoseLog>, nowMillis: Long): Map<String, Any?> {
        val total = logs.sumOf { it.dosePpm }
        val status = ReportFormatter.statusFor(total)
        return mapOf(
            "workerID" to workerId,
            "date" to shiftDate,
            "totalExposure" to total,
            "status" to status.name,
            "timestamp" to nowMillis,
            "captureCount" to logs.size,
        )
    }
}
