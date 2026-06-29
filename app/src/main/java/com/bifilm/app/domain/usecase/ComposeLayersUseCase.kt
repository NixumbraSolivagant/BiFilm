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
    suspend operator fun invoke(projectId: String): Bitmap? {
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

        val renders = ArrayList<LayerRender>(layers.size)
        // Decode each layer, conform size to the first layer.
        for ((index, layer) in layers.withIndex()) {
            val bmp = if (index == 0) firstBmp else loadConformed(layer, width, height, firstBmp)
                ?: continue
            renders += LayerRender(
                bitmap = bmp,
                blendModeIndex = BlendMode.fromName(layer.blendMode).uniformValue,
                exposureGain = com.bifilm.app.render.engine.ExposureApplier.apply(layer.exposureStops),
                opacity = layer.opacity,
                mask = layer.maskPath?.let { p -> loadMask(p, width, height) }
            )
        }
        // 不再需要 firstBmp 单独保留, 因为它已经被并进 renders[0].
        val result = composer.composeLayers(width, height, renders)
        for (r in renders) {
            if (r.mask?.isRecycled == false) { /* masks reused next call */ }
            if (r.bitmap !== firstBmp && r.bitmap.isRecycled.not()) r.bitmap.recycle()
        }
        return result
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
