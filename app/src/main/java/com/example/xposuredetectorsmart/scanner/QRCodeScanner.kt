package com.example.xposuredetectorsmart.scanner

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import timber.log.Timber
import javax.inject.Inject

/** Thin wrapper around ML Kit's barcode scanner, tuned to QR codes only. */
class QRCodeScanner @Inject constructor() {

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()

    private val scanner = BarcodeScanning.getClient(options)

    /**
     * Analyzes a CameraX frame and invokes [onResult] with the first decoded QR string, if any.
     * Always closes [imageProxy] when done, regardless of success/failure.
     */
    @OptIn(ExperimentalGetImage::class)
    fun scanFrame(imageProxy: ImageProxy, onResult: (String?) -> Unit) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            onResult(null)
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val value = barcodes.firstOrNull { it.rawValue != null }?.rawValue
                onResult(value)
            }
            .addOnFailureListener { e ->
                Timber.w(e, "QR scan failed")
                onResult(null)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    fun close() {
        scanner.close()
    }
}
