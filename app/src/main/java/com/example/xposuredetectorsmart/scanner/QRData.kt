package com.example.xposuredetectorsmart.scanner

import java.time.LocalDate

data class QRData(
    val workerId: String,
    val date: LocalDate,
    val location: String,
    val shift: String,
)

class QRParseException(message: String) : Exception(message)
