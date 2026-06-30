package com.bifilm.app.ui.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bifilm.app.data.db.LayerDao
import com.bifilm.app.data.db.LayerEntity
import com.bifilm.app.data.db.ProjectDao
import com.bifilm.app.data.db.ProjectEntity
import com.bifilm.app.di.AppContainer
import com.bifilm.app.domain.model.BlendMode
import com.bifilm.app.domain.model.ScenePreset
import com.bifilm.app.domain.model.ScenePresets
import com.bifilm.app.domain.usecase.AddLayerUseCase
import com.bifilm.app.domain.usecase.ComposeLayersUseCase
import com.bifilm.app.domain.usecase.RemoveLayerUseCase
import com.bifilm.app.render.engine.BlendModeRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ComposeViewModel(
    val projectId: String,
    private val projectDao: ProjectDao,
    private val layerDao: LayerDao,
    private val compose: ComposeLayersUseCase,
    private val addLayer: AddLayerUseCase,
    private val removeLayer: RemoveLayerUseCase
) : ViewModel() {

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

    private val _output = MutableStateFlow<android.graphics.Bitmap?>(null)
    val output: StateFlow<android.graphics.Bitmap?> = _output.asStateFlow()

    private val _isComposing = MutableStateFlow(false)
    val isComposing: StateFlow<Boolean> = _isComposing.asStateFlow()

    /**
     * 当前选中的"场景"——用户视角的预设, 不是底层 BlendMode.
     * 选场景会自动把 mode 写到每张 layer (后端仍然按权威公式合成).
     */
    private val _scene = MutableStateFlow(ScenePresets.default())
    val scene: StateFlow<ScenePreset> = _scene.asStateFlow()

    /**
     * 黑白开关: true = 输出灰度 (Luminosity 0.299R + 0.587G + 0.114B).
     */
    private val _monochrome = MutableStateFlow(false)
    val monochrome: StateFlow<Boolean> = _monochrome.asStateFlow()

    /** 兼容旧 UI 引用: 把 scene 映射为底层 BlendMode. */
    val selectedMode: StateFlow<BlendMode> = _scene
        .map { it.mode }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _scene.value.mode)

    val availableModes: List<BlendMode> = BlendModeRegistry.all()
    val availableScenes: List<ScenePreset> = ScenePresets.ALL

    fun setScene(preset: ScenePreset) {
        if (_scene.value == preset) return
        _scene.value = preset
        viewModelScope.launch {
            val current = layers.value
            val updates = current.filter {
                it.blendMode != preset.mode.name ||
                    it.exposureStops != preset.suggestedExposureStops
            }
            if (updates.isNotEmpty()) {
                for (l in updates) {
                    layerDao.update(
                        l.copy(
                            blendMode = preset.mode.name,
                            exposureStops = preset.suggestedExposureStops
                        )
                    )
                }
            }
            // 关键: 写完 DB 后立刻合成一次. 不依赖 LaunchedEffect (layers.size 不会变).
            // 即便 layers 为空也调一次, use case 内部会快速返回 null (no layers).
            requestRecompose()
        }
    }

    fun setMonochrome(enabled: Boolean) {
        if (_monochrome.value == enabled) return
        _monochrome.value = enabled
        requestRecompose()
    }

    fun setBlendMode(mode: BlendMode) {
        val matched = ScenePresets.ALL.firstOrNull { it.mode == mode } ?: ScenePresets.SILHOUETTE_TEXTURE
        setScene(matched)
    }

    private var recomposeJob: kotlinx.coroutines.Job? = null

    fun requestRecompose() {
        recomposeJob?.cancel()
        recomposeJob = viewModelScope.launch {
            _isComposing.value = true
            try {
                // decode + blend 全部在 Default 线程执行, 不占主线程.
                _output.value = withContext(Dispatchers.Default) {
                    compose(projectId, _monochrome.value)
                }
            } finally {
                _isComposing.value = false
            }
        }
    }

    fun addLayerFromUri(uri: android.net.Uri) {
        viewModelScope.launch {
            addLayer(projectId, uri)
            requestRecompose()
        }
    }

    fun remove(layer: LayerEntity) {
        viewModelScope.launch {
            removeLayer(layer.id)
            requestRecompose()
        }
    }

    fun updateLayer(layer: LayerEntity, modeName: String? = null, exposure: Float? = null, opacity: Float? = null) {
        viewModelScope.launch {
            val updated = layer.copy(
                blendMode = modeName ?: layer.blendMode,
                exposureStops = exposure ?: layer.exposureStops,
                opacity = opacity ?: layer.opacity
            )
            layerDao.update(updated)
            requestRecompose()
        }
    }

    override fun onCleared() {
        super.onCleared()
        _output.value?.recycle()
        _output.value = null
    }

    class Factory(
        private val container: AppContainer,
        private val projectId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ComposeViewModel(
            projectId = projectId,
            projectDao = container.database.projectDao(),
            layerDao = container.database.layerDao(),
            compose = container.composeLayersUseCase,
            addLayer = container.addLayerUseCase,
            removeLayer = container.removeLayerUseCase
        ) as T
    }
}
