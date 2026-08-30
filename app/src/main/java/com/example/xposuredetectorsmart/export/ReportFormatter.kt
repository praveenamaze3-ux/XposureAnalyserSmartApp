package com.example.xposuredetectorsmart.export

import com.example.xposuredetectorsmart.database.entities.DoseLog
import com.example.xposuredetectorsmart.imageprocessing.H2SRiskLevel
import com.example.xposuredetectorsmart.utils.Constants
import com.example.xposuredetectorsmart.utils.DateUtils
import com.example.xposuredetectorsmart.utils.DoseAggregation

data class ReportRow(
    val timestamp: String,
    val dosePpm: Double,
    val confidence: Float,
    val status: H2SRiskLevel,
)

object ReportFormatter {

    /**
     * Classifies a cumulative dose (ppm·hr) into a risk level by converting it to a shift-average
     * concentration (dose / shiftDurationHours) first, matching [H2SRiskLevel]'s own thresholds.
     * [shiftDurationHours] defaults to the app's configured shift length when the real per-worker
     * value isn't available at the call site (e.g. offline report generation).
     */
    fun statusFor(
        cumulativeDosePpmHours: Double,
        shiftDurationHours: Double = Constants.DEFAULT_SHIFT_DURATION_HOURS.toDouble(),
    ): H2SRiskLevel {
        val shiftAveragePpm = cumulativeDosePpmHours / shiftDurationHours
        return when {
            shiftAveragePpm < Constants.RISK_MODERATE_MIN_PPM -> H2SRiskLevel.SAFE
            shiftAveragePpm <= Constants.RISK_HIGH_MIN_PPM -> H2SRiskLevel.MODERATE
            shiftAveragePpm <= Constants.RISK_DANGEROUS_MIN_PPM -> H2SRiskLevel.HIGH
            else -> H2SRiskLevel.DANGEROUS
        }
    }

    fun toRows(logs: List<DoseLog>, shiftDurationHours: Double = Constants.DEFAULT_SHIFT_DURATION_HOURS.toDouble()): List<ReportRow> {
        val sorted = logs.sortedBy { it.timestamp }
        val runningCumulative = DoseAggregation.runningCumulative(sorted)
        return sorted.zip(runningCumulative).map { (log, cumulative) ->
            ReportRow(
                timestamp = DateUtils.formatTimestamp(log.timestamp),
                dosePpm = log.dosePpm,
                confidence = log.confidence,
                status = statusFor(cumulative, shiftDurationHours),
            )
        }
    }
}
