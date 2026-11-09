package com.kinetix.pro.editor.export

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.kinetix.pro.core.model.ClipSection
import com.kinetix.pro.core.model.EditProject
import com.kinetix.pro.editor.effects.Media3VideoEffectsBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.pow

sealed class ExportProgressState {
    data class Progress(val clipIndex: Int, val totalClips: Int, val percent: Int) : ExportProgressState()
    data class ClipDone(val clipIndex: Int, val outputFile: File) : ExportProgressState()
    data class AllCompleted(val outputFiles: List<File>) : ExportProgressState()
    data class Failed(val error: String) : ExportProgressState()
}

class Media3ExportEngine(private val context: Context) {

    @OptIn(UnstableApi::class)
    fun exportProject(
        project: EditProject,
        outputDirectory: File
    ): Flow<ExportProgressState> = flow {
        if (project.sourceUri.isBlank()) {
            emit(ExportProgressState.Failed("No source video loaded in project"))
            return@flow
        }

        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs()
        }

        val clips = if (project.clips.isNotEmpty()) {
            project.clips
        } else {
            listOf(ClipSection(0L, project.sourceDurationMs.coerceAtLeast(1000L)))
        }
        val totalClips = clips.size
        val exportedFiles = mutableListOf<File>()

        val audioProcessors = mutableListOf<AudioProcessor>()
        val speedVal = project.speed.coerceIn(0.25f, 4.0f)
        val pitchRatio = (2.0.pow(project.voice.pitchSemitones.toDouble() / 12.0)).toFloat().coerceIn(0.5f, 2.0f)
        if (kotlin.math.abs(speedVal - 1.0f) > 0.02f || kotlin.math.abs(pitchRatio - 1.0f) > 0.02f) {
            val sonic = SonicAudioProcessor().apply {
                setSpeed(speedVal)
                setPitch(pitchRatio)
            }
            audioProcessors.add(sonic)
        }
        val isProjectMuted = project.music.originalVideoVolume <= 0.02f

        for (i in clips.indices) {
            val clip = clips[i]
            val clipVideoEffects = Media3VideoEffectsBuilder.buildEffects(
                project = project,
                clip = clip,
                isComparingOriginal = false,
                includePresentationAspectRatio = true
            )
            val isClipMuted = isProjectMuted || clip.isMuted || clip.volume <= 0.02f
            val partNum = String.format("%02d", i + 1)
            val baseName = project.name.replace(Regex("[^a-zA-Z0-9_-]"), "_").lowercase().ifBlank { "kinetix_edit" }
            val outputFile = File(outputDirectory, "${baseName}_part_${partNum}_${System.currentTimeMillis() % 10000}.mp4")

            emit(ExportProgressState.Progress(i + 1, totalClips, 0))

            val result = renderClipWithTransformer(
                sourceUri = project.sourceUri,
                clip = clip,
                audioProcessors = audioProcessors,
                removeAudio = isClipMuted,
                videoEffects = clipVideoEffects,
                outputFile = outputFile
            ) { currentPercent ->
                emit(ExportProgressState.Progress(i + 1, totalClips, currentPercent))
            }

            if (result.isSuccess) {
                exportedFiles.add(outputFile)
                emit(ExportProgressState.ClipDone(i + 1, outputFile))
            } else {
                emit(
                    ExportProgressState.Failed(
                        result.exceptionOrNull()?.localizedMessage ?: "Export failed on clip ${i + 1}"
                    )
                )
                return@flow
            }
        }

        emit(ExportProgressState.AllCompleted(exportedFiles))
    }

    @OptIn(UnstableApi::class)
    private suspend fun renderClipWithTransformer(
        sourceUri: String,
        clip: ClipSection,
        audioProcessors: List<AudioProcessor>,
        removeAudio: Boolean,
        videoEffects: List<androidx.media3.common.Effect>,
        outputFile: File,
        emitProgress: suspend (Int) -> Unit
    ): Result<File> {
        withContext(Dispatchers.IO) {
            if (outputFile.exists()) {
                outputFile.delete()
            }
        }

        val mediaItem = MediaItem.Builder()
            .setUri(sourceUri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(clip.startMs.coerceAtLeast(0L))
                    .setEndPositionMs(clip.endMs.takeIf { it > clip.startMs } ?: (clip.startMs + 15000L))
                    .build()
            )
            .build()

        val editedMediaItem = EditedMediaItem.Builder(mediaItem)
            .setRemoveAudio(removeAudio)
            .setEffects(Effects(audioProcessors, videoEffects))
            .build()

        var isDone = false
        var exportError: Throwable? = null

        return try {
            val transformer = withContext(Dispatchers.Main) {
                val listener = object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        isDone = true
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        isDone = true
                        exportError = exportException
                    }
                }

                Transformer.Builder(context)
                    .setVideoMimeType(MimeTypes.VIDEO_H264)
                    .setAudioMimeType(MimeTypes.AUDIO_AAC)
                    .addListener(listener)
                    .build()
                    .also { it.start(editedMediaItem, outputFile.absolutePath) }
            }

            val progressHolder = ProgressHolder()
            while (!isDone) {
                delay(120)
                val progress = withContext(Dispatchers.Main) {
                    val state = transformer.getProgress(progressHolder)
                    if (state == Transformer.PROGRESS_STATE_AVAILABLE) progressHolder.progress else -1
                }
                if (progress >= 0) {
                    emitProgress(progress)
                }
            }

            val error = exportError
            if (error != null) {
                Result.failure(error)
            } else if (outputFile.exists() && outputFile.length() > 0L) {
                Result.success(outputFile)
            } else {
                Result.failure(IllegalStateException("Rendered output file was empty"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
