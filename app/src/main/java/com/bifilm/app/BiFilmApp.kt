package com.bifilm.app

import android.app.Application
import com.bifilm.app.data.db.BiFilmDatabase
import com.bifilm.app.data.image.ImageStore
import com.bifilm.app.di.AppContainer
import com.bifilm.app.di.DefaultAppContainer

class BiFilmApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        com.bifilm.app.util.Logger.d("App", "BiFilmApp onCreate")
        container = DefaultAppContainer(this)
    }
}
