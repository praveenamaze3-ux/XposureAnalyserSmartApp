package com.example.xposuredetectorsmart.scanner

import com.example.xposuredetectorsmart.database.entities.WorkerContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShiftSessionValidatorTest {

    private val validator = ShiftSessionValidator()

    private fun context(shiftExpiresAt: Long) = WorkerContext(
        workerId = "WRK_1",
        industryId = "acme_chemicals",
        shiftDate = "2026-08-25",
        locationCode = "",
        shiftStartedAt = 0L,
        shiftExpiresAt = shiftExpiresAt,
        phoneModel = "Pixel 8",
        appVersion = "1.0",
    )

    @Test
    fun `not expired before the expiry timestamp`() {
        assertFalse(validator.isExpired(context(shiftExpiresAt = 1_000L), nowMillis = 500L))
    }

    @Test
    fun `expired exactly at the expiry timestamp`() {
        assertTrue(validator.isExpired(context(shiftExpiresAt = 1_000L), nowMillis = 1_000L))
    }

    @Test
    fun `expired after the expiry timestamp`() {
        assertTrue(validator.isExpired(context(shiftExpiresAt = 1_000L), nowMillis = 1_500L))
    }
}
