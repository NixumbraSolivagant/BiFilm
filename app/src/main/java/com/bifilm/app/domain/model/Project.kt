package com.bifilm.app.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class Project(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val frameCount: Int,
    val frameWidth: Int,
    val frameHeight: Int,
    val thumbnailPath: String? = null,
    val filmStockId: String = FilmStocks.FOMA_PAN_100.id,
    /**
     * 这一卷里的第几张 (0..38).
     * 0 = 还没按快门的空白画幅, 用于占位 / 预览.
     * 1..38 = 已曝光或准备曝光的第 N 张.
     */
    val frameIndexInRoll: Int = 1,
    /** 拍摄事件说明 (生日/聚会/旅行/随手记). null 表示不写. */
    val eventNote: String? = null
)

data class Layer(
    val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val order: Int,
    val sourcePath: String,
    val blendMode: BlendMode = BlendMode.SCREEN,
    val exposureStops: Float = 0f,
    val opacity: Float = 1f,
    val maskPath: String? = null,
    val maskHardness: Int = 100
)

/**
 * 把胶卷 + 张编号 + 事件翻译为用户看得懂的标题.
 *
 *   0 张: "Fomapan 100 · 空白画幅 · 2026-06-30"
 *   5 张: "Fomapan 100 · 第 5 张 · 周末公园散步 · 2026-06-30"
 */
fun projectDisplayTitle(
    stock: FilmStock,
    frameIndexInRoll: Int,
    eventNote: String?,
    createdAt: Long
): String {
    val head = when (frameIndexInRoll) {
        0 -> "${stock.displayName} · 空白画幅"
        else -> "${stock.displayName} · 第 $frameIndexInRoll 张"
    }
    val event = eventNote?.trim()?.takeIf { it.isNotEmpty() }
    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(createdAt))
    return if (event != null) "$head · $event · $date" else "$head · $date"
}
