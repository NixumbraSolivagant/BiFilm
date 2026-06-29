package com.bifilm.app.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.bifilm.app.domain.model.BlendMode

/**
 * 6 种混合模式横向选择条.
 * 单击切换, 长按弹tooltip（实现留 hooks, 当前仅显示模式名）.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun BlendModePicker(
    selected: BlendMode,
    onSelect: (BlendMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val modes = BlendMode.entries
    Row(modifier = modifier.fillMaxWidth()) {
        modes.forEach { mode ->
            BlendModeChip(
                mode = mode,
                isSelected = mode == selected,
                onClick = { onSelect(mode) },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun BlendModeChip(
    mode: BlendMode,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .combinedClickable(onClick = onClick, onLongClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = mode.name.first().toString(),
                style = MaterialTheme.typography.titleSmall,
                color = fg
            )
            Text(
                text = mode.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = fg
            )
        }
    }
}
