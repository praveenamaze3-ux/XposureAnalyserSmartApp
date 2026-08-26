package com.example.xposuredetectorsmart.imageprocessing

import com.example.xposuredetectorsmart.database.entities.ColorProfile
import com.example.xposuredetectorsmart.utils.Constants
import com.example.xposuredetectorsmart.utils.RgbColor
import timber.log.Timber

data class ScaleResult(
    val scale: RgbColor,
    val meanSquareError: Double,
    val referenceWhite: RgbColor,
)

/**
 * Pure math for deriving per-channel correction scale factors from a white reference patch plus
 * calibration history. Deliberately free of Bitmap/OpenCV so it can be unit-tested on the host JVM;
 * [ColorCorrector] applies the resulting scale to actual pixels via OpenCV.
 */
object ColorCorrectionMath {

    fun compute(whitePatch: ColorPatch?, history: List<ColorProfile>): ScaleResult {
        val effectiveWhite = resolveEffectiveWhite(whitePatch, history)

        var scaleR = safeScale(255.0 / effectiveWhite.r)
        var scaleG = safeScale(255.0 / effectiveWhite.g)
        var scaleB = safeScale(255.0 / effectiveWhite.b)

        val avgScale = (scaleR + scaleG + scaleB) / 3.0
        scaleR = clampDeviation(scaleR, avgScale)
        scaleG = clampDeviation(scaleG, avgScale)
        scaleB = clampDeviation(scaleB, avgScale)

        val mse = if (whitePatch != null) {
            val target = 255.0
            val errR = whitePatch.avgColor.r * scaleR - target
            val errG = whitePatch.avgColor.g * scaleG - target
            val errB = whitePatch.avgColor.b * scaleB - target
            (errR * errR + errG * errG + errB * errB) / 3.0
        } else {
            0.0
        }

        return ScaleResult(RgbColor(scaleR, scaleG, scaleB), mse, effectiveWhite)
    }

    private fun resolveEffectiveWhite(whitePatch: ColorPatch?, history: List<ColorProfile>): RgbColor {
        val recentHistory = history.take(Constants.COLOR_PROFILE_HISTORY_SIZE)

        if (whitePatch == null && recentHistory.isEmpty()) {
            Timber.w("No white patch detected and no calibration history; assuming neutral white")
            return RgbColor(255.0, 255.0, 255.0)
        }

        // Weighted average: the strip's own patch (this scan) counts as heavily as the whole
        // history combined, so a single bad historical calibration can't dominate.
        var sumR = 0.0
        var sumG = 0.0
        var sumB = 0.0
        var weight = 0.0

        if (whitePatch != null) {
            val currentWeight = recentHistory.size.coerceAtLeast(1).toDouble()
            sumR += whitePatch.avgColor.r * currentWeight
            sumG += whitePatch.avgColor.g * currentWeight
            sumB += whitePatch.avgColor.b * currentWeight
            weight += currentWeight
        }

        recentHistory.forEach { profile ->
            sumR += profile.whiteR
            sumG += profile.whiteG
            sumB += profile.whiteB
            weight += 1.0
        }

        return RgbColor(sumR / weight, sumG / weight, sumB / weight)
    }

    private fun safeScale(scale: Double): Double {
        if (scale.isNaN() || scale.isInfinite()) return 1.0
        return scale.coerceIn(Constants.MIN_CORRECTION_SCALE, Constants.MAX_CORRECTION_SCALE)
    }

    private fun clampDeviation(scale: Double, average: Double): Double {
        val maxAllowed = average * Constants.MAX_DEVIATION_FACTOR
        val minAllowed = average / Constants.MAX_DEVIATION_FACTOR
        return scale.coerceIn(minAllowed, maxAllowed)
    }
}
