package com.bifilm.app.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
        ) {
            // ── 预览: 钉死在上方, 不可滚动 ─────────────────────────
            PreviewPanel(
                output = output,
                layers = layers,
                isComposing = isComposing,
                scene = scene,
                monochrome = monochrome
            )

            // ── 下方可滚动面板 (Surface 浮起来, 圆角, 跟预览有视觉分割) ─────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                shadowElevation = 2.dp,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SelectedSceneBar(scene)

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
                    LayerStrip(paths = layers.map { it.sourcePath })

                    Spacer(Modifier.height(2.dp))
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

// ── 预览面板 (钉死, 不滚动) ───────────────────────────────────────────

@Composable
private fun PreviewPanel(
    output: android.graphics.Bitmap?,
    layers: List<com.bifilm.app.data.db.LayerEntity>,
    isComposing: Boolean,
    scene: ScenePreset,
    monochrome: Boolean
) {
    // 选中场景时, 预览上方贴一个标签条 (跟着输出一起移动, 不滚动不消失).
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 预览框 (带阴影, 1:1 黑色底)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 5f)           // 接近一张 4:5 胶片
                .shadow(8.dp, RoundedCornerShape(20.dp), clip = false)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black)
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
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }

            // 右下角小徽章: 显示当前模式 (彩色 / 黑白)
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.55f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
            ) {
                Text(
                    text = if (monochrome) "黑白" else "彩色",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            // 左上角小徽章: 图层数
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.55f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = "${layers.size} 层",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun Placeholder(text: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

// ── 选中场景的提示条 ─────────────────────────────────────────────────

@Composable
private fun SelectedSceneBar(scene: ScenePreset) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = scene.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.width(8.dp))
                GroupChip(scene.group)
            }
            Text(
                text = scene.what,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
            )
            scene.exposureNote?.let { note ->
                Text(
                    text = "提示: $note",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f)
                )
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
@Composable
private fun SceneCard(
    preset: ScenePreset,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceContainerHigh

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
            ) else null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = preset.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
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
private fun LayerStrip(paths: List<String>) {
    if (paths.isEmpty()) {
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
        items(paths, key = { it }) { path -> LayerThumb(path) }
    }
}

@Composable
private fun LayerThumb(path: String) {
    val thumb by produceState<android.graphics.Bitmap?>(null, path) {
        value = withContext(Dispatchers.IO) {
            ImageStore.decodeRespectingOrientation(java.io.File(path), maxLongEdge = 200)
        }
    }
    val bm = thumb
    Card(
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.height(80.dp)
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
