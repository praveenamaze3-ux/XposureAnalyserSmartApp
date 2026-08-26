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

    fun hoursSinceMidnight(epochMillis: Long): Double {
        val dateTime = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(epochMillis),
            ZoneId.systemDefault(),
        )
        return dateTime.hour + dateTime.minute / 60.0
    }
}
