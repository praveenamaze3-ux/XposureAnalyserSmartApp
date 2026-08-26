package com.example.xposuredetectorsmart.repository

import com.example.xposuredetectorsmart.database.dao.AuditLogDao
import com.example.xposuredetectorsmart.database.entities.AuditAction
import com.example.xposuredetectorsmart.database.entities.AuditLog
import com.example.xposuredetectorsmart.security.AuditSigner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import javax.inject.Inject

data class AuditEntry(
    val id: Long,
    val action: String,
    val workerId: String,
    val timestamp: Long,
    val details: String,
    val isTamperFree: Boolean,
)

class AuditRepository @Inject constructor(
    private val auditLogDao: AuditLogDao,
    private val auditSigner: AuditSigner,
) {

    suspend fun log(action: AuditAction, workerId: String, details: Map<String, Any?> = emptyMap()) {
        val timestamp = System.currentTimeMillis()
        val detailsJson = JSONObject(details).toString()
        val payload = signablePayload(action.name, workerId, timestamp, detailsJson)
        val signature = auditSigner.sign(payload)

        auditLogDao.insert(
            AuditLog(
                action = action.name,
                workerId = workerId,
                timestamp = timestamp,
                details = detailsJson,
                signature = signature,
            ),
        )
    }

    fun observeLogsForDate(startOfDayMillis: Long, endOfDayMillis: Long): Flow<List<AuditEntry>> =
        auditLogDao.getAuditLogsForDate(startOfDayMillis, endOfDayMillis).map { it.toVerifiedEntries() }

    fun observeAllLogs(): Flow<List<AuditEntry>> =
        auditLogDao.getAllAuditLogs().map { it.toVerifiedEntries() }

    private fun List<AuditLog>.toVerifiedEntries(): List<AuditEntry> = map { log ->
        val payload = signablePayload(log.action, log.workerId, log.timestamp, log.details)
        AuditEntry(
            id = log.id,
            action = log.action,
            workerId = log.workerId,
            timestamp = log.timestamp,
            details = log.details,
            isTamperFree = auditSigner.verify(payload, log.signature),
        )
    }

    private fun signablePayload(action: String, workerId: String, timestamp: Long, detailsJson: String): String =
        "$action|$workerId|$timestamp|$detailsJson"
}
