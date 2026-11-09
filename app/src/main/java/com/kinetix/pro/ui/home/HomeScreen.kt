package com.kinetix.pro.ui.home

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.kinetix.pro.KinetixApplication
import com.kinetix.pro.core.model.DownloadFormat
import com.kinetix.pro.core.model.DownloadItem
import com.kinetix.pro.core.model.DownloadStatus
import com.kinetix.pro.core.model.ExtractedMediaMetadata
import com.kinetix.pro.core.model.MediaType
import com.kinetix.pro.core.ui.components.ObsidianCard
import com.kinetix.pro.core.ui.components.ObsidianInnerCard
import com.kinetix.pro.core.ui.components.ObsidianUrlInput
import com.kinetix.pro.core.ui.components.PlatformBadge
import com.kinetix.pro.core.ui.components.RecommendedPill
import com.kinetix.pro.core.ui.components.SunsetIconButton
import com.kinetix.pro.core.ui.components.SunsetOutlinedButton
import com.kinetix.pro.core.ui.components.SunsetPrimaryButton
import com.kinetix.pro.core.ui.components.formatDuration
import com.kinetix.pro.core.ui.components.formatFileSize
import com.kinetix.pro.core.ui.theme.ContentHigh
import com.kinetix.pro.core.ui.theme.ContentMedium
import com.kinetix.pro.core.ui.theme.ObsidianCanvas
import com.kinetix.pro.core.ui.theme.ObsidianStroke
import com.kinetix.pro.core.ui.theme.ObsidianSurface1
import com.kinetix.pro.core.ui.theme.ObsidianSurface2
import com.kinetix.pro.core.ui.theme.PrimarySunset
import com.kinetix.pro.core.ui.theme.StatusError
import com.kinetix.pro.core.ui.theme.StatusSuccess
import com.kinetix.pro.core.ui.theme.SunsetTint15
import com.kinetix.pro.core.ui.theme.TertiaryAmber
import com.kinetix.pro.data.repository.KinetixRepository
import com.kinetix.pro.downloader.ChunkedHttpDownloader
import com.kinetix.pro.downloader.DownloadProgressUpdate
import com.kinetix.pro.downloader.DownloaderEngine
import com.kinetix.pro.downloader.ExtractionResult
import com.kinetix.pro.downloader.MediaStoreExporter
import com.kinetix.pro.downloader.StreamMuxer
import com.kinetix.pro.downloader.YtDlpNativeEngine
import com.kinetix.pro.service.DownloadForegroundService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private object DownloadSessionStore {
    private val lock = Any()
    private val activeJobs = mutableMapOf<String, Job>()
    private var hasHydratedFromDb = false

    var activeDownloads by mutableStateOf<List<DownloadItem>>(emptyList())
    var statusStageMap by mutableStateOf<Map<String, String>>(emptyMap())

    @Synchronized
    fun hasActiveDuplicate(sourceUrl: String, formatId: String): Boolean {
        val normUrl = sourceUrl.trim()
        return activeDownloads.any {
            it.status == DownloadStatus.DOWNLOADING &&
                it.sourceUrl.trim().equals(normUrl, ignoreCase = true) &&
                it.format.formatId == formatId
        }
    }

    @Synchronized
    fun upsertDownload(item: DownloadItem) {
        activeDownloads = listOf(item) + activeDownloads.filter { it.id != item.id }
    }

    @Synchronized
    fun updateDownload(itemId: String, transform: (DownloadItem) -> DownloadItem) {
        activeDownloads = activeDownloads.map { cur ->
            if (cur.id == itemId) transform(cur) else cur
        }
    }

    @Synchronized
    fun setStage(itemId: String, text: String) {
        statusStageMap = statusStageMap + (itemId to text)
    }

    @Synchronized
    fun registerJob(itemId: String, job: Job) {
        activeJobs[itemId] = job
    }

    @Synchronized
    fun unregisterJob(itemId: String) {
        activeJobs.remove(itemId)
    }

    @Synchronized
    fun cancelAndRemoveDownload(itemId: String) {
        activeJobs.remove(itemId)?.cancel()
        YtDlpNativeEngine.cancelProcess("dl_$itemId")
        activeDownloads = activeDownloads.filter { it.id != itemId }
        statusStageMap = statusStageMap - itemId
    }

    @Synchronized
    fun clearFinished(): List<DownloadItem> {
        val removed = activeDownloads.filter { it.status != DownloadStatus.DOWNLOADING }
        activeDownloads = activeDownloads.filter { it.status == DownloadStatus.DOWNLOADING }
        return removed
    }

    @Synchronized
    fun hydrateFromSaved(savedList: List<DownloadItem>) {
        if (hasHydratedFromDb) return
        hasHydratedFromDb = true
        val existingIds = activeDownloads.map { it.id }.toSet()
        val dedupedSaved = savedList
            .filter { it.id !in existingIds }
            .distinctBy { "${it.sourceUrl.trim().lowercase()}_${it.format.formatId}" }
        if (dedupedSaved.isNotEmpty()) {
            activeDownloads = activeDownloads + dedupedSaved
        }
    }
}

private val URL_EXTRACT_REGEX = Regex("""https?://[^\s,"'<>]+""", RegexOption.IGNORE_CASE)

