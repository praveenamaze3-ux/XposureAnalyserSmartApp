package com.example.xposuredetectorsmart

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.xposuredetectorsmart.imageprocessing.PatchSampler
import com.example.xposuredetectorsmart.imageprocessing.PatchType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * android.graphics.Bitmap requires a real Android runtime, so this is an instrumented test
 * rather than a host JVM unit test.
 */
@RunWith(AndroidJUnit4::class)
class PatchSamplerTest {

    private val sampler = PatchSampler()

    private fun syntheticStrip(): Bitmap {
        val bitmap = Bitmap.createBitmap(300, 100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK)
        val paint = Paint()

        paint.color = Color.rgb(240, 240, 240) // white patch
        canvas.drawRect(10f, 10f, 60f, 60f, paint)

        paint.color = Color.rgb(150, 150, 150) // grey patch
        canvas.drawRect(120f, 10f, 170f, 60f, paint)

        paint.color = Color.rgb(120, 20, 20) // ink patch (high saturation)
        canvas.drawRect(230f, 10f, 280f, 60f, paint)

        return bitmap
    }

    @Test
    fun samplesTheAverageColorAroundATappedPointInAUniformPatch() {
        val bitmap = syntheticStrip()
        val patch = sampler.sample(bitmap, PatchType.WHITE, Point(35, 35), radiusPx = 10)

        assertEquals(240.0, patch.avgColor.r, 0.5)
        assertEquals(240.0, patch.avgColor.g, 0.5)
        assertEquals(240.0, patch.avgColor.b, 0.5)
    }

    @Test
    fun highSaturationPatchReportsHighSaturation() {
        val bitmap = syntheticStrip()
        val patch = sampler.sample(bitmap, PatchType.INK, Point(255, 35), radiusPx = 10)

        assertTrue("ink patch should have high saturation", patch.saturation > 0.3)
    }

    @Test
    fun clampsTheSampleBoxToBitmapBoundsNearAnEdge() {
        val bitmap = syntheticStrip()
        val patch = sampler.sample(bitmap, PatchType.WHITE, Point(0, 0), radiusPx = 24)

        assertEquals(0, patch.bounds.left)
        assertEquals(0, patch.bounds.top)
        assertTrue(patch.bounds.right <= bitmap.width)
        assertTrue(patch.bounds.bottom <= bitmap.height)
    }
}
