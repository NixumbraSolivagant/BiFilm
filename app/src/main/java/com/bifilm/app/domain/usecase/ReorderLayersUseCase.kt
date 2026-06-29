package com.bifilm.app.domain.usecase

import com.bifilm.app.data.db.LayerDao

/**
 * 拖动重排: 把 fromOrder 移到 toOrder 处, 其他 layer 整体平移.
 * 数据量小 (<=9), 性能不是瓶颈.
 */
class ReorderLayersUseCase(private val layerDao: LayerDao) {
    suspend operator fun invoke(projectId: String, fromIndex: Int, toIndex: Int) {
        val list = layerDao.listForProject(projectId).toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices) return
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        list.forEachIndexed { idx, layer ->
            if (layer.order != idx) layerDao.update(layer.copy(order = idx))
        }
    }
}