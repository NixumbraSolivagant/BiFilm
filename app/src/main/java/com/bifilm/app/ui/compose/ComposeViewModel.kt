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
import com.bifilm.app.domain.usecase.AddLayerUseCase
import com.bifilm.app.domain.usecase.ComposeLayersUseCase
import com.bifilm.app.domain.usecase.RemoveLayerUseCase
import com.bifilm.app.render.engine.BlendModeRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    private val _selectedMode = MutableStateFlow(BlendMode.SCREEN)
    val selectedMode: StateFlow<BlendMode> = _selectedMode.asStateFlow()

    fun setBlendMode(mode: BlendMode) {
        _selectedMode.value = mode
    }

    fun availableModes(): List<BlendMode> = BlendModeRegistry.all()

    fun requestRecompose() {
        viewModelScope.launch {
            _isComposing.value = true
            try {
                _output.value = compose(projectId)
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