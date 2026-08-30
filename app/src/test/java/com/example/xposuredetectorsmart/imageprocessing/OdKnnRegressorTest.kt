package com.example.xposuredetectorsmart.imageprocessing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OdKnnRegressorTest {

    private val table = listOf(
        OdReferencePoint(0.0, 0.0, "test"),
        OdReferencePoint(1.0, 20.0, "test"),
        OdReferencePoint(2.0, 40.0, "test"),
        OdReferencePoint(3.0, 60.0, "test"),
        OdReferencePoint(4.0, 80.0, "test"),
    )

    @Test
    fun exactMatchReturnsThatPointsPpmDirectly() {
        val result = OdKnnRegressor.predict(table, opticalDensity = 2.0, k = 3)
        assertEquals(40.0, result.ppm, 1e-9)
        assertEquals(1, result.neighborsUsed)
        assertFalse(result.extrapolated)
    }

    @Test
    fun interpolatesBetweenNeighborsOnALinearCurve() {
        // Table is exactly linear (ppm = 20*od), so k-NN over it should reproduce that line
        // for any interior point, regardless of k.
        val result = OdKnnRegressor.predict(table, opticalDensity = 2.5, k = 4)
        assertEquals(50.0, result.ppm, 0.5)
        assertFalse(result.extrapolated)
    }

    @Test
    fun flagsExtrapolationOutsideTheReferenceRange() {
        val below = OdKnnRegressor.predict(table, opticalDensity = -0.5, k = 3)
        val above = OdKnnRegressor.predict(table, opticalDensity = 10.0, k = 3)

        assertTrue(below.extrapolated)
        assertTrue(above.extrapolated)
    }

    @Test
    fun kIsCoercedToTableSizeWhenLarger() {
        val result = OdKnnRegressor.predict(table, opticalDensity = 1.5, k = 999)
        assertEquals(table.size, result.neighborsUsed)
    }

    @Test
    fun closerNeighborsAreWeightedMoreHeavily() {
        // 0.1 is much closer to od=0.0 (ppm=0) than to od=1.0 (ppm=20), so the weighted result
        // should sit well below the midpoint.
        val result = OdKnnRegressor.predict(table, opticalDensity = 0.1, k = 2)
        assertTrue(result.ppm < 10.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsAnEmptyTable() {
        OdKnnRegressor.predict(emptyList(), opticalDensity = 1.0)
    }
}
