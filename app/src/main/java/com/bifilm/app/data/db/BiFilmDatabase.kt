package com.bifilm.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ProjectEntity::class, LayerEntity::class],
    version = 3,
    exportSchema = false
)
abstract class BiFilmDatabase : RoomDatabase() {

    abstract fun projectDao(): ProjectDao
    abstract fun layerDao(): LayerDao

    companion object {
        private const val DB_NAME = "bi_film.db"

        @Volatile
        private var INSTANCE: BiFilmDatabase? = null

        fun get(context: Context): BiFilmDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BiFilmDatabase::class.java,
                    DB_NAME
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
