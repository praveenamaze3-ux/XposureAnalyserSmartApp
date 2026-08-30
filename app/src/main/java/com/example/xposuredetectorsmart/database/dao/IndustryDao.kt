package com.example.xposuredetectorsmart.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.xposuredetectorsmart.database.entities.Industry

@Dao
interface IndustryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(industry: Industry)

    @Query("SELECT * FROM industries WHERE industryId = :industryId LIMIT 1")
    suspend fun getById(industryId: String): Industry?
}
