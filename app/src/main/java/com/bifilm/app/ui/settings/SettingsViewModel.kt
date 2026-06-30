package com.bifilm.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bifilm.app.data.prefs.BiFilmSettings
import com.bifilm.app.data.prefs.SettingsRepository
import com.bifilm.app.di.AppContainer
import com.bifilm.app.domain.model.ScenePresets
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repo: SettingsRepository
) : ViewModel() {

    /**
     * 包装一个非空 BiFilmSettings + loading 标志. 首帧之前 loading=true,
     * UI 看到 loading 时显示菊花; 之后只暴露有效的非空数据.
     */
    val state: StateFlow<SettingsUiState> = repo.settings
        .map { SettingsUiState(loading = false, settings = it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState.Initial)

    fun setBlendMode(modeName: String) = viewModelScope.launch {
        repo.setPreferredBlendMode(modeName)
    }

    fun setScene(id: String) = viewModelScope.launch {
        repo.setDefaultSceneId(id)
    }

    fun setExposureStops(stops: Float) = viewModelScope.launch {
        repo.setDefaultExposure(stops)
    }

    fun setFrameCount(n: Int) = viewModelScope.launch {
        repo.setDefaultFrameCount(n)
    }

    fun setFocalMm(mm: Int) = viewModelScope.launch {
        repo.setDefaultFocal(mm)
    }

    fun setFilmStockId(id: String) = viewModelScope.launch {
        repo.setDefaultFilmStock(id)
    }

    fun setMaskHardness(v: Int) = viewModelScope.launch {
        repo.setMaskHardness(v)
    }

    fun resetDefaults() = viewModelScope.launch {
        // 重置 = 把每个字段写回 DEFAULT 中的值.
        val d = SettingsRepository.DEFAULT
        repo.setPreferredBlendMode(d.preferredBlendMode)
        repo.setDefaultSceneId(d.defaultSceneId)
        repo.setDefaultExposure(d.defaultExposureStops)
        repo.setDefaultFrameCount(d.defaultFrameCount)
        repo.setDefaultFocal(d.defaultFocalMm)
        repo.setDefaultFilmStock(d.defaultFilmStockId)
        repo.setMaskHardness(d.maskHardness)
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(container.settingsRepository) as T
    }
}

data class SettingsUiState(
    val loading: Boolean,
    val settings: BiFilmSettings?
) {
    companion object {
        val Initial = SettingsUiState(loading = true, settings = null)
    }
}

/**
 * 设置页里要用到的可选常量.
 */
object SettingsOptions {
    /** 帧数可选值: 与 HomeViewModel.FRAME_COUNT_PRESETS 保持一致. */
    val FRAME_COUNTS = listOf(2, 3, 4, 6, 8)

    /** 焦段可选值 (mm). 与 ZoomPicker.mmPresets 保持一致. */
    val FOCAL_MM_OPTIONS = listOf(16, 24, 35, 45, 50, 85, 135)

    /** 曝光档可选值 (1/3 步长, ±3EV). */
    val EXPOSURE_OPTIONS = (-9..9).map { it / 3f }

    val SCENES = ScenePresets.ALL
}