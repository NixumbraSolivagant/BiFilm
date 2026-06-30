package com.bifilm.app.ui.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bifilm.app.data.db.LayerDao
import com.bifilm.app.data.db.LayerEntity
import com.bifilm.app.data.db.ProjectDao
import com.bifilm.app.data.db.ProjectEntity
import com.bifilm.app.data.image.ImageStore
import com.bifilm.app.di.AppContainer
import com.bifilm.app.domain.model.BlendMode
import com.bifilm.app.domain.usecase.AddLayerUseCase
import com.bifilm.app.render.camera.CameraFrameBridge
import com.bifilm.app.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    }

    private val _frameCount = MutableStateFlow(2)
    val frameCount: StateFlow<Int> = _frameCount.asStateFlow()

    private val _exposureStops = MutableStateFlow(0f)
    val exposureStops: StateFlow<Float> = _exposureStops.asStateFlow()

    private val _blendMode = MutableStateFlow(BlendMode.SCREEN)
    val blendMode: StateFlow<BlendMode> = _blendMode.asStateFlow()

    val cameraBridge: CameraFrameBridge = cameraFactory()
    private var cameraStarted = false

    fun setFrameCount(n: Int) {
        _frameCount.value = n.coerceIn(1, 9)
    }

    fun setExposure(stops: Float) {
        _exposureStops.value = stops
    }

    fun setBlendMode(mode: BlendMode) {
        _blendMode.value = mode
    }

    /** 用户按快门: 保存文件 + 加 layer. saveBitmap 由 UI 提供 (M3 占位实现). */
    fun shutter(saveBitmap: suspend () -> File?) {
        if (_state.value == SessionState.Capturing) return
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
            // 一次拍完一格; 张数自动减一, 到 0 触发新一轮 (由 UI 重置回 N).
            val next = (_frameCount.value - 1).coerceAtLeast(1)
            _frameCount.value = if (next == 1 && _frameCount.value > 1) {
                // 轮回结束: 重置为初始张数 (UI 应保持上一周期大小)
                _frameCount.value
            } else {
                next
            }
        }
    }

    /** 用户从相册挑了一张图, 走 AddLayerUseCase 加一层. */
    fun importImage(uri: android.net.Uri) {
        if (_state.value == SessionState.Capturing) return
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
            cameraFactory = { CameraFrameBridge(contextProvider()) }
        ) as T
    }

    companion object {
        private const val TAG = "CaptureViewModel"
    }
}