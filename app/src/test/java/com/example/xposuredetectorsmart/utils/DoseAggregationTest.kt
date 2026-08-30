package com.example.xposuredetectorsmart.utils

import com.example.xposuredetectorsmart.database.entities.DoseLog
import org.junit.Assert.assertEquals
import org.junit.Test

class DoseAggregationTest {

    private fun log(stripSerial: String, dosePpm: Double, timestamp: Long) = DoseLog(
        workerId = "WRK_1",
        shiftDate = "2026-08-25",
        dosePpm = dosePpm,
        confidence = 0.9f,
        timestamp = timestamp,
        deviceModel = "Pixel 8",
        imageHash = "hash",
        correctionApplied = "{}",
        location = "LocationA",
        stripSerial = stripSerial,
    )

    @Test
    fun `re-checking the same strip does not add readings on top of each other`() {
        val logs = listOf(
            log("STRIP_1", dosePpm = 10.0, timestamp = 1L),
            log("STRIP_1", dosePpm = 15.0, timestamp = 2L),
            log("STRIP_1", dosePpm = 22.0, timestamp = 3L),
        )
        // Only the strip's peak (latest) reading counts, not the sum of all three re-checks.
        assertEquals(22.0, DoseAggregation.cumulativeDose(logs), 0.001)
    }

    @Test
    fun `a new strip's dose adds on top of the previous strip's final reading`() {
        val logs = listOf(
            log("STRIP_1", dosePpm = 10.0, timestamp = 1L),
            log("STRIP_1", dosePpm = 22.0, timestamp = 2L), // strip 1 retired at 22.0
            log("STRIP_2", dosePpm = 5.0, timestamp = 3L),  // fresh strip issued
        )
        assertEquals(27.0, DoseAggregation.cumulativeDose(logs), 0.001)
    }

    @Test
    fun `running cumulative reflects each strip's peak-so-far at every row`() {
        val logs = listOf(
            log("STRIP_1", dosePpm = 10.0, timestamp = 1L),
            log("STRIP_1", dosePpm = 22.0, timestamp = 2L),
            log("STRIP_2", dosePpm = 5.0, timestamp = 3L),
            log("STRIP_2", dosePpm = 12.0, timestamp = 4L),
        )
        assertEquals(listOf(10.0, 22.0, 27.0, 34.0), DoseAggregation.runningCumulative(logs))
    }

    @Test
    fun `a lower re-check reading does not lower the strip's counted peak`() {
        // Later readings should be monotonic in practice, but a spurious lower re-check must not
        // reduce the counted dose for that strip.
        val logs = listOf(
            log("STRIP_1", dosePpm = 20.0, timestamp = 1L),
            log("STRIP_1", dosePpm = 8.0, timestamp = 2L),
        )
        assertEquals(20.0, DoseAggregation.cumulativeDose(logs), 0.001)
    }
}
