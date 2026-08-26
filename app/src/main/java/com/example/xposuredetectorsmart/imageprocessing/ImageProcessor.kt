package com.example.xposuredetectorsmart.imageprocessing

import android.graphics.Bitmap
import com.example.xposuredetectorsmart.database.entities.ColorProfile
import timber.log.Timber
import javax.inject.Inject

sealed class ProcessingOutcome {
    data class Success(
        val ppm: Double,
        val confidence: Float,
        val correctedBitmap: Bitmap,
        val patches: List<ColorPatch>,
        val correction: CorrectionResult,
        val quality: QualityMetrics,
        val dose: InterpolatedDose,
    ) : ProcessingOutcome()

    data class Failure(val reason: String) : ProcessingOutcome()
}

/**
 * Orchestrates the full capture -> dose pipeline: reference patch detection, adaptive color
 * correction, ink patch re-detection on the corrected image, dose interpolation and quality
 * scoring.
 */
class ImageProcessor @Inject constructor(
    private val referenceDetector: ReferenceDetector,
    private val colorCorrector: ColorCorrector,
    private val doseCalculator: DoseCalculator,
    private val qualityValidator: QualityValidator,
) {

    fun process(bitmap: Bitmap, colorProfileHistory: List<ColorProfile>): ProcessingOutcome {
        val originalPatches = referenceDetector.detect(bitmap)
        val whitePatch = originalPatches.find { it.type == PatchType.WHITE }

        if (whitePatch == null) {
            Timber.w("No white reference patch detected; correction will fall back to history/neutral white")
        }

        val correction = colorCorrector.correct(bitmap, whitePatch, colorProfileHistory)

        val correctedPatches = referenceDetector.detect(correction.correctedBitmap)
        val inkPatch = correctedPatches.find { it.type == PatchType.INK }
            ?: originalPatches.find { it.type == PatchType.INK }

        if (inkPatch == null) {
            return ProcessingOutcome.Failure("No ink patch detected on the strip. Reposition and retry.")
        }

        val quality = qualityValidator.assess(correction.correctedBitmap, inkPatch)
        val dose = doseCalculator.calculate(inkPatch.avgColor)

        return ProcessingOutcome.Success(
            ppm = dose.ppm,
            confidence = quality.confidence.toFloat(),
            correctedBitmap = correction.correctedBitmap,
            patches = correctedPatches.ifEmpty { originalPatches },
            correction = correction,
            quality = quality,
            dose = dose,
        )
    }
}
