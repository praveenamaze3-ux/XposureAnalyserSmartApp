package com.example.xposuredetectorsmart

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.xposuredetectorsmart.database.entities.DoseLog
import com.example.xposuredetectorsmart.sync.FirebaseSync
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end sync check against the Firestore *emulator* (never production Firestore).
 *
 * Prerequisite: run `firebase emulators:start --only firestore` on the host machine before
 * executing this test (default emulator port 8080; 10.0.2.2 is how the Android emulator reaches
 * the host loopback). Without it, this test will fail to connect - it will not fall back to
 * writing into your real project.
 *
 * @HiltAndroidTest/HiltAndroidRule are required here even though this test builds FirebaseSync
 * by hand rather than injecting it: the app also registers a @AndroidEntryPoint FCM service
 * (AlertMessagingService), and without the rule creating the Hilt component up front, that
 * service crashes the instrumentation process if the system starts it mid-test-run.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class FirebaseIntegrationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var firestore: FirebaseFirestore
    private lateinit var firebaseSync: FirebaseSync

    @Before
    fun setUp() {
        hiltRule.inject()
        firestore = FirebaseFirestore.getInstance().apply { useEmulator("10.0.2.2", 8080) }
        firebaseSync = FirebaseSync(firestore)
    }

    @Test
    fun uploadedDoseLogIsReadableBackFromFirestore() = runBlocking {
        val log = DoseLog(
            id = 999,
            workerId = "WRK_TEST",
            shiftDate = "2026-08-25",
            dosePpm = 12.5,
            confidence = 0.75f,
            timestamp = System.currentTimeMillis(),
            deviceModel = "test-device",
            imageHash = "test-hash",
            correctionApplied = "{}",
            location = "TestLocation",
        )

        val result = firebaseSync.uploadDoseLog(log)
        assertTrue("upload should succeed against the emulator: ${result.exceptionOrNull()}", result.isSuccess)

        val snapshot = firestore.collection("dose_logs").document("WRK_TEST_999").get().await()
        assertTrue(snapshot.exists())
        assertEquals("WRK_TEST", snapshot.getString("workerID"))
        assertEquals(12.5, snapshot.getDouble("dosePPM"))
    }
}
