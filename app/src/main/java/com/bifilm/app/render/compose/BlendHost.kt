package com.bifilm.app.render.compose

import android.graphics.Bitmap

/**
 * 渲染 Host 接口：API 33+ 走 AGSL RuntimeShader，
 * API 26~32 走 OpenGL ES 兜底（见 M6）。
 */
interface BlendHost {
    suspend fun composite(
        canvasBitmap: Bitmap,
        layers: List<LayerRender>
    ): Bitmap
}

data class LayerRender(
    val bitmap: Bitmap,
    val blendModeIndex: Int,
    val exposureGain: Float,
    val opacity: Float,
    val mask: Bitmap? = null
)
