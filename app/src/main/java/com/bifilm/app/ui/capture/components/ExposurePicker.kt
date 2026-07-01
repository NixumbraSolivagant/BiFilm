package com.bifilm.app.ui.capture.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bifilm.app.domain.model.ExposureStops
import kotlin.math.roundToInt

/**
 * EV 滑杆: 整合"当前值徽章 + 滑杆 + 两端刻度"三个区块,
 * 用一个紧凑布局完成, 不再拆 row.
 */
@Composable
fun ExposurePicker(
    stops: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val steps = ((ExposureStops.MAX - ExposureStops.MIN) / ExposureStops.STEP).toInt()
    val position = ((stops - ExposureStops.MIN) / ExposureStops.STEP).roundToInt()
        .coerceIn(0, steps)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "−3 EV",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 3.dp)
            ) {
                Text(
                    text = formatEv(stops),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "+3 EV",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = position.toFloat(),
            onValueChange = { v ->
                val idx = v.roundToInt().coerceIn(0, steps)
                val newStops = (ExposureStops.MIN + idx * ExposureStops.STEP).coerceIn(
                    ExposureStops.MIN, ExposureStops.MAX
                )
                onChange((newStops * 100f).roundToInt() / 100f)
            },
            valueRange = 0f..steps.toFloat(),
            steps = steps - 1,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun formatEv(v: Float): String {
    if (kotlin.math.abs(v) < 0.05f) return "0 EV"
    val sign = if (v >= 0) "+" else "−"
    val abs = kotlin.math.abs(v)
    val whole = abs.toInt()
    val frac = ((abs - whole) * 3f).toInt()
    return if (frac == 0) "$sign${whole} EV" else "$sign${whole}⅓ EV"
}