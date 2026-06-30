package com.bifilm.app.ui.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bifilm.app.BiFilmApp
import com.bifilm.app.R
import com.bifilm.app.data.image.ImageStore
import com.bifilm.app.domain.model.SceneGroup
import com.bifilm.app.domain.model.ScenePreset
import com.bifilm.app.domain.model.ScenePresets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeScreen(
    projectId: String,
    onBack: () -> Unit,
    onExport: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val container = remember(context) {
        (context.applicationContext as BiFilmApp).container
    }
    val viewModel: ComposeViewModel = viewModel(
        factory = ComposeViewModel.Factory(container, projectId)
    )
    val layers by viewModel.layers.collectAsStateWithLifecycle()
    val output by viewModel.output.collectAsStateWithLifecycle()
    val isComposing by viewModel.isComposing.collectAsStateWithLifecycle()
    val scene by viewModel.scene.collectAsStateWithLifecycle()
    val monochrome by viewModel.monochrome.collectAsStateWithLifecycle()

    // 长按删除图层弹窗
    var layerToDelete by remember { androidx.compose.runtime.mutableStateOf<com.bifilm.app.data.db.LayerEntity?>(null) }
    if (layerToDelete != null) {
        AlertDialog(
            onDismissRequest = { layerToDelete = null },
            title = { Text(stringResource(R.string.alert_delete_title)) },
            text = { Text(stringResource(R.string.alert_delete_message)) },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        viewModel.remove(layerToDelete!!)
                        layerToDelete = null
                    }
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { layerToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // 用 size + 边界 path 作为合成触发条件. mode/stops 由 setScene 在 VM 内自己触发.
    val composeTrigger = remember(layers) {
        val first = layers.firstOrNull()?.sourcePath.orEmpty()
        val last = layers.lastOrNull()?.sourcePath.orEmpty()
        "${layers.size}|$first|$last"
    }
    LaunchedEffect(composeTrigger) {
        if (layers.isEmpty()) return@LaunchedEffect
        delay(200)
        viewModel.requestRecompose()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_compose)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.requestRecompose() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // ── 上方钉死区: 预览 + 选中场景备注 + 概要参数 ───────────
            PinHeader(
                output = output,
                layers = layers,
                isComposing = isComposing,
                scene = scene,
                monochrome = monochrome
            )

            // ── 下方可滚动面板 (圆角面板浮起来, 给预览留视觉边界) ─────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                shadowElevation = 4.dp,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 场景卡 (按 group 分组)
                    SceneSections(
                        selected = scene,
                        onSelect = { viewModel.setScene(it) }
                    )

                    // 画面切换 (彩色 / 黑白)
                    ColorModeRow(monochrome) { viewModel.setMonochrome(it) }

                    // 图层缩略图
                    SectionHeader(
                        title = stringResource(R.string.header_layers, layers.size)
                    )
                    LayerStrip(
                        layers = layers,
                        onLongPress = { layerToDelete = it }
                    )

                    Spacer(Modifier.height(4.dp))
                    FilledTonalButton(
                        onClick = { onExport(projectId) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(text = stringResource(R.string.action_export)) }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

// ── 上方钉死区: 预览 + 选中场景备注 ────────────────────────────────

@Composable
private fun PinHeader(
    output: android.graphics.Bitmap?,
    layers: List<com.bifilm.app.data.db.LayerEntity>,
    isComposing: Boolean,
    scene: ScenePreset,
    monochrome: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 预览框
        PreviewBox(
            output = output,
            layers = layers,
            isComposing = isComposing,
            monochrome = monochrome
        )

        // 备注条: 选中场景的解释 (固定不动, 永远显示当前选项)
        SceneNote(scene)
    }
}

@Composable
private fun PreviewBox(
    output: android.graphics.Bitmap?,
    layers: List<com.bifilm.app.data.db.LayerEntity>,
    isComposing: Boolean,
    monochrome: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 5f)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false
            )
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF0A0A0A))   // 深炭黑底, 比纯黑更柔和
    ) {
        val firstPath = layers.firstOrNull()?.sourcePath
        val fallback by produceState<android.graphics.Bitmap?>(null, firstPath) {
            if (firstPath == null) {
                value = null
            } else {
                value = withContext(Dispatchers.IO) {
                    ImageStore.decodeRespectingOrientation(
                        java.io.File(firstPath),
                        maxLongEdge = 1080
                    )
                }
            }
        }
        when {
            output != null -> Image(
                bitmap = output.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            fallback != null -> Image(
                bitmap = fallback!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
            else -> Placeholder(stringResource(R.string.hint_pick_layers))
        }
        if (isComposing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 3.dp
                )
            }
        }

        // 左上角徽章: 图层数 (深底白字, 玻璃感)
        Badge(
            text = stringResource(R.string.badge_layers, layers.size),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
        )

        // 右下角徽章: 彩色 / 黑白
        Badge(
            text = if (monochrome)
                stringResource(R.string.color_mono)
            else
                stringResource(R.string.color_color),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(14.dp)
        )
    }
}

/**
 * 玻璃感徽章 — 半透白字, 用于预览图角标.
 */
