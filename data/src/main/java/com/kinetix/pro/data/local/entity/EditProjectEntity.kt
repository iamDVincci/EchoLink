package com.kinetix.pro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "edit_projects")
data class EditProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sourceUri: String,
    val sourceDurationMs: Long,
    val sourceWidth: Int,
    val sourceHeight: Int,
    val clipsJson: String,
    val activeClipIndex: Int,
    val canvasJson: String,
    val filterJson: String,
    val adjustmentJson: String,
    val textsJson: String,
    val voiceJson: String,
    val musicJson: String,
    val speed: Float,
    val createdAtEpochMs: Long,
    val lastModifiedEpochMs: Long
)
