package com.bifilm.app.ui.capture.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bifilm.app.ui.common.PillChip
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 焦段选择器: 自动 wrap 的 PillChip 行.
 *
 * 预设档 (mm): 16 / 24 / 35 / 45 / 50 / 85 / 135
 * 仅显示在相机支持范围内的档 (通过 mm → ratio 换算过滤).
 *
 * @param ratio            当前焦段倍数 (zoomRatio)
 * @param min              最小倍数
 * @param max              最大倍数
 * @param equivFocalAt1x   主摄 1x 等效焦段 (mm), null 表示无数据
 * @param onPick           选中的快捷档 (倍数)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ZoomPicker(
    ratio: Float,
    min: Float,
    max: Float,
    equivFocalAt1x: Float?,
    onPick: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val useMm = equivFocalAt1x != null && equivFocalAt1x > 0f
    val baseFocal = equivFocalAt1x ?: 50f

    val mmPresets = listOf(16, 24, 35, 45, 50, 85, 135)

    val visiblePresets = mmPresets.map { mm -> mm to (mm.toFloat() / baseFocal) }
        .filter { (_, r) -> r in min..max }

    val closestPreset = visiblePresets.minByOrNull { (_, r) -> abs(ratio - r) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "焦段",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (useMm) "${(baseFocal * ratio).roundToInt()}mm"
                else "${formatRatio(ratio)}×",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            visiblePresets.forEach { (mm, presetRatio) ->
                val isSelected = closestPreset?.let { abs(ratio - it.second) < 0.05f } == true
                    && abs(ratio - presetRatio) < 0.05f
                PillChip(
                    text = "${mm}mm",
                    selected = isSelected,
                    onClick = { onPick(presetRatio) }
                )
            }
        }
    }
}

private fun formatRatio(r: Float): String {
    return when {
        abs(r - r.roundToInt()) < 0.02f -> r.roundToInt().toString()
        else -> String.format("%.1f", r)
    }
}