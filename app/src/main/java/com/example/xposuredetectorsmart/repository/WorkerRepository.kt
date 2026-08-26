package com.example.xposuredetectorsmart.repository

import com.example.xposuredetectorsmart.database.dao.WorkerContextDao
import com.example.xposuredetectorsmart.database.entities.WorkerContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class WorkerRepository @Inject constructor(
    private val workerContextDao: WorkerContextDao,
) {
    suspend fun saveContext(context: WorkerContext): Long = workerContextDao.insert(context)

    fun observeLatestContext(): Flow<WorkerContext?> = workerContextDao.observeLatestContext()

    suspend fun getLatestContext(): WorkerContext? = workerContextDao.getLatestContext()

    suspend fun getLatestForWorker(workerId: String): WorkerContext? =
        workerContextDao.getLatestForWorker(workerId)

    fun observeKnownWorkerIds(): Flow<List<String>> = workerContextDao.observeKnownWorkerIds()
}
