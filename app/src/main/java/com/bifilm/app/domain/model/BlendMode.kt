package com.bifilm.app.domain.model

/**
 * 6 种混合模式，覆盖胶片多重曝光常见工作流。
 * - SCREEN: 最贴近负片多次曝光（暗部透出）的默认模式
 * - ADDITIVE: 纯加法，需手调曝光补偿
 * - MULTIPLY: 暗部叠加（适合暗背景上的亮物）
 * - LIGHTEN: 取两者较亮像素
 * - DARKEN: 取两者较暗像素
 * - AVERAGE: 自动平衡曝光
 */
enum class BlendMode(val uniformValue: Int, val displayName: String) {
    SCREEN(0, "叠加"),
    ADDITIVE(1, "加法"),
    MULTIPLY(2, "正片叠底"),
    LIGHTEN(3, "变亮"),
    DARKEN(4, "变暗"),
    AVERAGE(5, "平均");

    companion object {
        fun fromName(name: String): BlendMode =
            entries.firstOrNull { it.name == name } ?: SCREEN
    }
}
