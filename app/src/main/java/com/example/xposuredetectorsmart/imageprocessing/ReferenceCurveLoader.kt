package com.example.xposuredetectorsmart.imageprocessing

import android.content.Context
import com.example.xposuredetectorsmart.utils.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads the od->ppm reference table (see [OdKnnRegressor]) from the bundled asset
 * [Constants.REFERENCE_CURVE_ASSET_PATH]. Regenerate that asset from real calibration data via
 * tools/reference_curve_calibration/export_reference_curve.py rather than editing it by hand.
 */
@Singleton
class ReferenceCurveLoader @Inject constructor(@ApplicationContext private val context: Context) {

    val referenceTable: List<OdReferencePoint> by lazy { load() }

    private fun load(): List<OdReferencePoint> {
        return try {
            val json = context.assets.open(Constants.REFERENCE_CURVE_ASSET_PATH).use { stream ->
                BufferedReader(InputStreamReader(stream)).readText()
            }
            val array = JSONArray(json)
            val points = ArrayList<OdReferencePoint>(array.length())
            for (i in 0 until array.length()) {
                val entry = array.getJSONObject(i)
                points.add(
                    OdReferencePoint(
                        opticalDensity = entry.getDouble("od"),
                        ppm = entry.getDouble("ppm"),
                        source = entry.optString("source", "unknown"),
                    ),
                )
            }
            require(points.isNotEmpty()) { "Reference curve asset contained no points." }
            points.sortedBy { it.opticalDensity }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load reference curve asset; falling back to built-in safety-net curve")
            FALLBACK_TABLE
        }
    }

    companion object {
        // Minimal built-in curve used only if the bundled asset is missing/corrupt, so a broken
        // build asset degrades accuracy rather than crashing every capture.
        private val FALLBACK_TABLE = listOf(
            OdReferencePoint(0.0, 0.0, "fallback"),
            OdReferencePoint(0.5, 12.3, "fallback"),
            OdReferencePoint(1.0, 24.6, "fallback"),
            OdReferencePoint(1.5, 40.1, "fallback"),
            OdReferencePoint(2.0, 58.8, "fallback"),
            OdReferencePoint(2.5, 80.8, "fallback"),
        )
    }
}
