package com.example.xposuredetectorsmart.repository

import com.example.xposuredetectorsmart.database.dao.ColorProfileDao
import com.example.xposuredetectorsmart.database.entities.ColorProfile
import com.example.xposuredetectorsmart.imageprocessing.ColorPatch
import com.example.xposuredetectorsmart.imageprocessing.CorrectionResult
import com.example.xposuredetectorsmart.imageprocessing.PatchType
import com.example.xposuredetectorsmart.utils.Constants
import javax.inject.Inject

class ColorProfileRepository @Inject constructor(
    private val colorProfileDao: ColorProfileDao,
) {
    suspend fun getHistory(deviceModel: String, workerId: String): List<ColorProfile> =
        colorProfileDao.getRecentProfiles(deviceModel, workerId, Constants.COLOR_PROFILE_HISTORY_SIZE)

    suspend fun recordCalibration(
        deviceModel: String,
        workerId: String,
        patches: List<ColorPatch>,
        correction: CorrectionResult,
        timestamp: Long,
    ) {
        val white = patches.find { it.type == PatchType.WHITE }?.avgColor ?: correction.referenceWhite
        val grey = patches.find { it.type == PatchType.GREY }?.avgColor

        val previousCount = colorProfileDao.getRecentProfiles(deviceModel, workerId, 1)
            .firstOrNull()?.calibrationCount ?: 0

        colorProfileDao.insert(
            ColorProfile(
                deviceModel = deviceModel,
                workerId = workerId,
                whiteR = white.r.toFloat(),
                whiteG = white.g.toFloat(),
                whiteB = white.b.toFloat(),
                greyR = (grey?.r ?: white.r).toFloat(),
                greyG = (grey?.g ?: white.g).toFloat(),
                greyB = (grey?.b ?: white.b).toFloat(),
                calibrationDate = timestamp,
                calibrationCount = previousCount + 1,
                meanSquareError = correction.meanSquareError,
                isActive = true,
            ),
        )
    }
}
