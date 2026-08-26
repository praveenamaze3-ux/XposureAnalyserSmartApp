package com.example.xposuredetectorsmart.imageprocessing

import com.example.xposuredetectorsmart.utils.RgbColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DoseCalculatorTest {

    private val calculator = DoseCalculator()

    @Test
    fun `exact match on a reference color returns that reference ppm`() {
        val result = calculator.calculate(RgbColor(210.0, 195.0, 170.0)) // exact 10ppm reference
        assertEquals(10.0, result.ppm, 0.01)
    }

    @Test
    fun `interpolates between the two nearest reference points`() {
        // Midpoint between the 10ppm and 25ppm reference colors should land near 17.5ppm.
        val midpoint = RgbColor(
            (210.0 + 180.0) / 2,
            (195.0 + 150.0) / 2,
            (170.0 + 120.0) / 2,
        )
        val result = calculator.calculate(midpoint)
        assertTrue("expected ppm between 10 and 25 but was ${result.ppm}", result.ppm in 10.0..25.0)
        assertEquals(17.5, result.ppm, 1.0)
    }

    @Test
    fun `darker ink yields higher ppm than lighter ink`() {
        val light = calculator.calculate(RgbColor(200.0, 190.0, 165.0))
        val dark = calculator.calculate(RgbColor(40.0, 25.0, 20.0))
        assertTrue(dark.ppm > light.ppm)
    }

    @Test
    fun `never returns a ppm outside the lookup table bounds`() {
        val belowRange = calculator.calculate(RgbColor(255.0, 255.0, 255.0))
        val aboveRange = calculator.calculate(RgbColor(0.0, 0.0, 0.0))
        assertTrue(belowRange.ppm in 10.0..200.0)
        assertTrue(aboveRange.ppm in 10.0..200.0)
    }
}
