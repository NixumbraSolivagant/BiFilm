package com.bifilm.app.domain.model

/**
 * 胶卷 (film stock / 母题)——每个胶卷 = 一种"底片品牌".
 * 用户在新建项目时选定一个胶卷, 再选一张编号 (0..38).
 * 38 张是 135 胶卷的标准容量. 编号 0 表示还没按快门.
 */
data class FilmStock(
    val id: String,
    val displayName: String,
    val brand: String,
    val iso: Int,
    val type: FilmType
)

enum class FilmType(val label: String) {
    /** 黑白负片 */
    BW_NEGATIVE("黑白负片"),
    /** 彩色负片 */
    COLOR_NEGATIVE("彩色负片"),
    /** 电影卷 (彩色正片工艺) */
    CINEMA("电影卷")
}

/**
 * 内置胶卷目录. UI 直接拿 ALL 列出.
 */
object FilmStocks {
    val FOMA_PAN_100 = FilmStock(
        id = "foma_pan_100",
        displayName = "Fomapan 100",
        brand = "Foma",
        iso = 100,
        type = FilmType.BW_NEGATIVE
    )
    val FOMA_PAN_400 = FilmStock(
        id = "foma_pan_400",
        displayName = "Fomapan 400",
        brand = "Foma",
        iso = 400,
        type = FilmType.BW_NEGATIVE
    )
    val CINESTILL_5219 = FilmStock(
        id = "cinestill_5219",
        displayName = "CineStill 5219 (电影卷)",
        brand = "CineStill",
        iso = 500,
        type = FilmType.CINEMA
    )

    val ALL: List<FilmStock> = listOf(
        FOMA_PAN_100,
        FOMA_PAN_400,
        CINESTILL_5219
    )

    fun byId(id: String?): FilmStock = ALL.firstOrNull { it.id == id } ?: FOMA_PAN_100

    /** 胶卷张数硬上限 (135 一卷的物理容量). */
    const val ROLL_CAPACITY = 38

    /** 合法张编号范围: 0..ROLL_CAPACITY. 0 表示空白画幅 (未按快门). */
    val FRAME_RANGE: IntRange = 0..ROLL_CAPACITY
}
