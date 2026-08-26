package com.example.xposuredetectorsmart.imageprocessing

import com.example.xposuredetectorsmart.utils.ColorUtils
import com.example.xposuredetectorsmart.utils.RgbColor
import javax.inject.Inject

data class InterpolatedDose(
    val ppm: Double,
    val nearestPpm: Double,
    val nearestDistance: Double,
    val secondPpm: Double,
    val secondDistance: Double,
)

/**
 * RGB -> PPM lookup + linear interpolation.
 *
 * NOTE: the reference colors below are placeholder values describing a plausible monotonic
 * white/tan -> dark-brown darkening curve for a lead-acetate style H2S strip. Replace with the
 * actual manufacturer-calibrated RGB reference swatches for the strip lot in production use.
 */
class DoseCalculator @Inject constructor() {

    private val referenceTable: List<Pair<Double, RgbColor>> = listOf(
        10.0 to RgbColor(210.0, 195.0, 170.0),
        25.0 to RgbColor(180.0, 150.0, 120.0),
        50.0 to RgbColor(140.0, 105.0, 80.0),
        100.0 to RgbColor(95.0, 65.0, 50.0),
        150.0 to RgbColor(55.0, 35.0, 30.0),
        200.0 to RgbColor(25.0, 15.0, 15.0),
    )

    fun calculate(inkColor: RgbColor): InterpolatedDose {
        val distances = referenceTable
            .map { (ppm, color) -> ppm to ColorUtils.distance(inkColor, color) }
            .sortedBy { it.second }

        val nearest = distances[0]
        val second = distances.getOrElse(1) { distances[0] }

        if (nearest.second < 1e-6) {
            return InterpolatedDose(nearest.first, nearest.first, 0.0, second.first, second.second)
        }

        val dist1 = nearest.second
        val dist2 = second.second.coerceAtLeast(1e-6)

        val ppm = (nearest.first * dist2 + second.first * dist1) / (dist1 + dist2)

        return InterpolatedDose(
            ppm = ppm,
            nearestPpm = nearest.first,
            nearestDistance = dist1,
            secondPpm = second.first,
            secondDistance = dist2,
        )
    }
}
