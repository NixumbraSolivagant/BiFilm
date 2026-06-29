package com.bifilm.app.domain.model

import kotlin.math.pow

/**
 * 曝光档位: -3..+3 EV, step 1/3
 */
@JvmInline
value class ExposureStops(val stops: Float) {

    val gain: Float get() = 2f.pow(stops)

    companion object {
        val ZERO = ExposureStops(0f)
        const val MIN = -3f
        const val MAX = 3f
        const val STEP = 1f / 3f
    }
}

/**
 * 给定总张数 n 与混合模式，算出每张建议曝光补偿。
 * ADDITIVE 累加亮度, 必须对每张欠曝；其它模式按需。
 */
fun perLayerCompensation(mode: BlendMode, layerCount: Int): Float {
    if (layerCount <= 1) return 0f
    return when (mode) {
        BlendMode.ADDITIVE -> -log2(layerCount.toFloat())
        BlendMode.AVERAGE -> 0f
        BlendMode.MULTIPLY -> -1f / layerCount
        BlendMode.SCREEN -> -0.15f * (layerCount - 1).toFloat()
        BlendMode.LIGHTEN -> -0.1f * (layerCount - 1).toFloat()
        BlendMode.DARKEN -> 0.1f * (layerCount - 1).toFloat()
    }
}

private fun log2(x: Float): Float = kotlin.math.ln(x) / kotlin.math.ln(2f)
