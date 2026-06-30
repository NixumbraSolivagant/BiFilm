package com.bifilm.app.domain.usecase

import android.graphics.Bitmap
import com.bifilm.app.data.db.LayerDao
import com.bifilm.app.data.db.LayerEntity
import com.bifilm.app.data.image.ImageStore
import com.bifilm.app.domain.model.BlendMode
import com.bifilm.app.render.compose.LayerRender
import com.bifilm.app.render.engine.BlendComposer
import com.bifilm.app.render.engine.ExposureApplier
import com.bifilm.app.util.Logger

/**
 * 给定 projectId, 把它的全部 layers 按 order 顺序混成一张 ARGB_8888.
 *
 * 输入层位图来自文件系统 (ImageStore), 按 inSampleSize 限制最长边 1080px 防 OOM.
 * 蒙版是可选的 ALPHA_8 bitmap.
 */
class ComposeLayersUseCase(
    private val layerDao: LayerDao,
    private val composer: BlendComposer,
    private val maxLongEdgePx: Int = 1080
) {
    /**
     * 默认调用, 不切换灰度.
     */
    suspend operator fun invoke(projectId: String): Bitmap? = invoke(projectId, monochrome = false)

    /**
     * @param monochrome 输出时是否转为灰度 (Rec. 601: Y = 0.299R + 0.587G + 0.114B).
     */
    suspend operator fun invoke(projectId: String, monochrome: Boolean): Bitmap? {
        val layers = layerDao.listForProject(projectId)
        if (layers.isEmpty()) {
            Logger.d(TAG, "no layers for $projectId")
            return null
        }

        val firstFile = java.io.File(layers.first().sourcePath)
        val firstBmp = ImageStore.decodeRespectingOrientation(firstFile, maxLongEdgePx)
            ?: run {
                Logger.e(TAG, "first layer decode failed: ${firstFile.absolutePath}")
                return null
            }
        val width = firstBmp.width
        val height = firstBmp.height

        // AVERAGE 模式需要总层数 N 来做归一化 (Nikon Z7 官方规范).
        // 见 https://onlinemanual.nikonimglib.com/z7/en/09_menu_guide_03_28.html
        // "the gain for each exposure is divided by the total number of exposures
        //  (the gain for each exposure is set to 1/2 for 2 exposures, 1/3 for 3...)".
        // 这时"总层数"以数据库记录为准, 即使后续 decode 失败也算入 N.
        val totalLayers = layers.size

        val renders = ArrayList<LayerRender>(totalLayers)
        for ((index, layer) in layers.withIndex()) {
            val bmp = if (index == 0) firstBmp else loadConformed(layer, width, height, firstBmp)
                ?: continue
            val mode = BlendMode.fromName(layer.blendMode)
            val stopsGain = com.bifilm.app.render.engine.ExposureApplier.apply(layer.exposureStops)
            // AVERAGE 模式: gain /= N 实现"自动曝光补偿"避免爆掉 (Nikon Z7 规范).
            val normalizedGain = if (mode == BlendMode.AVERAGE) {
                stopsGain / totalLayers
            } else {
                stopsGain
            }
            renders += LayerRender(
                bitmap = bmp,
                blendModeIndex = mode.uniformValue,
                exposureGain = normalizedGain,
                opacity = layer.opacity,
                mask = layer.maskPath?.let { p -> loadMask(p, width, height) }
            )
        }
        if (renders.isEmpty()) {
            Logger.d(TAG, "no usable layers after decode for $projectId")
            firstBmp.recycle()
            return null
        }
        // 不再需要 firstBmp 单独保留, 因为它已经被并进 renders[0].
        val result = composer.composeLayers(width, height, renders)
        for (r in renders) {
            if (r.mask?.isRecycled == false) { /* masks reused next call */ }
            if (r.bitmap !== firstBmp && r.bitmap.isRecycled.not()) r.bitmap.recycle()
        }
        if (monochrome) {
            desaturateRec601InPlace(result)
        }
        return result
    }

    /**
     * Rec. 601 灰度化: Y = 0.299R + 0.587G + 0.114B. 直接读写原生 int buffer, 一遍循环搞定.
     */
    private fun desaturateRec601InPlace(bmp: Bitmap) {
        val w = bmp.width
        val h = bmp.height
        val total = w * h
        val arr = IntArray(total)
        bmp.getPixels(arr, 0, w, 0, 0, w, h)
        var i = 0
        while (i < total) {
            val px = arr[i]
            val a = (px ushr 24) and 0xFF
            val r = (px ushr 16) and 0xFF
            val g = (px ushr 8) and 0xFF
            val b = px and 0xFF
            val y = (0.299f * r + 0.587f * g + 0.114f * b).toInt().coerceIn(0, 255)
            arr[i] = (a shl 24) or (y shl 16) or (y shl 8) or y
            i++
        }
        bmp.setPixels(arr, 0, w, 0, 0, w, h)
    }

    private fun loadConformed(layer: LayerEntity, width: Int, height: Int, reference: Bitmap): Bitmap? {
        val src = ImageStore.decodeRespectingOrientation(java.io.File(layer.sourcePath), maxLongEdgePx)
            ?: return null
        if (src.width == width && src.height == height) return src
        val scaled = Bitmap.createScaledBitmap(src, width, height, true)
        if (scaled !== src) src.recycle()
        return scaled
    }

    private fun loadMask(path: String, width: Int, height: Int): Bitmap? {
        val file = java.io.File(path)
        if (!file.exists()) return null
        val opts = android.graphics.BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ALPHA_8
        }
        val bmp = android.graphics.BitmapFactory.decodeFile(file.absolutePath, opts) ?: return null
        if (bmp.width == width && bmp.height == height) return bmp
        val scaled = Bitmap.createScaledBitmap(bmp, width, height, true)
        if (scaled !== bmp) bmp.recycle()
        return scaled
    }

    companion object {
        private const val TAG = "ComposeLayersUseCase"
    }
}
