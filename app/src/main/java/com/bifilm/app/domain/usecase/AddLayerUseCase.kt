package com.bifilm.app.domain.usecase

import com.bifilm.app.data.db.LayerDao
import com.bifilm.app.data.db.LayerEntity
import com.bifilm.app.data.image.ImageStore
import com.bifilm.app.domain.model.BlendMode
import com.bifilm.app.util.Logger
import java.util.UUID

class AddLayerUseCase(
    private val layerDao: LayerDao,
    private val imageStore: ImageStore
) {
    suspend operator fun invoke(
        projectId: String,
        sourceUri: android.net.Uri
    ): String {
        val file = imageStore.copyUriToLayerFile(sourceUri, projectId)
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
        Logger.d("AddLayerUseCase", "added ${layer.id} order=$order")
        return layer.id
    }
}
