package com.bifilm.app.ui.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bifilm.app.data.db.LayerDao
import com.bifilm.app.data.db.LayerEntity
import com.bifilm.app.data.db.ProjectDao
import com.bifilm.app.data.db.ProjectEntity
import com.bifilm.app.data.image.ImageStore
import com.bifilm.app.data.prefs.SettingsRepository
import com.bifilm.app.di.AppContainer
import com.bifilm.app.domain.model.BlendMode
import com.bifilm.app.domain.model.ScenePresets
import com.bifilm.app.domain.usecase.AddLayerUseCase
import com.bifilm.app.render.camera.CameraFrameBridge
import com.bifilm.app.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

/**
 * 状态机:
 *   Idle         -> 无层累积.
 *   LivePreview  -> 预览中, 用户可以拍照.
 *   Capturing    -> 拍照处理中.
 *   Exporting    -> 出图 (M7).
 *   Error(...)   -> 错误.
 */
class CaptureViewModel(
    val projectId: String,
    private val projectDao: ProjectDao,
    private val layerDao: LayerDao,
    private val imageStore: ImageStore,
    private val addLayerUseCase: AddLayerUseCase,
    private val settingsRepository: SettingsRepository,
    cameraFactory: () -> CameraFrameBridge
) : ViewModel() {

    sealed class SessionState {
        data object Idle : SessionState()
        data object LivePreview : SessionState()
        data object Capturing : SessionState()
        data object Exporting : SessionState()
        data class Error(val reason: String) : SessionState()
    }

    private val _state = MutableStateFlow<SessionState>(SessionState.Idle)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    val project: StateFlow<ProjectEntity?> =
        MutableStateFlow<ProjectEntity?>(null).also { flow ->
            viewModelScope.launch { flow.value = projectDao.findById(projectId) }
        }.asStateFlow()

    val layers: StateFlow<List<LayerEntity>> =
        layerDao.observeForProject(projectId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            val entity = projectDao.findById(projectId)
            if (entity != null && entity.frameCount >= 2) {
                _frameCount.value = entity.frameCount.coerceIn(2, 8)
            }
        }
        // 把设置里的"默认场景 / 默认曝光 / 默认混合模式 / 默认焦段"灌进 VM.
        // 这一步只在 VM 创建时执行一次; 之后用户在取景页的修改不会回写到设置.
        viewModelScope.launch {
            val s = settingsRepository.settings.first()
            val scene = ScenePresets.byId(s.defaultSceneId) ?: ScenePresets.default()
            // 曝光档: 场景带的优先 (合理值), 否则用 settings 的全局默认
            val ev = if (scene.suggestedExposureStops != 0f)
                scene.suggestedExposureStops
            else s.defaultExposureStops
            _exposureStops.value = ev.coerceIn(
                com.bifilm.app.domain.model.ExposureStops.MIN,
                com.bifilm.app.domain.model.ExposureStops.MAX
            )
            // 混合模式: settings 里的 preferredBlendMode 优先 (用户在设置里改的就生效)
            val blendMode = runCatching { BlendMode.valueOf(s.preferredBlendMode) }
                .getOrDefault(scene.mode)
            _blendMode.value = blendMode
            // 焦段: 等相机 ready 后 (refreshCameraCapabilities) 会 clamp 到合法范围.
            val base = cameraBridge.equivalentFocalAt1x() ?: 50f
            val desiredRatio = (s.defaultFocalMm.toFloat() / base)
            _zoomRatio.value = desiredRatio.coerceAtLeast(1f)
        }
    }

    private val _frameCount = MutableStateFlow(2)
    val frameCount: StateFlow<Int> = _frameCount.asStateFlow()

    /** 项目创建时锁定的张数上限 (从 DB 读, 不在 VM 内修改). */
    private val frameCountLimit: Int get() = _frameCount.value

    private val _exposureStops = MutableStateFlow(0f)
    val exposureStops: StateFlow<Float> = _exposureStops.asStateFlow()

    private val _blendMode = MutableStateFlow(BlendMode.SCREEN)
    val blendMode: StateFlow<BlendMode> = _blendMode.asStateFlow()

    // 焦段 (相机启动后从 bridge 读 zoom range 初始化)
    private val _zoomMin = MutableStateFlow(1f)
    val zoomMin: StateFlow<Float> = _zoomMin.asStateFlow()
    private val _zoomMax = MutableStateFlow(1f)
    val zoomMax: StateFlow<Float> = _zoomMax.asStateFlow()
    private val _zoomRatio = MutableStateFlow(1f)
    val zoomRatio: StateFlow<Float> = _zoomRatio.asStateFlow()
    // 主摄 1x 等效焦段 (mm), null 表示无法获取 (厂商没暴露 sensor size).
    private val _equivFocalAt1x = MutableStateFlow<Float?>(null)
    val equivFocalAt1x: StateFlow<Float?> = _equivFocalAt1x.asStateFlow()

    val cameraBridge: CameraFrameBridge = cameraFactory()
    private var cameraStarted = false

    /**
     * 相机启动后被 UI 调用: 把 EC 范围 / zoom 范围同步给 UI, 并把当前的曝光/焦段推到相机.
     */
    fun refreshCameraCapabilities() {
        cameraBridge.zoomRange()?.let { range ->
            _zoomMin.value = range.start
            _zoomMax.value = range.endInclusive
            if (_zoomRatio.value < range.start || _zoomRatio.value > range.endInclusive) {
                _zoomRatio.value = 1f.coerceIn(range.start, range.endInclusive)
            }
            cameraBridge.setZoomRatio(_zoomRatio.value)
        }
        _equivFocalAt1x.value = cameraBridge.equivalentFocalAt1x()
        // EC: 立刻把当前 stops 推给相机
        applyExposureToCamera()
    }

    fun setExposure(stops: Float) {
        val clamped = stops.coerceIn(
            com.bifilm.app.domain.model.ExposureStops.MIN,
            com.bifilm.app.domain.model.ExposureStops.MAX
        )
        _exposureStops.value = clamped
        applyExposureToCamera()
    }

    fun setZoom(ratio: Float) {
        val range = cameraBridge.zoomRange() ?: return
        val clamped = ratio.coerceIn(range.start, range.endInclusive)
        _zoomRatio.value = clamped
        cameraBridge.setZoomRatio(clamped)
    }

    /**
     * 把当前 EV 推给相机: UI 上的 -3..+3 (1/3 step) 转换为 CameraX 的 EC index
     * (厂商差异: 1/3 / 1/2 / 1/6 EV 一档). 计算失败就静默跳过.
     */
    private fun applyExposureToCamera() {
        val idx = cameraBridge.evStopsToEcIndex(_exposureStops.value) ?: return
        cameraBridge.setExposureCompensation(idx)
    }

    fun setBlendMode(mode: BlendMode) {
        _blendMode.value = mode
    }

    /** 用户按快门: 保存文件 + 加 layer. saveBitmap 由 UI 提供 (M3 占位实现). */
    fun shutter(saveBitmap: suspend () -> File?) {
        if (_state.value == SessionState.Capturing) return
        val current = layers.value
        if (current.size >= frameCountLimit) return  // 拍满, 不接受.
        viewModelScope.launch {
            _state.value = SessionState.Capturing
            val file = try {
                saveBitmap()
            } catch (t: Throwable) {
                Logger.e(TAG, "saveBitmap failed", t)
                _state.value = SessionState.Error("save failed: ${t.message}")
                return@launch
            }
            if (file == null || !file.exists()) {
                _state.value = SessionState.Error("image file not created")
                return@launch
            }
            insertLayerFromFile(file)
            _state.value = SessionState.LivePreview
            // 张数上限由 frameCountLimit 锁定, 此处不再递减 frameCount.
        }
    }

    /** 用户从相册挑了一张图, 走 AddLayerUseCase 加一层. */
    fun importImage(uri: android.net.Uri) {
        if (_state.value == SessionState.Capturing) return
        if (layers.value.size >= frameCountLimit) return  // 加满, 不接受.
        viewModelScope.launch {
            _state.value = SessionState.Capturing
            try {
                addLayerUseCase(projectId, uri)
                _state.value = SessionState.LivePreview
            } catch (t: Throwable) {
                Logger.e(TAG, "importImage failed", t)
                _state.value = SessionState.Error("import failed: ${t.message}")
            }
        }
    }

    private suspend fun insertLayerFromFile(file: File) {
        val order = (layerDao.listForProject(projectId).maxOfOrNull { it.order } ?: -1) + 1
        val layer = LayerEntity(
            id = UUID.randomUUID().toString(),
            projectId = projectId,
            order = order,
            sourcePath = file.absolutePath,
            blendMode = _blendMode.value.name,
            exposureStops = _exposureStops.value,
            opacity = 1f,
            maskPath = null
        )
        layerDao.insert(layer)
        Logger.d(TAG, "captured ${layer.id}")
    }

    fun setError(reason: String) {
        _state.value = SessionState.Error(reason)
    }

    fun reset() {
        _state.value = SessionState.Idle
    }

    fun markCameraStarted() {
        if (!cameraStarted) {
            cameraStarted = true
            _state.value = SessionState.LivePreview
            // 相机就绪: 同步 EC 范围 / zoom 范围, 把当前曝光和焦段推过去.
            refreshCameraCapabilities()
        }
    }

    fun isCameraStarted(): Boolean = cameraStarted

    override fun onCleared() {
        super.onCleared()
        cameraBridge.shutdown()
    }

    class Factory(
        private val container: AppContainer,
        private val projectId: String,
        private val contextProvider: () -> android.content.Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = CaptureViewModel(
            projectId = projectId,
            projectDao = container.database.projectDao(),
            layerDao = container.database.layerDao(),
            imageStore = container.imageStore,
            addLayerUseCase = container.addLayerUseCase,
            settingsRepository = container.settingsRepository,
            cameraFactory = { CameraFrameBridge(contextProvider()) }
        ) as T
    }

    companion object {
        private const val TAG = "CaptureViewModel"
    }
}