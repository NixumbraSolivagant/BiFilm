package com.bifilm.app.render.compose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import com.bifilm.app.util.Logger

/**
 * AGSL RuntimeShader 路径 (API 33+, 即 Android 13+).
 *
 * 渲染流程 (ping-pong):
 *   front <-- 当前累积可见状态 (喂给 uAccumulated)
 *   back  <-- 临时目标 (写 back 后再做 swap)
 *
 *   for each layer:
 *     shader.setInputShader("uAccumulated", bitmapShader(front))
 *     shader.setInputShader("uIncoming",    bitmapShader(layer))
 *     shader.setInputShader("uMaskIncoming", mask or blank)
 *     shader.setXxxUniform(...)
 *     canvas(out = back).drawRect(0,0,w,h,paint_with_shader)
 *     swap(front, back)
 *
 *   return front (the final accumulator)
 *
 * 这样避免 AGSL 在同一 SkSurface 上的 read-after-write 未定义行为.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class AgslBlendHostImpl(context: Context) : BlendHost {

    private val shader: RuntimeShader
    private val outputPaint: Paint
    private val blankShader: BitmapShader

    init {
        val source = readShaderSource(context)
        shader = RuntimeShader(source)
        outputPaint = Paint().apply {
            isAntiAlias = false
            isFilterBitmap = true
        }
        val blank = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
        blank.setPixel(0, 0, 0xFF)
        blankShader = BitmapShader(blank, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }

    private fun readShaderSource(context: Context): String {
        val resId = context.resources.getIdentifier("blend_agsl", "raw", context.packageName)
        require(resId != 0) { "blend_agsl not found in res/raw" }
        return context.resources.openRawResource(resId).bufferedReader().use { it.readText() }
    }

    override suspend fun composite(
        canvasBitmap: Bitmap,
        layers: List<LayerRender>
    ): Bitmap {
        if (layers.isEmpty()) return canvasBitmap
        if (canvasBitmap.isRecycled) {
            throw IllegalStateException("canvas bitmap is recycled")
        }

        val width = canvasBitmap.width
        val height = canvasBitmap.height

        // Seed front with the caller-provided canvas content (typically transparent).
        var front: Bitmap = canvasBitmap.copy(Bitmap.Config.ARGB_8888, true)

        for ((index, layer) in layers.withIndex()) {
            val back: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(back)
            val frontShader = BitmapShader(front, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            val incomingShader = BitmapShader(layer.bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            val maskShader = layer.mask?.let {
                BitmapShader(it, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            } ?: blankShader

            shader.setInputShader("uAccumulated", frontShader)
            shader.setInputShader("uIncoming", incomingShader)
            shader.setInputShader("uMaskIncoming", maskShader)
            shader.setIntUniform("uMode", layer.blendModeIndex)
            shader.setFloatUniform("uOpacity", layer.opacity.coerceIn(0f, 1f))
            shader.setFloatUniform("uExposureGain", layer.exposureGain.coerceIn(0f, 8f))
            shader.setFloatUniform("uSize", width.toFloat(), height.toFloat())

            outputPaint.shader = shader
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), outputPaint)
            outputPaint.shader = null

            // Cycle: previous front is now obsolete.
            if (index > 0 || front !== canvasBitmap) front.recycle()
            front = back
            Logger.d(
                TAG,
                "composited layer #$index mode=${layer.blendModeIndex} gain=${layer.exposureGain} opacity=${layer.opacity}"
            )
        }

        return front
    }

    companion object {
        private const val TAG = "AgslBlendHostImpl"
    }
}