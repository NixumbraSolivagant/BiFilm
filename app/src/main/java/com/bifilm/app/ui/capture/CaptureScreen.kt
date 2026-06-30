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
import com.bifilm.app.ui.capture.components.FrameCountPicker
import com.bifilm.app.ui.capture.components.ShutterButton

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
    val currentState: CaptureViewModel.SessionState = state

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
                        totalFrames = if (frameCount < 2) 2 else frameCount,
                        exposureStops = viewModel.exposureStops.value
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = stringResource(R.string.label_frame_count), style = MaterialTheme.typography.labelMedium)
                FrameCountPicker(
                    selected = if (frameCount < 2) 2 else frameCount,
                    onSelect = viewModel::setFrameCount
                )
                Spacer(Modifier.height(4.dp))
                Text(text = stringResource(R.string.label_exposure), style = MaterialTheme.typography.labelMedium)
                ExposurePicker(
                    stops = viewModel.exposureStops.value,
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
                        isEnabled = currentState !is CaptureViewModel.SessionState.Capturing
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
                    enabled = currentState !is CaptureViewModel.SessionState.Capturing
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