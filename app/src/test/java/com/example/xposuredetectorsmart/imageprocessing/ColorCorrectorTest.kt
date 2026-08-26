package com.example.xposuredetectorsmart.imageprocessing

import android.graphics.Rect
import com.example.xposuredetectorsmart.database.entities.ColorProfile
import com.example.xposuredetectorsmart.utils.Constants
import com.example.xposuredetectorsmart.utils.RgbColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exercises [ColorCorrectionMath] - the pure scale-derivation logic behind [ColorCorrector].
 * The actual pixel transform (applyToMat) requires the OpenCV native library and a real Bitmap,
 * so that part is covered by an instrumented test instead (see androidTest).
 */
class ColorCorrectorTest {

    private fun whitePatch(r: Double, g: Double, b: Double) = ColorPatch(
        type = PatchType.WHITE,
        bounds = Rect(0, 0, 10, 10),
        avgColor = RgbColor(r, g, b),
        saturation = 0.0,
    )

    @Test
    fun `a perfectly white patch yields scale factors of exactly 1`() {
        val result = ColorCorrectionMath.compute(whitePatch(255.0, 255.0, 255.0), emptyList())
        assertEquals(1.0, result.scale.r, 0.001)
        assertEquals(1.0, result.scale.g, 0.001)
        assertEquals(1.0, result.scale.b, 0.001)
        assertEquals(0.0, result.meanSquareError, 0.001)
    }

    @Test
    fun `a dim white patch yields scale factors greater than 1`() {
        val result = ColorCorrectionMath.compute(whitePatch(200.0, 190.0, 180.0), emptyList())
        assertTrue(result.scale.r > 1.0)
        assertTrue(result.scale.g > 1.0)
        assertTrue(result.scale.b > 1.0)
    }

    @Test
    fun `scale factors are always clamped within the configured sanity bounds`() {
        // An extremely dark "white" patch would naively demand a huge scale factor.
        val result = ColorCorrectionMath.compute(whitePatch(5.0, 5.0, 5.0), emptyList())
        assertTrue(result.scale.r <= Constants.MAX_CORRECTION_SCALE)
        assertTrue(result.scale.g <= Constants.MAX_CORRECTION_SCALE)
        assertTrue(result.scale.b <= Constants.MAX_CORRECTION_SCALE)
    }

    @Test
    fun `missing white patch falls back to calibration history`() {
        val history = listOf(
            ColorProfile(
                deviceModel = "Pixel 8",
                workerId = "WRK_1",
                whiteR = 240f, whiteG = 240f, whiteB = 240f,
                greyR = 128f, greyG = 128f, greyB = 128f,
                calibrationDate = 1L,
                calibrationCount = 1,
                meanSquareError = 0.0,
                isActive = true,
            ),
        )
        val result = ColorCorrectionMath.compute(null, history)
        // 255/240 ~= 1.0625
        assertEquals(1.0625, result.scale.r, 0.01)
    }

    @Test
    fun `no patch and no history falls back to a neutral (no-op) correction`() {
        val result = ColorCorrectionMath.compute(null, emptyList())
        assertEquals(1.0, result.scale.r, 0.001)
        assertEquals(1.0, result.scale.g, 0.001)
        assertEquals(1.0, result.scale.b, 0.001)
    }

    @Test
    fun `a strongly color-cast white patch has its outlier channel pulled back toward average`() {
        // Blue channel is wildly underexposed relative to R and G (naively: 255/20 = 12.75,
        // clamped first to the global max of 3.0). clampDeviation should then pull it in further,
        // toward the other two channels' scale, rather than leaving it pinned at the global max.
        val result = ColorCorrectionMath.compute(whitePatch(250.0, 245.0, 20.0), emptyList())
        assertTrue("expected clampDeviation to pull the outlier below the global max", result.scale.b < Constants.MAX_CORRECTION_SCALE)
        assertTrue("blue scale should still be the largest of the three", result.scale.b > result.scale.r)
        assertTrue("clamped scale should stay within the configured sanity bounds", result.scale.b <= Constants.MAX_CORRECTION_SCALE)
    }
}
