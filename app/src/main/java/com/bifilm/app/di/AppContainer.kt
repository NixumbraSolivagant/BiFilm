package com.bifilm.app.di

import android.content.Context
import com.bifilm.app.data.db.BiFilmDatabase
import com.bifilm.app.data.image.ImageStore
import com.bifilm.app.data.prefs.SettingsRepository
import com.bifilm.app.domain.usecase.AddLayerUseCase
import com.bifilm.app.domain.usecase.ComposeLayersUseCase
import com.bifilm.app.domain.usecase.ExportProjectUseCase
import com.bifilm.app.domain.usecase.ImportNegativeFrameUseCase
import com.bifilm.app.domain.usecase.RemoveLayerUseCase
import com.bifilm.app.domain.usecase.ReorderLayersUseCase
import com.bifilm.app.render.compose.BlendHostFactory
import com.bifilm.app.render.engine.BlendComposer

interface AppContainer {
    val database: BiFilmDatabase
    val imageStore: ImageStore
    val settingsRepository: SettingsRepository
    val blendComposer: BlendComposer

    val composeLayersUseCase: ComposeLayersUseCase
    val addLayerUseCase: AddLayerUseCase
    val removeLayerUseCase: RemoveLayerUseCase
    val reorderLayersUseCase: ReorderLayersUseCase
    val exportProjectUseCase: ExportProjectUseCase
    val importNegativeFrameUseCase: ImportNegativeFrameUseCase
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val appContext = context.applicationContext

    override val database: BiFilmDatabase by lazy { BiFilmDatabase.get(appContext) }
    override val imageStore: ImageStore by lazy { ImageStore(appContext) }
    override val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }
    override val blendComposer: BlendComposer by lazy {
        BlendComposer(BlendHostFactory.create(appContext))
    }
    override val composeLayersUseCase: ComposeLayersUseCase by lazy {
        ComposeLayersUseCase(database.layerDao(), blendComposer)
    }
    override val addLayerUseCase: AddLayerUseCase by lazy {
        AddLayerUseCase(database.layerDao(), imageStore)
    }
    override val removeLayerUseCase: RemoveLayerUseCase by lazy {
        RemoveLayerUseCase(database.layerDao())
    }
    override val reorderLayersUseCase: ReorderLayersUseCase by lazy {
        ReorderLayersUseCase(database.layerDao())
    }
    override val exportProjectUseCase: ExportProjectUseCase by lazy {
        ExportProjectUseCase(appContext, imageStore)
    }
    override val importNegativeFrameUseCase: ImportNegativeFrameUseCase by lazy {
        ImportNegativeFrameUseCase(database.layerDao())
    }
}
