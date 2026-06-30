package com.bifilm.app.domain.model

/**
 * 6 种胶片多重曝光混合模式.
 *
 * 公式实现参考权威来源:
 *  - Nikon Z7 / Z8 / D850 官方手册 "Multiple exposure overlay mode"
 *    (https://onlinemanual.nikonimglib.com/z7/en/09_menu_guide_03_28.html)
 *    Add: "exposures are overlaid without modification; gain is not adjusted."
 *    Average: "the gain for each exposure is divided by the total number of
 *    exposures (1/2 for 2, 1/3 for 3, …)". 这是我们 AVERAGE 的归一化来源.
 *  - Canon EOS 1D X Mark III 官方支持文档 ART176405
 *    Additive: "exposure of each single image is added cumulatively";
 *    Bright/Dark: "compares pixels at the same position, and bright (or dark)
 *    portions are retained".
 *  - Adobe Photoshop 多重曝光教程: Screen 是 "mimic the accumulation of light
 *    on a single frame of film", 公式 `1 − (1−a)(1−b)`.
 *  - Howard Grill, "Additive Mode for Multiple Exposure": 手动按张数倒数减档
 *    (-1 stop / 2 张, -2 stops / 4 张) = 让 ADDITIVE 在视觉上等价于 AVERAGE.
 *  - H. Lewis (1937), "Additive Exposures in Process Photography", doi:10.5594/j12125
 *    光化学反应层面, 多次曝光的密度 D 各次曝光能量 (A+B+...) 的函数,
 *    即"光量累加 (clamp + 负片 H-D 曲线处理)". 这支持 SCREEN / ADDITIVE
 *    作为胶片物理等价的两个表达 (前者靠 toes curve, 后者靠线性).
 *
 * 每种模式的最后一步都是 `out = mix(a, mixed, mask * opacity)` (mask 默认 1,
 * opacity 默认 1.0). mask 和 opacity 都控制"这层在当前像素的替换强度".
 */
