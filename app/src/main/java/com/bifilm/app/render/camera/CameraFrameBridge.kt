package com.bifilm.app.render.camera

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.bifilm.app.util.Logger
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.UUID
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * CameraX 桥: 把相机帧送进 UI 预览, 并执行拍照写文件.
 *
 * 用例绑定: Preview + ImageAnalysis + ImageCapture 共存.
 *   - Preview: UI 预览
 *   - ImageAnalysis: 可选 (M3 占位)
 *   - ImageCapture: 快门
 */
class CameraFrameBridge(private val context: Context) {

    @Volatile private var provider: ProcessCameraProvider? = null
    @Volatile private var camera: Camera? = null
    @Volatile private var preview: Preview? = null
    @Volatile private var analysis: ImageAnalysis? = null
    @Volatile private var capture: ImageCapture? = null
    @Volatile private var ownerRef: LifecycleOwner? = null

    val currentCamera: Camera? get() = camera

    suspend fun startPreview(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider,
        analysisExecutor: Executor = ContextCompat.getMainExecutor(context),
        onImageAvailable: (ImageProxy) -> Unit = {}
    ) {
        val future: ListenableFuture<ProcessCameraProvider> =
            ProcessCameraProvider.getInstance(context)
        val provider = try {
            suspendCancellableCoroutine<ProcessCameraProvider> { cont ->
                future.addListener({
                    try {
                        cont.resume(future.get())
                    } catch (t: Throwable) {
                        cont.resumeWithException(t)
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        } catch (t: Throwable) {
            Logger.e(TAG, "camera provider unavailable", t)
            throw t
        }
        this.provider = provider
        this.ownerRef = lifecycleOwner

        val previewUseCase = Preview.Builder().build().apply {
            setSurfaceProvider(surfaceProvider)
        }
        val analysisUseCase = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        analysisUseCase.setAnalyzer(analysisExecutor) { proxy ->
            try { onImageAvailable(proxy) } finally { proxy.close() }
        }
        val captureUseCase = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        provider.unbindAll()
        camera = provider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            previewUseCase,
            analysisUseCase,
            captureUseCase
        )
        preview = previewUseCase
        analysis = analysisUseCase
        capture = captureUseCase
        Logger.d(TAG, "preview bound with capture")
    }

    fun setExposureCompensation(value: Int): Boolean {
        val cam = camera ?: return false
        val range = cam.cameraInfo.exposureState.exposureCompensationRange
        val clamped = value.coerceIn(range.lower, range.upper)
        cam.cameraControl.setExposureCompensationIndex(clamped)
        return true
    }

    fun exposureCompensationRange(): IntRange? {
        val cam = camera ?: return null
        return cam.cameraInfo.exposureState.exposureCompensationRange.let { it.lower..it.upper }
    }

    /** 拍照并写到 [targetFile]. 成功: 返回 File. */
    suspend fun takePicture(targetFile: File): File = suspendCancellableCoroutine { cont ->
        val useCase = capture
        val owner = ownerRef
        if (useCase == null || owner == null) {
            cont.resumeWithException(IllegalStateException("camera not ready"))
            return@suspendCancellableCoroutine
        }
        val output = ImageCapture.OutputFileOptions.Builder(targetFile).build()
        useCase.takePicture(
            output,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                    Logger.d(TAG, "captured to ${targetFile.absolutePath}")
                    cont.resume(targetFile)
                }

                override fun onError(exception: ImageCaptureException) {
                    Logger.e(TAG, "capture failed", exception)
                    cont.resumeWithException(exception)
                }
            }
        )
    }

    fun shutdown() {
        provider?.unbindAll()
        preview = null
        analysis = null
        capture = null
        camera = null
        ownerRef = null
    }

    fun isReadyForCapture(): Boolean = camera != null && capture != null

    /** 给定项目 ID 生成唯一文件名. */
    fun newCaptureFile(projectId: String): File {
        val dir = java.io.File(context.filesDir, "bifilm/projects/$projectId").apply {
            if (!exists()) mkdirs()
        }
        return java.io.File(dir, "capture-${UUID.randomUUID()}.jpg")
    }

    companion object {
        private const val TAG = "CameraFrameBridge"
    }
}