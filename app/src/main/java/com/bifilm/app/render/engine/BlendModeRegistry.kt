package com.bifilm.app.render.engine

import com.bifilm.app.domain.model.BlendMode

/**
 * BlendMode 枚举 <-> shader uniform 的统一入口.
 * 以后端 shader (AGSL / GLSL ES 兜底) 共享同一编码.
 */
object BlendModeRegistry {

    /** shader uMode uniform 值. */
    fun uniformValue(mode: BlendMode): Int = mode.uniformValue

    /** 把名称解析回 enum, 失败时回退到 SCREEN. */
    fun parse(name: String): BlendMode = BlendMode.fromName(name)

    fun all(): List<BlendMode> = BlendMode.entries
}
