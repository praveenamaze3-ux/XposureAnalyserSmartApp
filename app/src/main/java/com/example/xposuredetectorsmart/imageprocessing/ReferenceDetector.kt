package com.example.xposuredetectorsmart.imageprocessing

import android.graphics.Bitmap
import android.graphics.Rect
import com.example.xposuredetectorsmart.utils.ColorUtils
import com.example.xposuredetectorsmart.utils.RgbColor
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Scalar
import org.opencv.geometry.Geometry
import org.opencv.imgproc.Imgproc
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.min

/**
 * Detects the white / grey / ink reference patches on a strip photo using HSV thresholding
 * followed by contour detection, per the OSHA strip layout convention.
 */
class ReferenceDetector @Inject constructor() {

    private data class HsvRange(val lower: Scalar, val upper: Scalar)

    private val ranges = mapOf(
        PatchType.WHITE to HsvRange(Scalar(0.0, 0.0, 200.0), Scalar(180.0, 30.0, 255.0)),
        PatchType.GREY to HsvRange(Scalar(0.0, 0.0, 80.0), Scalar(180.0, 30.0, 200.0)),
        PatchType.INK to HsvRange(Scalar(0.0, 100.0, 50.0), Scalar(180.0, 255.0, 200.0)),
    )

    private val minContourArea = 200.0

    fun detect(bitmap: Bitmap): List<ColorPatch> {
        val rgba = Mat()
        val rgb = Mat()
        val hsv = Mat()
        try {
            Utils.bitmapToMat(bitmap, rgba)
            Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2RGB)
            Imgproc.cvtColor(rgb, hsv, Imgproc.COLOR_RGB2HSV)

            return ranges.mapNotNull { (type, range) -> detectPatch(type, rgb, hsv, range) }
        } finally {
            rgba.release()
            rgb.release()
            hsv.release()
        }
    }

    private fun detectPatch(type: PatchType, rgb: Mat, hsv: Mat, range: HsvRange): ColorPatch? {
        val mask = Mat()
        try {
            Core.inRange(hsv, range.lower, range.upper, mask)

            val contours = mutableListOf<MatOfPoint>()
            val hierarchy = Mat()
            try {
                Imgproc.findContours(
                    mask,
                    contours,
                    hierarchy,
                    Imgproc.RETR_EXTERNAL,
                    Imgproc.CHAIN_APPROX_SIMPLE,
                )
            } finally {
                hierarchy.release()
            }

            val largest = contours.maxByOrNull { Geometry.contourArea(it) }
            if (largest == null || Geometry.contourArea(largest) < minContourArea) {
                Timber.d("No usable %s patch found (largest area below threshold)", type)
                contours.forEach { it.release() }
                return null
            }

            val boundingRect = Geometry.boundingRect(largest)
            contours.forEach { it.release() }

            val x = boundingRect.x.coerceIn(0, rgb.cols() - 1)
            val y = boundingRect.y.coerceIn(0, rgb.rows() - 1)
            val w = min(boundingRect.width, rgb.cols() - x).coerceAtLeast(1)
            val h = min(boundingRect.height, rgb.rows() - y).coerceAtLeast(1)
            val safeRect = org.opencv.core.Rect(x, y, w, h)

            val submat = rgb.submat(safeRect)
            val meanScalar = try {
                Core.mean(submat)
            } finally {
                submat.release()
            }

            val avgColor = RgbColor(meanScalar.`val`[0], meanScalar.`val`[1], meanScalar.`val`[2])
            val saturation = ColorUtils.saturation(avgColor)

            return ColorPatch(
                type = type,
                bounds = Rect(x, y, x + w, y + h),
                avgColor = avgColor,
                saturation = saturation,
            )
        } finally {
            mask.release()
        }
    }
}
