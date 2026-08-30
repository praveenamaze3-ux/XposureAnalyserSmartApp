package com.example.xposuredetectorsmart.scanner

import com.example.xposuredetectorsmart.utils.Constants
import javax.inject.Inject

/**
 * Parses the permanent wristband QR payload format: "h2s-worker:{industryId}:{workerId}".
 * This is an opaque reference only - no shift/date/location is baked in, so the same QR is
 * printed once at registration and reused for the worker's entire tenure at that industry.
 * e.g. "h2s-worker:acme_chemicals:5f3e8b2a1c9d4e6f"
 */
class QRParser @Inject constructor() {

    fun parse(raw: String): QRData {
        val trimmed = raw.trim()
        if (!trimmed.startsWith(Constants.QR_WORKER_PREFIX_V2)) {
            throw QRParseException("QR payload missing '${Constants.QR_WORKER_PREFIX_V2}' prefix")
        }

        val payload = trimmed.removePrefix(Constants.QR_WORKER_PREFIX_V2)
        val parts = payload.split(":")
        if (parts.size != 2) {
            throw QRParseException("Expected 2 colon-separated fields, found ${parts.size}")
        }

        val (industryId, workerId) = parts
        if (industryId.isBlank()) {
            throw QRParseException("Industry id is empty")
        }
        if (workerId.isBlank()) {
            throw QRParseException("Worker id is empty")
        }

        return QRData(industryId = industryId.trim(), workerId = workerId.trim())
    }
}