@Composable
private fun Badge(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.Black.copy(alpha = 0.45f),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun Placeholder(text: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 用一个简单的字形代替图标, 不需要 resource drawable
            Text(
                text = "⧉",   // 类似矩阵叠加的 unicode 符号
                style = MaterialTheme.typography.displayMedium,
                color = Color.White.copy(alpha = 0.3f)
            )
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.55f)
            )
        }
    }
}

// ── 选中场景的备注条 (钉死显示, 永远可见) ────────────────────────

@Composable
private fun SceneNote(scene: ScenePreset) {
    // 不再使用 primaryContainer 色块 (跟整体 surface 主题色抢戏).
    // 用更轻的 outlined 卡片 + 中性低饱和度, 跟下面的场景卡呼应.
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 第一行: group 小标 + 标题
            Row(verticalAlignment = Alignment.CenterVertically) {
                GroupChip(scene.group)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = scene.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            // 第二行: 白话解释
            Text(
                text = scene.what,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // 可选的曝光提示, 单独一行小字
            scene.exposureNote?.let { note ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.badge_tip),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** 小徽章, 标在 "当前场景" 旁边. */
@Composable
private fun GroupChip(group: SceneGroup) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    ) {
        Text(
            text = group.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

// ── 分组的场景卡 (按 SceneGroup 顺序, 组内按 ScenePresets.ALL 顺序) ──

@Composable
private fun SceneSections(
    selected: ScenePreset,
    onSelect: (ScenePreset) -> Unit
) {
    val grouped = remember(selected) {
        SceneGroup.values().map { group ->
            group to ScenePresets.ALL.filter { it.group == group }
        }.filter { it.second.isNotEmpty() }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(stringResource(R.string.header_scene))
        grouped.forEach { (group, items) ->
            SectionGroupTitle(group.label)
            SceneGrid(
                items = items,
                selected = selected,
                onSelect = onSelect
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun SectionGroupTitle(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun SceneGrid(
    items: List<ScenePreset>,
    selected: ScenePreset,
    onSelect: (ScenePreset) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { preset ->
                    SceneCard(
                        preset = preset,
                        isSelected = preset.id == selected.id,
                        onClick = { onSelect(preset) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
/**
 * 场景卡:
 * - 未选: surface + outlineVariant 描边, elevation 0, 文本 on-surface.
 * - 选中: 4dp 阴影 + 1.5dp 主色描边 + 左侧 3dp accent bar + 右上一个细 check mark.
 *
 * 不再用大块 primaryContainer 充底 (在浅色模式会很跳).
 */
@Composable
private fun SceneCard(
    preset: ScenePreset,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
    else
        MaterialTheme.colorScheme.surface

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 3.dp else 0.dp
        ),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(
                1.5.dp, MaterialTheme.colorScheme.primary
            ) else androidx.compose.foundation.BorderStroke(
                1.dp, MaterialTheme.colorScheme.outlineVariant
            ),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // 选中时左侧细条 accent
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(3.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .padding(start = if (isSelected) 6.dp else 0.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = preset.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    // 选中时画一个对勾 (纯文字 ✓), 不依赖图标资源
                    if (isSelected) {
                        Text(
                            text = "✓",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = preset.what,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected)
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// ── 彩色 / 黑白 切换 ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorModeRow(
    monochrome: Boolean,
    onChange: (Boolean) -> Unit
) {
    SectionHeader(stringResource(R.string.header_color))
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = !monochrome,
            onClick = { onChange(false) },
            shape = SegmentedButtonDefaults.itemShape(0, 2)
        ) { Text(stringResource(R.string.color_color)) }
        SegmentedButton(
            selected = monochrome,
            onClick = { onChange(true) },
            shape = SegmentedButtonDefaults.itemShape(1, 2)
        ) { Text(stringResource(R.string.color_mono)) }
    }
}

// ── 图层缩略图 ───────────────────────────────────────────────────────

@Composable
private fun LayerStrip(
    layers: List<com.bifilm.app.data.db.LayerEntity>,
    onLongPress: (com.bifilm.app.data.db.LayerEntity) -> Unit
) {
    if (layers.isEmpty()) {
        Text(
            text = stringResource(R.string.hint_no_layers),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(layers, key = { it.id }) { layer -> LayerThumb(layer, onLongPress) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LayerThumb(
    layer: com.bifilm.app.data.db.LayerEntity,
    onLongPress: (com.bifilm.app.data.db.LayerEntity) -> Unit
) {
    val thumb by produceState<android.graphics.Bitmap?>(null, layer.sourcePath) {
        value = withContext(Dispatchers.IO) {
            ImageStore.decodeRespectingOrientation(java.io.File(layer.sourcePath), maxLongEdge = 200)
        }
    }
    val bm = thumb
    Card(
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier
            .height(80.dp)
            .combinedClickable(
                onClick = { },
                onLongClick = { onLongPress(layer) }
            )
    ) {
        if (bm != null) {
            Image(
                bitmap = bm.asImageBitmap(),
                contentDescription = stringResource(R.string.cd_layer_thumb),
                modifier = Modifier
                    .height(80.dp)
                    .aspectRatio(1f),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .height(80.dp)
                    .aspectRatio(1f)
                    .background(Color.DarkGray)
            )
        }
    }
}
