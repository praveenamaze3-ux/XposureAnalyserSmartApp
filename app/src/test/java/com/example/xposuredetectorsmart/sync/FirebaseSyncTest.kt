package com.example.xposuredetectorsmart.sync

import com.example.xposuredetectorsmart.database.entities.DoseLog
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies the Firestore document shape [FirestoreMappers] produces from Room entities.
 * The actual network call in [FirebaseSync] wraps Firebase's Task API, which isn't meaningfully
 * mockable without an emulator; this test covers the part that's pure and easy to get wrong -
 * the document id scheme and field mapping the app/Firestore security rules depend on.
 */
class FirebaseSyncTest {

    private val log = DoseLog(
        id = 42,
        workerId = "WRK_4838",
        shiftDate = "2026-08-25",
        dosePpm = 37.5,
        confidence = 0.82f,
        timestamp = 1_700_000_000_000L,
        deviceModel = "Pixel 8",
        imageHash = "abc123",
        correctionApplied = "{\"scaleR\":1.1}",
        location = "LocationA",
    )

    @Test
    fun `dose log doc id combines worker id and row id`() {
        assertEquals("WRK_4838_42", FirestoreMappers.doseLogDocId(log))
    }

    @Test
    fun `dose log data maps every field the security rules and dashboard rely on`() {
        val data = FirestoreMappers.doseLogData(log)
        assertEquals("WRK_4838", data["workerID"])
        assertEquals("2026-08-25", data["shiftDate"])
        assertEquals(37.5, data["dosePPM"])
        assertEquals(0.82f, data["confidence"])
        assertEquals(1_700_000_000_000L, data["timestamp"])
        assertEquals("LocationA", data["location"])
        assertEquals("Pixel 8", data["device"])
        assertEquals("abc123", data["imageHash"])
    }

    @Test
    fun `shift report doc id combines worker id and date`() {
        assertEquals("WRK_4838_2026-08-25", FirestoreMappers.shiftReportDocId("WRK_4838", "2026-08-25"))
    }

    @Test
    fun `shift report status reflects cumulative exposure`() {
        val normal = FirestoreMappers.shiftReportData("WRK_1", "2026-08-25", listOf(log.copy(dosePpm = 2.0)), 0L)
        val critical = FirestoreMappers.shiftReportData("WRK_1", "2026-08-25", listOf(log.copy(dosePpm = 150.0)), 0L)

        assertEquals("NORMAL", normal["status"])
        assertEquals("CRITICAL", critical["status"])
    }
}
