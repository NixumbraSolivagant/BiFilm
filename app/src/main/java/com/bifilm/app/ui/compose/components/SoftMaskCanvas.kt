package com.bifilm.app.ui.compose.components

import android.graphics.Path
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path as ComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.bifilm.app.render.mask.MaskRenderer
import java.io.File

/**
 * 蒙版画布: 用户拖动产生 path, 实时画到 mask bitmap.
 *  - 笔刷硬度 / 半径由参数控制.
 *  - 路径在 ViewModel/Canvas 同步构建.
 *  - 完成后由 caller 把 mask bitmap 写盘.
 */
@Composable
fun SoftMaskCanvas(
    renderer: MaskRenderer,
    hardness: Int = 70,
    radius: Float = 36f,
    maskFile: File?,
    onMaskChanged: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentPath = remember { mutableStateOf(Path()) }
    val composePath = remember { mutableStateOf(ComposePath()) }
    var lastX by remember { mutableStateOf(0f) }
    var lastY by remember { mutableStateOf(0f) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        currentPath.value = Path().apply { moveTo(offset.x, offset.y) }
                        composePath.value = ComposePath().apply { moveTo(offset.x, offset.y) }
                        lastX = offset.x; lastY = offset.y
                        renderer.strokePoint(offset.x, offset.y, hardness, radius)
                        onMaskChanged()
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        currentPath.value.lineTo(change.position.x, change.position.y)
                        composePath.value.lineTo(change.position.x, change.position.y)
                        lastX = change.position.x
                        lastY = change.position.y
                        renderer.strokePoint(lastX, lastY, hardness, radius)
                        onMaskChanged()
                    },
                    onDragEnd = {
                        renderer.strokePath(currentPath.value, hardness, radius)
                        maskFile?.let { renderer.save(it) }
                        onMaskChanged()
                    },
                    onDragCancel = {
                        maskFile?.let { renderer.save(it) }
                        onMaskChanged()
                    }
                )
            }
    ) {
        drawPath(
            path = composePath.value,
            color = Color(0xFFC8A467).copy(alpha = 0.6f),
            style = Stroke(width = (radius.coerceAtMost(8f)).dp.toPx())
        )
    }
}