package com.bifilm.app.render.compose

import android.graphics.Bitmap
import android.graphics.Color
import com.bifilm.app.domain.model.BlendMode
import com.bifilm.app.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * 纯 CPU 兜底: 当 AGSL/GLES 都不可用时使用.
 *
 * 像素混合在 [Dispatchers.Default] 上执行, 按 CPU 核数均分扫描线并行处理.
 * 严格按权威来源 (Nikon Z7 / Canon 1DX III / Adobe / H. Lewis 1937) 实现 6 种混合模式.
 */
class SoftwareBlendHost : BlendHost {

    override suspend fun composite(
        canvasBitmap: Bitmap,
        layers: List<LayerRender>
    ): Bitmap = withContext(Dispatchers.Default) {
        if (layers.isEmpty()) return@withContext canvasBitmap
        if (canvasBitmap.isRecycled) {
            throw IllegalStateException("canvas bitmap is recycled")
        }

        val w = canvasBitmap.width
        val h = canvasBitmap.height
        val total = w * h

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val acc = IntArray(total)
        canvasBitmap.getPixels(acc, 0, w, 0, 0, w, h)

        // ── 第一层: 以 src + gain 作为 seed ───────────────────────────
        val first = layers.first()
        val firstSrc = readPixels(first.bitmap, w, h)
        val firstMask = first.mask?.let { readMask(it, w, h) }
        val gain0 = first.exposureGain.coerceIn(0f, 8f)
        val op0 = first.opacity.coerceIn(0f, 1f)
        parallelFor(0, total) { i ->
            val px = firstSrc[i]
            val fa = (px ushr 24) / 255f
            val fr = ((px ushr 16) and 0xFF) / 255f * gain0
            val fgG = ((px ushr 8) and 0xFF) / 255f * gain0
            val fb = (px and 0xFF) / 255f * gain0
            val m = firstMask?.get(i)?.let { (it.toInt() and 0xFF) / 255f } ?: 1f
            val alpha = (fa * op0 * m).coerceIn(0f, 1f)
            acc[i] = (
                ((alpha * 255).toInt().coerceIn(0, 255) shl 24) or
                    ((fr.coerceIn(0f, 1f) * 255).toInt().coerceIn(0, 255) shl 16) or
                    ((fgG.coerceIn(0f, 1f) * 255).toInt().coerceIn(0, 255) shl 8) or
                    (fb.coerceIn(0f, 1f) * 255).toInt().coerceIn(0, 255)
                )
        }
        Logger.d(TAG, "sw seed mode=${first.blendModeIndex} gain=$gain0 opacity=$op0")

        // ── 剩余层: 并行逐像素混合 ──────────────────────────────────
        for ((index, layer) in layers.drop(1).withIndex()) {
            val idx = index + 1
            val src = readPixels(layer.bitmap, w, h)
            val mask = layer.mask?.let { readMask(it, w, h) }
            val modeIndex = layer.blendModeIndex
            val gain = layer.exposureGain.coerceIn(0f, 8f)
            val opacity = layer.opacity.coerceIn(0f, 1f)

            parallelFor(0, total) { i ->
                val bg = acc[i]
                val ba = (bg ushr 24) / 255f
                val br = ((bg ushr 16) and 0xFF) / 255f
                val bgG = ((bg ushr 8) and 0xFF) / 255f
                val bb = (bg and 0xFF) / 255f

                val px = src[i]
                val fa = (px ushr 24) / 255f
                val fr = ((px ushr 16) and 0xFF) / 255f * gain
                val fgG = ((px ushr 8) and 0xFF) / 255f * gain
                val fb = (px and 0xFF) / 255f * gain
                val m = mask?.get(i)?.let { (it.toInt() and 0xFF) / 255f } ?: 1f

                // 内联 blendPixel: extension 展开, 避免函数引用类型问题.
                val mix = (m * opacity).coerceIn(0f, 1f)
                val inv = 1f - mix
                val (mr, mg, mb) = when (modeIndex) {
                    0 /* SCREEN */ -> Triple(
                        1f - (1f - br) * (1f - fr),
                        1f - (1f - bgG) * (1f - fgG),
                        1f - (1f - bb) * (1f - fb)
                    )
                    1, 5 /* ADDITIVE, AVERAGE */ -> Triple(
                        (br + fr).coerceIn(0f, 1f),
                        (bgG + fgG).coerceIn(0f, 1f),
                        (bb + fb).coerceIn(0f, 1f)
                    )
                    2 /* MULTIPLY */ -> Triple(
                        br * fr,
                        bgG * fgG,
                        bb * fb
                    )
                    3 /* LIGHTEN */ -> Triple(
                        maxOf(br, fr),
                        maxOf(bgG, fgG),
                        maxOf(bb, fb)
                    )
                    4 /* DARKEN */ -> Triple(
                        minOf(br, fr),
                        minOf(bgG, fgG),
                        minOf(bb, fb)
                    )
                    else -> Triple(br, bgG, bb)
                }
                val nr = (br * inv + mr * mix).coerceIn(0f, 1f)
                val ng = (bgG * inv + mg * mix).coerceIn(0f, 1f)
                val nb = (bb * inv + mb * mix).coerceIn(0f, 1f)
                val na = (ba + (fa - ba) * mix).coerceIn(0f, 1f)

                acc[i] = (
                    ((na * 255).toInt().coerceIn(0, 255) shl 24) or
                        ((nr * 255).toInt().coerceIn(0, 255) shl 16) or
                        ((ng * 255).toInt().coerceIn(0, 255) shl 8) or
                        (nb * 255).toInt().coerceIn(0, 255)
                    )
            }
            Logger.d(TAG, "sw layer #$idx mode=$modeIndex gain=$gain opacity=$opacity")
        }

        out.setPixels(acc, 0, w, 0, 0, w, h)
        out
    }

