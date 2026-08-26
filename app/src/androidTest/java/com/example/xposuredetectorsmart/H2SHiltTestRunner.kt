package com.example.xposuredetectorsmart

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/**
 * Swaps in the generated Hilt test application so @HiltAndroidTest instrumented tests can inject
 * fakes/mocks. Uses the [HiltTestApp]-generated application (not the generic HiltTestApplication)
 * because it still implements Configuration.Provider - see [WorkManagerTestApp] for why that's
 * required.
 */
class H2SHiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application {
        return super.newApplication(cl, HiltTestApp_Application::class.java.name, context)
    }
}
