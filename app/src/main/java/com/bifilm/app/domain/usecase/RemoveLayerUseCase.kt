package com.bifilm.app.domain.usecase

import com.bifilm.app.data.db.LayerDao

class RemoveLayerUseCase(private val layerDao: LayerDao) {
    suspend operator fun invoke(layerId: String) {
        layerDao.deleteById(layerId)
    }
}
