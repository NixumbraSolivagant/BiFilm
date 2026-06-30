@file:JvmName("BlendModeSelfCheck")

package com.bifilm.app.domain.model

/**
 * 给 IDE 跑 (右键 -> Run) 的自检入口. 不依赖 Android / Robolectric / JUnit.
 *
 * 在 IntelliJ 里打开这个文件, 在 [main] 函数左侧点运行图标 (▶) 即可看到 PASS/FAIL 输出.
 *
 * 测试覆盖 6 个模式 (SCREEN / ADDITIVE / MULTIPLY / LIGHTEN / DARKEN / AVERAGE) 与
 * mask / opacity 行为, 像素值以 1e-3 容差与权威来源 (Nikon Z7 / Canon 1DX III / Adobe) 的
 * 公式对比.
 */
fun main() {
    val errors = BlendMode.selfCheck()
    if (errors.isEmpty()) {
        println("BlendMode.selfCheck: ALL ${countChecks()} CHECKS PASS (per Nikon Z7 / Canon 1DX III / Adobe references)")
    } else {
        System.err.println("BlendMode.selfCheck FAILED with ${errors.size} errors:")
        errors.forEach { System.err.println("  - $it") }
        kotlin.system.exitProcess(1)
    }
}

private fun countChecks(): Int = 13 // 当前 selfCheck 中的断言数量
