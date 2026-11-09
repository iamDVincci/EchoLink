package com.kinetix.pro.core.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class DownloadStatus {
    QUEUED,
    CONNECTING,
    DOWNLOADING,
    PROCESSING_MUXING,
    COMPLETED,
    PAUSED,
    FAILED,
    CANCELLED
}

@Serializable
data class DownloadItem(
    val id: String = UUID.randomUUID().toString(),
    val sourceUrl: String,
    val title: String,
    val thumbnailUrl: String? = null,
    val platform: String = "Universal",
    val durationSeconds: Long = 0L,
    val format: DownloadFormat,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val progressPercent: Int = 0,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val etaSeconds: Long = 0L,
    val localFilePath: String? = null,
    val mediaStoreUri: String? = null,
    val errorMessage: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val completedAtEpochMs: Long? = null
)
