package com.example.xposuredetectorsmart.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.xposuredetectorsmart.database.entities.AuditLog
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {

    @Insert
    suspend fun insert(log: AuditLog): Long

    @Query("SELECT * FROM audit_logs WHERE timestamp BETWEEN :startOfDay AND :endOfDay ORDER BY timestamp DESC")
    fun getAuditLogsForDate(startOfDay: Long, endOfDay: Long): Flow<List<AuditLog>>

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogs(): Flow<List<AuditLog>>
}
