package com.bifilm.app.ui.capture

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bifilm.app.BiFilmApp
import com.bifilm.app.R
import com.bifilm.app.render.camera.CameraFrameBridge
import com.bifilm.app.ui.capture.components.ApertureIndicator
import com.bifilm.app.ui.capture.components.ExposurePicker
import com.bifilm.app.ui.capture.components.ShutterButton
import com.bifilm.app.ui.capture.components.ZoomPicker
import com.bifilm.app.ui.common.FilmStrip
import com.bifilm.app.ui.common.Hairline
import com.bifilm.app.ui.common.SectionLabel
import com.bifilm.app.ui.common.SectionTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    projectId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val container = remember(context) { (context.applicationContext as BiFilmApp).container }
    val viewModel: CaptureViewModel = viewModel(
        factory = CaptureViewModel.Factory(container, projectId) { context.applicationContext }
    )

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.importImage(uri)
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()
    val frameCount by viewModel.frameCount.collectAsStateWithLifecycle()
    val layers by viewModel.layers.collectAsStateWithLifecycle()
    val zoomRatio by viewModel.zoomRatio.collectAsStateWithLifecycle()
    val zoomMin by viewModel.zoomMin.collectAsStateWithLifecycle()
    val zoomMax by viewModel.zoomMax.collectAsStateWithLifecycle()
    val equivFocalAt1x by viewModel.equivFocalAt1x.collectAsStateWithLifecycle()
    val exposureStops by viewModel.exposureStops.collectAsStateWithLifecycle()
    val currentState: CaptureViewModel.SessionState = state

    val atFrameLimit = layers.size >= frameCount
    val shutterEnabled = currentState !is CaptureViewModel.SessionState.Capturing && !atFrameLimit

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "实时取景",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (atFrameLimit)
                                "已完成 · ${layers.size}/${frameCount} 张"
                            else
                                "第 ${layers.size.coerceAtLeast(0) + 1} 张 / 共 ${frameCount} 张",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── 取景区: 16:9 比例, 黑底, 金色边 ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .clip(RoundedCornerShape(20.dp))
                        .border(
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            RoundedCornerShape(20.dp)
                        )
                        .aspectRatio(3f / 4f)
                ) {
                    if (hasCameraPermission) {
                        CameraPreview(
                            bridge = viewModel.cameraBridge,
                            onCameraStarted = { viewModel.markCameraStarted() },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        PermissionPlaceholder(modifier = Modifier.fillMaxSize())
                    }

                    // 顶部进度点
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                    ) {
                        ApertureIndicator(
                            frameCount = layers.size.coerceAtLeast(0),
                            totalFrames = frameCount,
                            exposureStops = exposureStops
                        )
                    }

                    // 错误浮层
                    if (currentState is CaptureViewModel.SessionState.Error) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text(
                                        text = "错误: ${currentState.reason}",
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── 暗房面板: 上下可滚, 推到屏幕底 ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 顶部 FilmStrip 装饰
                FilmStrip(
                    modifier = Modifier
                        .align(Alignment.Start)
                        .let { it }
                )

                Spacer(Modifier.height(2.dp))

                // 焦段
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        ZoomPicker(
                            ratio = zoomRatio,
                            min = zoomMin,
                            max = zoomMax,
                            equivFocalAt1x = equivFocalAt1x,
                            onPick = viewModel::setZoom
                        )
                    }
                }

                // 曝光档
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        ExposurePicker(
                            stops = exposureStops,
                            onChange = viewModel::setExposure
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // 快门按钮 + 拍满提示
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShutterButton(
                        onShutter = {
                            viewModel.shutter {
                                if (!viewModel.cameraBridge.isReadyForCapture()) null
                                else viewModel.cameraBridge.takePicture(
                                    viewModel.cameraBridge.newCaptureFile(projectId)
                                )
                            }
                        },
                        isEnabled = shutterEnabled
                    )
                }

                // 拍满提示行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (atFrameLimit) {
                        StatusPill(
                            text = "已拍满 · 去合成页",
                            icon = Icons.Filled.PhotoLibrary,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.badge_remaining, layers.size, frameCount),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // 导入相册按钮 (画幅未满时可点)
                Button(
                    onClick = {
                        pickImageLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !atFrameLimit &&
                        currentState !is CaptureViewModel.SessionState.Capturing,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(
                        1.dp, MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_pick_images), fontWeight = FontWeight.Medium)
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun StatusPill(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = tint.copy(alpha = 0.14f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = tint,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PermissionPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "⌀",  // 单字符: 镜头盖
                style = MaterialTheme.typography.displayMedium,
                color = Color.White.copy(alpha = 0.4f)
            )
            Text(
                text = stringResource(R.string.hint_camera_permission),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun CameraPreview(
    bridge: CameraFrameBridge,
    onCameraStarted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val surfaceHolder = remember { SurfaceProviderHolder() }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
                surfaceHolder.previewView = this
            }
        }
    )
    LaunchedEffect(lifecycleOwner, bridge) {
        val holder = surfaceHolder.previewView ?: return@LaunchedEffect
        val surfaceProvider = holder.surfaceProvider
        try {
            bridge.startPreview(
                lifecycleOwner = lifecycleOwner,
                surfaceProvider = surfaceProvider
            )
            onCameraStarted()
        } catch (t: Throwable) {
            com.bifilm.app.util.Logger.e("CameraPreview", "start failed", t)
        }
    }
    DisposableEffect(Unit) {
        onDispose { bridge.shutdown() }
    }
}

private class SurfaceProviderHolder {
    var previewView: PreviewView? = null
}