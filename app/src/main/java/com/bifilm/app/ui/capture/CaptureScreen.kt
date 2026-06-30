package com.bifilm.app.ui.capture

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.camera.view.PreviewView
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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

    // 张数上限: 项目创建时固定, layers.size 是当前已有层数.
    // 拍满后 shutter + 相册都禁用.
    val atFrameLimit = layers.size >= frameCount
    val shutterEnabled = currentState !is CaptureViewModel.SessionState.Capturing && !atFrameLimit

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_capture)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(8.dp)) {
                if (hasCameraPermission) {
                    CameraPreview(
                        bridge = viewModel.cameraBridge,
                        onCameraStarted = { viewModel.markCameraStarted() },
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = stringResource(R.string.hint_camera_permission),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)) {
                    ApertureIndicator(
                        frameCount = layers.size.coerceAtLeast(0),
                        totalFrames = frameCount,
                        exposureStops = exposureStops
                    )
                }

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

            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 张数 (项目创建时已固定, 此处只展示, 不能改).
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.label_frame_count),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.badge_frames_fixed, frameCount),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }
                // 焦段: 0.5x / 1x / 2x / 3x 快捷档
                Text(
                    text = stringResource(R.string.label_zoom),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ZoomPicker(
                    ratio = zoomRatio,
                    min = zoomMin,
                    max = zoomMax,
                    equivFocalAt1x = equivFocalAt1x,
                    onPick = viewModel::setZoom
                )
                Spacer(Modifier.height(2.dp))
                Text(text = stringResource(R.string.label_exposure), style = MaterialTheme.typography.labelMedium)
                ExposurePicker(
                    stops = exposureStops,
                    onChange = viewModel::setExposure
                )
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
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
                // 剩余张数提示 (拍满后显示提示)
                if (atFrameLimit) {
                    Text(
                        text = stringResource(R.string.badge_remaining, layers.size, frameCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        pickImageLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !atFrameLimit && currentState !is CaptureViewModel.SessionState.Capturing
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_pick_images))
                }
            }
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