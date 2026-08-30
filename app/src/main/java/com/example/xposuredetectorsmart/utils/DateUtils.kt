package com.example.xposuredetectorsmart.utils

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateUtils {

    private val isoDate: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun today(): LocalDate = LocalDate.now()

    fun todayIso(): String = today().format(isoDate)

    fun parseIsoDate(value: String): LocalDate = LocalDate.parse(value, isoDate)

    fun formatIsoDate(date: LocalDate): String = date.format(isoDate)

    fun startOfDayMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun endOfDayMillis(date: LocalDate): Long =
        date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli().minus(1)

    fun formatTimestamp(epochMillis: Long, pattern: String = "MMM d, HH:mm"): String {
        val dateTime = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(epochMillis),
            ZoneId.systemDefault(),
        )
        return dateTime.format(DateTimeFormatter.ofPattern(pattern))
    }

    fun formatShiftWindow(startedAt: Long, expiresAt: Long, nowMillis: Long = System.currentTimeMillis()): String =
        "Started ${formatTimestamp(startedAt, "HH:mm")} · Expires ${formatTimestamp(expiresAt, "HH:mm")} · Now ${formatTimestamp(nowMillis, "HH:mm")}"

    /** Elapsed time between [startMillis] and [nowMillis], formatted as e.g. "2h 17m". */
    fun formatElapsedHm(startMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val elapsed = (nowMillis - startMillis).coerceAtLeast(0)
        val hours = elapsed / 3_600_000
        val minutes = (elapsed % 3_600_000) / 60_000
        return "${hours}h ${minutes}m"
    }

    /**
     * Hours elapsed between [startMillis] and [nowMillis] - the worker's actual worked time so
     * far, as opposed to the industry's scheduled/configured shift length. Average-dose
     * (shift-average ppm) calculations should divide by this, not by a fixed shift duration,
     * or a shift that ends early/late would understate/overstate the real average concentration.
     */
    fun elapsedHours(startMillis: Long, nowMillis: Long = System.currentTimeMillis()): Double =
        (nowMillis - startMillis).coerceAtLeast(0) / 3_600_000.0

    fun hoursSinceMidnight(epochMillis: Long): Double {
        val dateTime = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(epochMillis),
            ZoneId.systemDefault(),
        )
        return dateTime.hour + dateTime.minute / 60.0
    }
}
