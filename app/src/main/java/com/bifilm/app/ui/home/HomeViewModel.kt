package com.bifilm.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bifilm.app.data.db.ProjectDao
import com.bifilm.app.data.db.ProjectEntity
import com.bifilm.app.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class HomeViewModel(
    private val projectDao: ProjectDao,
    private val onProjectCreated: (String) -> Unit
) : ViewModel() {

    val projects: StateFlow<List<ProjectEntity>> =
        projectDao.observeAll().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createProject() {
        createProjectWith(frameCount = DEFAULT_FRAME_COUNT)
    }

    fun createProjectWith(frameCount: Int, onProjectCreated: (String) -> Unit = ::onProjectCreatedInternal) {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val coerced = frameCount.coerceIn(2, 8)
        viewModelScope.launch {
            projectDao.upsert(
                ProjectEntity(
                    id = id,
                    title = "新项目 ${now % 10000}",
                    createdAt = now,
                    updatedAt = now,
                    frameCount = coerced,
                    frameWidth = 1080,
                    frameHeight = 1920,
                    thumbnailPath = null
                )
            )
            onProjectCreated(id)
        }
    }

    private fun onProjectCreatedInternal(id: String) = onProjectCreated(id)

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
            HomeViewModel(container.database.projectDao(), onProjectCreated) as T
    }

    companion object {
        val FRAME_COUNT_PRESETS = listOf(2, 3, 4, 6, 8)
        private const val DEFAULT_FRAME_COUNT = 2
    }
}