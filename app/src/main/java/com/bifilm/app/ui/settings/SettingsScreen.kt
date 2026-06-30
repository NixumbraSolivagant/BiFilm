package com.bifilm.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bifilm.app.BiFilmApp
import com.bifilm.app.R
import com.bifilm.app.data.prefs.BiFilmSettings
import com.bifilm.app.domain.model.FilmStocks
import com.bifilm.app.domain.model.ScenePresets
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val container = remember(context) { (context.applicationContext as BiFilmApp).container }
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(container))
    val ui by vm.state.collectAsStateWithLifecycle()

    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Filled.RestartAlt, contentDescription = "重置默认")
                    }
                }
            )
        }
    ) { padding ->
        if (ui.loading || ui.settings == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SectionTitle("默认值")
                Text(
                    text = "新建项目时这些值会自动填入, 之后还能再改.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                SettingsContent(s = ui.settings!!, vm = vm)
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("恢复默认值") },
            text = { Text("所有自定义值都会被覆盖为内置默认. 现有项目不受影响.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.resetDefaults()
                    showResetDialog = false
                }) { Text("确认重置") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SettingsContent(s: BiFilmSettings, vm: SettingsViewModel) {
    // 1. 默认场景预设 (驱动混合模式 + 默认曝光档 + 提示语)
    val selectedSceneId = s.defaultSceneId
    val selectedScene = ScenePresets.byId(selectedSceneId) ?: ScenePresets.default()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LabeledHint("默认场景", "新建项目自动选这个. 改场景也会同步下面两项默认值.")
            ChipRow(
                items = SettingsOptions.SCENES.map { it.id to it.title },
                selectedId = selectedSceneId,
                onSelect = vm::setScene
            )
            selectedScene.exposureNote?.let { note ->
                Text(
                    text = "提示: $note",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // 2. 默认混合模式 (仅混合算法, 不影响曝光)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LabeledHint("默认混合算法", "只影响叠加公式, 不改变曝光档.")
            ChipRow(
                items = listOf(
                    "SCREEN" to "滤色",
                    "ADDITIVE" to "加色",
                    "LIGHTEN" to "提亮",
                    "AVERAGE" to "平均",
                    "DARKEN" to "压暗"
                ),
                selectedId = s.preferredBlendMode,
                onSelect = vm::setBlendMode
            )
        }
    }

    // 3. 默认张数 (2~8)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LabeledHint("默认张数", "新建项目时直接用这个数字, 不再弹选择.")
            ChipRow(
                items = SettingsOptions.FRAME_COUNTS.map { it to "${it} 张" },
                selectedId = s.defaultFrameCount,
                onSelect = vm::setFrameCount
            )
        }
    }

    // 4. 默认焦段 (mm)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LabeledHint("默认焦段", "进入取景页时自动套用. 手机不支持的档会被自动隐藏.")
            ChipRow(
                items = SettingsOptions.FOCAL_MM_OPTIONS.map { it to "${it}mm" },
                selectedId = s.defaultFocalMm,
                onSelect = vm::setFocalMm
            )
        }
    }

    // 4b. 默认胶卷
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LabeledHint("默认胶卷", "新建项目时自动套用. 切换后, 新项目沿用对应胶卷的事件编号.")
            ChipRow(
                items = FilmStocks.ALL.map { it.id to it.displayName },
                selectedId = s.defaultFilmStockId,
                onSelect = vm::setFilmStockId
            )
        }
    }

    // 5. 默认曝光档 (1/3 step, ±3EV)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LabeledHint("默认曝光档", "进入取景页时自动套用, 1/3 步长, 范围 ±3EV.")
            val current = SettingsOptions.EXPOSURE_OPTIONS.minByOrNull { abs(it - s.defaultExposureStops) }
                ?: s.defaultExposureStops
            Slider(
                value = current,
                onValueChange = { vm.setExposureStops(snapToThird(it)) },
                valueRange = -3f..3f,
                steps = SettingsOptions.EXPOSURE_OPTIONS.size - 2,
                modifier = Modifier.fillMaxWidth()
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("−3 EV", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = formatEv(current),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("+3 EV", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    // 6. 蒙版硬度 (滑杆)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LabeledHint("蒙版硬度", "局部混合时蒙版边缘硬度. 0 = 柔, 100 = 硬.")
            Slider(
                value = s.maskHardness.toFloat(),
                onValueChange = { vm.setMaskHardness(it.toInt()) },
                valueRange = 0f..100f,
                steps = 99,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = s.maskHardness.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
    }

    // 7. 导出参数只读 (避免误改导致出图问题)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("导出最长边", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "${s.exportMaxLongEdgePx} px",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "固定值, 不允许用户改 (避免改错导致导不出图)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = { /* 由 TopBar 重置按钮触发 */ }, modifier = Modifier.fillMaxWidth()) {
        Text("通过右上角 ↺ 按钮恢复全部默认")
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun LabeledHint(label: String, hint: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun <T> ChipRow(
    items: List<Pair<T, String>>,
    selectedId: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { (id, label) ->
            FilterChip(
                selected = id == selectedId,
                onClick = { onSelect(id) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

private fun formatEv(stops: Float): String {
    if (abs(stops) < 0.05f) return "0 EV"
    val sign = if (stops > 0) "+" else "−"
    val whole = stops.toInt()
    val frac = ((abs(stops) - abs(whole)) * 3f).toInt()
    return when {
        frac == 0 -> "$sign${abs(whole)} EV"
        else -> "$sign${abs(whole)}⅓ EV"
    }
}

private fun snapToThird(v: Float): Float {
    val snapped = (v * 3f).toInt() / 3f
    return snapped.coerceIn(-3f, 3f)
}

@Composable
private fun stringResource(id: Int): String =
    androidx.compose.ui.res.stringResource(id)