    private fun readPixels(bmp: Bitmap, w: Int, h: Int): IntArray {
        if (bmp.width == w && bmp.height == h) {
            val buf = IntArray(w * h)
            bmp.getPixels(buf, 0, w, 0, 0, w, h)
            return buf
        }
        val scaled = Bitmap.createScaledBitmap(bmp, w, h, true)
        val buf = IntArray(w * h)
        scaled.getPixels(buf, 0, w, 0, 0, w, h)
        if (scaled !== bmp) scaled.recycle()
        return buf
    }

    private fun readMask(mask: Bitmap, w: Int, h: Int): ByteArray? {
        if (mask.width == w && mask.height == h) {
            val buf = ByteArray(w * h)
            val tmp = IntArray(w * h)
            mask.getPixels(tmp, 0, w, 0, 0, w, h)
            for (i in buf.indices) buf[i] = (tmp[i] ushr 24).toByte()
            return buf
        }
        val scaled = Bitmap.createScaledBitmap(mask, w, h, true)
        val buf = ByteArray(w * h)
        val tmp = IntArray(w * h)
        scaled.getPixels(tmp, 0, w, 0, 0, w, h)
        for (i in buf.indices) buf[i] = (tmp[i] ushr 24).toByte()
        if (scaled !== mask) scaled.recycle()
        return buf
    }

    companion object {
        private const val TAG = "SoftwareBlendHost"

        /** 并发上限: 避免 CPU 争抢. N = min(可用核数, 4). */
        private val PARALLELISM = minOf(Runtime.getRuntime().availableProcessors(), 4)

        /**
         * 把 [from, until) 均分为 PARALLELISM 段, 各段并发执行.
         * 数据量小于阈值时直接顺序跑, 避免调度开销.
         */
        private suspend fun parallelFor(from: Int, until: Int, block: (Int) -> Unit) = coroutineScope {
            if (until - from < PARALLELISM * 64) {
                for (i in from until until) block(i)
                return@coroutineScope
            }
            val chunk = (until - from + PARALLELISM - 1) / PARALLELISM
            val jobs = (0 until PARALLELISM).map { tid ->
                async {
                    val start = from + tid * chunk
                    val end = minOf(start + chunk, until)
                    for (i in start until end) block(i)
                }
            }
            jobs.awaitAll()
        }
    }
}
