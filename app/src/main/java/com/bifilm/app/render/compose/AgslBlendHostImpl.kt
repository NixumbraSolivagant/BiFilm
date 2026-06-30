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
 * AGSL RuntimeShader 路径 (API 33+).
 *
 * 渲染流程 (两个固定 buffer 交替复用):
 *   ping <-- pong 交替作为读写目标, 不每帧分配.
 *
 *   for each layer:
 *     shader.setInputShader("uAccumulated", bitmapShader(readBuffer))
 *     shader.setInputShader("uIncoming",    bitmapShader(layer))
 *     shader.setInputShader("uMaskIncoming", mask or blank)
 *     shader.setXxxUniform(...)
 *     canvas(out = writeBuffer).drawRect(...)
 *     swap(readBuffer, writeBuffer)
 *
 *   return readBuffer (最后交换的结果)
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class AgslBlendHostImpl(private val context: Context) : BlendHost {

    private val shader: RuntimeShader
    private val outputPaint: Paint
    private val blankShader: BitmapShader
    private val softwareRenderingDisallowed: Boolean
    val isUsable: Boolean get() = !softwareRenderingDisallowed

    /** ping-pong pair, 按需懒分配. */
    private var ping: Bitmap? = null
    private var pong: Bitmap? = null
    private var lastWidth = 0
    private var lastHeight = 0

    private val shaderSource: String

    init {
        shaderSource = readShaderSource(context)
        shader = RuntimeShader(shaderSource)
        outputPaint = Paint().apply {
            isAntiAlias = false
            isFilterBitmap = true
        }
        val blank = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)
        blank.setPixel(0, 0, 0xFF)
        blankShader = BitmapShader(blank, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

        softwareRenderingDisallowed = runCatching {
            val probe = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            val c = Canvas(probe)
            val p = Paint().apply { shader = RuntimeShader(shaderSource) }
            c.drawRect(0f, 0f, 1f, 1f, p)
            probe.recycle()
            false
        }.getOrElse {
            Logger.w(TAG, "AGSL unavailable in software canvas: ${it.message}")
            true
        }
    }

    private fun readShaderSource(context: Context): String {
        val resId = context.resources.getIdentifier("blend_agsl", "raw", context.packageName)
        require(resId != 0) { "blend_agsl not found in res/raw" }
        return context.resources.openRawResource(resId).bufferedReader().use { it.readText() }
    }

    private fun ensureBuffers(width: Int, height: Int) {
        if (width == lastWidth && height == lastHeight) return
        ping?.recycle()
        pong?.recycle()
        ping = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        pong = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        lastWidth = width
        lastHeight = height
        Logger.d(TAG, "AGSL buffers allocated: ${width}x${height}")
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
        ensureBuffers(width, height)

        val p0 = ping!!
        val p1 = pong!!

        // Seed: 用 canvasBitmap 内容填充 ping, pong 透明.
        val total = width * height
        val acc = IntArray(total)
        canvasBitmap.getPixels(acc, 0, width, 0, 0, width, height)
        p0.setPixels(acc, 0, width, 0, 0, width, height)
        p1.eraseColor(0)

        // read = ping (seed), write = pong
        var read = p0
        var write = p1

        for ((index, layer) in layers.withIndex()) {
            val canvas = Canvas(write)
            val readShader = BitmapShader(read, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            val incomingShader = BitmapShader(layer.bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            val maskShader = layer.mask?.let {
                BitmapShader(it, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            } ?: blankShader

            shader.setInputShader("uAccumulated", readShader)
            shader.setInputShader("uIncoming", incomingShader)
            shader.setInputShader("uMaskIncoming", maskShader)
            shader.setIntUniform("uMode", layer.blendModeIndex)
            shader.setFloatUniform("uOpacity", layer.opacity.coerceIn(0f, 1f))
            shader.setFloatUniform("uExposureGain", layer.exposureGain.coerceIn(0f, 8f))
            shader.setFloatUniform("uSize", width.toFloat(), height.toFloat())

            outputPaint.shader = shader
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), outputPaint)
            outputPaint.shader = null

            // Swap: 下一次循环, write buffer 变成新的 read.
            val tmp = read
            read = write
            write = tmp

            Logger.d(
                TAG,
                "AGSL layer #$index mode=${layer.blendModeIndex} gain=${layer.exposureGain} opacity=${layer.opacity}"
            )
        }

        // 最后 read 持有结果 (经过了 layers.size 次 swap).
        return read
    }

    fun release() {
        ping?.recycle()
        pong?.recycle()
        ping = null
        pong = null
        lastWidth = 0
        lastHeight = 0
    }

    companion object {
        private const val TAG = "AgslBlendHostImpl"
    }
}
