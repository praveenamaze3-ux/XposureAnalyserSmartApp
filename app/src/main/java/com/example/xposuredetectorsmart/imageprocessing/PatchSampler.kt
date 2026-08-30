package com.example.xposuredetectorsmart.imageprocessing

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import com.example.xposuredetectorsmart.utils.ColorUtils
import com.example.xposuredetectorsmart.utils.RgbColor
import javax.inject.Inject

/**
 * Samples the average color in a small square around a worker-tapped point on the strip photo.
 * Replaces automatic HSV/contour detection: the worker pinpoints the White Ref, Grey Ref, and
 * exposed strip-color patches directly, which is more reliable than auto-detection across the
 * range of real-world lighting/strip conditions in the field.
 */
class PatchSampler @Inject constructor() {

    fun sample(bitmap: Bitmap, type: PatchType, point: Point, radiusPx: Int = 5): ColorPatch {
        val left = (point.x - radiusPx).coerceIn(0, bitmap.width - 1)
        val top = (point.y - radiusPx).coerceIn(0, bitmap.height - 1)
        val right = (point.x + radiusPx).coerceIn(left + 1, bitmap.width)
        val bottom = (point.y + radiusPx).coerceIn(top + 1, bitmap.height)

        var sumR = 0.0
        var sumG = 0.0
        var sumB = 0.0
        var count = 0
        for (y in top until bottom) {
            for (x in left until right) {
                val pixel = bitmap.getPixel(x, y)
                sumR += Color.red(pixel)
                sumG += Color.green(pixel)
                sumB += Color.blue(pixel)
                count++
            }
        }

        val avgColor = RgbColor(sumR / count, sumG / count, sumB / count)
        return ColorPatch(
            type = type,
            bounds = Rect(left, top, right, bottom),
            avgColor = avgColor,
            saturation = ColorUtils.saturation(avgColor),
        )
    }
}
