package com.example.xposuredetectorsmart.imageprocessing

import android.graphics.Rect
import com.example.xposuredetectorsmart.utils.RgbColor

enum class PatchType { WHITE, GREY, INK }

data class ColorPatch(
    val type: PatchType,
    val bounds: Rect,
    val avgColor: RgbColor,
    val saturation: Double,
)
