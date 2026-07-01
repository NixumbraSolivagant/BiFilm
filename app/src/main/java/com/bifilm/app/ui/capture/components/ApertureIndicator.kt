package com.bifilm.app.ui.capture.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * "取景指示条": 显示 "1/4" (第 1 张 / 共 4 张) + 当前 EV.
 * 极简, 浮在取景框顶部.
 */
@Composable
 fun ApertureIndicator(
    frameCount: Int,
    totalFrames: Int,
    exposureStops: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 三段进度点
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(totalFrames) { i ->
                    val filled = i < frameCount
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .size(if (filled) 7.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (filled)
                                    MaterialTheme.colorScheme.primary
                                else
                                    androidx.compose.ui.graphics.Color.White.copy(alpha = 0.3f)
                            )
                    )
                }
            }
            Text(
                text = "${frameCount.coerceAtLeast(0)}/${totalFrames}",
                style = MaterialTheme.typography.labelLarge,
                color = androidx.compose.ui.graphics.Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "·",
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f)
            )
            Text(
                text = formatExposureShort(exposureStops),
                style = MaterialTheme.typography.labelLarge,
                color = androidx.compose.ui.graphics.Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun formatExposureShort(v: Float): String {
    if (kotlin.math.abs(v) < 0.05f) return "0EV"
    val sign = if (v >= 0) "+" else "−"
    val abs = kotlin.math.abs(v)
    val whole = abs.toInt()
    val frac = ((abs - whole) * 3f).toInt()
    return if (frac == 0) "${sign}${whole}EV" else "${sign}${whole}⅓EV"
}