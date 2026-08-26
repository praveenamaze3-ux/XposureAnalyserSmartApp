package com.example.xposuredetectorsmart.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.xposuredetectorsmart.database.entities.ColorProfile

@Dao
interface ColorProfileDao {

    @Insert
    suspend fun insert(profile: ColorProfile): Long

    @Query(
        "SELECT * FROM color_profiles WHERE deviceModel = :deviceModel AND workerId = :workerId " +
            "ORDER BY calibrationDate DESC LIMIT :limit"
    )
    suspend fun getRecentProfiles(deviceModel: String, workerId: String, limit: Int): List<ColorProfile>

    @Query("SELECT * FROM color_profiles WHERE workerId = :workerId ORDER BY calibrationDate DESC")
    suspend fun getProfilesForWorker(workerId: String): List<ColorProfile>

    @Query(
        "SELECT * FROM color_profiles WHERE deviceModel = :deviceModel AND workerId = :workerId " +
            "ORDER BY calibrationDate DESC LIMIT 1"
    )
    suspend fun getLatestProfile(deviceModel: String, workerId: String): ColorProfile?
}
