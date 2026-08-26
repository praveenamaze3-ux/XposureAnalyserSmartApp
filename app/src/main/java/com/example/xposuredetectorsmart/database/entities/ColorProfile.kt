package com.example.xposuredetectorsmart.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Per-device, per-worker calibration history used to seed adaptive color correction.
 * One row per scan (not one row per device+worker) so the corrector can average the
 * last N calibrations, per the adaptive correction spec.
 */
@Entity(
    tableName = "color_profiles",
    indices = [Index(value = ["deviceModel", "workerId"])],
)
data class ColorProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deviceModel: String,
    val workerId: String,
    val whiteR: Float,
    val whiteG: Float,
    val whiteB: Float,
    val greyR: Float,
    val greyG: Float,
    val greyB: Float,
    val calibrationDate: Long,
    val calibrationCount: Int,
    val meanSquareError: Double,
    val isActive: Boolean,
)
