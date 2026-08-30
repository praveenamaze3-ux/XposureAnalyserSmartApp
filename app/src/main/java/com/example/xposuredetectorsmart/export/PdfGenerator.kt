package com.example.xposuredetectorsmart.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.xposuredetectorsmart.database.entities.DoseLog
import com.example.xposuredetectorsmart.imageprocessing.H2SRiskLevel
import com.example.xposuredetectorsmart.utils.Constants
import com.example.xposuredetectorsmart.utils.DoseAggregation
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * Generates an OSHA-style shift exposure report as a PDF using Android's built-in PdfDocument
 * API. (iText was intentionally not used: iText 7+ is AGPL-licensed, which would require either
 * open-sourcing this app or a commercial license - PdfDocument avoids that entirely.)
 */
class PdfGenerator @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val pageWidth = 595 // A4 at 72dpi
    private val pageHeight = 842

    /** [shiftDurationHours] is the worker's actual elapsed work time (for risk classification), defaulting to the app's configured shift length when the real value isn't available at the call site. */
    fun generate(
        workerId: String,
        department: String,
        shiftDate: String,
        location: String,
        logs: List<DoseLog>,
        shiftDurationHours: Double = Constants.DEFAULT_SHIFT_DURATION_HOURS.toDouble(),
    ): File {
        val document = PdfDocument()
        val titlePaint = Paint().apply { textSize = 20f; isFakeBoldText = true; color = Color.BLACK }
        val headerPaint = Paint().apply { textSize = 12f; isFakeBoldText = true; color = Color.DKGRAY }
        val bodyPaint = Paint().apply { textSize = 11f; color = Color.BLACK }
        val footerPaint = Paint().apply { textSize = 9f; color = Color.GRAY }
        val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }

        val rows = ReportFormatter.toRows(logs, shiftDurationHours)
        val cumulative = DoseAggregation.cumulativeDose(logs)
        val overallStatus = ReportFormatter.statusFor(cumulative, shiftDurationHours)

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = 40f

        canvas.drawText("H2S Dose Exposure Report", 40f, y, titlePaint)
        y += 28f
        canvas.drawText("Worker: $workerId    Department: $department", 40f, y, bodyPaint)
        y += 18f
        canvas.drawText("Shift date: $shiftDate    Location: $location", 40f, y, bodyPaint)
        y += 18f
        canvas.drawText(
            "Cumulative exposure: %.2f ppm·hr    Shift avg: %.2f ppm    Status: %s".format(
                cumulative,
                cumulative / shiftDurationHours,
                overallStatus.name,
            ),
            40f,
            y,
            bodyPaint,
        )
        y += 24f
        canvas.drawLine(40f, y, pageWidth - 40f, y, linePaint)
        y += 20f

        if (logs.isNotEmpty()) {
            val chartHeight = 160f
            drawCumulativeChart(canvas, logs.sortedBy { it.timestamp }, top = y, height = chartHeight, shiftDurationHours = shiftDurationHours)
            y += chartHeight + 24f
        }

        canvas.drawText("Timestamp", 40f, y, headerPaint)
        canvas.drawText("PPM", 220f, y, headerPaint)
        canvas.drawText("Confidence", 320f, y, headerPaint)
        canvas.drawText("Status", 440f, y, headerPaint)
        y += 8f
        canvas.drawLine(40f, y, pageWidth - 40f, y, linePaint)
        y += 16f

        for (row in rows) {
            if (y > pageHeight - 80f) {
                drawFooter(canvas, footerPaint, pageNumber)
                document.finishPage(page)
                pageNumber += 1
                page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas
                y = 40f
            }

            canvas.drawText(row.timestamp, 40f, y, bodyPaint)
            canvas.drawText("%.2f".format(row.dosePpm), 220f, y, bodyPaint)
            canvas.drawText("%.0f%%".format(row.confidence * 100), 320f, y, bodyPaint)
            canvas.drawText(row.status.name, 440f, y, bodyPaint.apply { color = riskColor(row.status) })
            bodyPaint.color = Color.BLACK
            y += 18f
        }

        y += 20f
        canvas.drawLine(40f, y, pageWidth - 40f, y, linePaint)
        y += 20f
        canvas.drawText(
            "Reference (shift-average ppm): Moderate >= ${Constants.RISK_MODERATE_MIN_PPM}, High >= ${Constants.RISK_HIGH_MIN_PPM}, " +
                "Dangerous >= ${Constants.RISK_DANGEROUS_MIN_PPM}.",
            40f,
            y,
            footerPaint,
        )
        y += 14f
        canvas.drawText(
            "This report is generated from field colorimetric strip readings and is not a substitute for calibrated instrumentation.",
            40f,
            y,
            footerPaint,
        )

        drawFooter(canvas, footerPaint, pageNumber)
        document.finishPage(page)

        val reportsDir = File(context.getExternalFilesDir(null), "reports").apply { mkdirs() }
        val outputFile = File(reportsDir, "H2S_Report_${workerId}_$shiftDate.pdf")
        FileOutputStream(outputFile).use { document.writeTo(it) }
        document.close()

        return outputFile
    }

    private fun riskColor(risk: H2SRiskLevel): Int = Color.parseColor(risk.colorHex)

    private fun drawFooter(canvas: android.graphics.Canvas, paint: Paint, pageNumber: Int) {
        canvas.drawText("Page $pageNumber", (pageWidth - 80).toFloat(), (pageHeight - 20).toFloat(), paint)
    }

    /** Self-contained cumulative-exposure line chart with Moderate/High/Dangerous reference lines. */
    private fun drawCumulativeChart(
        canvas: android.graphics.Canvas,
        sortedLogs: List<DoseLog>,
        top: Float,
        height: Float,
        shiftDurationHours: Double,
    ) {
        val left = 40f
        val right = pageWidth - 40f
        val bottom = top + height

        val cumulativeSeries = DoseAggregation.runningCumulative(sortedLogs)
        val dangerousDoseLine = Constants.RISK_DANGEROUS_MIN_PPM * shiftDurationHours
        val maxValue = (cumulativeSeries.maxOrNull() ?: 0.0).coerceAtLeast(dangerousDoseLine * 1.1)

        val axisPaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }
        val linePaint = Paint().apply { color = Color.rgb(30, 100, 200); strokeWidth = 2.5f; isAntiAlias = true }
        val refPaint = Paint().apply { color = Color.rgb(220, 100, 0); strokeWidth = 1f; style = Paint.Style.STROKE }
        val labelPaint = Paint().apply { color = Color.GRAY; textSize = 8f }

        canvas.drawLine(left, top, left, bottom, axisPaint)
        canvas.drawLine(left, bottom, right, bottom, axisPaint)

        fun yFor(value: Double): Float = (bottom - (value / maxValue * height)).toFloat()

        // Reference lines are expressed as the cumulative dose (ppm·hr) that puts the shift
        // average right at each risk threshold, given this worker's configured shift length.
        val highY = yFor(Constants.RISK_HIGH_MIN_PPM * shiftDurationHours)
        canvas.drawLine(left, highY, right, highY, refPaint)
        canvas.drawText("High (${Constants.RISK_HIGH_MIN_PPM} ppm avg)", left + 4, highY - 3, labelPaint)

        val dangerousY = yFor(dangerousDoseLine)
        canvas.drawLine(left, dangerousY, right, dangerousY, refPaint)
        canvas.drawText("Dangerous (${Constants.RISK_DANGEROUS_MIN_PPM} ppm avg)", left + 4, dangerousY - 3, labelPaint)

        if (cumulativeSeries.size < 2) return

        val stepX = (right - left) / (cumulativeSeries.size - 1)
        var prevX = left
        var prevY = yFor(cumulativeSeries[0])
        for (i in 1 until cumulativeSeries.size) {
            val x = left + stepX * i
            val yPos = yFor(cumulativeSeries[i])
            canvas.drawLine(prevX, prevY, x, yPos, linePaint)
            prevX = x
            prevY = yPos
        }
    }
}
