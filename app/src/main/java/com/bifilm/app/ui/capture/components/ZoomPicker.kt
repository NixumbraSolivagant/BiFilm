package com.bifilm.app.ui.capture.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 焦段选择条: 固定毫米档的快捷 chip + 当前数值显示.
 *
 * 预设档 (mm): 16 / 24 / 35 / 45 / 50 / 85 / 135
 * 仅显示在相机支持范围内的档 (通过 mm → ratio 换算过滤).
 *
 * 有 35mm 等效焦段时显示 mm (e.g. "24mm / 50mm / 100mm"),
 * 无法获取时回落为倍数 (e.g. "1x / 2x / 3x").
 *
 * @param ratio            当前焦段倍数 (zoomRatio)
 * @param min              最小倍数
 * @param max              最大倍数
 * @param equivFocalAt1x   主摄 1x 等效焦段 (mm), null 表示无数据
 * @param onPick           选中的快捷档 (倍数)
 */
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

    // 预设档: 固定毫米数.
    // 注意: 这些是绝对焦段, 不是相对 1x 的倍数.
    // 倍数 = mm / baseFocal, 由相机 1x 等效决定.
    val mmPresets = listOf(16, 24, 35, 45, 50, 85, 135)

    // 把 mm 档换算为 zoomRatio，过滤掉超出 [min..max] 的档.
    val visiblePresets = mmPresets.map { mm -> mm to (mm.toFloat() / baseFocal) }
        .filter { (_, r) -> r in min..max }

    // 选中的判断: 取 ratio 最接近的档.
    val closestPreset = visiblePresets.minByOrNull { (_, r) -> abs(ratio - r) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        visiblePresets.forEach { (mm, presetRatio) ->
            val isSelected = closestPreset?.let { abs(ratio - it.second) < 0.05f } == true
                && abs(ratio - presetRatio) < 0.05f
            AssistChip(
                onClick = { onPick(presetRatio) },
                label = {
                    Text(
                        text = "${mm}mm",
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surface,
                    labelColor = if (isSelected)
                        MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface
                )
            )
        }

        // 右侧精确值
        Text(
            text = if (useMm) {
                "${(baseFocal * ratio).roundToInt()}mm"
            } else {
                "${formatRatio(ratio)}x"
            },
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(start = 4.dp)
                .width(72.dp),
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatRatio(r: Float): String {
    return when {
        abs(r - r.roundToInt()) < 0.02f -> r.roundToInt().toString()
        else -> String.format("%.1f", r)
    }
}
