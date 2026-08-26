package com.example.xposuredetectorsmart

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.testing.CustomTestApplication
import dagger.hilt.components.SingletonComponent

/**
 * The production manifest disables WorkManager's default on-app-start initializer (see
 * AndroidManifest.xml) so it can be initialized on-demand from [H2SDoseReaderApp]'s
 * Configuration.Provider instead. Hilt's generic HiltTestApplication doesn't implement
 * Configuration.Provider, so without this, any instrumented test that launches an Activity
 * (which schedules WorkManager jobs) crashes with "WorkManager is not initialized properly".
 *
 * @CustomTestApplication base classes can't use @Inject fields, so the worker factory is fetched
 * via an EntryPoint instead of member injection.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WorkerFactoryEntryPoint {
    fun workerFactory(): HiltWorkerFactory
}

open class WorkManagerTestApp : Application(), Configuration.Provider {
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(
                EntryPointAccessors.fromApplication(this, WorkerFactoryEntryPoint::class.java).workerFactory(),
            )
            .build()
}

@CustomTestApplication(WorkManagerTestApp::class)
interface HiltTestApp
