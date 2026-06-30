package com.bifilm.app.render.camera

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import androidx.camera.camera2.interop.Camera2CameraInfo
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
import kotlin.math.roundToInt

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

        // 幂等: 同一 owner 上已绑定就直接跳过, 否则重启.
        if (camera != null && ownerRef === lifecycleOwner) {
            Logger.d(TAG, "preview already bound, skip rebind")
            return
        }
        provider.unbindAll()

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

    /**
     * 把 EV 档数 (UI 上的 -3..+3) 转成 CameraX 的 EC index.
     *
     * 不同厂商的 EC step 大小不一样 (常见 1/3 EV, 也有 1/2 或 1/6),
     * 真实大小从 Camera2 characteristic CONTROL_AE_COMPENSATION_STEP 读.
     * ratio = numerator/denominator, 单位 EV. 比如 1/3 EV 一档.
     */
    fun evStopsToEcIndex(stops: Float): Int? {
        val cam = camera ?: return null
        val info = Camera2CameraInfo.from(cam.cameraInfo)
        val rational = info.getCameraCharacteristic(
            CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP
        ) ?: return null
        val stepEv = rational.toDouble()    // 一档 = stepEv EV
        if (stepEv <= 0.0) return null
        return (stops / stepEv).roundToInt()
    }

    fun ecIndexToEvStops(index: Int): Float? {
        val cam = camera ?: return null
        val info = Camera2CameraInfo.from(cam.cameraInfo)
        val rational = info.getCameraCharacteristic(
            CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP
        ) ?: return null
        val stepEv = rational.toDouble()
        if (stepEv <= 0.0) return null
        return (index.toDouble() * stepEv).toFloat()
    }

    // ── 焦段 (zoom) ──────────────────────────────────────────────────

    /** 焦段范围 (最小倍数..最大倍数). 没相机或不支持时返回 null. */
    fun zoomRange(): ClosedFloatingPointRange<Float>? {
        val cam = camera ?: return null
        val state = cam.cameraInfo.zoomState.value ?: return null
        return state.minZoomRatio..state.maxZoomRatio
    }

    /** 当前焦段倍数. */
    fun currentZoomRatio(): Float? = camera?.cameraInfo?.zoomState?.value?.zoomRatio

    /** 设置焦段. 成功返回 true, 被相机自己 clamp 到合法范围. */
    fun setZoomRatio(ratio: Float): Boolean {
        val cam = camera ?: return false
        val range = zoomRange() ?: return false
        val clamped = ratio.coerceIn(range.start, range.endInclusive)
        cam.cameraControl.setZoomRatio(clamped)
        return true
    }

    /**
     * 把相机的物理焦距 (mm) + sensor 物理尺寸 换算为 35mm 等效焦段.
     *
     * 原理:
     *   crop = 36 / sensor_physical_width_in_35mm_format
     *   equiv_focal = focal_length_mm * crop  (在 1x 时)
     *
     * 因为我们用的是 `zoomRatio` (相对 1x 的倍数), UI 上显示
     *   equiv_35mm = equiv_focal_at_1x * zoomRatio
     *
     * 厂商若没暴露 SENSOR_INFO_PHYSICAL_SIZE (少见) 就返回 null,
     * UI 此时回落到 "1x / 2x / 3x" 倍数显示.
     */
    fun equivalentFocalAt1x(): Float? {
        val cam = camera ?: return null
        val chars = Camera2CameraInfo.from(cam.cameraInfo)
        val physicalSize = chars.getCameraCharacteristic(
            CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE
        ) ?: return null
        val focals = chars.getCameraCharacteristic(
            CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
        ) ?: return null
        if (physicalSize.width <= 0f || focals.isEmpty()) return null
        // 取最短焦距 (默认主摄档, 不含副摄切换).
        val focalMm = focals.minOrNull()
            ?.takeIf { it > 0f }
            ?: return null
        // sensor physical size 是 mm; 35mm 等效基线宽度 = 36 mm.
        val crop = 36.0f / physicalSize.width
        return focalMm * crop
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