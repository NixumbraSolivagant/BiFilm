package com.bifilm.app.ui.export

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bifilm.app.data.db.ProjectDao
import com.bifilm.app.di.AppContainer
import com.bifilm.app.domain.usecase.ComposeLayersUseCase
import com.bifilm.app.domain.usecase.ExportProjectUseCase
import com.bifilm.app.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExportViewModel(
    val projectId: String,
    private val projectDao: ProjectDao,
    private val compose: ComposeLayersUseCase,
    private val export: ExportProjectUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<ExportState>(ExportState.Idle)
    val state: StateFlow<ExportState> = _state.asStateFlow()

    private val _bitmap = MutableStateFlow<Bitmap?>(null)
    val bitmap: StateFlow<Bitmap?> = _bitmap.asStateFlow()

    fun prepare() {
        if (_state.value != ExportState.Idle) return
        viewModelScope.launch {
            _state.value = ExportState.Composing
            val composed = try {
                compose(projectId)
            } catch (t: Throwable) {
                Logger.e(TAG, "compose failed", t)
                null
            }
            if (composed == null) {
                _state.value = ExportState.Error("合成失败: 请确保至少有 1 张图层")
                return@launch
            }
            _bitmap.value = composed
            _state.value = ExportState.ReadyToExport
        }
    }

    fun exportToGallery() {
        val bmp = _bitmap.value ?: return
        if (_state.value == ExportState.Exporting) return
        viewModelScope.launch {
            _state.value = ExportState.Exporting
            try {
                val title = projectDao.findById(projectId)?.title ?: "bifilm"
                val uri = export.exportToGallery(bmp, title)
                if (uri != null) {
                    _state.value = ExportState.Done(uri.toString())
                } else {
                    _state.value = ExportState.Error("相册保存失败")
                }
            } catch (t: Throwable) {
                Logger.e(TAG, "export error", t)
                _state.value = ExportState.Error("导出出错: ${t.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        _bitmap.value?.recycle()
        _bitmap.value = null
    }

    sealed class ExportState {
        data object Idle : ExportState()
        data object Composing : ExportState()
        data object ReadyToExport : ExportState()
        data object Exporting : ExportState()
        data class Done(val uri: String) : ExportState()
        data class Error(val message: String) : ExportState()
    }

    class Factory(
        private val container: AppContainer,
        private val projectId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ExportViewModel(
            projectId = projectId,
            projectDao = container.database.projectDao(),
            compose = container.composeLayersUseCase,
            export = container.exportProjectUseCase
        ) as T
    }

    companion object {
        private const val TAG = "ExportViewModel"
    }
}