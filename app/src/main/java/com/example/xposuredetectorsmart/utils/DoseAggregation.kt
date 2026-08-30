package com.example.xposuredetectorsmart.utils

import com.example.xposuredetectorsmart.database.entities.DoseLog

/**
 * Each physical strip integrates dose over its own wear period, so re-checking the same strip
 * multiple times must not add each reading on top of the last - only its latest (peak) reading
 * counts. A shift's cumulative dose is the sum of each distinct strip's peak reading.
 */
object DoseAggregation {

    fun cumulativeDose(logs: List<DoseLog>): Double =
        logs.groupBy { it.stripSerial }.values.sumOf { group -> group.maxOf { it.dosePpm } }

    /** Running per-row cumulative for a chronological (ascending-timestamp) list of logs. */
    fun runningCumulative(logsSortedByTimestamp: List<DoseLog>): List<Double> {
        val peakByStrip = mutableMapOf<String, Double>()
        return logsSortedByTimestamp.map { log ->
            val current = peakByStrip.getOrDefault(log.stripSerial, 0.0)
            if (log.dosePpm > current) peakByStrip[log.stripSerial] = log.dosePpm
            peakByStrip.values.sum()
        }
    }
}
