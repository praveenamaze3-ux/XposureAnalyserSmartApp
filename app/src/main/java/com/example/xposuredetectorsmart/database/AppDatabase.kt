package com.example.xposuredetectorsmart.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.xposuredetectorsmart.database.dao.AuditLogDao
import com.example.xposuredetectorsmart.database.dao.ColorProfileDao
import com.example.xposuredetectorsmart.database.dao.DoseLogDao
import com.example.xposuredetectorsmart.database.dao.WorkerContextDao
import com.example.xposuredetectorsmart.database.entities.AuditLog
import com.example.xposuredetectorsmart.database.entities.ColorProfile
import com.example.xposuredetectorsmart.database.entities.DoseLog
import com.example.xposuredetectorsmart.database.entities.WorkerContext

@Database(
    entities = [DoseLog::class, WorkerContext::class, ColorProfile::class, AuditLog::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun doseLogDao(): DoseLogDao
    abstract fun workerContextDao(): WorkerContextDao
    abstract fun colorProfileDao(): ColorProfileDao
    abstract fun auditLogDao(): AuditLogDao
}
