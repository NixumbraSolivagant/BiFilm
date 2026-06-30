package com.bifilm.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bifilm.app.data.db.ProjectDao
import com.bifilm.app.data.db.ProjectEntity
import com.bifilm.app.data.prefs.SettingsRepository
import com.bifilm.app.di.AppContainer
import com.bifilm.app.domain.model.FilmStock
import com.bifilm.app.domain.model.FilmStocks
import com.bifilm.app.domain.model.projectDisplayTitle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class HomeViewModel(
    private val projectDao: ProjectDao,
    private val settingsRepository: SettingsRepository,
    private val onProjectCreated: (String) -> Unit
) : ViewModel() {

    val projects: StateFlow<List<ProjectEntity>> =
        projectDao.observeAll().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * 扁平化的"首页卡片"列表 — 每个 ProjectEntity 展开成一张独立卡片,
     * 不再按胶卷分组. 排序: 最近编辑的在前.
     */
    val cards: StateFlow<List<HomeCardItem>> =
        projects.map { list ->
            list.sortedByDescending { it.updatedAt }.map { project ->
                HomeCardItem(
                    project = project,
                    stock = FilmStocks.byId(project.filmStockId)
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _pendingCreate = MutableStateFlow<CreateProjectModel?>(null)
    val pendingCreate: StateFlow<CreateProjectModel?> = _pendingCreate.asStateFlow()

    fun requestCreate() {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val stock = FilmStocks.byId(settings.defaultFilmStockId)
            val maxIdx = projectDao.maxFrameIndex(stock.id) ?: 0
            // 若上一张已经达到 38, 跳回 0 让用户开新卷 (但用户也能自己改回任意编号).
            val nextIdx = if (maxIdx >= FilmStocks.ROLL_CAPACITY) 0 else maxIdx + 1
            _pendingCreate.value = CreateProjectModel(
                stock = stock,
                frameIndexInRoll = nextIdx.coerceIn(FilmStocks.FRAME_RANGE),
                frameCount = settings.defaultFrameCount,
                eventNote = ""
            )
        }
    }

    fun updatePending(model: CreateProjectModel) {
        _pendingCreate.value = model
    }

    fun cancelPending() {
        _pendingCreate.value = null
    }

    fun confirmPending(onDone: (String) -> Unit) {
        val m = _pendingCreate.value ?: return
        _pendingCreate.value = null
        createProjectWith(m, onDone)
    }

    private fun createProjectWith(model: CreateProjectModel, onProjectCreated: (String) -> Unit) {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val coerced = model.frameCount.coerceIn(2, 8)
        val title = projectDisplayTitle(
            stock = model.stock,
            frameIndexInRoll = model.frameIndexInRoll,
            eventNote = model.eventNote.takeIf { it.isNotBlank() },
            createdAt = now
        )
        viewModelScope.launch {
            projectDao.upsert(
                ProjectEntity(
                    id = id,
                    title = title,
                    createdAt = now,
                    updatedAt = now,
                    frameCount = coerced,
                    frameWidth = 1080,
                    frameHeight = 1920,
                    thumbnailPath = null,
                    filmStockId = model.stock.id,
                    frameIndexInRoll = model.frameIndexInRoll,
                    eventNote = model.eventNote.takeIf { it.isNotBlank() }
                )
            )
            onProjectCreated(id)
        }
    }

    fun rename(project: ProjectEntity, newTitle: String) {
        viewModelScope.launch {
            projectDao.update(project.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
        }
    }

    fun touch(id: String) {
        viewModelScope.launch {
            projectDao.findById(id)?.let { existing ->
                projectDao.update(existing.copy(updatedAt = System.currentTimeMillis()))
            }
        }
    }

    fun delete(project: ProjectEntity) {
        viewModelScope.launch {
            projectDao.delete(project)
        }
    }

    class Factory(
        private val container: AppContainer,
        private val onProjectCreated: (String) -> Unit
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(
                projectDao = container.database.projectDao(),
                settingsRepository = container.settingsRepository,
                onProjectCreated = onProjectCreated
            ) as T
    }
}

/**
 * 创建项目前在 BottomSheet 上编辑的临时模型. 不持久化, 只是 UI 状态.
 */
data class CreateProjectModel(
    val stock: FilmStock,
    val frameIndexInRoll: Int,
    val frameCount: Int,
    val eventNote: String
) {
    companion object {
        val EMPTY = CreateProjectModel(
            stock = FilmStocks.FOMA_PAN_100,
            frameIndexInRoll = 1,
            frameCount = 2,
            eventNote = ""
        )
    }
}

/**
 * Home 一张独立卡片所需的全部展示数据 — 由 VM 预解析, UI 直接渲染.
 */
data class HomeCardItem(
    val project: ProjectEntity,
    val stock: FilmStock
)