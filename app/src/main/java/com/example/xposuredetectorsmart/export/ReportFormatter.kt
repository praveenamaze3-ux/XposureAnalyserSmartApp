package com.example.xposuredetectorsmart.export

import com.example.xposuredetectorsmart.database.entities.DoseLog
import com.example.xposuredetectorsmart.utils.Constants
import com.example.xposuredetectorsmart.utils.DateUtils

enum class ExposureStatus { NORMAL, ALERT, CRITICAL }

data class ReportRow(
    val timestamp: String,
    val dosePpm: Double,
    val confidence: Float,
    val status: ExposureStatus,
)

object ReportFormatter {

    fun statusFor(cumulativePpm: Double): ExposureStatus = when {
        cumulativePpm >= Constants.IDLH_PPM -> ExposureStatus.CRITICAL
        cumulativePpm >= Constants.OSHA_PEL_8HR -> ExposureStatus.ALERT
        else -> ExposureStatus.NORMAL
    }

    fun toRows(logs: List<DoseLog>): List<ReportRow> {
        var cumulative = 0.0
        return logs.sortedBy { it.timestamp }.map { log ->
            cumulative += log.dosePpm
            ReportRow(
                timestamp = DateUtils.formatTimestamp(log.timestamp),
                dosePpm = log.dosePpm,
                confidence = log.confidence,
                status = statusFor(cumulative),
            )
        }
    }
}
