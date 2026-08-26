package com.example.xposuredetectorsmart.imageprocessing

import android.graphics.Bitmap
import com.example.xposuredetectorsmart.utils.Constants
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.imgproc.Imgproc
import javax.inject.Inject

data class QualityMetrics(
    val saturation: Double,
    val contrast: Double,
    val sharpness: Double,
    val confidence: Double,
)

/**
 * Sanity/quality checks on a captured strip photo: saturation of the detected ink patch,
 * overall image contrast (std dev of luminance) and sharpness (Laplacian variance), combined
 * into a single confidence score.
 */
class QualityValidator @Inject constructor() {

    fun assess(bitmap: Bitmap, inkPatch: ColorPatch?): QualityMetrics {
        val saturation = inkPatch?.saturation?.coerceIn(0.0, 1.0) ?: 0.0
        val contrast = computeContrast(bitmap)
        val sharpness = computeSharpness(bitmap)

        val confidence = (
            saturation * Constants.WEIGHT_SATURATION +
                contrast * Constants.WEIGHT_CONTRAST +
                sharpness * Constants.WEIGHT_SHARPNESS
            ).coerceIn(0.0, 1.0)

        return QualityMetrics(saturation, contrast, sharpness, confidence)
    }

    private fun computeContrast(bitmap: Bitmap): Double {
        val rgba = Mat()
        val gray = Mat()
        return try {
            Utils.bitmapToMat(bitmap, rgba)
            Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
            val mean = MatOfDouble()
            val stdDev = MatOfDouble()
            org.opencv.core.Core.meanStdDev(gray, mean, stdDev)
            val std = stdDev.toArray().firstOrNull() ?: 0.0
            mean.release()
            stdDev.release()
            // Std dev of an 8-bit grayscale image maxes out around ~127.5 for a checkerboard;
            // normalize against a realistic "well-lit, high-contrast strip" ceiling instead.
            (std / MAX_EXPECTED_STD_DEV).coerceIn(0.0, 1.0)
        } finally {
            rgba.release()
            gray.release()
        }
    }

    private fun computeSharpness(bitmap: Bitmap): Double {
        val rgba = Mat()
        val gray = Mat()
        val laplacian = Mat()
        return try {
            Utils.bitmapToMat(bitmap, rgba)
            Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.Laplacian(gray, laplacian, CvType.CV_64F)
            val mean = MatOfDouble()
            val stdDev = MatOfDouble()
            org.opencv.core.Core.meanStdDev(laplacian, mean, stdDev)
            val std = stdDev.toArray().firstOrNull() ?: 0.0
            mean.release()
            stdDev.release()
            val variance = std * std
            (variance / MAX_EXPECTED_LAPLACIAN_VARIANCE).coerceIn(0.0, 1.0)
        } finally {
            rgba.release()
            gray.release()
            laplacian.release()
        }
    }

    companion object {
        private const val MAX_EXPECTED_STD_DEV = 80.0
        private const val MAX_EXPECTED_LAPLACIAN_VARIANCE = 1500.0
    }
}