enum class BlendMode(
    val uniformValue: Int,
    val displayName: String,
    val displayDescription: String,
    val source: String
) {
    SCREEN(
        uniformValue = 0,
        displayName = "叠加",
        displayDescription = "Screen = 1 − (1−a)(1−b). 最贴近胶片负片多次曝光: " +
            "暗部被另一张图的暗部穿透, 亮部叠加. Photoshop 数码双重曝光的默认模式.",
        source = "Adobe 多重曝光教程 / Photoshop CAfe"
    ),
    ADDITIVE(
        uniformValue = 1,
        displayName = "加法",
        displayDescription = "Additive = a + b·gain, 自动 clamp [0,1]. 胶片相机的" +
            "光量累加 (N 张每张能量相加). 需要手动减档防爆, 默认每张减 −log₂(N) 档.",
        source = "Nikon Z7 手册 (Add) / Canon 1DX III (Additive)"
    ),
    MULTIPLY(
        uniformValue = 2,
        displayName = "正片叠底",
        displayDescription = "Multiply = a × (b·gain). 去白留暗, 适合叠加胶片纹理、" +
            "漏光、暗背景上的暗物. 与 SCREEN 互为反义.",
        source = "Adobe Photoshop Guide"
    ),
    LIGHTEN(
        uniformValue = 3,
        displayName = "变亮",
        displayDescription = "Lighten = max(a, b·gain) (每像素每通道独立). 适合" +
            "亮物叠在暗底 (月亮、灯光、流星), 在相机的 Bright 模式下.",
        source = "Canon 1DX III / Nikon Lighten"
    ),
    DARKEN(
        uniformValue = 4,
        displayName = "变暗",
        displayDescription = "Darken = min(a, b·gain) (每像素每通道独立). 适合" +
            "去除高光与反射 (隔着玻璃拍摄); 胶片纹理叠加时反向使用.",
        source = "Canon 1DX III (Dark) / Nikon Darken"
    ),
    AVERAGE(
        uniformValue = 5,
        displayName = "平均",
        displayDescription = "Average: 内部走 ADDITIVE 路径 (a + b · gain), 但 gain" +
            "已被上游归一化为 2^stops / N (N 为总层数). 等价于胶片光量累加 + 自动减档," +
            "不需要用户手动调曝光. Nikon Z7 / Canon AVG 模式的官方定义.",
        source = "Nikon Z7 手册 (Average) / Canon 1DX III"
    );

    /**
     * 公式端点: 在 AGSL / GLES / CPU 三个 host 上调用, 必须返回完全一致的结果.
     * 输入是 0..1 浮点的 RGB 三通道, mask 0..1, opacity 0..1.
     */
    fun blendPixel(
        ar: Float, ag: Float, ab: Float,
        br: Float, bg: Float, bb: Float,
        maskAlpha: Float,
        opacity: Float
    ): Triple<Float, Float, Float> {
        val m = (maskAlpha * opacity).coerceIn(0f, 1f)
        val inv = 1f - m
        val (mr, mg, mb) = when (this) {
            SCREEN -> floatArrayOf(
                1f - (1f - ar) * (1f - br),
                1f - (1f - ag) * (1f - bg),
                1f - (1f - ab) * (1f - bb)
            ).let { Triple(it[0], it[1], it[2]) }
            ADDITIVE, AVERAGE -> {
                val nr = (ar + br).coerceIn(0f, 1f)
                val ng = (ag + bg).coerceIn(0f, 1f)
                val nb = (ab + bb).coerceIn(0f, 1f)
                Triple(nr, ng, nb)
            }
            MULTIPLY -> Triple(ar * br, ag * bg, ab * bb)
            LIGHTEN -> Triple(maxOf(ar, br), maxOf(ag, bg), maxOf(ab, bb))
            DARKEN -> Triple(minOf(ar, br), minOf(ag, bg), minOf(ab, bb))
        }
        val nr = ar * inv + mr * m
        val ng = ag * inv + mg * m
        val nb = ab * inv + mb * m
        return Triple(nr, ng, nb)
    }

    companion object {
        fun fromName(name: String): BlendMode =
            entries.firstOrNull { it.name == name } ?: SCREEN

        /**
         * 自检: 在 IDE 里以 main() 跑一下, 确认 6 个模式都按权威公式给出预期像素值.
         */
        @JvmStatic
        fun selfCheck(): List<String> {
            val errors = mutableListOf<String>()
            fun assertClose(name: String, actual: Float, expected: Float, tol: Float = 1e-3f) {
                if (kotlin.math.abs(actual - expected) > tol) {
                    errors += "$name: expected $expected, got $actual"
                }
            }

            val op = 1.0f
            val m = 1.0f

            run {
                val (r, g, b) = SCREEN.blendPixel(1f, 0f, 0f, 0f, 1f, 0f, m, op)
                assertClose("SCREEN R", r, 1f)
                assertClose("SCREEN G", g, 1f)
                assertClose("SCREEN B", b, 0f)
            }
            run {
                val (r, _, _) = SCREEN.blendPixel(0f, 0f, 0f, 0.4f, 0.4f, 0.4f, m, op)
                assertClose("SCREEN B+a -> a", r, 0.4f)
            }
            run {
                val (r, _, _) = ADDITIVE.blendPixel(0.3f, 0.3f, 0.3f, 0.5f, 0.5f, 0.5f, m, op)
                assertClose("ADDITIVE", r, 0.55f)
            }
            run {
                val (r, _, _) = ADDITIVE.blendPixel(0.8f, 0.8f, 0.8f, 0.5f, 0.5f, 0.5f, m, op)
                assertClose("ADDITIVE clamp", r, 1f)
            }
            run {
                val (r, _, _) = MULTIPLY.blendPixel(0.5f, 0.5f, 0.5f, 0.4f, 0.4f, 0.4f, m, op)
                assertClose("MULTIPLY", r, 0.2f)
            }
            run {
                val (r, _, _) = LIGHTEN.blendPixel(0.3f, 0.3f, 0.3f, 0.7f, 0.7f, 0.7f, m, op)
                assertClose("LIGHTEN", r, 0.7f)
            }
            run {
                val (r, _, _) = DARKEN.blendPixel(0.3f, 0.3f, 0.3f, 0.7f, 0.7f, 0.7f, m, op)
                assertClose("DARKEN", r, 0.3f)
            }
            run {
                val (r, _, _) = AVERAGE.blendPixel(0.3f, 0.3f, 0.3f, 0.5f, 0.5f, 0.5f, m, op)
                assertClose("AVERAGE == ADDITIVE", r, 0.55f)
            }
            run {
                val (r, _, _) = SCREEN.blendPixel(0f, 0f, 0f, 1f, 1f, 1f, 0.5f, 1.0f)
                assertClose("SCREEN mask=0.5", r, 0.5f)
            }
            run {
                val (r, _, _) = SCREEN.blendPixel(0.2f, 0.2f, 0.2f, 1f, 1f, 1f, 0f, 1f)
                assertClose("SCREEN mask=0", r, 0.2f)
            }
            run {
                val (r, _, _) = SCREEN.blendPixel(0f, 0f, 0f, 1f, 1f, 1f, 1f, 0.5f)
                assertClose("SCREEN opacity=0.5", r, 0.5f)
            }

            return errors
        }
    }
}
