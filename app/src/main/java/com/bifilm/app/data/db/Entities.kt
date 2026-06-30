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
    val thumbnailPath: String?,
    /** 所属胶卷 id (FK by convention, 不加外键方便跨版本迁移). */
    val filmStockId: String = "foma_pan_100",
    /** 在该胶卷内的"张编号" (0..ROLL_CAPACITY). 0 表示空白画幅. */
    val frameIndexInRoll: Int = 1,
    /** 用户填写的拍摄事件说明 (生日/聚会/旅行). */
    val eventNote: String? = null
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
