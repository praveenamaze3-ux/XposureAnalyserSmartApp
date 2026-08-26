package com.example.xposuredetectorsmart.utils

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/** Plain RGB triple in the 0..255 range (doubles so correction math doesn't clip early). */
data class RgbColor(val r: Double, val g: Double, val b: Double) {
    fun clamped() = RgbColor(r.coerceIn(0.0, 255.0), g.coerceIn(0.0, 255.0), b.coerceIn(0.0, 255.0))
}

object ColorUtils {

    /** Euclidean distance between two RGB colors. */
    fun distance(a: RgbColor, b: RgbColor): Double {
        val dr = a.r - b.r
        val dg = a.g - b.g
        val db = a.b - b.b
        return sqrt(dr * dr + dg * dg + db * db)
    }

    /** HSV-style saturation of an RGB color, normalized 0..1. */
    fun saturation(color: RgbColor): Double {
        val maxC = max(color.r, max(color.g, color.b))
        val minC = min(color.r, min(color.g, color.b))
        if (maxC <= 0.0) return 0.0
        return (maxC - minC) / maxC
    }

    /** Rec. 601 perceptual luminance. */
    fun luminance(color: RgbColor): Double =
        0.299 * color.r + 0.587 * color.g + 0.114 * color.b

    /** Applies scale factors then inverse-gamma correction to a single channel value. */
    fun correctChannel(value: Double, scale: Double, gamma: Double): Double {
        val scaled = (value * scale).coerceIn(0.0, 255.0)
        val normalized = scaled / 255.0
        return (normalized.pow(1.0 / gamma) * 255.0).coerceIn(0.0, 255.0)
    }
}
