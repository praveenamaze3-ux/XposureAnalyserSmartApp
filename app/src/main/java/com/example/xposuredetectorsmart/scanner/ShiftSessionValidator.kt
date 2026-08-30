package com.example.xposuredetectorsmart.scanner

import com.example.xposuredetectorsmart.database.entities.WorkerContext
import javax.inject.Inject

/** Pure check for whether a shift session (started at wristband QR scan time) has passed its expiry. */
class ShiftSessionValidator @Inject constructor() {
    fun isExpired(context: WorkerContext, nowMillis: Long = System.currentTimeMillis()): Boolean =
        nowMillis >= context.shiftExpiresAt
}