private fun extractUrlsFromText(raw: String): List<String> {
    val matches = URL_EXTRACT_REGEX.findAll(raw).map { it.value.trim() }.filter { it.isNotBlank() }.distinct().toList()
    return if (matches.isNotEmpty()) matches else listOfNotNull(raw.trim().takeIf { it.isNotBlank() })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    sharedUrl: String? = null,
    onSharedUrlConsumed: () -> Unit = {},
    onNavigateToEditor: (projectId: String, videoUri: String?, title: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val downloaderEngine = remember { DownloaderEngine(context = context.applicationContext) }
    val chunkedDownloader = remember { ChunkedHttpDownloader() }
    val mediaStoreExporter = remember { MediaStoreExporter(context) }
    val repository = remember { KinetixRepository(context) }

    var urlInput by remember { mutableStateOf("") }
    var batchUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var detectedClipboardUrl by remember { mutableStateOf<String?>(null) }
    var isFetchingMetadata by remember { mutableStateOf(false) }
    var fetchErrorText by remember { mutableStateOf<String?>(null) }
    var extractionResult by remember { mutableStateOf<ExtractedMediaMetadata?>(null) }
    var showFormatPickerSheet by remember { mutableStateOf(false) }
    var selectedFormat by remember { mutableStateOf<DownloadFormat?>(null) }
    var previewingItem by remember { mutableStateOf<DownloadItem?>(null) }

    val activeDownloads = DownloadSessionStore.activeDownloads
    val statusStageMap = DownloadSessionStore.statusStageMap

    LaunchedEffect(Unit) {
        runCatching {
            repository.observeAllDownloads().collect { savedList ->
                val validSaved = savedList.filter { item ->
                    val path = item.localFilePath.orEmpty()
                    path.startsWith("file://") && File(Uri.parse(path).path.orEmpty()).let { it.exists() && it.length() > 0L }
                }
                if (validSaved.isNotEmpty()) {
                    DownloadSessionStore.hydrateFromSaved(validSaved)
                }
            }
        }
    }

    // Android System Photo/Video Picker for direct local studio import
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val localUriStr = withContext(Dispatchers.IO) {
                    try {
                        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
                        if (!dir.exists()) dir.mkdirs()
                        val importedFile = File(dir, "imported_studio_${System.currentTimeMillis()}.mp4")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(importedFile).use { output ->
                                input.copyTo(output, bufferSize = 64 * 1024)
                            }
                        }
                        if (importedFile.exists() && importedFile.length() > 0L) {
                            Uri.fromFile(importedFile).toString()
                        } else {
                            uri.toString()
                        }
                    } catch (_: Exception) {
                        uri.toString()
                    }
                }
                onNavigateToEditor("local_import_${System.currentTimeMillis()}", localUriStr, "Local Studio Video")
            }
        }
    }

    fun updateItemState(itemId: String, transform: (DownloadItem) -> DownloadItem) {
        DownloadSessionStore.updateDownload(itemId, transform)
    }

    fun setStageText(itemId: String, text: String) {
        DownloadSessionStore.setStage(itemId, text)
    }

    fun deleteDownloadPermanently(item: DownloadItem) {
        val appCtx = context.applicationContext
        DownloadSessionStore.cancelAndRemoveDownload(item.id)
        if (previewingItem?.id == item.id) {
            previewingItem = null
        }
        val cleanupScope = runCatching { KinetixApplication.instance.appScope }.getOrDefault(coroutineScope)
        cleanupScope.launch(Dispatchers.IO) {
            runCatching { repository.deleteDownload(item.id) }
            runCatching {
                mediaStoreExporter.deleteExportedMedia(
                    mediaStoreUriString = item.mediaStoreUri,
                    localFilePath = item.localFilePath
                )
            }
            if (DownloadSessionStore.activeDownloads.none { it.status == DownloadStatus.DOWNLOADING }) {
                runCatching { DownloadForegroundService.stop(appCtx) }
            }
        }
    }

    fun startRealDownload(item: DownloadItem, openInStudioWhenDone: Boolean = false, forceRefreshUrl: Boolean = false) {
        val appCtx = context.applicationContext
        if (!forceRefreshUrl && DownloadSessionStore.hasActiveDuplicate(item.sourceUrl, item.format.formatId)) {
            return
        }
        DownloadSessionStore.upsertDownload(item)
        setStageText(item.id, "Connecting to stream...")
        val runningCount = DownloadSessionStore.activeDownloads.count { it.status == DownloadStatus.DOWNLOADING }
        val serviceTitle = if (runningCount > 1) "Downloading $runningCount videos simultaneously..." else item.title
        runCatching { DownloadForegroundService.start(appCtx, serviceTitle, item.progressPercent) }

        val downloadScope = runCatching { KinetixApplication.instance.appScope }.getOrDefault(coroutineScope)
        val job = downloadScope.launch {
            val dir = appCtx.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: appCtx.filesDir
            if (!dir.exists()) dir.mkdirs()
            val safeName = item.title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(40).ifBlank { "kinetix_video" }
            val ext = item.format.extension.ifBlank { "mp4" }
            val destFile = File(dir, "${safeName}_${item.id.take(6)}.$ext")

            try {
                // If directUrl is missing or retrying a failed download, re-extract fresh stream URLs first
                var streamUrl = item.format.directUrl.orEmpty()
                if (streamUrl.isBlank() || forceRefreshUrl) {
                    setStageText(item.id, "Refreshing high-speed stream URL...")
                    val refreshed = downloaderEngine.extractMetadata(item.sourceUrl)
                    if (refreshed is ExtractionResult.Success) {
                        val targetHeight = item.format.height ?: 1080
                        val matchedFmt = refreshed.metadata.formats.firstOrNull {
                            it.type == item.format.type &&
                                (item.format.type != MediaType.VIDEO || it.height == targetHeight) &&
                                !it.directUrl.isNullOrBlank()
                        } ?: refreshed.metadata.formats.firstOrNull {
                            it.type == item.format.type && !it.directUrl.isNullOrBlank()
                        }
                        if (matchedFmt != null && !matchedFmt.directUrl.isNullOrBlank()) {
                            streamUrl = matchedFmt.directUrl!!
                            updateItemState(item.id) { cur ->
                                cur.copy(
                                    title = refreshed.metadata.title.ifBlank { cur.title },
                                    thumbnailUrl = refreshed.metadata.thumbnailUrl?.ifBlank { cur.thumbnailUrl } ?: cur.thumbnailUrl,
                                    durationSeconds = refreshed.metadata.durationSeconds,
                                    format = cur.format.copy(
                                        directUrl = streamUrl,
                                        estimatedSizeBytes = matchedFmt.estimatedSizeBytes
                                    )
                                )
                            }
                        }
                    }
                }

                var downloadSucceeded = false
                var failureReason = "Stream download failed"

                // Case 1: Adaptive / HLS high-res video + audio stream pair (e.g., YouTube 1080p/2K/4K HLS/MP4 + AAC/M4A audio, or Reddit video + audio)
                if (streamUrl.contains(DownloaderEngine.ADAPTIVE_MUX_SEPARATOR) && item.format.type == MediaType.VIDEO) {
                    val parts = streamUrl.split(DownloaderEngine.ADAPTIVE_MUX_SEPARATOR)
                    val videoUrl = parts.getOrNull(0).orEmpty()
                    val audioUrl = parts.getOrNull(1).orEmpty()
                    val isVideoHls = videoUrl.contains(".m3u8", ignoreCase = true) || videoUrl.contains("/manifest/hls_playlist/", ignoreCase = true)
                    val isAudioHls = audioUrl.contains(".m3u8", ignoreCase = true) || audioUrl.contains("/manifest/hls_playlist/", ignoreCase = true)
                    val tempVideo = File(dir, if (isVideoHls) "tmp_v_${item.id}.ts" else "tmp_v_${item.id}.mp4")
                    val tempAudio = File(dir, if (isAudioHls) "tmp_a_${item.id}.aac" else "tmp_a_${item.id}.m4a")

                    try {
                        setStageText(item.id, "Downloading HD video stream...")
                        var videoOk = false
                        chunkedDownloader.downloadFile(videoUrl, tempVideo).collect { update ->
                            when (update) {
                                is DownloadProgressUpdate.Progress -> {
                                    val scaledPct = (update.percent * 0.78f).toInt().coerceIn(2, 78)
                                    updateItemState(item.id) { cur ->
                                        cur.copy(
                                            status = DownloadStatus.DOWNLOADING,
                                            progressPercent = scaledPct,
                                            bytesDownloaded = update.bytesDownloaded,
                                            totalBytes = update.totalBytes,
                                            speedBytesPerSec = update.speedBytesPerSec,
                                            etaSeconds = update.etaSeconds
                                        )
                                    }
                                }
                                is DownloadProgressUpdate.Completed -> {
                                    videoOk = true
                                }
                                is DownloadProgressUpdate.Failed -> {
                                    failureReason = update.error
                                }
                            }
                        }

                        if (videoOk && audioUrl.startsWith("http")) {
                            setStageText(item.id, "Downloading audio track...")
                            chunkedDownloader.downloadFile(audioUrl, tempAudio).collect { update ->
                                if (update is DownloadProgressUpdate.Progress) {
                                    val scaledPct = 78 + (update.percent * 0.14f).toInt().coerceIn(0, 14)
                                    updateItemState(item.id) { cur ->
                                        cur.copy(
                                            progressPercent = scaledPct,
                                            speedBytesPerSec = update.speedBytesPerSec
                                        )
                                    }
                                }
                            }
                        }

                        if (videoOk && tempVideo.exists() && tempVideo.length() > 0L) {
                            setStageText(item.id, "Muxing HD video & audio...")
                            updateItemState(item.id) { cur -> cur.copy(progressPercent = 95) }
                            downloadSucceeded = StreamMuxer.muxVideoAndAudio(
                                videoFile = tempVideo,
                                audioFile = tempAudio,
                                outputFile = destFile,
                                context = appCtx
                            )
                        }
                    } catch (ce: CancellationException) {
                        throw ce
                    } catch (e: Exception) {
                        failureReason = e.localizedMessage ?: "Adaptive stream error"
                    } finally {
                        runCatching { if (tempVideo.exists()) tempVideo.delete() }
                        runCatching { if (tempAudio.exists()) tempAudio.delete() }
                    }
                }

                // Case 2: Direct or HLS Audio stream with local FFmpeg MP3/WAV/M4A conversion
                if (!downloadSucceeded && item.format.type == MediaType.AUDIO && streamUrl.startsWith("http")) {
                    val rawAudioUrl = streamUrl.substringBefore(DownloaderEngine.ADAPTIVE_MUX_SEPARATOR)
                    val isHlsAudio = rawAudioUrl.contains(".m3u8", ignoreCase = true) || rawAudioUrl.contains("/manifest/hls_playlist/", ignoreCase = true)
                    val tempAudioRaw = File(dir, if (isHlsAudio) "tmp_aud_${item.id}.aac" else "tmp_aud_${item.id}.m4a")
                    try {
                        setStageText(item.id, "Downloading high-bitrate audio stream...")
                        var rawOk = false
                        chunkedDownloader.downloadFile(rawAudioUrl, tempAudioRaw).collect { update ->
                            when (update) {
                                is DownloadProgressUpdate.Progress -> {
                                    val scaledPct = (update.percent * 0.85f).toInt().coerceIn(2, 85)
                                    updateItemState(item.id) { cur ->
                                        cur.copy(
                                            status = DownloadStatus.DOWNLOADING,
                                            progressPercent = scaledPct,
                                            bytesDownloaded = update.bytesDownloaded,
                                            totalBytes = update.totalBytes,
                                            speedBytesPerSec = update.speedBytesPerSec,
                                            etaSeconds = update.etaSeconds
                                        )
                                    }
                                }
                                is DownloadProgressUpdate.Completed -> {
                                    rawOk = true
                                }
                                is DownloadProgressUpdate.Failed -> {
                                    failureReason = update.error
                                }
                            }
                        }

                        if (rawOk && tempAudioRaw.exists() && tempAudioRaw.length() > 0L) {
                            setStageText(item.id, "Converting to ${ext.uppercase()} via FFmpeg...")
                            updateItemState(item.id) { cur -> cur.copy(progressPercent = 92) }
                            val converted = YtDlpNativeEngine.convertAudioWithFfmpeg(
                                context = appCtx,
                                sourceFile = tempAudioRaw,
                                outputFile = destFile,
                                targetExtension = ext
                            )
                            if (converted && destFile.exists() && destFile.length() > 0L) {
                                downloadSucceeded = true
                            } else if (ext.lowercase() == "m4a" || ext.lowercase() == "aac") {
                                tempAudioRaw.copyTo(destFile, overwrite = true)
                                downloadSucceeded = destFile.exists() && destFile.length() > 0L
                            }
                        }
                    } catch (ce: CancellationException) {
                        throw ce
                    } catch (e: Exception) {
                        failureReason = e.localizedMessage ?: "Audio stream error"
                    } finally {
                        runCatching { if (tempAudioRaw.exists()) tempAudioRaw.delete() }
                    }
                }

                // Case 3: Single direct HTTP video/image stream
                if (!downloadSucceeded && streamUrl.startsWith("http") && !streamUrl.contains(DownloaderEngine.ADAPTIVE_MUX_SEPARATOR) && item.format.type != MediaType.AUDIO) {
                    try {
                        setStageText(item.id, "Downloading direct stream...")
                        chunkedDownloader.downloadFile(streamUrl, destFile).collect { update ->
                            when (update) {
                                is DownloadProgressUpdate.Progress -> {
                                    updateItemState(item.id) { current ->
                                        current.copy(
                                            status = DownloadStatus.DOWNLOADING,
                                            progressPercent = update.percent.coerceIn(2, 99),
                                            bytesDownloaded = update.bytesDownloaded,
                                            totalBytes = update.totalBytes,
                                            speedBytesPerSec = update.speedBytesPerSec,
                                            etaSeconds = update.etaSeconds
                                        )
                                    }
                                }
                                is DownloadProgressUpdate.Completed -> {
                                    downloadSucceeded = true
                                }
                                is DownloadProgressUpdate.Failed -> {
                                    failureReason = update.error
                                }
                            }
                        }
                    } catch (ce: CancellationException) {
                        throw ce
                    } catch (e: Exception) {
                        failureReason = e.localizedMessage ?: "HTTP stream error"
                    }
                }

                // Case 4: Embedded yt-dlp + FFmpeg Universal Engine (handles 1,700+ sites, or fallback if direct URL expired)
                if (!downloadSucceeded) {
                    setStageText(item.id, "Extracting & merging via yt-dlp + FFmpeg...")
                    val ytdlpResult = YtDlpNativeEngine.downloadMedia(
                        context = appCtx,
                        sourceUrl = item.sourceUrl,
                        format = item.format,
                        destFile = destFile,
                        processId = "dl_${item.id}"
                    ) { pct, etaSec, line ->
                        val cleanStage = when {
                            line.contains("[Merger]", ignoreCase = true) || line.contains("Merging", ignoreCase = true) ->
                                "Merging video & audio via FFmpeg..."
                            line.contains("[ExtractAudio]", ignoreCase = true) ->
                                "Converting audio via FFmpeg..."
                            else -> "Downloading stream ($pct%)..."
                        }
                        setStageText(item.id, cleanStage)
                        updateItemState(item.id) { cur ->
                            cur.copy(
                                status = DownloadStatus.DOWNLOADING,
                                progressPercent = pct.coerceIn(3, 99),
                                etaSeconds = etaSec
                            )
                        }
                    }

                    if (ytdlpResult.isSuccess && destFile.exists() && destFile.length() > 0L) {
                        downloadSucceeded = true
                    } else {
                        failureReason = ytdlpResult.exceptionOrNull()?.localizedMessage ?: failureReason
                    }
                }

                // Finalize & Export to MediaStore (Movies/Kinetix Pro)
                if (downloadSucceeded && destFile.exists() && destFile.length() > 0L) {
                    setStageText(item.id, "Saving to Gallery...")
                    val exportedUri = mediaStoreExporter.exportToMediaStore(
                        sourceFile = destFile,
                        targetFileName = destFile.name,
                        mediaType = item.format.type
                    )
                    val localUriStr = Uri.fromFile(destFile).toString()
                    var completedItem: DownloadItem? = null
                    updateItemState(item.id) { current ->
                        current.copy(
                            status = DownloadStatus.COMPLETED,
                            progressPercent = 100,
                            bytesDownloaded = destFile.length(),
                            totalBytes = destFile.length(),
                            etaSeconds = 0L,
                            localFilePath = localUriStr,
                            mediaStoreUri = exportedUri?.toString(),
                            completedAtEpochMs = System.currentTimeMillis()
                        ).also { completedItem = it }
                    }
                    completedItem?.let { saved ->
                        runCatching { repository.saveDownload(saved) }
                    }

                    if (openInStudioWhenDone && item.format.type == MediaType.VIDEO) {
                        withContext(Dispatchers.Main) {
                            onNavigateToEditor("project_${item.id}", localUriStr, item.title)
                        }
                    }
                } else {
                    updateItemState(item.id) { current ->
                        current.copy(
                            status = DownloadStatus.FAILED,
                            progressPercent = 0,
                            errorMessage = failureReason.take(140)
                        )
                    }
                }
            } catch (_: CancellationException) {
                runCatching { if (destFile.exists()) destFile.delete() }
            } finally {
                DownloadSessionStore.unregisterJob(item.id)
                if (DownloadSessionStore.activeDownloads.none { it.status == DownloadStatus.DOWNLOADING }) {
                    runCatching { DownloadForegroundService.stop(appCtx) }
                }
            }
        }
        DownloadSessionStore.registerJob(item.id, job)
    }

    fun startBatchParallelDownloads(urls: List<String>, audioOnly: Boolean) {
        val distinctUrls = urls.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (distinctUrls.isEmpty()) return
        val downloadScope = runCatching { KinetixApplication.instance.appScope }.getOrDefault(coroutineScope)
        distinctUrls.forEach { targetUrl ->
            downloadScope.launch {
                val res = downloaderEngine.extractMetadata(targetUrl)
                if (res is ExtractionResult.Success) {
                    val chosenFormat = if (audioOnly) {
                        res.metadata.formats.firstOrNull { it.extension == "mp3" }
                            ?: res.metadata.formats.firstOrNull { it.isAudioOnly }
                    } else {
                        res.metadata.formats.firstOrNull { it.type == MediaType.VIDEO && it.isRecommended }
                            ?: res.metadata.formats.firstOrNull { it.type == MediaType.VIDEO }
                            ?: res.metadata.formats.firstOrNull()
                    }
                    if (chosenFormat != null) {
                        val newItem = DownloadItem(
                            sourceUrl = targetUrl,
                            title = res.metadata.title,
                            thumbnailUrl = res.metadata.thumbnailUrl,
                            platform = res.metadata.platform,
                            durationSeconds = res.metadata.durationSeconds,
                            format = chosenFormat,
                            status = DownloadStatus.DOWNLOADING,
                            progressPercent = 4
                        )
                        startRealDownload(newItem)
                    }
                }
            }
        }
    }

    val fetchMetadataAction: (String) -> Unit = { rawUrlToFetch ->
        val urls = (batchUrls + extractUrlsFromText(rawUrlToFetch)).distinct()
        if (urls.size > 1) {
            urlInput = ""
            batchUrls = emptyList()
            fetchErrorText = null
            startBatchParallelDownloads(urls, audioOnly = false)
        } else if (urls.size == 1) {
            val clean = urls.first()
            coroutineScope.launch {
                isFetchingMetadata = true
                fetchErrorText = null
                val result = downloaderEngine.extractMetadata(clean)
                isFetchingMetadata = false
                when (result) {
                    is ExtractionResult.Success -> {
                        extractionResult = result.metadata
                        selectedFormat = result.metadata.formats.firstOrNull { it.isRecommended }
                            ?: result.metadata.formats.firstOrNull()
                        showFormatPickerSheet = true
                    }
                    is ExtractionResult.Error -> {
                        fetchErrorText = result.message
                    }
                }
            }
        }
    }

    // Consume incoming Android Share Sheet URL automatically
    LaunchedEffect(sharedUrl) {
        if (!sharedUrl.isNullOrBlank()) {
            urlInput = sharedUrl
            fetchMetadataAction(sharedUrl)
            onSharedUrlConsumed()
        }
    }

    // Clipboard auto-detect
    LaunchedEffect(Unit) {
        runCatching {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clipData = clipboard?.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val text = clipData.getItemAt(0)?.text?.toString()?.trim()
                if (!text.isNullOrBlank() && (text.startsWith("http://") || text.startsWith("https://"))) {
                    detectedClipboardUrl = text
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianCanvas)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Kinetix Pro",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ContentHigh,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Universal Video Downloader & NLE Studio",
                        fontSize = 13.sp,
                        color = ContentMedium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                SunsetOutlinedButton(
                    text = "Import Video",
                    onClick = {
                        videoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                    icon = Icons.Default.VideoFile,
                    modifier = Modifier.height(40.dp)
                )
            }
        }

        // Paste Bar & Actions
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ObsidianUrlInput(
                    value = urlInput,
                    onValueChange = {
                        urlInput = it
                        fetchErrorText = null
                    },
                    onPasteClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
                        if (!clip.isNullOrBlank()) {
                            urlInput = clip
                            fetchMetadataAction(clip)
                        } else {
                            fetchErrorText = "Clipboard is empty. Copy a video link from YouTube, TikTok, Instagram, X, Facebook, or Reddit first."
                        }
                    },
                    onSubmit = {
                        if (urlInput.isNotBlank()) fetchMetadataAction(urlInput)
                    }
                )

                // Error Banner if link resolution failed
                if (fetchErrorText != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(StatusError.copy(alpha = 0.14f))
                            .border(1.dp, StatusError.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = StatusError,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = fetchErrorText!!,
                                color = ContentHigh,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Clipboard Auto-Detect Chip
                val currentClipboardUrl = detectedClipboardUrl
                if (currentClipboardUrl != null && urlInput.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SunsetTint15)
                            .border(1.dp, PrimarySunset.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable {
                                urlInput = currentClipboardUrl
                                fetchMetadataAction(currentClipboardUrl)
                                detectedClipboardUrl = null
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Clipboard Link: ",
                                    color = TertiaryAmber,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = currentClipboardUrl,
                                    color = ContentHigh,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "Fetch →",
                                color = PrimarySunset,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                // Multi-Link Batch Queue Chips (for downloading multiple videos at once)
                if (urlInput.isNotBlank() || batchUrls.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (urlInput.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ObsidianSurface2)
                                        .border(1.dp, PrimarySunset.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                        .clickable {
                                            val extracted = extractUrlsFromText(urlInput)
                                            if (extracted.isNotEmpty()) {
                                                batchUrls = (batchUrls + extracted).distinct()
                                                urlInput = ""
                                                fetchErrorText = null
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = PrimarySunset,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Add to Multi-Download Queue",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ContentHigh
                                    )
                                }
                            }
                            if (batchUrls.isNotEmpty()) {
                                Text(
                                    text = "Clear Queue (${batchUrls.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusError,
                                    modifier = Modifier.clickable { batchUrls = emptyList() }
                                )
                            }
                        }

                        if (batchUrls.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(batchUrls, key = { it }) { queuedUrl ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(SunsetTint15)
                                            .border(1.dp, PrimarySunset.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = queuedUrl.removePrefix("https://").removePrefix("http://").take(28),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ContentHigh,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove from queue",
                                            tint = ContentMedium,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { batchUrls = batchUrls.filter { it != queuedUrl } }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                val pendingUrlCount = (batchUrls + extractUrlsFromText(urlInput)).distinct().size
                val hasAnyUrlInput = urlInput.isNotBlank() || batchUrls.isNotEmpty()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SunsetPrimaryButton(
                        text = when {
                            isFetchingMetadata -> "Extracting Stream..."
                            pendingUrlCount > 1 -> "Download $pendingUrlCount Videos"
                            else -> "Fetch Video & Formats"
                        },
                        onClick = { fetchMetadataAction(urlInput) },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Download,
                        isLoading = isFetchingMetadata,
                        enabled = hasAnyUrlInput && !isFetchingMetadata
                    )

                    SunsetOutlinedButton(
                        text = if (pendingUrlCount > 1) "Save $pendingUrlCount MP3s" else "Save MP3",
                        onClick = {
                            val urls = (batchUrls + extractUrlsFromText(urlInput)).distinct()
                            if (urls.size > 1) {
                                urlInput = ""
                                batchUrls = emptyList()
                                fetchErrorText = null
                                startBatchParallelDownloads(urls, audioOnly = true)
                            } else if (urls.size == 1) {
                                val targetUrl = urls.first()
                                urlInput = ""
                                batchUrls = emptyList()
                                coroutineScope.launch {
                                    isFetchingMetadata = true
                                    fetchErrorText = null
                                    val result = downloaderEngine.extractMetadata(targetUrl)
                                    isFetchingMetadata = false
                                    when (result) {
                                        is ExtractionResult.Success -> {
                                            val mp3Format = result.metadata.formats.firstOrNull { it.extension == "mp3" }
                                                ?: result.metadata.formats.firstOrNull { it.isAudioOnly }
                                            if (mp3Format != null) {
                                                val newItem = DownloadItem(
                                                    sourceUrl = targetUrl,
                                                    title = result.metadata.title,
                                                    thumbnailUrl = result.metadata.thumbnailUrl,
                                                    platform = result.metadata.platform,
                                                    durationSeconds = result.metadata.durationSeconds,
                                                    format = mp3Format,
                                                    status = DownloadStatus.DOWNLOADING,
                                                    progressPercent = 4
                                                )
                                                startRealDownload(newItem)
                                            }
                                        }
                                        is ExtractionResult.Error -> {
                                            fetchErrorText = result.message
                                        }
                                    }
                                }
                            }
                        },
                        icon = Icons.Default.Audiotrack,
                        enabled = hasAnyUrlInput && !isFetchingMetadata
                    )
                }
            }
        }

        // Active & Completed Downloads Queue
        if (activeDownloads.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val runningCount = activeDownloads.count { it.status == DownloadStatus.DOWNLOADING }
                    Text(
                        text = if (runningCount > 0) {
                            "Downloads (${activeDownloads.size} • $runningCount active)"
                        } else {
                            "Downloads (${activeDownloads.size})"
                        },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = ContentHigh
                    )
                    if (activeDownloads.any { it.status == DownloadStatus.COMPLETED || it.status == DownloadStatus.FAILED }) {
                        Text(
                            text = "Clear Finished",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimarySunset,
                            modifier = Modifier.clickable {
                                DownloadSessionStore.clearFinished()
                            }
                        )
                    }
                }
            }

            items(activeDownloads, key = { it.id }) { item ->
                ObsidianCard(
                    modifier = Modifier.fillMaxWidth(),
                    isHighlighted = item.status == DownloadStatus.COMPLETED
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PlatformBadge(platformName = item.platform)
                                Text(
                                    text = item.format.resolutionLabel ?: item.format.extension.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ContentMedium
                                )
                            }
                            Text(
                                text = when (item.status) {
                                    DownloadStatus.COMPLETED -> "SAVED TO DEVICE"
                                    DownloadStatus.FAILED -> "FAILED"
                                    else -> "${item.progressPercent}%"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = when (item.status) {
                                    DownloadStatus.COMPLETED -> StatusSuccess
                                    DownloadStatus.FAILED -> StatusError
                                    else -> PrimarySunset
                                }
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (!item.thumbnailUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = item.thumbnailUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(width = 72.dp, height = 50.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ContentHigh,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val stageText = statusStageMap[item.id]
                                if (item.status == DownloadStatus.DOWNLOADING && !stageText.isNullOrBlank()) {
                                    Text(
                                        text = stageText,
                                        fontSize = 11.sp,
                                        color = TertiaryAmber,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }

                        if (item.status != DownloadStatus.FAILED) {
                            LinearProgressIndicator(
                                progress = { item.progressPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = if (item.status == DownloadStatus.COMPLETED) StatusSuccess else PrimarySunset,
                                trackColor = ObsidianSurface2,
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (item.status) {
                                    DownloadStatus.COMPLETED ->
                                        "Movies/Kinetix Pro • ${formatFileSize(item.totalBytes)}"
                                    DownloadStatus.FAILED ->
                                        item.errorMessage ?: "Could not download stream"
                                    else -> {
                                        val spd = if (item.speedBytesPerSec > 0L) "${formatFileSize(item.speedBytesPerSec)}/s • " else ""
                                        val eta = if (item.etaSeconds > 0L) "ETA ${item.etaSeconds}s" else "Streaming..."
                                        "$spd$eta"
                                    }
                                },
                                fontSize = 11.sp,
                                color = if (item.status == DownloadStatus.FAILED) StatusError else ContentMedium,
                                modifier = Modifier.weight(1f)
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                when (item.status) {
                                    DownloadStatus.COMPLETED -> {
                                        SunsetIconButton(
                                            icon = Icons.Default.PlayArrow,
                                            onClick = { previewingItem = item },
                                            tint = ContentHigh,
                                            size = 36
                                        )
                                        SunsetIconButton(
                                            icon = Icons.Default.Share,
                                            onClick = {
                                                runCatching {
                                                    val path = Uri.parse(item.localFilePath.orEmpty()).path
                                                    val file = if (path != null) File(path) else null
                                                    if (file != null && file.exists()) {
                                                        val shareUri = FileProvider.getUriForFile(
                                                            context,
                                                            "${context.packageName}.fileprovider",
                                                            file
                                                        )
                                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                                            type = if (item.format.type == MediaType.AUDIO) "audio/*" else "video/*"
                                                            putExtra(Intent.EXTRA_STREAM, shareUri)
                                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                        }
                                                        context.startActivity(Intent.createChooser(intent, "Share Media"))
                                                    }
                                                }
                                            },
                                            tint = ContentMedium,
                                            size = 36
                                        )
                                        SunsetIconButton(
                                            icon = Icons.Default.Delete,
                                            onClick = { deleteDownloadPermanently(item) },
                                            tint = StatusError,
                                            size = 36
                                        )
                                        if (item.format.type == MediaType.VIDEO) {
                                            SunsetPrimaryButton(
                                                text = "Edit in Studio",
                                                onClick = {
                                                    onNavigateToEditor(
                                                        "project_${item.id}",
                                                        item.localFilePath,
                                                        item.title
                                                    )
                                                },
                                                icon = Icons.Default.Edit,
                                                modifier = Modifier.height(36.dp)
                                            )
                                        }
                                    }
                                    DownloadStatus.FAILED -> {
                                        SunsetOutlinedButton(
                                            text = "Retry",
                                            onClick = {
                                                startRealDownload(
                                                    item = item.copy(
                                                        status = DownloadStatus.DOWNLOADING,
                                                        progressPercent = 4,
                                                        errorMessage = null
                                                    ),
                                                    forceRefreshUrl = true
                                                )
                                            },
                                            icon = Icons.Default.Refresh,
                                            modifier = Modifier.height(34.dp)
                                        )
                                        IconButton(
                                            onClick = { deleteDownloadPermanently(item) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusError)
                                        }
                                    }
                                    else -> {
                                        IconButton(
                                            onClick = { DownloadSessionStore.cancelAndRemoveDownload(item.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Cancel",
                                                tint = ContentMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Supported Platforms & Studio Import Card
        item {
            ObsidianCard(
                modifier = Modifier.fillMaxWidth(),
                isHighlighted = true
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pro NLE Video Studio",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ContentHigh
                            )
                            Text(
                                text = "Import any video from your phone or edit downloaded videos with frame timeline, split, trim, LUTs, audio mix & 4K export",
                                fontSize = 12.sp,
                                color = ContentMedium
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        SunsetPrimaryButton(
                            text = "Pick Video",
                            onClick = {
                                videoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            icon = Icons.Default.Movie,
                            modifier = Modifier.height(38.dp)
                        )
                    }

                    ObsidianInnerCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Powered by Native Stream Engine + Embedded yt-dlp & FFmpeg (1,700+ Sites)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ContentMedium
                            )
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(
                                    listOf(
                                        "YouTube & Shorts",
                                        "TikTok (No Watermark)",
                                        "Instagram Reels",
                                        "X / Twitter",
                                        "Facebook",
                                        "Reddit",
                                        "Pinterest",
                                        "Vimeo",
                                        "Twitch",
                                        "SoundCloud"
                                    )
                                ) {
                                    PlatformBadge(platformName = it)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // In-App Media Player Preview Modal
    if (previewingItem != null) {
        val mediaItem = previewingItem!!
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val previewPlayer = remember(mediaItem.localFilePath) {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(mediaItem.localFilePath.orEmpty()))
                prepare()
                playWhenReady = true
            }
        }
        DisposableEffect(previewPlayer) {
            onDispose { previewPlayer.release() }
        }

        ModalBottomSheet(
            onDismissRequest = { previewingItem = null },
            sheetState = sheetState,
            containerColor = ObsidianSurface1
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = mediaItem.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ContentHigh,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(9f / 16f)
                        .height(360.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = true
                                player = previewPlayer
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (mediaItem.format.type == MediaType.VIDEO) {
                        SunsetPrimaryButton(
                            text = "Open in Studio Editor",
                            onClick = {
                                val targetUri = mediaItem.localFilePath
                                val title = mediaItem.title
                                val id = mediaItem.id
                                previewingItem = null
                                onNavigateToEditor("project_$id", targetUri, title)
                            },
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Edit
                        )
                    }
                    SunsetOutlinedButton(
                        text = "Delete",
                        onClick = { deleteDownloadPermanently(mediaItem) },
                        icon = Icons.Default.Delete
                    )
                    SunsetOutlinedButton(
                        text = "Close",
                        onClick = { previewingItem = null }
                    )
                }
            }
        }
    }

    // Format Picker Bottom Sheet
    if (showFormatPickerSheet && extractionResult != null) {
        val meta = extractionResult!!
        var selectedTabIndex by remember { mutableIntStateOf(0) }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showFormatPickerSheet = false },
            sheetState = sheetState,
            containerColor = ObsidianSurface1,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .width(42.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(ObsidianStroke)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Media preview card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!meta.thumbnailUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = meta.thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(width = 96.dp, height = 68.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PlatformBadge(platformName = meta.platform)
                        Text(
                            text = meta.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = ContentHigh,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${meta.uploader ?: "Creator"} • ${formatDuration(meta.durationSeconds)}",
                            fontSize = 12.sp,
                            color = ContentMedium
                        )
                    }
                }

                // Tabs: Video, Audio, Image
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = ObsidianSurface2,
                    contentColor = PrimarySunset,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = PrimarySunset
                        )
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, ObsidianStroke, RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = {
                            selectedTabIndex = 0
                            selectedFormat = meta.formats.firstOrNull { it.type == MediaType.VIDEO && it.isRecommended }
                                ?: meta.formats.firstOrNull { it.type == MediaType.VIDEO }
                        },
                        text = { Text("Video", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        icon = { Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = {
                            selectedTabIndex = 1
                            selectedFormat = meta.formats.firstOrNull { it.type == MediaType.AUDIO && it.isRecommended }
                                ?: meta.formats.firstOrNull { it.type == MediaType.AUDIO }
                        },
                        text = { Text("Audio", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        icon = { Icon(Icons.Default.Audiotrack, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedTabIndex == 2,
                        onClick = {
                            selectedTabIndex = 2
                            selectedFormat = meta.formats.firstOrNull { it.type == MediaType.IMAGE }
                        },
                        text = { Text("Image", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                        icon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                val currentType = when (selectedTabIndex) {
                    0 -> MediaType.VIDEO
                    1 -> MediaType.AUDIO
                    else -> MediaType.IMAGE
                }
                val filteredFormats = meta.formats.filter { it.type == currentType }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredFormats) { format ->
                        val isSelected = selectedFormat?.formatId == format.formatId
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) SunsetTint15 else ObsidianSurface2)
                                .border(
                                    if (isSelected) 1.5.dp else 1.dp,
                                    if (isSelected) PrimarySunset else ObsidianStroke,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedFormat = format }
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = format.resolutionLabel ?: "${format.extension.uppercase()} (${format.audioBitrateKbps} kbps)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) PrimarySunset else ContentHigh
                                        )
                                        if (format.isRecommended) {
                                            RecommendedPill()
                                        }
                                    }
                                    Text(
                                        text = "${format.extension.uppercase()} • ${format.vcodec ?: format.acodec ?: "H.264/AAC"} • ${format.fps} fps",
                                        fontSize = 11.sp,
                                        color = ContentMedium
                                    )
                                }

                                Text(
                                    text = formatFileSize(format.estimatedSizeBytes),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ContentHigh
                                )
                            }
                        }
                    }
                }

                // Action buttons: Download to Device OR Download & Open in Studio
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SunsetPrimaryButton(
                        text = "Download",
                        onClick = {
                            val chosen = selectedFormat ?: meta.formats.firstOrNull()
                            if (chosen != null) {
                                val item = DownloadItem(
                                    sourceUrl = meta.url,
                                    title = meta.title,
                                    thumbnailUrl = meta.thumbnailUrl,
                                    platform = meta.platform,
                                    durationSeconds = meta.durationSeconds,
                                    format = chosen,
                                    status = DownloadStatus.DOWNLOADING,
                                    progressPercent = 4
                                )
                                showFormatPickerSheet = false
                                urlInput = ""
                                startRealDownload(item, openInStudioWhenDone = false)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Download
                    )

                    if (currentType == MediaType.VIDEO) {
                        SunsetOutlinedButton(
                            text = "Download & Edit",
                            onClick = {
                                val chosen = selectedFormat ?: meta.formats.firstOrNull { it.type == MediaType.VIDEO }
                                if (chosen != null) {
                                    val item = DownloadItem(
                                        sourceUrl = meta.url,
                                        title = meta.title,
                                        thumbnailUrl = meta.thumbnailUrl,
                                        platform = meta.platform,
                                        durationSeconds = meta.durationSeconds,
                                        format = chosen,
                                        status = DownloadStatus.DOWNLOADING,
                                        progressPercent = 4
                                    )
                                    showFormatPickerSheet = false
                                    urlInput = ""
                                    startRealDownload(item, openInStudioWhenDone = true)
                                }
                            },
                            icon = Icons.Default.Edit
                        )
                    }
                }
            }
        }
    }
}
