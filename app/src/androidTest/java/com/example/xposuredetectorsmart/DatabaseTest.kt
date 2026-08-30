package com.example.xposuredetectorsmart

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.xposuredetectorsmart.database.AppDatabase
import com.example.xposuredetectorsmart.database.entities.AuditAction
import com.example.xposuredetectorsmart.database.entities.AuditLog
import com.example.xposuredetectorsmart.database.entities.DoseLog
import com.example.xposuredetectorsmart.database.entities.WorkerContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private lateinit var database: AppDatabase

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun insertAndReadDoseLog() = runBlocking {
        val id = database.doseLogDao().insert(
            DoseLog(
                workerId = "WRK_1",
                shiftDate = "2026-08-25",
                dosePpm = 42.0,
                confidence = 0.9f,
                timestamp = 1000L,
                deviceModel = "Pixel 8",
                imageHash = "hash",
                correctionApplied = "{}",
                location = "LocationA",
                stripSerial = "CAP_1000",
            ),
        )

        val stored = database.doseLogDao().getById(id)
        assertEquals("WRK_1", stored?.workerId)
        assertEquals(42.0, stored?.dosePpm)
        assertEquals(false, stored?.isSynced)
    }

    @Test
    fun cumulativeDoseSumsEachDistinctStripsPeakReading() = runBlocking {
        val dao = database.doseLogDao()
        // Two re-checks of the same strip - only the peak (15.0) should count.
        dao.insert(baseLog(dosePpm = 10.0, stripSerial = "STRIP_1"))
        dao.insert(baseLog(dosePpm = 15.0, stripSerial = "STRIP_1"))
        // A second, distinct strip adds on top.
        dao.insert(baseLog(dosePpm = 7.0, stripSerial = "STRIP_2"))
        dao.insert(baseLog(workerId = "WRK_OTHER", dosePpm = 999.0, stripSerial = "STRIP_1"))

        val total = dao.observeCumulativeDose("WRK_1", "2026-08-25").first()
        assertEquals(22.0, total, 0.001)
    }

    @Test
    fun markAsSyncedUpdatesFlags() = runBlocking {
        val dao = database.doseLogDao()
        val id = dao.insert(baseLog())
        dao.markAsSynced(id, 5000L)

        val stored = dao.getById(id)
        assertTrue(stored?.isSynced == true)
        assertEquals(5000L, stored?.syncTimestamp)
    }

    @Test
    fun auditLogRoundTripsThroughDatabase() = runBlocking {
        val dao = database.auditLogDao()
        dao.insert(
            AuditLog(
                action = AuditAction.SCAN_QR.name,
                workerId = "WRK_1",
                timestamp = 100L,
                details = "{}",
                signature = "deadbeef",
            ),
        )

        val logs = dao.getAllAuditLogs().first()
        assertEquals(1, logs.size)
        assertEquals("deadbeef", logs.first().signature)
    }

    @Test
    fun workerContextTracksLatestScanPerWorker() = runBlocking {
        val dao = database.workerContextDao()
        dao.insert(workerContext(shiftStartedAt = 100L))
        dao.insert(workerContext(shiftStartedAt = 200L))

        val latest = dao.getLatestForWorker("WRK_1")
        assertEquals(200L, latest?.shiftStartedAt)
    }

    private fun baseLog(workerId: String = "WRK_1", dosePpm: Double = 10.0, stripSerial: String = "CAP_TEST") = DoseLog(
        workerId = workerId,
        shiftDate = "2026-08-25",
        dosePpm = dosePpm,
        confidence = 0.9f,
        timestamp = System.currentTimeMillis(),
        deviceModel = "Pixel 8",
        imageHash = "hash",
        correctionApplied = "{}",
        location = "LocationA",
        stripSerial = stripSerial,
    )

    private fun workerContext(shiftStartedAt: Long) = WorkerContext(
        workerId = "WRK_1",
        industryId = "acme_chemicals",
        shiftDate = "2026-08-25",
        locationCode = "LocationA",
        phoneModel = "Pixel 8",
        appVersion = "1.0",
        shiftStartedAt = shiftStartedAt,
        shiftExpiresAt = shiftStartedAt + 8 * 60 * 60 * 1000L,
    )
}
