package com.example.xposuredetectorsmart.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.xposuredetectorsmart.database.dao.AuditLogDao
import com.example.xposuredetectorsmart.database.dao.DoseLogDao
import com.example.xposuredetectorsmart.database.dao.IndustryDao
import com.example.xposuredetectorsmart.database.dao.WorkerContextDao
import com.example.xposuredetectorsmart.database.dao.WorkerProfileDao
import com.example.xposuredetectorsmart.database.entities.AuditLog
import com.example.xposuredetectorsmart.database.entities.DoseLog
import com.example.xposuredetectorsmart.database.entities.Industry
import com.example.xposuredetectorsmart.database.entities.WorkerContext
import com.example.xposuredetectorsmart.database.entities.WorkerProfile

@Database(
    entities = [
        DoseLog::class,
        WorkerContext::class,
        AuditLog::class,
        Industry::class,
        WorkerProfile::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun doseLogDao(): DoseLogDao
    abstract fun workerContextDao(): WorkerContextDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun industryDao(): IndustryDao
    abstract fun workerProfileDao(): WorkerProfileDao
}
