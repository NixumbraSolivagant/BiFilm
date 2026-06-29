package com.bifilm.app.render.mask

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import com.bifilm.app.util.Logger
import java.io.File
import java.io.FileOutputStream

/**
 * 把用户的涂抹转换为 ALPHA_8 mask bitmap.
 *  - 在指定 (width, height) 上工作.
 *  - 多次涂抹使用 SRC_OVER 累加 alpha.
 *  - 提供 save() 写入文件 (PNG).
 */
class MaskRenderer(
    private val width: Int,
    private val height: Int,
    private val initial: Bitmap? = null
) {
    val bitmap: Bitmap = initial ?: Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8)
    private val canvas: Canvas = Canvas(bitmap)
    private val brush = SoftMaskBrush()

    fun setHardness(value: Int) {
        @Suppress("UNUSED_VARIABLE")
        val unused = brush  // 实际通过创建新 MaskRenderer 实例生效; 当前保持单笔刷.
        // 当前实现简化: 用单笔刷硬度. 如果需要动态切换, 这里重建 brush.
        // 留 hook: 在 M8 polish 时强化.
        // (此处未真正动态换 brush -- 由 Compose 侧根据 hardness 触发重建.)
        Logger.d(TAG, "hardness requested: $value (note: rebuild MaskRenderer for new hardness)")
    }

    fun strokePath(path: Path, hardness: Int = 70, radius: Float = 36f) {
        val tempBrush = SoftMaskBrush(hardness = hardness, radius = radius)
        tempBrush.strokePath(canvas, path)
    }

    fun strokePoint(x: Float, y: Float, hardness: Int = 70, radius: Float = 36f) {
        val tempBrush = SoftMaskBrush(hardness = hardness, radius = radius)
        tempBrush.stroke(canvas, x, y)
    }

    fun clear() {
        canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR)
    }

    fun save(file: File) {
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        Logger.d(TAG, "mask saved to ${file.absolutePath}")
    }

    companion object {
        private const val TAG = "MaskRenderer"
    }
}