package com.bifilm.app.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bifilm.app.BiFilmApp
import com.bifilm.app.R
import com.bifilm.app.data.db.ProjectEntity
import com.bifilm.app.ui.common.EmptyState
import kotlinx.coroutines.launch

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

    var showNewProjectSheet by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(container, navigateCompose)
    )
    val projects by viewModel.projects.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_home),
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNewProjectSheet = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.action_new_project)) }
            )
        }
    ) { padding ->
        if (projects.isEmpty()) {
            EmptyState(
                message = stringResource(R.string.hint_no_projects),
                modifier = Modifier.padding(padding)
            )
        } else {
            ProjectGrid(
                projects = projects,
                onOpenCompose = { id -> viewModel.touch(id); onOpenCompose(id) },
                onOpenCapture = { id -> viewModel.touch(id); onOpenCapture(id) },
                onLongPressDelete = viewModel::delete,
                modifier = Modifier.padding(padding)
            )
        }
    }

    if (showNewProjectSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNewProjectSheet = false },
            sheetState = sheetState
        ) {
            NewProjectSheet(
                presets = HomeViewModel.FRAME_COUNT_PRESETS,
                onConfirm = { frameCount ->
                    viewModel.createProjectWith(frameCount) { id ->
                        navigateCapture(id)
                    }
                    showNewProjectSheet = false
                    coroutineScope.launch { sheetState.hide() }
                },
                onCancel = {
                    showNewProjectSheet = false
                    coroutineScope.launch { sheetState.hide() }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewProjectSheet(
    presets: List<Int>,
    onConfirm: (Int) -> Unit,
    onCancel: () -> Unit
) {
    var selected by rememberSaveable { mutableStateOf(presets.first()) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "选择合成张数",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "想要几层叠在一起? 常见组合: 2 / 3 / 4 / 6 / 8。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { n ->
                FilterChip(
                    selected = selected == n,
                    onClick = { selected = n },
                    label = { Text("${n} 张") }
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
        ) {
            androidx.compose.material3.TextButton(onClick = onCancel) {
                Text("取消")
            }
            androidx.compose.material3.Button(onClick = { onConfirm(selected) }) {
                Text("创建并开始拍摄")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ProjectGrid(
    projects: List<ProjectEntity>,
    onOpenCompose: (String) -> Unit,
    onOpenCapture: (String) -> Unit,
    onLongPressDelete: (ProjectEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(projects, key = { it.id }) { project ->
            ProjectCard(
                project = project,
                onOpenCompose = { onOpenCompose(project.id) },
                onOpenCapture = { onOpenCapture(project.id) },
                onLongPress = { onLongPressDelete(project) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun ProjectCard(
    project: ProjectEntity,
    onOpenCompose: () -> Unit,
    onOpenCapture: () -> Unit,
    onLongPress: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(0.85f),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(onClick = onOpenCompose, onLongClick = onLongPress)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Layers,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "${project.frameCount} 层",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )
            }
            IconButton(
                onClick = onOpenCapture,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = stringResource(R.string.action_open_camera),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
