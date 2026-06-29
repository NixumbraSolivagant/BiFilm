package com.bifilm.app.render.mask

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import kotlin.math.min

/**
 * 软笔刷: 把 path 用高斯模糊的圆头画到 mask bitmap 上.
 *  - hardness: 0 (羽化极大) .. 100 (实心圆)
 *  - radius:  笔刷半径 px
 *  - strength: 0..1, 透明度 (用于软笔刷时不直接覆盖 alpha = 255)
 */
class SoftMaskBrush(
    private val hardness: Int = 70,
    private val radius: Float = 36f,
    private val strength: Float = 1f
) {
    /** 圆心画在 path 当前点. */
    fun stroke(canvas: android.graphics.Canvas, x: Float, y: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = (strength * 255).toInt().coerceIn(0, 255).shl(24) // ARGB alpha
            style = Paint.Style.FILL
            isFilterBitmap = true
        }
        // hardness 控制 BlurMaskFilter radius
        val sigma = radius * (1f - hardness / 100f)
        if (sigma > 1f) {
            paint.maskFilter = BlurMaskFilter(sigma, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawCircle(x, y, radius, paint)
    }

    /** 沿 path 拉一道笔刷 (粗略: 间隔 4px 取点画圆). */
    fun strokePath(canvas: android.graphics.Canvas, path: Path, step: Float = 4f) {
        val bounds = android.graphics.RectF()
        path.computeBounds(bounds, true)
        val width = bounds.width().coerceAtLeast(1f)
        val height = bounds.height().coerceAtLeast(1f)
        val total = (width + height).coerceAtLeast(1f)
        val samples = min(200, (total / step).toInt().coerceAtLeast(2))
        val measure = android.graphics.PathMeasure(path, false)
        val pos = FloatArray(2)
        for (i in 0..samples) {
            val t = i.toFloat() / samples
            measure.getPosTan(t * measure.length, pos, null)
            stroke(canvas, pos[0], pos[1])
        }
    }
}