package com.bifilm.app.ui.capture.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * "光圈指示器": 显示当前帧数 + 当前层的曝光档做参考.
 * 极简实现 — 摄影app里是非常小的视觉提示.
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
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "f/$frameCount",
                style = MaterialTheme.typography.labelLarge
            )
            Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(50)).padding(2.dp).size(4.dp)) {
                Text("", color = Color.Transparent, modifier = Modifier.size(4.dp))
            }
            Text(
                text = "${if (exposureStops >= 0) "+" else ""}${"%.1f".format(exposureStops)} EV",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
            Text(
                text = " / $totalFrames",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
