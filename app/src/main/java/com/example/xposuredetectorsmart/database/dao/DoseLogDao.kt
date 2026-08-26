package com.example.xposuredetectorsmart.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.xposuredetectorsmart.database.entities.DoseLog
import kotlinx.coroutines.flow.Flow

@Dao
interface DoseLogDao {

    @Insert
    suspend fun insert(log: DoseLog): Long

    @Update
    suspend fun update(log: DoseLog)

    @Query("SELECT * FROM dose_logs WHERE id = :id")
    fun observeById(id: Long): Flow<DoseLog?>

    @Query("SELECT * FROM dose_logs WHERE id = :id")
    suspend fun getById(id: Long): DoseLog?

    @Query("SELECT * FROM dose_logs WHERE workerId = :workerId AND shiftDate = :shiftDate ORDER BY timestamp ASC")
    fun getDoseLogsForShift(workerId: String, shiftDate: String): Flow<List<DoseLog>>

    @Query("SELECT * FROM dose_logs WHERE workerId = :workerId AND shiftDate = :shiftDate ORDER BY timestamp ASC")
    suspend fun getDoseLogsForShiftOnce(workerId: String, shiftDate: String): List<DoseLog>

    @Query("SELECT * FROM dose_logs WHERE isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedLogs(): List<DoseLog>

    @Query("UPDATE dose_logs SET isSynced = 1, syncTimestamp = :syncTime WHERE id = :id")
    suspend fun markAsSynced(id: Long, syncTime: Long)

    @Query("SELECT * FROM dose_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<DoseLog>>

    @Query("SELECT COALESCE(SUM(dosePpm), 0.0) FROM dose_logs WHERE workerId = :workerId AND shiftDate = :shiftDate")
    fun observeCumulativeDose(workerId: String, shiftDate: String): Flow<Double>
}
