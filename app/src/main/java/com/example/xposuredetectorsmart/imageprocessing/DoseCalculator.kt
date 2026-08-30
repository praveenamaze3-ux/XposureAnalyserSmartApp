package com.example.xposuredetectorsmart.imageprocessing

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import com.example.xposuredetectorsmart.utils.Constants
import javax.inject.Inject
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round

/** Risk classification bands, based on shift-average ppm concentration. */
enum class H2SRiskLevel(val description: String, val colorHex: String) {
    SAFE("Normal / Safe (< 1.0 ppm)", "#4CAF50"),
    MODERATE("Caution: ACGIH TLV Exceeded (1.0 - 5.0 ppm)", "#FF9800"),
    HIGH("Warning: Approaching OSHA PEL (5.0 - 10.0 ppm)", "#FF5722"),
    DANGEROUS("DANGER: OSHA PEL / Ceiling Exceeded (> 10.0 ppm)", "#F44336"),
}

data class DoseResult(
    val opticalDensity: Double,
    val totalDosePpmHours: Double,
    val shiftAveragePpm: Double,
    val eightHourTwaPpm: Double,
    val riskLevel: H2SRiskLevel,
    val warningMessage: String?,
    /** How many reference-curve anchors the k-NN regression averaged over for this reading. */
    val referencePointsUsed: Int,
    /** True if the optical density fell outside the reference curve's calibrated range. */
    val extrapolated: Boolean,
)

/**
 * H2S exposure engine based on Beer-Lambert diffuse-reflectance optical density between a blank
 * (White Ref) zone and the exposed strip zone. The blank/sample luminance ratio inherently
 * cancels out ambient lighting variance without a separate color-correction step.
 *
 * The optical density -> dose mapping is a k-NN inverse-distance-weighted regression over a
 * learned reference table (see [OdKnnRegressor], [ReferenceCurveLoader]), rather than a fixed
 * regression formula - swap in denser, manufacturer-calibrated anchor points without touching
 * this class.
 */
class DoseCalculator @Inject constructor(private val referenceCurveLoader: ReferenceCurveLoader) {

    /** [shiftDurationHours] is the worker's actual elapsed work time since shift start, not the industry's configured/scheduled shift length. */
    fun calculate(bitmap: Bitmap, blankZone: Rect, sampleZone: Rect, shiftDurationHours: Double): DoseResult {
        require(shiftDurationHours > 0.0) { "Shift duration must be greater than zero." }

        val yBlank = robustLuminance(bitmap, blankZone)
        val ySample = robustLuminance(bitmap, sampleZone)

        val clampedSample = max(0.001, min(yBlank, ySample))
        val clampedBlank = max(0.01, yBlank)

        val opticalDensity = log10(clampedBlank / clampedSample)

        val belowDetectionLimit = opticalDensity < Constants.MIN_DETECTABLE_OPTICAL_DENSITY
        val prediction = OdKnnRegressor.predict(referenceCurveLoader.referenceTable, opticalDensity)
        val dose = if (belowDetectionLimit) 0.0 else prediction.ppm.coerceAtLeast(0.0)

        val shiftAvgPpm = dose / shiftDurationHours
        val eightHourTwa = dose / 8.0

        val risk = when {
            shiftAvgPpm < 1.0 -> H2SRiskLevel.SAFE
            shiftAvgPpm <= 5.0 -> H2SRiskLevel.MODERATE
            shiftAvgPpm <= 10.0 -> H2SRiskLevel.HIGH
            else -> H2SRiskLevel.DANGEROUS
        }

        val warning = when {
            shiftAvgPpm >= 10.0 -> "Evacuate area and report to site safety officer immediately."
            belowDetectionLimit -> "Stain below detectable limit. Normal atmosphere."
            prediction.extrapolated -> "Reading is outside the calibrated reference range; treat with reduced confidence."
            else -> null
        }

        return DoseResult(
            opticalDensity = roundTo(opticalDensity, 3),
            totalDosePpmHours = roundTo(dose, 2),
            shiftAveragePpm = roundTo(shiftAvgPpm, 2),
            eightHourTwaPpm = roundTo(eightHourTwa, 2),
            riskLevel = risk,
            warningMessage = warning,
            referencePointsUsed = prediction.neighborsUsed,
            extrapolated = prediction.extrapolated,
        )
    }

    /** Trimmed-mean CIE 1931 relative luminance over an ROI, rejecting glare/highlight pixels. */
    private fun robustLuminance(bitmap: Bitmap, roi: Rect): Double {
        val left = roi.left.coerceIn(0, bitmap.width - 1)
        val top = roi.top.coerceIn(0, bitmap.height - 1)
        val right = roi.right.coerceIn(left + 1, bitmap.width)
        val bottom = roi.bottom.coerceIn(top + 1, bitmap.height)

        val luminances = mutableListOf<Double>()
        for (y in top until bottom) {
            for (x in left until right) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel) / 255.0
                val g = Color.green(pixel) / 255.0
                val b = Color.blue(pixel) / 255.0

                // Glare / highlight rejection (> 96% saturated)
                if (r > 0.96 && g > 0.96 && b > 0.96) continue

                val rLin = sRgbToLinear(r)
                val gLin = sRgbToLinear(g)
                val bLin = sRgbToLinear(b)

                luminances.add(0.2126 * rLin + 0.7152 * gLin + 0.0722 * bLin)
            }
        }

        if (luminances.isEmpty()) return 1.0

        luminances.sort()
        val trimCount = (luminances.size * 0.10).toInt()
        val trimmed = if (luminances.size - 2 * trimCount > 0) {
            luminances.subList(trimCount, luminances.size - trimCount)
        } else {
            luminances
        }
        return trimmed.average()
    }

    private fun sRgbToLinear(channel: Double): Double =
        if (channel <= 0.04045) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)

    private fun roundTo(value: Double, decimals: Int): Double {
        val factor = 10.0.pow(decimals)
        return round(value * factor) / factor
    }
}
