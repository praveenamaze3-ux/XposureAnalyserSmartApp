package com.example.xposuredetectorsmart.imageprocessing

import com.example.xposuredetectorsmart.utils.Constants

/**
 * One calibration anchor: a stain optical density paired with its known ppm-hours dose, plus
 * where that anchor came from (a specific manufacturer chart lot, a lab exposure run, etc).
 */
data class OdReferencePoint(
    val opticalDensity: Double,
    val ppm: Double,
    val source: String,
)

data class KnnPrediction(
    val ppm: Double,
    val neighborsUsed: Int,
    /** True when [opticalDensity] fell outside the reference table's calibrated OD range. */
    val extrapolated: Boolean,
)

/**
 * k-NN inverse-distance-weighted regression over the optical-density -> ppm reference table.
 *
 * Directly generalizes the old nearest-2 Euclidean interpolation to nearest-k: same idea (weight
 * each candidate by how close it is), just averaged over a bigger, denser reference set instead
 * of picking exactly two points. Pure Kotlin, no training step - the "model" is the reference
 * table itself plus this fixed weighting rule.
 */
object OdKnnRegressor {

    fun predict(
        table: List<OdReferencePoint>,
        opticalDensity: Double,
        k: Int = Constants.KNN_K,
    ): KnnPrediction {
        require(table.isNotEmpty()) { "Reference curve table must not be empty." }

        val minOd = table.minOf { it.opticalDensity }
        val maxOd = table.maxOf { it.opticalDensity }
        val extrapolated = opticalDensity < minOd || opticalDensity > maxOd

        val byDistance = table
            .map { point -> point to kotlin.math.abs(point.opticalDensity - opticalDensity) }
            .sortedBy { it.second }

        val exactMatch = byDistance.first()
        if (exactMatch.second < Constants.KNN_DISTANCE_EPSILON) {
            return KnnPrediction(ppm = exactMatch.first.ppm, neighborsUsed = 1, extrapolated = extrapolated)
        }

        val neighbors = byDistance.take(k.coerceIn(1, table.size))

        var weightedSum = 0.0
        var weightTotal = 0.0
        for ((point, distance) in neighbors) {
            val weight = 1.0 / distance.coerceAtLeast(Constants.KNN_DISTANCE_EPSILON)
            weightedSum += weight * point.ppm
            weightTotal += weight
        }

        return KnnPrediction(
            ppm = weightedSum / weightTotal,
            neighborsUsed = neighbors.size,
            extrapolated = extrapolated,
        )
    }
}
