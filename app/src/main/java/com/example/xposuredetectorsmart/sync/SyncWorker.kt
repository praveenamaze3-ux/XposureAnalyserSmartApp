package com.example.xposuredetectorsmart.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.xposuredetectorsmart.repository.DoseRepository
import com.example.xposuredetectorsmart.utils.Constants
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Periodic background catch-up sync. WorkManager's minimum periodic interval is 15 minutes
 * (the OS enforces this to protect battery), so the spec's "every 30s while online" behavior is
 * covered two ways: [DoseRepository.saveDoseLog] does a best-effort immediate upload right after
 * every capture, and [enqueueImmediate] fires a one-off catch-up the moment connectivity returns -
 * this periodic job is just the reliability net underneath both.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val doseRepository: DoseRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val syncedCount = doseRepository.syncUnsyncedLogs()
        Timber.i("SyncWorker: synced %d dose log(s)", syncedCount)
        Result.success()
    } catch (e: Exception) {
        Timber.e(e, "SyncWorker failed")
        Result.retry()
    }

    companion object {
        private const val IMMEDIATE_SYNC_WORK_NAME = "h2s_immediate_sync"

        fun enqueuePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(
                Constants.SYNC_INTERVAL_MINUTES, TimeUnit.MINUTES,
            ).setConstraints(constraints).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                Constants.SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun enqueueImmediate(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_SYNC_WORK_NAME,
                androidx.work.ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }
}
