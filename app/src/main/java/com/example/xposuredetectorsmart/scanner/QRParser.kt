package com.example.xposuredetectorsmart.scanner

import com.example.xposuredetectorsmart.utils.Constants
import java.time.LocalDate
import java.time.format.DateTimeParseException
import javax.inject.Inject

/**
 * Parses the fixed QR payload format:
 * "h2s-dose:WRK_{ID}|{DATE}|{LOCATION}|{SHIFT_TYPE}"
 * e.g. "h2s-dose:WRK_4838|2026-08-25|LocationA|morning"
 */
class QRParser @Inject constructor() {

    fun parse(raw: String): QRData {
        val trimmed = raw.trim()
        if (!trimmed.startsWith(Constants.QR_PREFIX)) {
            throw QRParseException("QR payload missing '${Constants.QR_PREFIX}' prefix")
        }

        val payload = trimmed.removePrefix(Constants.QR_PREFIX)
        val parts = payload.split("|")
        if (parts.size != 4) {
            throw QRParseException("Expected 4 pipe-separated fields, found ${parts.size}")
        }

        val (rawWorkerId, rawDate, location, shift) = parts

        if (!rawWorkerId.startsWith(Constants.QR_WORKER_PREFIX)) {
            throw QRParseException("Worker id must start with '${Constants.QR_WORKER_PREFIX}'")
        }
        val workerId = rawWorkerId.removePrefix(Constants.QR_WORKER_PREFIX).trim()
        if (workerId.isEmpty()) {
            throw QRParseException("Worker id is empty")
        }

        val date = try {
            LocalDate.parse(rawDate.trim())
        } catch (e: DateTimeParseException) {
            throw QRParseException("Invalid date '$rawDate', expected ISO format YYYY-MM-DD")
        }

        if (location.isBlank()) {
            throw QRParseException("Location is empty")
        }
        if (shift.isBlank()) {
            throw QRParseException("Shift type is empty")
        }

        return QRData(
            workerId = rawWorkerId, // keep WRK_ prefix as the canonical worker id used across the app
            date = date,
            location = location.trim(),
            shift = shift.trim(),
        )
    }
}
