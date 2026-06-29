package com.bifilm.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bifilm_settings")

data class BiFilmSettings(
    val preferredBlendMode: String,
    val defaultExposureStops: Float,
    val defaultFrameCount: Int,
    val maskHardness: Int,
    val exportMaxLongEdgePx: Int
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val BlendMode = stringPreferencesKey("preferred_blend_mode")
        val Exposure = floatPreferencesKey("default_exposure_stops")
        val FrameCount = intPreferencesKey("default_frame_count")
        val MaskHardness = intPreferencesKey("mask_hardness")
        val ExportMaxEdge = intPreferencesKey("export_max_edge")
    }

    val settings: Flow<BiFilmSettings> = context.dataStore.data.map { prefs ->
        BiFilmSettings(
            preferredBlendMode = prefs[Keys.BlendMode] ?: DEFAULT.preferredBlendMode,
            defaultExposureStops = prefs[Keys.Exposure] ?: DEFAULT.defaultExposureStops,
            defaultFrameCount = prefs[Keys.FrameCount] ?: DEFAULT.defaultFrameCount,
            maskHardness = prefs[Keys.MaskHardness] ?: DEFAULT.maskHardness,
            exportMaxLongEdgePx = prefs[Keys.ExportMaxEdge] ?: DEFAULT.exportMaxLongEdgePx
        )
    }

    suspend fun setPreferredBlendMode(mode: String) {
        context.dataStore.edit { it[Keys.BlendMode] = mode }
    }

    suspend fun setDefaultExposure(stops: Float) {
        context.dataStore.edit { it[Keys.Exposure] = stops }
    }

    suspend fun setDefaultFrameCount(count: Int) {
        context.dataStore.edit { it[Keys.FrameCount] = count }
    }

    suspend fun setMaskHardness(value: Int) {
        context.dataStore.edit { it[Keys.MaskHardness] = value.coerceIn(0, 100) }
    }

    companion object {
        val DEFAULT = BiFilmSettings(
            preferredBlendMode = "SCREEN",
            defaultExposureStops = 0f,
            defaultFrameCount = 2,
            maskHardness = 70,
            exportMaxLongEdgePx = 1080
        )
    }
}
