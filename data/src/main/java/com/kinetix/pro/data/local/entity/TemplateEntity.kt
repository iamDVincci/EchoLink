package com.kinetix.pro.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val schemaVersion: Int,
    val coverFrameUrl: String?,
    val includesJson: String,
    val canvasJson: String,
    val filterJson: String,
    val adjustmentJson: String,
    val textsJson: String,
    val voiceJson: String,
    val musicJson: String,
    val speed: Float,
    val author: String,
    val isPresetBundled: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)
