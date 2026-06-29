package com.bifilm.app.ui.capture.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
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
 * 自定义快门按钮: 圆形, 点击触发 shutter.
 * 按下时模拟白闪效果 (通过外部 state 管理, 这里只画圆环).
 */
@Composable
fun ShutterButton(
    onShutter: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true
) {
    var pressed by remember { mutableStateOf(false) }
    Canvas(
        modifier = modifier
            .size(78.dp)
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
        // 外圈
        drawCircle(
            color = if (isEnabled) Color.White else Color.LightGray.copy(alpha = 0.5f),
            radius = radius,
            center = center,
            style = Stroke(width = 5.dp.toPx())
        )
        // 内圆
        drawCircle(
            color = if (pressed) Color.White else Color.LightGray,
            radius = radius * 0.78f,
            center = center
        )
    }
}
