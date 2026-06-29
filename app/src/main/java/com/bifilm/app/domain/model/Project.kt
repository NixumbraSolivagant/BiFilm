package com.bifilm.app.domain.model

import java.util.UUID

data class Project(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val frameCount: Int,
    val frameWidth: Int,
    val frameHeight: Int,
    val thumbnailPath: String? = null
)

data class Layer(
    val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val order: Int,
    val sourcePath: String,
    val blendMode: BlendMode = BlendMode.SCREEN,
    val exposureStops: Float = 0f,
    val opacity: Float = 1f,
    val maskPath: String? = null,
    val maskHardness: Int = 100
)
