package com.bifilm.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bifilm.app.BiFilmApp
import com.bifilm.app.R
import com.bifilm.app.data.prefs.BiFilmSettings
import com.bifilm.app.domain.model.FilmStocks
import com.bifilm.app.domain.model.ScenePresets
import com.bifilm.app.ui.common.FilmStrip
import com.bifilm.app.ui.common.PillChip
import com.bifilm.app.ui.common.SectionCard
import com.bifilm.app.ui.common.SectionTitle
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val container = remember(context) { (context.applicationContext as BiFilmApp).container }
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(container))
    val ui by vm.state.collectAsStateWithLifecycle()

    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "设置",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "默认值",
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
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Filled.RestartAlt, contentDescription = "重置默认")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FilmStrip()
                Text(
                    text = "新建项目时这些值会自动填入, 之后还能再改.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                SettingsContent(s = ui.settings!!, vm = vm)

                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { /* 由 TopBar 重置按钮触发 */ },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("通过右上角 ↺ 按钮恢复全部默认")
                }
                Spacer(Modifier.height(16.dp))
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
private fun SettingsContent(s: BiFilmSettings, vm: SettingsViewModel) {
    // 1. 默认场景预设
    val selectedSceneId = s.defaultSceneId
    val selectedScene = ScenePresets.byId(selectedSceneId) ?: ScenePresets.default()

    SectionCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                SectionTitle("默认场景")
                Spacer(Modifier.height(2.dp))
                Text(
                    text = selectedScene.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
            ) {
                Text(
                    text = selectedScene.group.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        ChoiceChipFlow(
            items = SettingsOptions.SCENES.map { it.id to it.title },
            selectedId = selectedSceneId,
            onSelect = vm::setScene
        )
        selectedScene.exposureNote?.let { note ->
            Spacer(Modifier.height(10.dp))
            InfoLine("提示", note)
        }
    }

    // 2. 默认混合算法
    SectionCard {
        Column {
            SectionTitle("默认混合算法")
            Spacer(Modifier.height(8.dp))
            ChoiceChipFlow(
                items = listOf(
                    "SCREEN" to "滤色",
                    "ADDITIVE" to "加色",
                    "MULTIPLY" to "正片叠底",
                    "LIGHTEN" to "提亮",
                    "DARKEN" to "压暗",
                    "AVERAGE" to "平均"
                ),
                selectedId = s.preferredBlendMode,
                onSelect = vm::setBlendMode
            )
            Spacer(Modifier.height(10.dp))
            InfoLine("注意", "只影响叠加公式, 不改变曝光档.")
        }
    }

    // 3. 默认张数
    SectionCard {
        Column {
            SectionTitle("默认张数")
            Spacer(Modifier.height(8.dp))
            ChoiceChipFlow(
                items = SettingsOptions.FRAME_COUNTS.map { it to "${it} 张" },
                selectedId = s.defaultFrameCount,
                onSelect = vm::setFrameCount
            )
            Spacer(Modifier.height(10.dp))
            InfoLine("提示", "新建项目时直接用这个数字, 不再弹选择.")
        }
    }

    // 4. 默认焦段
    SectionCard {
        Column {
            SectionTitle("默认焦段")
            Spacer(Modifier.height(8.dp))
            ChoiceChipFlow(
                items = SettingsOptions.FOCAL_MM_OPTIONS.map { it to "${it}mm" },
                selectedId = s.defaultFocalMm,
                onSelect = vm::setFocalMm
            )
            Spacer(Modifier.height(10.dp))
            InfoLine("提示", "进入取景页时自动套用. 手机不支持的档会被自动隐藏.")
        }
    }

    // 4b. 默认胶卷
    SectionCard {
        Column {
            SectionTitle("默认胶卷")
            Spacer(Modifier.height(8.dp))
            ChoiceChipFlow(
                items = FilmStocks.ALL.map { it.id to it.displayName },
                selectedId = s.defaultFilmStockId,
                onSelect = vm::setFilmStockId
            )
            Spacer(Modifier.height(10.dp))
            InfoLine("提示", "新建项目时自动套用. 切换后, 新项目沿用对应胶卷的事件编号.")
        }
    }

    // 5. 默认曝光档
    SectionCard {
        Column {
            SectionTitle("默认曝光档")
            val current = SettingsOptions.EXPOSURE_OPTIONS.minByOrNull { abs(it - s.defaultExposureStops) }
                ?: s.defaultExposureStops
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("−3 EV", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = formatEv(current),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                Text("+3 EV", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(4.dp))
            Slider(
                value = current,
                onValueChange = { vm.setExposureStops(snapToThird(it)) },
                valueRange = -3f..3f,
                steps = SettingsOptions.EXPOSURE_OPTIONS.size - 2,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            InfoLine("提示", "进入取景页时自动套用, 1/3 步长, 范围 ±3EV.")
        }
    }

    // 6. 蒙版硬度
    SectionCard {
        Column {
            SectionTitle("蒙版硬度")
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("柔", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = s.maskHardness.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                Text("硬", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(4.dp))
            Slider(
                value = s.maskHardness.toFloat(),
                onValueChange = { vm.setMaskHardness(it.toInt()) },
                valueRange = 0f..100f,
                steps = 99,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            InfoLine("提示", "局部混合时蒙版边缘硬度. 0 = 柔, 100 = 硬.")
        }
    }

    // 7. 导出参数 (只读)
    SectionCard {
        Column {
            SectionTitle("导出参数")
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("导出最长边", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = "${s.exportMaxLongEdgePx} px",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "只读",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            InfoLine("注意", "固定值, 不允许用户改 (避免改错导致导不出图).")
        }
    }
}

/** 卡内嵌入的提示行: 「提示」/「注意」金色小标签 + 灰色说明. */
@Composable
private fun InfoLine(tag: String, text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        ) {
            Text(
                text = tag,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Wrap 布局的 chip 行. 自动换行, 间距统一. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun <T> ChoiceChipFlow(
    items: List<Pair<T, String>>,
    selectedId: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { (id, label) ->
            PillChip(
                text = label,
                selected = id == selectedId,
                onClick = { onSelect(id) }
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