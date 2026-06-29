package com.bifilm.app.render.engine

import android.graphics.Bitmap
import android.graphics.Color
import com.bifilm.app.render.compose.BlendHost
import com.bifilm.app.render.compose.LayerRender

/**
 * 累积合成引擎: 按顺序把每层混合进累积.
 *
 * 实现:
 *   1. 创建一张透明的 accumulator (ARGB_8888, 与目标尺寸一致).
 *   2. 调用 host.composite(accumulator, layers), 由 host 内部做 ping-pong.
 *   3. 把 host 返回的最终位图交给调用方.
 *
 * 调用方负责 recycle 自己持有的输入层位图与蒙版.
 */
class BlendComposer(private val host: BlendHost) {

    suspend fun composeLayers(
        width: Int,
        height: Int,
        layers: List<LayerRender>
    ): Bitmap {
        require(width > 0 && height > 0) { "width/height must be positive: ${width}x$height" }

        // 空的 ARGB_8888 (alpha = 0) 作为 seed.
        val seed = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        seed.eraseColor(Color.TRANSPARENT)
        val result = host.composite(seed, layers)
        if (result !== seed) seed.recycle()
        return result
    }
}