package com.bifilm.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val frameCount: Int,
    val frameWidth: Int,
    val frameHeight: Int,
    val thumbnailPath: String?
)

@Entity(
    tableName = "layers",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class LayerEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val order: Int,
    val sourcePath: String,
    val blendMode: String,
    val exposureStops: Float,
    val opacity: Float,
    val maskPath: String?,
    val maskHardness: Int = 100
)
