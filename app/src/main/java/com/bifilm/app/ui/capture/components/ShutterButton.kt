package com.bifilm.app.ui.capture.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * 相机快门按钮: 套圈 + 内圆, 按下时放大内圆模拟"按下感".
 *
 * - 套圈: 主色 (琥珀金)
 * - 内圆: 主色底, 白边
 * - 禁用: 半透明灰
 *
 * 套圈做成金色, 整按钮看起来更像"真相机快门".
 */
@Composable
fun ShutterButton(
    onShutter: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
) {
    var pressed by remember { mutableStateOf(false) }
    val ringColor = MaterialTheme.colorScheme.primary
    val innerColor = MaterialTheme.colorScheme.primary
    val edgeColor = MaterialTheme.colorScheme.onPrimary

    Canvas(
        modifier = modifier
            .size(80.dp)
            .pointerInput(isEnabled) {
                detectTapGestures(
                    onPress = {
                        if (isEnabled) {
                            pressed = true
                            try {
                                tryAwaitRelease()
                            } finally {
                                pressed = false
                            }
                            onShutter()
                        }
                    }
                )
            }
    ) {
        val radius = min(size.width, size.height) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        val ringAlpha = if (isEnabled) 1f else 0.35f
        val innerAlpha = if (isEnabled) 1f else 0.5f

        // 外圈
        drawCircle(
            color = ringColor.copy(alpha = ringAlpha),
            radius = radius,
            center = center,
            style = Stroke(width = 5.dp.toPx())
        )
        // 内圆
        drawCircle(
            color = innerColor.copy(alpha = innerAlpha),
            radius = radius * (if (pressed) 0.84f else 0.78f),
            center = center
        )
        // 内圆描边 (高对比, 像相机按钮白圈)
        drawCircle(
            color = edgeColor.copy(alpha = if (isEnabled) 0.9f else 0.4f),
            radius = radius * (if (pressed) 0.84f else 0.78f),
            center = center,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}