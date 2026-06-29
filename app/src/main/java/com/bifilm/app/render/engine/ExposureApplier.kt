package com.bifilm.app.render.engine

import com.bifilm.app.domain.model.BlendMode
import com.bifilm.app.domain.model.ExposureStops
import com.bifilm.app.domain.model.perLayerCompensation
import kotlin.math.pow

/**
 * 把曝光档 (-3..+3 EV, step 1/3) 转成 shader 用 gain (2^stops).
 * - 0 stops -> 1.0
 * - -1 stops -> 0.5
 * - +1 stops -> 2.0
 */
object ExposureApplier {

    fun apply(stops: Float): Float = 2f.pow(stops)

    /** 给定总张数与混合模式, 返回建议的每张曝光补偿 (stops). */
    fun suggested(layerCount: Int, mode: BlendMode): Float =
        perLayerCompensation(mode, layerCount).coerceIn(ExposureStops.MIN, ExposureStops.MAX)
}
