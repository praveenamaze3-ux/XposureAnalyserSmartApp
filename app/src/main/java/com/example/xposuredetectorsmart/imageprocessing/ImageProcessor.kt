package com.example.xposuredetectorsmart.imageprocessing

import android.graphics.Bitmap
import android.graphics.Point
import com.example.xposuredetectorsmart.utils.RgbColor
import javax.inject.Inject

sealed class ProcessingOutcome {
    data class Success(
        val ppm: Double,
        val confidence: Float,
        val sampleColor: RgbColor,
        val blankColor: RgbColor,
        val patches: List<ColorPatch>,
        val dose: DoseResult,
        val quality: QualityMetrics,
    ) : ProcessingOutcome()

    data class Failure(val reason: String) : ProcessingOutcome()
}

/** The three points the worker taps on the captured photo to mark each reference patch. */
data class ManualPatchPoints(val white: Point, val grey: Point, val ink: Point)

/**
 * Orchestrates the full capture -> dose pipeline: samples the worker-pinpointed reference
 * patches (10x10px average around each tap), Beer-Lambert optical-density dose calculation
 * between the White Ref (blank) and strip (sample) zones, and quality scoring.
 */
class ImageProcessor @Inject constructor(
    private val patchSampler: PatchSampler,
    private val doseCalculator: DoseCalculator,
    private val qualityValidator: QualityValidator,
) {

    /** [shiftDurationHours] is the worker's actual elapsed work time since shift start, not the industry's configured/scheduled shift length. */
    fun process(bitmap: Bitmap, points: ManualPatchPoints, shiftDurationHours: Double): ProcessingOutcome {
        val whitePatch = patchSampler.sample(bitmap, PatchType.WHITE, points.white)
        val greyPatch = patchSampler.sample(bitmap, PatchType.GREY, points.grey)
        val inkPatch = patchSampler.sample(bitmap, PatchType.INK, points.ink)

        val dose = doseCalculator.calculate(bitmap, whitePatch.bounds, inkPatch.bounds, shiftDurationHours)
        val quality = qualityValidator.assess(bitmap, inkPatch)

        return ProcessingOutcome.Success(
            ppm = dose.totalDosePpmHours,
            confidence = quality.confidence.toFloat(),
            sampleColor = inkPatch.avgColor,
            blankColor = whitePatch.avgColor,
            patches = listOf(whitePatch, greyPatch, inkPatch),
            dose = dose,
            quality = quality,
        )
    }
}
