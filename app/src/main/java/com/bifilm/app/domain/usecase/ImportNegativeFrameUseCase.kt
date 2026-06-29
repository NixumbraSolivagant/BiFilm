package com.bifilm.app.domain.usecase

import com.bifilm.app.data.db.LayerDao
import com.bifilm.app.data.db.LayerEntity
import com.bifilm.app.domain.model.BlendMode
import com.bifilm.app.util.Logger
import java.io.File
import java.util.UUID

/**
 * 把一张已导出结果当作新层反向加入当前项目.
 * 路径: 已有位图文件 (典型来源是 MediaStore Uri 复制后落地).
 */
class ImportNegativeFrameUseCase(
    private val layerDao: LayerDao
) {
    suspend operator fun invoke(projectId: String, sourcePath: String): String {
        val file = File(sourcePath)
        require(file.exists()) { "File not found: $sourcePath" }
        val order = (layerDao.listForProject(projectId).maxOfOrNull { it.order } ?: -1) + 1
        val layer = LayerEntity(
            id = UUID.randomUUID().toString(),
            projectId = projectId,
            order = order,
            sourcePath = file.absolutePath,
            blendMode = BlendMode.SCREEN.name,
            exposureStops = 0f,
            opacity = 1f,
            maskPath = null
        )
        layerDao.insert(layer)
        Logger.d(TAG, "imported negative ${layer.id} order=$order")
        return layer.id
    }

    companion object {
        private const val TAG = "ImportNegativeFrameUseCase"
    }
}
