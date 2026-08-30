package com.example.xposuredetectorsmart.scanner

data class QRData(
    val industryId: String,
    val workerId: String,
)

class QRParseException(message: String) : Exception(message)
