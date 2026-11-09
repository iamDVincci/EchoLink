package com.kinetix.pro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "export_jobs")
data class ExportJobEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val clipIndex: Int,
    val totalClips: Int,
    val targetResolution: String,
    val targetBitrateKbps: Int,
    val targetFps: Int,
    val outputFilePath: String?,
    val mediaStoreUri: String?,
    val progressPercent: Int,
    val status: String, // QUEUED, RENDERING, COMPLETED, FAILED, CANCELLED
    val errorMessage: String?,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long?
)
