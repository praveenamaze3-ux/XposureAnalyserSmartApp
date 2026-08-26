package com.example.xposuredetectorsmart.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

object BitmapUtils {

    fun rotate(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun resizeMaxDimension(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val largestSide = max(bitmap.width, bitmap.height)
        if (largestSide <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / largestSide
        val newWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val newHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun max(a: Int, b: Int) = if (a > b) a else b

    /** Crops a centered square region covering [fraction] of the shorter side. */
    fun centerCrop(bitmap: Bitmap, fraction: Float): Bitmap {
        val side = (minOf(bitmap.width, bitmap.height) * fraction).toInt().coerceAtLeast(1)
        val left = ((bitmap.width - side) / 2).coerceAtLeast(0)
        val top = ((bitmap.height - side) / 2).coerceAtLeast(0)
        val safeSide = minOf(side, bitmap.width - left, bitmap.height - top)
        return Bitmap.createBitmap(bitmap, left, top, safeSide, safeSide)
    }

    /** SHA-256 digest of the JPEG-encoded bitmap bytes, used as a tamper-evident image fingerprint. */
    fun sha256Hash(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        val digest = MessageDigest.getInstance("SHA-256").digest(stream.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
