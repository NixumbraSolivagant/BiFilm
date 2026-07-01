package com.bifilm.app.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bifilm.app.BiFilmApp
import com.bifilm.app.R
import com.bifilm.app.data.db.ProjectEntity
import com.bifilm.app.domain.model.FilmStock
import com.bifilm.app.domain.model.FilmStocks
import com.bifilm.app.domain.model.projectDisplayTitle
import com.bifilm.app.ui.common.EmptyState
import com.bifilm.app.ui.common.FilmStrip
import com.bifilm.app.ui.common.Hairline
import com.bifilm.app.ui.common.SectionTitle
import com.bifilm.app.ui.common.ValueBadge
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ════════════════════════════════════════════════════════════════════════════
// Home screen
// ════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenCapture: (String) -> Unit,
    onOpenCompose: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val container = remember(context) {
        (context.applicationContext as BiFilmApp).container
    }
    val navigateCompose = remember(onOpenCompose) { { id: String -> onOpenCompose(id) } }
    val navigateCapture = remember(onOpenCapture) { { id: String -> onOpenCapture(id) } }
    val coroutineScope = rememberCoroutineScope()

    var showCreateSheet by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(container, navigateCompose)
    )
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val pendingCreate by viewModel.pendingCreate.collectAsStateWithLifecycle()

    var pendingDelete by remember { mutableStateOf<ProjectEntity?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "胶卷",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Projects · ${cards.size} 卷",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.requestCreate()
                    showCreateSheet = true
                },
                icon = {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                text = { Text("新建项目", fontWeight = FontWeight.SemiBold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 10.dp
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (cards.isEmpty()) {
                EmptyState(
                    message = "还没有胶卷. 点右下角新建第一个项目.",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                HomeList(
                    cards = cards,
                    onCardClick = { viewModel.touch(it.project.id); onOpenCompose(it.project.id) },
                    onCaptureClick = { viewModel.touch(it.project.id); onOpenCapture(it.project.id) },
                    onDeleteClick = { pendingDelete = it.project }
                )
            }
        }
    }

    if (showCreateSheet && pendingCreate != null) {
        ModalBottomSheet(
            onDismissRequest = { showCreateSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            CreateProjectSheet(
                model = pendingCreate!!,
                onChange = viewModel::updatePending,
                onConfirm = {
                    showCreateSheet = false
                    coroutineScope.launch { sheetState.hide() }
                    viewModel.confirmPending { id -> navigateCapture(id) }
                },
                onCancel = {
                    showCreateSheet = false
                    viewModel.cancelPending()
                    coroutineScope.launch { sheetState.hide() }
                }
            )
        }
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除这个项目?") },
            text = {
                Text(
                    text = "「${projectDisplayTitle(FilmStocks.byId(target.filmStockId), target.frameIndexInRoll, target.eventNote, target.createdAt)}」及其全部图层将被删除.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(target)
                    pendingDelete = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════
// 列表
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun HomeList(
    cards: List<HomeCardItem>,
    onCardClick: (HomeCardItem) -> Unit,
    onCaptureClick: (HomeCardItem) -> Unit,
    onDeleteClick: (HomeCardItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp, top = 8.dp, bottom = 112.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("已冲印")
                FilmStrip(modifier = Modifier.padding(start = 2.dp))
            }
            Spacer(Modifier.height(4.dp))
        }
        items(cards, key = { it.project.id }) { card ->
            ProjectCard(
                card = card,
                onClick = { onCardClick(card) },
                onCaptureClick = { onCaptureClick(card) },
                onDeleteClick = { onDeleteClick(card) }
            )
        }
    }
}

@Composable
private fun ProjectCard(
    card: HomeCardItem,
    onClick: () -> Unit,
    onCaptureClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val project = card.project
    val stock = card.stock

    val dateText = remember(project.updatedAt) {
        val df = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        df.format(Date(project.updatedAt))
    }
    val eventText = project.eventNote?.trim()?.takeIf { it.isNotEmpty() }
    val isBlank = project.frameIndexInRoll == 0
    val layers = card.layerCount.coerceAtLeast(0)
    val isDone = layers >= project.frameCount

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 左: 缩略图 (带胶片感光晕)
            FrameThumbnail(project = project, stock = stock)

            Spacer(Modifier.width(14.dp))

            // 右: 信息
            Column(modifier = Modifier.weight(1f)) {
                // 第一行: 张号 + 状态徽章 (空白画幅 / 已成片 / 拍摄中)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isBlank) "空白画幅" else "No.${project.frameIndexInRoll}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(8.dp))
                    when {
                        isBlank -> StatusBadge("未拍", muted = true)
                        isDone -> StatusBadge("已满", accent = true)
                        else -> StatusBadge("已拍 $layers/${project.frameCount}")
                    }
                }

                Spacer(Modifier.height(6.dp))

                // 第二行: 胶卷名 (亮一些, 强调)
                Text(
                    text = stock.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )

                // 第三行: 胶卷规格
                Text(
                    text = "${stock.brand} · ISO ${stock.iso} · ${stock.type.label}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 第四行: 事件 (可选)
                if (eventText != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = eventText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2
                    )
                }

                Spacer(Modifier.height(10.dp))
                Hairline(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(10.dp))

                // 第五行: 日期 + 操作图标
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CaptionText(dateText, icon = Icons.Filled.Image)
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        FilledTonalIconButton(
                            icon = Icons.Filled.CameraAlt,
                            contentDescription = "回取景",
                            onClick = onCaptureClick
                        )
                        FilledTonalIconButton(
                            icon = Icons.Filled.Delete,
                            contentDescription = "删除",
                            onClick = onDeleteClick,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CaptionText(text: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatusBadge(text: String, accent: Boolean = false, muted: Boolean = false) {
    val (bg, fg) = when {
        accent -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        muted -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) to MaterialTheme.colorScheme.primary
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bg
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun FilledTonalIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun FrameThumbnail(
    project: ProjectEntity,
    stock: FilmStock,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    val borderColor = MaterialTheme.colorScheme.outline

    val bitmap = remember(project.thumbnailPath) {
        project.thumbnailPath?.let { runCatching { android.graphics.BitmapFactory.decodeFile(it) }.getOrNull() }
    }

    Box(
        modifier = modifier
            .width(96.dp)
            .height(128.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, borderColor, shape),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // 占位: 胶片感 — 上胶卷名 + 中大数字 + 下 ISO
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stock.brand,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (project.frameIndexInRoll == 0) "·"
                    else project.frameIndexInRoll.toString(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "ISO ${stock.iso}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 顶部小胶片条装饰
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            repeat(4) { i ->
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(
                                alpha = if (i % 2 == 0) 0.8f else 0.4f
                            ),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// 创建项目 BottomSheet
// ════════════════════════════════════════════════════════════════════════════

private val FRAME_COUNT_OPTIONS = listOf(2, 3, 4, 6, 8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateProjectSheet(
    model: CreateProjectModel,
    onChange: (CreateProjectModel) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 顶部带 FilmStrip 的标题
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "新建项目",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            FilmStrip()
        }

        // 胶卷
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("胶卷")
            FilmPicker(
                selected = model.stock,
                onSelect = { onChange(model.copy(stock = it)) }
            )
        }

        // 张编号 0..N — 水平齿轮式转盘
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("张编号 · 第 ${model.frameIndexInRoll} 张")
            FrameDialPicker(
                selected = model.frameIndexInRoll,
                onSelect = { onChange(model.copy(frameIndexInRoll = it)) }
            )
        }

        // 事件描述
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("事件 (可选)")
            OutlinedTextField(
                value = model.eventNote,
                onValueChange = { onChange(model.copy(eventNote = it)) },
                placeholder = { Text("生日 / 聚会 / 旅行…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 合成张数
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle("合成张数 · ${model.frameCount} 张")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FRAME_COUNT_OPTIONS.forEach { n ->
                    com.bifilm.app.ui.common.PillChip(
                        text = "${n} 张",
                        selected = model.frameCount == n,
                        onClick = { onChange(model.copy(frameCount = n)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 预览标题
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(0.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                SectionTitle("预览")
                Spacer(Modifier.height(4.dp))
                Text(
                    text = projectDisplayTitle(
                        stock = model.stock,
                        frameIndexInRoll = model.frameIndexInRoll,
                        eventNote = model.eventNote.takeIf { it.isNotBlank() },
                        createdAt = System.currentTimeMillis()
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) { Text("取消") }
            FilledTonalButton(
                onClick = onConfirm,
                modifier = Modifier.weight(2f),
                colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) { Text("创建并拍摄", fontWeight = FontWeight.SemiBold) }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** 胶卷三选一 — 一行卡片, 横排. */
@Composable
private fun FilmPicker(
    selected: FilmStock,
    onSelect: (FilmStock) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FilmStocks.ALL.forEach { stock ->
            val isSel = stock.id == selected.id
            Card(
                onClick = { onSelect(stock) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSel)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    else MaterialTheme.colorScheme.surface
                ),
                border = if (isSel)
                    androidx.compose.foundation.BorderStroke(
                        1.5.dp, MaterialTheme.colorScheme.primary
                    )
                else androidx.compose.foundation.BorderStroke(
                        1.dp, MaterialTheme.colorScheme.outlineVariant
                    )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stock.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        color = if (isSel)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "ISO ${stock.iso} · ${stock.type.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// 张编号转盘: 水平 Snap 转盘.
// ════════════════════════════════════════════════════════════════════════════

private const val DIAL_ITEM_WIDTH_DP = 44   // 单个画格的宽度

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FrameDialPicker(
    selected: Int,
    onSelect: (Int) -> Unit
) {
    val range = FilmStocks.FRAME_RANGE
    val containerHeight = 88.dp

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = selected.coerceIn(range)
    )

    val density = LocalDensity.current
    val itemWidthDp = DIAL_ITEM_WIDTH_DP.dp
    val itemWidthPx = with(density) { itemWidthDp.toPx() }

    val centralIndex by remember(listState, itemWidthPx, range) {
        derivedStateOf {
            val offset = listState.firstVisibleItemScrollOffset
            val idx = listState.firstVisibleItemIndex
            val delta = kotlin.math.round(offset / itemWidthPx).toInt()
            (idx + delta).coerceIn(range.first, range.last)
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { inProgress ->
                if (!inProgress) {
                    val v = centralIndex
                    if (v != selected) onSelect(v)
                }
            }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outlineVariant
        ),
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(containerHeight),
            contentAlignment = Alignment.Center
        ) {
            // 中央指示线
            Box(
                modifier = Modifier
                    .height(56.dp)
                    .width(1.5.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
            )
            // 上下小金点
            Box(
                modifier = Modifier
                    .offset(y = (-30).dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Box(
                modifier = Modifier
                    .offset(y = 30.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )

            val viewportWidthDp = LocalConfiguration.current.screenWidthDp.dp
            val sidePadding = (viewportWidthDp / 2) - (itemWidthDp / 2)
            val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
            LazyRow(
                state = listState,
                flingBehavior = flingBehavior,
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding = PaddingValues(horizontal = sidePadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(range.toList()) { n ->
                    DialCell(
                        value = n,
                        itemWidth = itemWidthDp,
                        itemHeight = containerHeight,
                        centralIndex = centralIndex,
                        onClick = { onSelect(n) }
                    )
                }
            }
        }
    }

    LaunchedEffect(selected) {
        if (selected in range && !listState.isScrollInProgress) {
            listState.animateScrollToItem(selected)
        }
    }
}

@Composable
private fun DialCell(
    value: Int,
    itemWidth: Dp,
    itemHeight: Dp,
    centralIndex: Int,
    onClick: () -> Unit
) {
    val isCenter = value == centralIndex
    val distance = kotlin.math.abs(value - centralIndex)
    val fontSize = when (distance) {
        0 -> 28.sp
        1 -> 19.sp
        2 -> 15.sp
        else -> 13.sp
    }
    val alpha = when (distance) {
        0 -> 1f
        1 -> 0.7f
        2 -> 0.45f
        else -> 0.25f
    }
    val color = when (distance) {
        0 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
    }
    val fontWeight = if (isCenter) FontWeight.SemiBold else FontWeight.Normal

    Box(
        modifier = Modifier
            .width(itemWidth)
            .height(itemHeight)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value.toString(),
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight
        )
    }
}