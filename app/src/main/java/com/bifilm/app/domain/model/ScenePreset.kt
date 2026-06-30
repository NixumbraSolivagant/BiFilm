package com.bifilm.app.domain.model

/**
 * 场景预设: 把底层混合模式翻译成用户语言.
 *
 * UI 上看到的只有: 4-6 字标题 + 一行"会发生什么"白话 + 所属分组的徽章.
 * 底层用的还是权威公式 (Nikon Z7 / Canon 1DX III / Kodak datasheet).
 */
data class ScenePreset(
    val id: String,
    val title: String,        // 4-6 个汉字, 大按钮上的标题
    val what: String,         // 一行白话, "会发生什么", 不超过 18 字
    val group: SceneGroup,    // 分组标签, UI 上分组显示
    val mode: BlendMode,
    val suggestedExposureStops: Float = 0f,
    /** 选了这个预设, UI 在预览旁边给出的一句话提示. null 表示没有特殊提示. */
    val exposureNote: String? = null
)

/**
 * 场景分组. UI 在卡片上方的 5 大类标题用这个枚举.
 * 顺序: 日常 > 夜景 > 风格 > 黑白 > 特殊 —— 前面的用得多.
 */
enum class SceneGroup(val label: String) {
    DAILY("日常"),
    NIGHT("夜景"),
    ART("风格"),
    MONO("黑白"),
    SPECIAL("特殊")
}

/**
 * 13 个场景预设. 顺序 = 同组内 UI 从左到右 / 从上到下的展示顺序.
 * 拍得多的场景优先, 都是日常题材; 一键兜底也在日常组.
 */
object ScenePresets {

    // ── 日常 (拍得最多, 排前面) ──────────────────────────────────

    val SILHOUETTE_TEXTURE = ScenePreset(
        id = "silhouette_texture",
        title = "人像装背景",
        what = "把剪影装到第二张纹理里",
        group = SceneGroup.DAILY,
        mode = BlendMode.SCREEN,
        suggestedExposureStops = -0.5f,
        exposureNote = "剪影张欠曝一点, 第二张 −½ 档"
    )

    val BRIGHT_SKY_STACK = ScenePreset(
        id = "bright_sky_stack",
        title = "晴天叠云",
        what = "蓝天白云水面, 互不压",
        group = SceneGroup.DAILY,
        mode = BlendMode.SCREEN,
        suggestedExposureStops = -0.3f,
        exposureNote = "晴天每张 −⅓ 档"
    )

    val OVERCAST_PORTRAIT = ScenePreset(
        id = "overcast_portrait",
        title = "阴天人像",
        what = "灰平光下提一点反差",
        group = SceneGroup.DAILY,
        mode = BlendMode.LIGHTEN,
        suggestedExposureStops = -0.5f,
        exposureNote = "阴天每张 −½ 档"
    )

    val WATER_MOTION = ScenePreset(
        id = "water_motion",
        title = "流水雾化",
        what = "光量真叠加, 出流动感",
        group = SceneGroup.DAILY,
        mode = BlendMode.ADDITIVE,
        suggestedExposureStops = -1f,
        exposureNote = "每张 −1 档, 2 张起"
    )

    val FILM_AUTO = ScenePreset(
        id = "film_auto",
        title = "胶片一键",
        what = "不确定时选这个",
        group = SceneGroup.DAILY,
        mode = BlendMode.AVERAGE,
        suggestedExposureStops = 0f
    )

    // ── 夜景 / 星空 ─────────────────────────────────────────────

    val NEON_HALATION = ScenePreset(
        id = "neon_halation",
        title = "霓虹灯会",
        what = "亮像素盖暗像素, 光晕感",
        group = SceneGroup.NIGHT,
        mode = BlendMode.LIGHTEN,
        suggestedExposureStops = -1f,
        exposureNote = "夜景每张 −1 档"
    )

    val STAR_TRAILS = ScenePreset(
        id = "star_trails",
        title = "星轨月轨",
        what = "多张同天叠加, 自动平衡",
        group = SceneGroup.NIGHT,
        mode = BlendMode.AVERAGE,
        suggestedExposureStops = 0f,
        exposureNote = "3 张以上更稳"
    )

    val MOON_IN_DARK = ScenePreset(
        id = "moon_in_dark",
        title = "月亮融夜景",
        what = "暗背景把月亮嵌进去",
        group = SceneGroup.NIGHT,
        mode = BlendMode.LIGHTEN,
        suggestedExposureStops = 0f,
        exposureNote = "月亮正常曝, 夜景略欠曝"
    )

    val DARK_OBJECT_REVEAL = ScenePreset(
        id = "dark_object_reveal",
        title = "暗夜取亮",
        what = "暗夜里的剪影/月光",
        group = SceneGroup.NIGHT,
        mode = BlendMode.LIGHTEN,
        suggestedExposureStops = 0f
    )

    // ── 风格化 ──────────────────────────────────────────────────

    val COLORS_FROM_DARK = ScenePreset(
        id = "colors_from_dark",
        title = "暗底染彩",
        what = "黑底 + 彩色纹理, 出底片感",
        group = SceneGroup.ART,
        mode = BlendMode.SCREEN,
        suggestedExposureStops = 0f
    )

    val ART_GHOST = ScenePreset(
        id = "art_ghost",
        title = "文艺鬼影",
        what = "人虚 + 景实, 中调互染",
        group = SceneGroup.ART,
        mode = BlendMode.AVERAGE,
        suggestedExposureStops = 0f
    )

    // ── 黑白 ────────────────────────────────────────────────────

    val MONO_PUSH = ScenePreset(
        id = "mono_push",
        title = "黑白高反差",
        what = "两次曝光, 推 push 质感",
        group = SceneGroup.MONO,
        mode = BlendMode.SCREEN,
        suggestedExposureStops = -0.5f,
        exposureNote = "每张 −½ 档压肩部"
    )

    // ── 特殊 ────────────────────────────────────────────────────

    val BEHIND_GLASS = ScenePreset(
        id = "behind_glass",
        title = "隔玻璃拍",
        what = "去掉反光和高光",
        group = SceneGroup.SPECIAL,
        mode = BlendMode.DARKEN,
        suggestedExposureStops = 0f,
        exposureNote = "两张尽量对齐再拍"
    )

    /**
     * UI 展示顺序 (顺序 = 组之间从上到下, 组内从左到右).
     * 拍得多的排在前面 (日常), 进阶排最后 (特殊).
     */
    val ALL: List<ScenePreset> = listOf(
        SILHOUETTE_TEXTURE,    // 日常
        BRIGHT_SKY_STACK,
        OVERCAST_PORTRAIT,
        WATER_MOTION,
        FILM_AUTO,
        NEON_HALATION,         // 夜景
        STAR_TRAILS,
        MOON_IN_DARK,
        DARK_OBJECT_REVEAL,
        COLORS_FROM_DARK,      // 风格
        ART_GHOST,
        MONO_PUSH,             // 黑白
        BEHIND_GLASS           // 特殊
    )

    fun byId(id: String?): ScenePreset? = ALL.firstOrNull { it.id == id }

    /** 默认: 经典胶片多重曝光题材"剪影 + 纹理", 60% 摄影爱用. */
    fun default(): ScenePreset = SILHOUETTE_TEXTURE
}
