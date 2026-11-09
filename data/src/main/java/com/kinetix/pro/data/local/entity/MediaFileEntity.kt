package com.kinetix.pro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_files")
data class MediaFileEntity(
    @PrimaryKey val id: String,
    val title: String,
    val filePath: String,
    val mediaStoreUri: String?,
    val mediaType: String, // VIDEO, AUDIO, IMAGE
    val mimeType: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    val thumbnailUrl: String?,
    val platformOrigin: String?,
    val isFavorite: Boolean = false,
    val addedAtEpochMs: Long = System.currentTimeMillis()
)
