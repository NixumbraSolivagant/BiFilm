package com.bifilm.app.ui.capture.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bifilm.app.domain.model.ExposureStops
import kotlin.math.roundToInt

/**
 * 水平刻度: -3..+3 EV step 1/3 EV.
 *
 *  [stops] 浮点 (-3..3)
 *  [onChange] 新值回调
 *
 * 显示当前 EV 值 (e.g. -1.0, +0.3).
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

    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "EV",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 6.dp)
        )
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
            modifier = Modifier.weight(1f)
        )
        Box(modifier = Modifier.padding(horizontal = 6.dp)) {
            Text(
                text = formatEv(stops),
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}

private fun formatEv(v: Float): String {
    val sign = if (v >= 0) "+" else "-"
    return "$sign${kotlin.math.abs(v).formatOneDecimal()}"
}

private fun Float.formatOneDecimal(): String {
    val rounded = (this * 10f).roundToInt() / 10f
    return String.format("%.1f", rounded)
}