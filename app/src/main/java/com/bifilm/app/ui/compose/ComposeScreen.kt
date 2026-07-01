package com.bifilm.app.ui.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.AlertDialog
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bifilm.app.BiFilmApp
import com.bifilm.app.R
import com.bifilm.app.data.image.ImageStore
import com.bifilm.app.domain.model.SceneGroup
import com.bifilm.app.domain.model.ScenePreset
import com.bifilm.app.domain.model.ScenePresets
import com.bifilm.app.ui.common.FilmStrip
import com.bifilm.app.ui.common.SectionTitle
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

    var layerToDelete by remember { mutableStateOf<com.bifilm.app.data.db.LayerEntity?>(null) }
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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.title_compose),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${layers.size} 张待合成 · ${scene.title}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.requestRecompose() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "重新合成")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // ── 上方钉死区: 预览 + 场景备注 ───────────
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                PreviewBox(
                    output = output,
                    layers = layers,
                    isComposing = isComposing,
                    monochrome = monochrome
                )

                Spacer(Modifier.height(12.dp))

                SceneNote(scene)
            }

            // ── 下方可滚动面板 ─────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                shadowElevation = 8.dp,
                tonalElevation = 0.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    FilmStrip()
                    SceneSections(
                        selected = scene,
                        onSelect = { viewModel.setScene(it) }
                    )

                    ColorModeRow(monochrome) { viewModel.setMonochrome(it) }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionTitle("图层 (${layers.size})")
                        LayerStrip(
                            layers = layers,
                            onLongPress = { layerToDelete = it }
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    FilledTonalButton(
                        onClick = { onExport(projectId) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.action_export),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

// ── 上方钉死区 ─────────────────────────────────────

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
                elevation = 16.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false
            )
            .clip(RoundedCornerShape(24.dp))
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

        Badge(
            text = stringResource(R.string.badge_layers, layers.size),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
        )

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

@Composable
private fun Badge(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.Black.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "⧉",
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

@Composable
private fun SceneNote(scene: ScenePreset) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
            Text(
                text = scene.what,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            scene.exposureNote?.let { note ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.badge_tip),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                    Spacer(Modifier.width(8.dp))
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

@Composable
private fun GroupChip(group: SceneGroup) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    ) {
        Text(
            text = group.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
        )
    }
}

// ── 分组的场景卡 ───────────────────────────────────

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
        SectionTitle(stringResource(R.string.header_scene))
        grouped.forEach { (group, items) ->
            SceneGrid(
                items = items,
                selected = selected,
                onSelect = onSelect
            )
        }
    }
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

@Composable
private fun SceneCard(
    preset: ScenePreset,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val container = if (isSelected)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    else
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = container,
        border = if (isSelected)
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
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
                    .padding(horizontal = 14.dp, vertical = 14.dp)
                    .padding(start = if (isSelected) 8.dp else 0.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = preset.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) {
                        Text(
                            text = "✓",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = preset.what,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorModeRow(
    monochrome: Boolean,
    onChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(stringResource(R.string.header_color))
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
}

@Composable
private fun LayerStrip(
    layers: List<com.bifilm.app.data.db.LayerEntity>,
    onLongPress: (com.bifilm.app.data.db.LayerEntity) -> Unit
) {
    if (layers.isEmpty()) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            border = BorderStroke(
                1.dp, MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.hint_no_layers),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(20.dp)
            )
        }
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
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}