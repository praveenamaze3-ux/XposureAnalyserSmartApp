package com.example.xposuredetectorsmart.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.xposuredetectorsmart.database.entities.WorkerProfile

@Dao
interface WorkerProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: WorkerProfile)

    @Query("SELECT * FROM worker_profiles WHERE workerId = :workerId LIMIT 1")
    suspend fun getById(workerId: String): WorkerProfile?
}
