package com.example.xposuredetectorsmart.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.xposuredetectorsmart.database.entities.WorkerContext
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkerContextDao {

    @Insert
    suspend fun insert(context: WorkerContext): Long

    @Query("SELECT * FROM worker_context ORDER BY shiftStartedAt DESC LIMIT 1")
    fun observeLatestContext(): Flow<WorkerContext?>

    @Query("SELECT * FROM worker_context ORDER BY shiftStartedAt DESC LIMIT 1")
    suspend fun getLatestContext(): WorkerContext?

    @Query("SELECT * FROM worker_context WHERE workerId = :workerId ORDER BY shiftStartedAt DESC LIMIT 1")
    suspend fun getLatestForWorker(workerId: String): WorkerContext?

    @Query("SELECT DISTINCT workerId FROM worker_context ORDER BY shiftStartedAt DESC")
    fun observeKnownWorkerIds(): Flow<List<String>>
}
