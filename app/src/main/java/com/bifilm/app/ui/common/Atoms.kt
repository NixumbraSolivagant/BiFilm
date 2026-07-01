package com.bifilm.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 一个统一的"设置卡": 圆角, 浅描边, 内边距 16dp.
 * 替代各页直接用 Card 的写法, 保证视觉一致.
 */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

/** 段标题: 小字 + 字距, 用于卡内分组标题. */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(bottom = 8.dp)
    )
}

/** 大段标题: 标签字体 + semi-bold + 字距. 用于屏幕内块标题. */
@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
    )
}

/**
 * Pill 风格 chip: 圆角 50, 三态.
 * - accent = 实心金底 (强调主选项)
 * - selected = 浅金底 + 金字 (选中但允许并列)
 * - default = 浅 surface 底 (未选)
 */
@Composable
fun PillChip(
    text: String,
    selected: Boolean = false,
    accent: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
) {
    val bg = when {
        accent -> MaterialTheme.colorScheme.primary
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val fg = when {
        accent -> MaterialTheme.colorScheme.onPrimary
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val border = if (selected || accent) null else BorderStroke(
        1.dp, MaterialTheme.colorScheme.outlineVariant
    )

    val shape = RoundedCornerShape(50)
    val clickableMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Surface(
        modifier = modifier
            .height(32.dp)
            .clip(shape)
            .then(clickableMod),
        shape = shape,
        color = bg,
        border = border
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            if (leading != null) {
                Icon(
                    imageVector = leading,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = fg
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = fg,
                fontWeight = if (selected || accent) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

/**
 * 关键参数值徽章: 琥珀金背景 + 黑字, 醒目.
 * 用于 EV / ISO / 张数 等"是数值"的关键参数.
 */
@Composable
fun ValueBadge(
    text: String,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.primary,
    foreground: Color = MaterialTheme.colorScheme.onPrimary
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = background
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = foreground,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/** 顶部"胶卷条"装饰 — 金线穿小点, 暗示胶片孔. */
@Composable
fun FilmStrip(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .height(2.dp)
                .width(20.dp)
                .background(color)
        )
        Box(
            modifier = Modifier
                .size(4.dp)
                .background(color)
        )
        Box(
            modifier = Modifier
                .height(2.dp)
                .width(20.dp)
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .size(4.dp)
                .background(color.copy(alpha = 0.4f))
        )
        Spacer(Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .size(4.dp)
                .background(color.copy(alpha = 0.4f))
        )
    }
}

/** 简单分隔线, 用 outlineVariant 配色. */
@Composable
fun Hairline(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outlineVariant
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color)
    )
}

/** 小图标 + 文字, 用作副标题或属性行. */
@Composable
fun CaptionIconLabel(
    text: String,
    icon: ImageVector = Icons.Filled.CameraAlt,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = tint
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = tint
        )
    }
}