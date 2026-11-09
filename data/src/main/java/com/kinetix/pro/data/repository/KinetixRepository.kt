package com.kinetix.pro.data.repository

import android.content.Context
import com.kinetix.pro.core.model.DownloadFormat
import com.kinetix.pro.core.model.DownloadItem
import com.kinetix.pro.core.model.DownloadStatus
import com.kinetix.pro.core.model.EditProject
import com.kinetix.pro.data.local.KinetixDatabase
import com.kinetix.pro.data.local.KinetixTypeConverters
import com.kinetix.pro.data.local.entity.DownloadEntity
import com.kinetix.pro.data.local.entity.EditProjectEntity
import com.kinetix.pro.data.local.entity.MediaFileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class KinetixRepository(context: Context) {

    private val db = KinetixDatabase.getInstance(context)
    private val converters = KinetixTypeConverters()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // --- EDIT PROJECTS (DRAFTS) ---

    fun observeAllProjects(): Flow<List<EditProject>> {
        return db.editProjectDao().getAllProjects().map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getProjectById(id: String): EditProject? {
        return db.editProjectDao().getProjectById(id)?.toDomain()
    }

    suspend fun saveProject(project: EditProject) {
        db.editProjectDao().insertOrUpdate(project.toEntity())
    }

    suspend fun deleteProject(id: String) {
        db.editProjectDao().deleteById(id)
    }

    // --- DOWNLOADS ---

    fun observeAllDownloads(): Flow<List<DownloadItem>> {
        return db.downloadDao().getAllDownloads().map { list ->
            list.mapNotNull { entity ->
                runCatching { entity.toDomain() }.getOrNull()
            }
        }
    }

    suspend fun saveDownload(item: DownloadItem) {
        db.downloadDao().insertOrUpdate(item.toEntity())
    }

    suspend fun deleteDownload(id: String) {
        db.downloadDao().deleteById(id)
    }

    // --- MEDIA FILES ---

    fun observeAllMediaFiles(): Flow<List<MediaFileEntity>> {
        return db.mediaFileDao().getAllMediaFiles()
    }

    suspend fun saveMediaFile(mediaFile: MediaFileEntity) {
        db.mediaFileDao().insert(mediaFile)
    }

    // --- MAPPERS ---

    private fun EditProject.toEntity(): EditProjectEntity {
        return EditProjectEntity(
            id = id,
            name = name,
            sourceUri = sourceUri,
            sourceDurationMs = sourceDurationMs,
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
            clipsJson = converters.fromClipList(clips),
            activeClipIndex = activeClipIndex,
            canvasJson = converters.fromCanvasConfig(canvas),
            filterJson = converters.fromFilterConfig(filter),
            adjustmentJson = converters.fromAdjustmentParams(adjustment),
            textsJson = converters.fromTextOverlayList(texts),
            voiceJson = converters.fromVoiceConfig(voice),
            musicJson = converters.fromMusicConfig(music),
            speed = speed,
            createdAtEpochMs = createdAtEpochMs,
            lastModifiedEpochMs = lastModifiedEpochMs
        )
    }

    private fun EditProjectEntity.toDomain(): EditProject {
        return EditProject(
            id = id,
            name = name,
            sourceUri = sourceUri,
            sourceDurationMs = sourceDurationMs,
            sourceWidth = sourceWidth,
            sourceHeight = sourceHeight,
            clips = converters.toClipList(clipsJson),
            activeClipIndex = activeClipIndex,
            canvas = converters.toCanvasConfig(canvasJson),
            filter = converters.toFilterConfig(filterJson),
            adjustment = converters.toAdjustmentParams(adjustmentJson),
            texts = converters.toTextOverlayList(textsJson),
            voice = converters.toVoiceConfig(voiceJson),
            music = converters.toMusicConfig(musicJson),
            speed = speed,
            createdAtEpochMs = createdAtEpochMs,
            lastModifiedEpochMs = lastModifiedEpochMs
        )
    }

    private fun DownloadItem.toEntity(): DownloadEntity {
        return DownloadEntity(
            id = id,
            sourceUrl = sourceUrl,
            title = title,
            thumbnailUrl = thumbnailUrl,
            platform = platform,
            durationSeconds = durationSeconds,
            formatJson = json.encodeToString(format),
            status = status.name,
            progressPercent = progressPercent,
            bytesDownloaded = bytesDownloaded,
            totalBytes = totalBytes,
            speedBytesPerSec = speedBytesPerSec,
            etaSeconds = etaSeconds,
            localFilePath = localFilePath,
            mediaStoreUri = mediaStoreUri,
            errorMessage = errorMessage,
            createdAtEpochMs = createdAtEpochMs,
            completedAtEpochMs = completedAtEpochMs
        )
    }

    private fun DownloadEntity.toDomain(): DownloadItem {
        return DownloadItem(
            id = id,
            sourceUrl = sourceUrl,
            title = title,
            thumbnailUrl = thumbnailUrl,
            platform = platform,
            durationSeconds = durationSeconds,
            format = json.decodeFromString<DownloadFormat>(formatJson),
            status = runCatching { DownloadStatus.valueOf(status) }.getOrDefault(DownloadStatus.COMPLETED),
            progressPercent = progressPercent,
            bytesDownloaded = bytesDownloaded,
            totalBytes = totalBytes,
            speedBytesPerSec = speedBytesPerSec,
            etaSeconds = etaSeconds,
            localFilePath = localFilePath,
            mediaStoreUri = mediaStoreUri,
            errorMessage = errorMessage,
            createdAtEpochMs = createdAtEpochMs,
            completedAtEpochMs = completedAtEpochMs
        )
    }
}
