package com.example.xposuredetectorsmart

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.xposuredetectorsmart.imageprocessing.DoseCalculator
import com.example.xposuredetectorsmart.imageprocessing.H2SRiskLevel
import com.example.xposuredetectorsmart.imageprocessing.ReferenceCurveLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * android.graphics.Bitmap requires a real Android runtime, so this is an instrumented test
 * rather than a host JVM unit test.
 */
@RunWith(AndroidJUnit4::class)
class DoseCalculatorTest {

    private val calculator = DoseCalculator(
        ReferenceCurveLoader(ApplicationProvider.getApplicationContext()),
    )

    private val blankZone = Rect(10, 10, 60, 60)
    private val sampleZone = Rect(120, 10, 170, 60)

    private fun cardBitmap(sampleGray: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(300, 100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)
        val paint = Paint()

        paint.color = Color.rgb(230, 230, 230) // blank/white reference
        canvas.drawRect(blankZone, paint)

        paint.color = Color.rgb(sampleGray, sampleGray, sampleGray)
        canvas.drawRect(sampleZone, paint)

        return bitmap
    }

    @Test
    fun unexposedStripSimilarToBlankYieldsZeroDoseAndSafeRisk() {
        val bitmap = cardBitmap(sampleGray = 225) // nearly identical to the blank reference
        val result = calculator.calculate(bitmap, blankZone, sampleZone, shiftDurationHours = 8.0)

        assertEquals(0.0, result.totalDosePpmHours, 0.01)
        assertEquals(H2SRiskLevel.SAFE, result.riskLevel)
        assertEquals(null, result.warningMessage)
    }

    @Test
    fun darkerSampleThanBlankProducesPositiveOpticalDensityAndDose() {
        val bitmap = cardBitmap(sampleGray = 60) // much darker than the blank reference
        val result = calculator.calculate(bitmap, blankZone, sampleZone, shiftDurationHours = 8.0)

        assertTrue("expected a positive optical density", result.opticalDensity > 0.0)
        assertTrue("expected a positive dose", result.totalDosePpmHours > 0.0)
    }

    @Test
    fun darkerSamplesProduceHigherRiskThanLighterSamples() {
        val lightResult = calculator.calculate(cardBitmap(sampleGray = 150), blankZone, sampleZone, shiftDurationHours = 1.0)
        val darkResult = calculator.calculate(cardBitmap(sampleGray = 20), blankZone, sampleZone, shiftDurationHours = 1.0)

        assertTrue(darkResult.totalDosePpmHours > lightResult.totalDosePpmHours)
        assertTrue(darkResult.riskLevel.ordinal >= lightResult.riskLevel.ordinal)
    }

    @Test
    fun shiftAveragePpmDividesTotalDoseByConfiguredShiftDuration() {
        val bitmap = cardBitmap(sampleGray = 40)
        val result4h = calculator.calculate(bitmap, blankZone, sampleZone, shiftDurationHours = 4.0)
        val result8h = calculator.calculate(bitmap, blankZone, sampleZone, shiftDurationHours = 8.0)

        // Same dose, but averaged over half the time -> roughly double the concentration.
        assertEquals(result4h.totalDosePpmHours, result8h.totalDosePpmHours, 0.01)
        assertTrue(result4h.shiftAveragePpm > result8h.shiftAveragePpm)
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroShiftDurationIsRejected() {
        calculator.calculate(cardBitmap(sampleGray = 100), blankZone, sampleZone, shiftDurationHours = 0.0)
    }
}
