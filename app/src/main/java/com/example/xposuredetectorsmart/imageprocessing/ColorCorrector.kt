package com.example.xposuredetectorsmart.imageprocessing

import android.graphics.Bitmap
import com.example.xposuredetectorsmart.database.entities.ColorProfile
import com.example.xposuredetectorsmart.utils.Constants
import com.example.xposuredetectorsmart.utils.RgbColor
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import javax.inject.Inject

data class CorrectionResult(
    val correctedBitmap: Bitmap,
    val scale: RgbColor,
    val meanSquareError: Double,
    val referenceWhite: RgbColor,
)

/**
 * Applies a white-balance style correction matrix derived from the strip's own white/grey
 * reference patches, blended with the worker's historical device calibration (see
 * [ColorCorrectionMath]), followed by an inverse-gamma correction on the actual pixels.
 */
class ColorCorrector @Inject constructor() {

    fun correct(
        bitmap: Bitmap,
        whitePatch: ColorPatch?,
        history: List<ColorProfile>,
    ): CorrectionResult {
        val scaleResult = ColorCorrectionMath.compute(whitePatch, history)
        val corrected = applyToMat(bitmap, scaleResult.scale.r, scaleResult.scale.g, scaleResult.scale.b)

        return CorrectionResult(
            correctedBitmap = corrected,
            scale = scaleResult.scale,
            meanSquareError = scaleResult.meanSquareError,
            referenceWhite = scaleResult.referenceWhite,
        )
    }

    private fun applyToMat(bitmap: Bitmap, scaleR: Double, scaleG: Double, scaleB: Double): Bitmap {
        val rgba = Mat()
        val rgb = Mat()
        val channels = ArrayList<Mat>(3)
        val merged = Mat()
        try {
            Utils.bitmapToMat(bitmap, rgba)
            Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2RGB)
            Core.split(rgb, channels)

            val scales = doubleArrayOf(scaleR, scaleG, scaleB)
            val invGamma = 1.0 / Constants.GAMMA

            for (i in 0 until 3) {
                val channel = channels[i]
                val tmp = Mat()
                channel.convertTo(tmp, CvType.CV_64F)
                Core.multiply(tmp, Scalar(scales[i]), tmp)
                Core.min(tmp, Scalar(255.0), tmp)
                Core.max(tmp, Scalar(0.0), tmp)
                Core.divide(tmp, Scalar(255.0), tmp)
                Core.pow(tmp, invGamma, tmp)
                Core.multiply(tmp, Scalar(255.0), tmp)
                tmp.convertTo(channel, CvType.CV_8U)
                tmp.release()
            }

            Core.merge(channels, merged)

            val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(merged, result)
            return result
        } finally {
            rgba.release()
            rgb.release()
            channels.forEach { it.release() }
            merged.release()
        }
    }
}
