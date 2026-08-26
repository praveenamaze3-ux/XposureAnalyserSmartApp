package com.example.xposuredetectorsmart

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.xposuredetectorsmart.imageprocessing.ColorCorrector
import com.example.xposuredetectorsmart.imageprocessing.PatchType
import com.example.xposuredetectorsmart.imageprocessing.ReferenceDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.OpenCVLoader

/**
 * Runs against the real OpenCV native library, so this must be an instrumented test (device or
 * emulator) rather than a host JVM unit test - `Utils.bitmapToMat`/`Core.inRange` etc. are JNI
 * calls into libopencv_java5.so, which only loads on-device.
 */
@RunWith(AndroidJUnit4::class)
class ReferenceDetectorTest {

    private val detector = ReferenceDetector()

    companion object {
        @BeforeClass
        @JvmStatic
        fun loadOpenCv() {
            assertTrue("OpenCV native library failed to load", OpenCVLoader.initLocal())
        }
    }

    private fun syntheticStrip(): Bitmap {
        val bitmap = Bitmap.createBitmap(300, 100, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.BLACK) // background: V=0, matches none of the WHITE/GREY/INK ranges
        val paint = Paint()

        paint.color = Color.rgb(240, 240, 240) // white patch, V~240 S~0
        canvas.drawRect(10f, 10f, 60f, 60f, paint)

        paint.color = Color.rgb(150, 150, 150) // grey patch, V~150 S~0
        canvas.drawRect(120f, 10f, 170f, 60f, paint)

        paint.color = Color.rgb(120, 20, 20) // ink patch: high saturation, mid value
        canvas.drawRect(230f, 10f, 280f, 60f, paint)

        return bitmap
    }

    @Test
    fun detectsAllThreeReferencePatches() {
        val patches = detector.detect(syntheticStrip())

        val white = patches.find { it.type == PatchType.WHITE }
        val grey = patches.find { it.type == PatchType.GREY }
        val ink = patches.find { it.type == PatchType.INK }

        assertNotNull("expected a WHITE patch", white)
        assertNotNull("expected a GREY patch", grey)
        assertNotNull("expected an INK patch", ink)

        assertTrue("white patch should be brighter than grey", white!!.avgColor.r > grey!!.avgColor.r)
        assertTrue("ink patch should have high saturation", ink!!.saturation > 0.3)
    }

    @Test
    fun colorCorrectionProducesAWhiterWhitePatchOnACorrectedImage() {
        val bitmap = syntheticStrip()
        val patches = detector.detect(bitmap)
        val white = patches.find { it.type == PatchType.WHITE }
        assertNotNull(white)

        val corrector = ColorCorrector()
        val result = corrector.correct(bitmap, white, history = emptyList())

        val correctedPatches = detector.detect(result.correctedBitmap)
        val correctedWhite = correctedPatches.find { it.type == PatchType.WHITE }
        assertNotNull("white patch should still be detectable after correction", correctedWhite)
        assertTrue(
            "corrected white patch should be at least as bright as the original",
            correctedWhite!!.avgColor.r >= white!!.avgColor.r - 1.0,
        )
    }
}
