package com.kinetix.pro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kinetix.pro.core.model.DownloadFormat
import com.kinetix.pro.core.model.DownloadItem
import com.kinetix.pro.core.model.DownloadStatus

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val sourceUrl: String,
    val title: String,
    val thumbnailUrl: String?,
    val platform: String,
    val durationSeconds: Long,
    val formatJson: String, // serialized DownloadFormat
    val status: String,     // DownloadStatus enum string
    val progressPercent: Int,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val speedBytesPerSec: Long,
    val etaSeconds: Long,
    val localFilePath: String?,
    val mediaStoreUri: String?,
    val errorMessage: String?,
    val createdAtEpochMs: Long,
    val completedAtEpochMs: Long?
)
