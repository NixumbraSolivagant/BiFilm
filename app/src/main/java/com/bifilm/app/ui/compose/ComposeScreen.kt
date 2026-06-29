package com.bifilm.app.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bifilm.app.BiFilmApp
import com.bifilm.app.R
import com.bifilm.app.data.image.ImageStore
import com.bifilm.app.ui.compose.components.BlendModePicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeScreen(
    projectId: String,
    onBack: () -> Unit,
    onExport: (String) -> Unit
) {
    val context = LocalContext.current
    val container = remember(context) {
        (context.applicationContext as BiFilmApp).container
    }
    val viewModel: ComposeViewModel = viewModel(
        factory = ComposeViewModel.Factory(container, projectId)
    )
    val layers by viewModel.layers.collectAsStateWithLifecycle()
    val output by viewModel.output.collectAsStateWithLifecycle()
    val isComposing by viewModel.isComposing.collectAsStateWithLifecycle()
    val selectedMode by viewModel.selectedMode.collectAsStateWithLifecycle()

    LaunchedEffect(layers) {
        if (layers.isNotEmpty()) viewModel.requestRecompose()
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
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // 顶部: 合成结果预览
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                when (val result = output) {
                    null -> Box(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.hint_pick_layers),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    else -> Image(
                        bitmap = result.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                if (isComposing) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 中部: 混合模式
            Text(text = stringResource(R.string.label_blend_mode), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            BlendModePicker(
                selected = selectedMode,
                onSelect = { mode ->
                    viewModel.setBlendMode(mode)
                    layers.forEach { l -> viewModel.updateLayer(l, modeName = mode.name) }
                }
            )

            Spacer(Modifier.height(16.dp))

            // 底部: 图层缩略图横排
            Text(text = "图层 (${layers.size})", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            LayerStrip(paths = layers.map { it.sourcePath })

            Spacer(Modifier.height(16.dp))
            FilledTonalButton(onClick = { onExport(projectId) }, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.action_export))
            }
        }
    }
}

@Composable
private fun LayerStrip(paths: List<String>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(paths) { path ->
            Card(
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(1.dp),
                modifier = Modifier.height(80.dp)
            ) {
                val thumb = remember(path) {
                    ImageStore.decodeRespectingOrientation(java.io.File(path), maxLongEdge = 200)
                }
                if (thumb != null) {
                    Image(
                        bitmap = thumb.asImageBitmap(),
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
    }
}
