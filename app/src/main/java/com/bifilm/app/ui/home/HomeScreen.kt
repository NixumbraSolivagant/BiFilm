package com.bifilm.app.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("胶卷", style = MaterialTheme.typography.headlineMedium) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.requestCreate()
                    showCreateSheet = true
                },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("新建") }
            )
        }
    ) { padding ->
        if (cards.isEmpty()) {
            EmptyState(
                message = "还没有胶卷. 点右下角新建第一个项目.",
                modifier = Modifier.padding(padding)
            )
        } else {
            EventList(
                cards = cards,
                onOpenCompose = { id -> viewModel.touch(id); onOpenCompose(id) },
                onOpenCapture = { id -> viewModel.touch(id); onOpenCapture(id) },
                onLongPressDelete = viewModel::delete,
                modifier = Modifier.padding(padding)
            )
        }
    }

    if (showCreateSheet && pendingCreate != null) {
        ModalBottomSheet(
            onDismissRequest = { showCreateSheet = false },
            sheetState = sheetState
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
}

// ════════════════════════════════════════════════════════════════════════════
// 首页: 扁平卡片列表, 每个项目独立一张卡, 不再按胶卷分组.
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun EventList(
    cards: List<HomeCardItem>,
    onOpenCompose: (String) -> Unit,
    onOpenCapture: (String) -> Unit,
    onLongPressDelete: (ProjectEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(cards, key = { it.project.id }) { card ->
            ProjectCard(
                card = card,
                onClick = { onOpenCompose(card.project.id) },
                onCaptureClick = { onOpenCapture(card.project.id) },
                onLongClick = { onLongPressDelete(card.project) }
            )
        }
        item { Spacer(Modifier.height(96.dp)) }  // 让 FAB 不压最后一项
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProjectCard(
    card: HomeCardItem,
    onClick: () -> Unit,
    onCaptureClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val project = card.project
    val stock = card.stock

    val dateText = remember(project.updatedAt) {
        val df = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        df.format(Date(project.updatedAt))
    }
    val eventText = project.eventNote?.trim()?.takeIf { it.isNotEmpty() }
    val isBlank = project.frameIndexInRoll == 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 左侧: 缩略图位 + 右上角相机按钮 (点击进 Capture).
            Box {
                FrameThumbnail(
                    project = project,
                    stock = stock,
                    modifier = Modifier
                        .width(84.dp)
                        .height(112.dp)
                )
                // 拍摄按钮: 浮在缩略图右上角, 点击进入 Capture 页.
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(26.dp)
                ) {
                    IconButton(
                        onClick = onCaptureClick,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = "拍摄",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(14.dp))

            // 右侧: 多行详细信息.
            Column(modifier = Modifier.weight(1f)) {
                // 主标题: 胶卷名 · 第 N 张 / 空白画幅
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isBlank) "空白画幅" else "第 ${project.frameIndexInRoll} 张",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "·",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stock.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.height(4.dp))

                // 胶卷规格行
                Text(
                    text = "${stock.brand} · ISO ${stock.iso} · ${stock.type.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(8.dp))

                // 事件描述
                if (eventText != null) {
                    Text(
                        text = eventText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2
                    )
                    Spacer(Modifier.height(6.dp))
                }

                // 元数据 chip 行
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InfoChip(
                        icon = Icons.Filled.GridView,
                        text = "合成 ${project.frameCount} 张"
                    )
                    InfoChip(
                        icon = Icons.Filled.AspectRatio,
                        text = "${project.frameWidth}×${project.frameHeight}"
                    )
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FrameThumbnail(
    project: ProjectEntity,
    stock: FilmStock,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(8.dp)
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)

    // 缩略图存在就显示, 否则用胶卷 + 张号占位.
    val bitmap = remember(project.thumbnailPath) {
        project.thumbnailPath?.let { runCatching { android.graphics.BitmapFactory.decodeFile(it) }.getOrNull() }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, borderColor, shape),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stock.brand,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (project.frameIndexInRoll == 0) "空"
                    else project.frameIndexInRoll.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "ISO ${stock.iso}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("新建项目", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)

        // 胶卷
        SectionLabel("胶卷")
        FilmPicker(
            selected = model.stock,
            onSelect = { onChange(model.copy(stock = it)) }
        )

        // 张编号 0..38 — 水平齿轮式转盘
        SectionLabel("张编号")
        FrameDialPicker(
            selected = model.frameIndexInRoll,
            onSelect = { onChange(model.copy(frameIndexInRoll = it)) }
        )

        // 事件描述
        SectionLabel("事件 (可选)")
        OutlinedTextField(
            value = model.eventNote,
            onValueChange = { onChange(model.copy(eventNote = it)) },
            placeholder = { Text("生日 / 聚会 / 旅行…") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // 合成张数
        SectionLabel("合成张数")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FRAME_COUNT_OPTIONS.forEach { n ->
                FilterChip(
                    selected = model.frameCount == n,
                    onClick = { onChange(model.copy(frameCount = n)) },
                    label = { Text("$n 张") }
                )
            }
        }

        // 预览标题
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    "预览",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = projectDisplayTitle(
                        stock = model.stock,
                        frameIndexInRoll = model.frameIndexInRoll,
                        eventNote = model.eventNote.takeIf { it.isNotBlank() },
                        createdAt = System.currentTimeMillis()
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
        ) {
            TextButton(onClick = onCancel) { Text("取消") }
            Button(onClick = onConfirm) { Text("创建并拍摄") }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold
    )
}

/** 胶卷三选一 — 一行卡片, 横排. */
@Composable
private fun FilmPicker(
    selected: FilmStock,
    onSelect: (FilmStock) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilmStocks.ALL.forEach { stock ->
            val isSel = stock.id == selected.id
            Card(
                onClick = { onSelect(stock) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSel)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surface
                ),
                border = if (isSel)
                    androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                else null,
                elevation = CardDefaults.cardElevation(if (isSel) 0.dp else 1.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stock.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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
// 关键:
// 1. item 宽度固定 (40dp), 容器宽度 = N × item (5.6 个, 实际靠 sidePadding 截断),
//    让"中央指示线 = 某 item 的中心"严格成立, 数字永远落在指示线上.
// 2. 用 derivedStateOf + snapshotFlow 实时算"离可视中心最近的 item", 把它
//    作为中心, 字号/颜色按距离分级, 形成齿轮缩放感.
// ════════════════════════════════════════════════════════════════════════════

private const val DIAL_ITEM_WIDTH_DP = 40   // 单个画格的宽度

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FrameDialPicker(
    selected: Int,
    onSelect: (Int) -> Unit
) {
    val range = FilmStocks.FRAME_RANGE
    val containerHeight = 96.dp

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = selected.coerceIn(range)
    )

    val density = LocalDensity.current
    val itemWidthDp = DIAL_ITEM_WIDTH_DP.dp
    val itemWidthPx = with(density) { itemWidthDp.toPx() }

    // 把"中心 item 索引"实时算出来给 DialCell 用 (字号/颜色区分).
    // LazyRow: sidePadding 把"可视区左边缘"放在 firstVisibleItemIndex 的中心,
    // 所以:
    //   - offset=0 (首可见 item 左边缘 = 可视区左 - itemWidth/2)
    //   - offset=itemWidth (首可见 item 已向右滑一格, 它已离开可视中心)
    // "逻辑中心索引" = idx - round(offset/itemWidth - 0.5).
    // 用 roundToInt 才能在 offset 越过 itemWidth/2 时正确切到 idx-1.
    val centralIndex by remember(listState, itemWidthPx, range) {
        derivedStateOf {
            val offset = listState.firstVisibleItemScrollOffset
            val idx = listState.firstVisibleItemIndex
            // 因为 sidePadding = viewportWidth/2 - itemWidth/2, 可视区中央
            // 永远落在某个 item 的中心上. 当 offset = 0 时中央 = item idx 的中心;
            // 当 offset = itemWidth/2 时中央 = idx 与 idx+1 的中点(此时仍归 idx);
            // 当 offset = itemWidth 时中央 = item idx+1 的中心.
            // 所以 delta = round(offset / itemWidth).
            val delta = kotlin.math.round(offset / itemWidthPx).toInt()
            (idx + delta).coerceIn(range.first, range.last)
        }
    }

    // 滑动中实时把中心值回写到 onSelect (节流).
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        // 容器按"侧 padding"把可视区压到恰好 N 个 item, 中央 item 中心 = 指示线.
        // 容器宽度 = M * itemWidth (M 较大, 例如 5). 视觉上看像转盘.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(containerHeight),
            contentAlignment = Alignment.Center
        ) {
            // 两端半透明渐隐
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                Color.Transparent,
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface
                            ),
                            startX = 0f,
                            endX = 96f  // 仅占容器前 96dp 的过渡, 简化处理
                        )
                    )
            )

            // 中央指示线
            Box(
                modifier = Modifier
                    .height(64.dp)
                    .width(2.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
            )
            // 指示线上下小点
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .offset(y = (-36).dp)
                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .offset(y = 36.dp)
                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
            )

            // 滚动列表 — SidePadding = 屏幕宽/2 - itemWidth/2, 让中央正好对齐 item 中心.
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

    // 外部 selected 变化时平滑滚到中央.
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
        0 -> 32.sp
        1 -> 22.sp
        2 -> 17.sp
        else -> 14.sp
    }
    val alpha = when (distance) {
        0 -> 1f
        1 -> 0.75f
        2 -> 0.45f
        else -> 0.25f
    }
    val color = when (distance) {
        0 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
    }
    val fontWeight = if (isCenter) FontWeight.Black else FontWeight.Normal

    Box(
        modifier = Modifier
            .width(itemWidth)
            .height(itemHeight)
            .clickable(onClick = onClick),
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