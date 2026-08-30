package com.example.xposuredetectorsmart.export

import android.content.Context
import android.graphics.Bitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

/** Saves a generated worker QR bitmap to disk so it can be shared/printed via the share sheet. */
class QrImageExporter @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun saveAsPngFile(bitmap: Bitmap, workerId: String): File {
        val qrDir = File(context.getExternalFilesDir(null), "qr_codes").apply { mkdirs() }
        val file = File(qrDir, "worker_qr_$workerId.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file
    }
}
