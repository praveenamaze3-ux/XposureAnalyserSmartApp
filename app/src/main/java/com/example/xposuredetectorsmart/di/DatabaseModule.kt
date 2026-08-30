package com.example.xposuredetectorsmart.di

import android.content.Context
import androidx.room.Room
import com.example.xposuredetectorsmart.database.AppDatabase
import com.example.xposuredetectorsmart.database.dao.AuditLogDao
import com.example.xposuredetectorsmart.database.dao.DoseLogDao
import com.example.xposuredetectorsmart.database.dao.IndustryDao
import com.example.xposuredetectorsmart.database.dao.WorkerContextDao
import com.example.xposuredetectorsmart.database.dao.WorkerProfileDao
import com.example.xposuredetectorsmart.utils.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, Constants.DATABASE_NAME)
            .fallbackToDestructiveMigration(true)
            .build()

    @Provides
    fun provideDoseLogDao(database: AppDatabase): DoseLogDao = database.doseLogDao()

    @Provides
    fun provideWorkerContextDao(database: AppDatabase): WorkerContextDao = database.workerContextDao()

    @Provides
    fun provideAuditLogDao(database: AppDatabase): AuditLogDao = database.auditLogDao()

    @Provides
    fun provideIndustryDao(database: AppDatabase): IndustryDao = database.industryDao()

    @Provides
    fun provideWorkerProfileDao(database: AppDatabase): WorkerProfileDao = database.workerProfileDao()
}
