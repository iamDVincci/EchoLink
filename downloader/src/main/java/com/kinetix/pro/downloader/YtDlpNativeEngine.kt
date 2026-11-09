package com.kinetix.pro.downloader

import android.content.Context
import com.kinetix.pro.core.model.AudioFormat
import com.kinetix.pro.core.model.DownloadFormat
import com.kinetix.pro.core.model.ExtractedMediaMetadata
import com.kinetix.pro.core.model.MediaType
import com.kinetix.pro.core.model.VideoResolution
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Production Android wrapper around embedded `yt-dlp` ([YoutubeDL]) and [FFmpeg].
 * Supports extracting and downloading real video & audio streams from 1,700+ websites
 * (YouTube, TikTok, Instagram, X/Twitter, Facebook, Reddit, Pinterest, Vimeo, Twitch, SoundCloud, etc.)
 * directly on the device without external proxy dependencies.
 */
object YtDlpNativeEngine {

    private val initMutex = Mutex()
    private val updateMutex = Mutex()

    @Volatile
    private var isInitialized = false

    @Volatile
    private var hasUpdatedThisSession = false

    suspend fun ensureInitialized(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext true
        initMutex.withLock {
            if (isInitialized) return@withContext true
            try {
                val appCtx = context.applicationContext
                YoutubeDL.getInstance().init(appCtx)
                runCatching { FFmpeg.getInstance().init(appCtx) }
                isInitialized = true
                true
            } catch (_: Throwable) {
                false
            }
        }
    }

    suspend fun updateYtDlpIfPossible(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (!ensureInitialized(context)) return@withContext false
        if (hasUpdatedThisSession) return@withContext true
        updateMutex.withLock {
            if (hasUpdatedThisSession) return@withContext true
            try {
                YoutubeDL.getInstance().updateYoutubeDL(
                    context.applicationContext,
                    YoutubeDL.UpdateChannel.STABLE
                )
                hasUpdatedThisSession = true
                true
            } catch (_: Throwable) {
                false
            }
        }
    }

    fun cancelProcess(processId: String) {
        runCatching {
            YoutubeDL.getInstance().destroyProcessById(processId)
        }
    }

    /**
     * Resolves the unpacked native `ffmpeg` binary and its `usr/lib` directory from `youtubedl-android`.
     */
    private fun findUnpackedFfmpegBinary(context: Context): Pair<File, File>? {
        val baseDir = File(context.applicationContext.noBackupFilesDir, "youtubedl-android/packages/ffmpeg")
        val bin = File(baseDir, "usr/bin/ffmpeg")
        val libDir = File(baseDir, "usr/lib")
        return if (bin.exists() && bin.canExecute()) bin to libDir else null
    }

    /**
     * Losslessly muxes video (`.ts` or `.mp4`) and audio (`.aac`, `.m4a`, or `.ts`) into `.mp4`
     * using the embedded `ffmpeg` binary.
     */
    suspend fun muxStreamsWithFfmpeg(
        context: Context,
        videoFile: File,
        audioFile: File,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        if (!ensureInitialized(context)) return@withContext false
        val (ffmpegBin, libDir) = findUnpackedFfmpegBinary(context) ?: return@withContext false
        try {
            if (outputFile.exists()) outputFile.delete()
            val cmd = listOf(
                ffmpegBin.absolutePath,
                "-y",
                "-i", videoFile.absolutePath,
                "-i", audioFile.absolutePath,
                "-c:v", "copy",
                "-c:a", "aac",
                "-b:a", "192k",
                "-movflags", "+faststart",
                "-shortest",
                outputFile.absolutePath
            )
            val pb = ProcessBuilder(cmd)
            pb.environment()["LD_LIBRARY_PATH"] = libDir.absolutePath
            pb.redirectErrorStream(true)
            val proc = pb.start()
            proc.inputStream.bufferedReader().use { it.readText() }
            val finished = proc.waitFor(90, TimeUnit.SECONDS)
            if (!finished) {
                proc.destroyForcibly()
                return@withContext false
            }
            outputFile.exists() && outputFile.length() > 0L
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Converts a downloaded audio stream (`.aac`, `.m4a`, `.ts`, or `.mp4`) into `.mp3`, `.m4a`, or `.wav`
     * using the embedded `ffmpeg` binary.
     */
    suspend fun convertAudioWithFfmpeg(
        context: Context,
        sourceFile: File,
        outputFile: File,
        targetExtension: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (!ensureInitialized(context)) return@withContext false
        val (ffmpegBin, libDir) = findUnpackedFfmpegBinary(context) ?: return@withContext false
        try {
            if (outputFile.exists()) outputFile.delete()
            val ext = targetExtension.lowercase()
            val codecArgs = when (ext) {
                "mp3" -> listOf("-vn", "-acodec", "libmp3lame", "-q:a", "2")
                "wav" -> listOf("-vn", "-acodec", "pcm_s16le", "-ar", "44100", "-ac", "2")
                else -> listOf("-vn", "-c:a", "aac", "-b:a", "256k")
            }
            val cmd = mutableListOf(
                ffmpegBin.absolutePath,
                "-y",
                "-i", sourceFile.absolutePath
            ).apply {
                addAll(codecArgs)
                add(outputFile.absolutePath)
            }
            val pb = ProcessBuilder(cmd)
            pb.environment()["LD_LIBRARY_PATH"] = libDir.absolutePath
            pb.redirectErrorStream(true)
            val proc = pb.start()
            proc.inputStream.bufferedReader().use { it.readText() }
            val finished = proc.waitFor(90, TimeUnit.SECONDS)
            if (!finished) {
                proc.destroyForcibly()
                return@withContext false
            }
            outputFile.exists() && outputFile.length() > 0L
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Renders an edited video clip with FFmpeg filtergraph (`crop`, `hflip`, `transpose`, `eq`,
     * `hue`, `unsharp`, `noise` film grain, `vignette`) to guarantee accurate export even when
     * hardware MediaCodec rejects complex filter combinations.
     */
    suspend fun renderEditedVideoWithFfmpeg(
        context: Context,
        sourceFile: File,
        outputFile: File,
        startMs: Long,
        endMs: Long,
        cropWidthPct: Float,
        cropHeightPct: Float,
        cropOffsetX: Float,
        cropOffsetY: Float,
        rotationDegrees: Float,
        isFlippedHorizontal: Boolean,
        brightness: Float,
        contrast: Float,
        saturation: Float,
        hueDegrees: Float,
        sharpenAmount: Float,
        grainAmount: Float,
        vignetteAmount: Float
    ): Boolean = withContext(Dispatchers.IO) {
        if (!ensureInitialized(context)) return@withContext false
        val (ffmpegBin, libDir) = findUnpackedFfmpegBinary(context) ?: return@withContext false
        try {
            if (outputFile.exists()) outputFile.delete()
            val vfFilters = mutableListOf<String>()

            val wFrac = (cropWidthPct / 100f).coerceIn(0.2f, 1.0f)
            val hFrac = (cropHeightPct / 100f).coerceIn(0.2f, 1.0f)
            if (wFrac < 0.995f || hFrac < 0.995f || kotlin.math.abs(cropOffsetX) > 0.005f || kotlin.math.abs(cropOffsetY) > 0.005f) {
                val xCenterFrac = (0.5f + cropOffsetX).coerceIn(wFrac * 0.5f, 1f - wFrac * 0.5f)
                val yCenterFrac = (0.5f + cropOffsetY).coerceIn(hFrac * 0.5f, 1f - hFrac * 0.5f)
                val xTopLeft = (xCenterFrac - wFrac * 0.5f).coerceIn(0f, 1f - wFrac)
                val yTopLeft = (yCenterFrac - hFrac * 0.5f).coerceIn(0f, 1f - hFrac)
                vfFilters.add(
                    String.format(
                        java.util.Locale.US,
                        "crop=trunc(iw*%.4f/2)*2:trunc(ih*%.4f/2)*2:iw*%.4f:ih*%.4f",
                        wFrac, hFrac, xTopLeft, yTopLeft
                    )
                )
            }

            if (isFlippedHorizontal) {
                vfFilters.add("hflip")
            }

            when (((rotationDegrees.toInt() % 360) + 360) % 360) {
                90 -> vfFilters.add("transpose=1")
                180 -> vfFilters.add("transpose=1,transpose=1")
                270 -> vfFilters.add("transpose=2")
            }

            if (kotlin.math.abs(brightness) > 0.01f ||
                kotlin.math.abs(contrast - 1f) > 0.01f ||
                kotlin.math.abs(saturation - 1f) > 0.01f
            ) {
                vfFilters.add(
                    String.format(
                        java.util.Locale.US,
                        "eq=brightness=%.3f:contrast=%.3f:saturation=%.3f",
                        brightness.coerceIn(-0.8f, 0.8f),
                        contrast.coerceIn(0.3f, 2.2f),
                        saturation.coerceIn(0f, 2.5f)
                    )
                )
            }

            if (kotlin.math.abs(hueDegrees) > 0.5f) {
                vfFilters.add(String.format(java.util.Locale.US, "hue=h=%.1f", hueDegrees.coerceIn(-180f, 180f)))
            }

            if (sharpenAmount > 0.02f) {
                val lumaAmount = (sharpenAmount * 2.2f).coerceIn(0.1f, 3.0f)
                vfFilters.add(String.format(java.util.Locale.US, "unsharp=5:5:%.2f:5:5:0.0", lumaAmount))
            }

            if (grainAmount > 0.02f) {
                val strength = (grainAmount * 36f).toInt().coerceIn(2, 45)
                vfFilters.add("noise=alls=$strength:allf=t+u")
            }

            if (vignetteAmount > 0.02f) {
                vfFilters.add("vignette=PI/4")
            }

            val startSec = (startMs.coerceAtLeast(0L)) / 1000.0
            val durSec = ((endMs - startMs).coerceAtLeast(500L)) / 1000.0

            val cmd = mutableListOf(
                ffmpegBin.absolutePath,
                "-y",
                "-ss", String.format(java.util.Locale.US, "%.3f", startSec),
                "-t", String.format(java.util.Locale.US, "%.3f", durSec),
                "-i", sourceFile.absolutePath
            )
            if (vfFilters.isNotEmpty()) {
                cmd.addAll(listOf("-vf", vfFilters.joinToString(",")))
            }
            cmd.addAll(
                listOf(
                    "-c:v", "libx264",
                    "-preset", "ultrafast",
                    "-crf", "21",
                    "-c:a", "aac",
                    "-b:a", "192k",
                    "-movflags", "+faststart",
                    outputFile.absolutePath
                )
            )
            val pb = ProcessBuilder(cmd)
            pb.environment()["LD_LIBRARY_PATH"] = libDir.absolutePath
            pb.redirectErrorStream(true)
            val proc = pb.start()
            proc.inputStream.bufferedReader().use { it.readText() }
            val finished = proc.waitFor(180, TimeUnit.SECONDS)
            if (!finished) {
                proc.destroyForcibly()
                return@withContext false
            }
            outputFile.exists() && outputFile.length() > 0L
        } catch (_: Throwable) {
            false
        }
    }

    private fun extractCleanErrorMessage(t: Throwable): String {
        val raw = t.localizedMessage ?: t.message ?: "Download failed"
        val nonWarningLines = raw.lines()
            .map { it.trim() }
            .filter { line ->
                line.isNotBlank() &&
                    !line.startsWith("WARNING:", ignoreCase = true) &&
                    !line.contains("older than 90 days", ignoreCase = true) &&
                    !line.contains("strongly recommended to always use", ignoreCase = true)
            }
        val errorLine = nonWarningLines.lastOrNull { it.startsWith("ERROR:", ignoreCase = true) }
            ?.removePrefix("ERROR:")
            ?.trim()
        return (errorLine ?: nonWarningLines.lastOrNull() ?: "Could not download stream. Please retry.")
            .take(140)
    }

    suspend fun extractMetadata(
        context: Context,
        url: String,
        platformName: String
    ): ExtractedMediaMetadata? = withContext(Dispatchers.IO) {
        if (!ensureInitialized(context)) return@withContext null
        try {
            val request = YoutubeDLRequest(url).apply {
                addOption("--no-playlist")
                addOption("--no-warnings")
                addOption("--socket-timeout", "20")
                addOption("--no-check-certificates")
                addOption("--geo-bypass")
                addOption("--extractor-args", "youtube:player_client=visionos,tv,ios,mweb")
            }

            val info = YoutubeDL.getInstance().getInfo(request) ?: return@withContext null
            val rawTitle = info.title?.takeIf { it.isNotBlank() } ?: "$platformName Video"
            val uploader = info.uploader?.takeIf { it.isNotBlank() } ?: "$platformName Creator"
            val durationSec = info.duration.toLong().coerceAtLeast(1L)
            val thumbnail = info.thumbnail?.takeIf { it.isNotBlank() } ?: ""

            val rawFormats = info.formats.orEmpty()

            // Find best direct MP4 URLs by height if available
            fun findDirectMp4WithAudio(maxHeight: Int): String? {
                return rawFormats
                    .filter { fmt ->
                        val ext = fmt.ext.orEmpty().lowercase()
                        val vcodec = fmt.vcodec.orEmpty().lowercase()
                        val acodec = fmt.acodec.orEmpty().lowercase()
                        val h = fmt.height
                        !fmt.url.isNullOrBlank() &&
                            (ext == "mp4" || vcodec.contains("avc") || vcodec.contains("h264")) &&
                            vcodec != "none" && acodec != "none" &&
                            (h <= 0 || h <= maxHeight)
                    }
                    .maxByOrNull { it.height.coerceAtLeast(1) }
                    ?.url
            }

            val bestMuxedUrl = findDirectMp4WithAudio(4320) ?: info.url

            // Estimate sizes from yt-dlp format list
            fun estimateSizeForHeight(targetHeight: Int, fallbackBytes: Long): Long {
                val matched = rawFormats
                    .filter { it.height in (targetHeight - 200)..(targetHeight + 200) && it.fileSize > 0L }
                    .maxByOrNull { it.fileSize }
                    ?.fileSize
                return if (matched != null && matched > 0L) matched else fallbackBytes
            }

            val videoFormats = listOf(
                DownloadFormat(
                    formatId = "ytdlp_2160p",
                    type = MediaType.VIDEO,
                    extension = "mp4",
                    resolutionLabel = VideoResolution.RES_2160P.label,
                    height = 2160,
                    width = 3840,
                    fps = 60,
                    vcodec = "h264 / vp9",
                    acodec = "aac",
                    estimatedSizeBytes = estimateSizeForHeight(2160, (durationSec * 2_200_000L).coerceAtLeast(4_500_000L)),
                    isRecommended = false,
                    directUrl = findDirectMp4WithAudio(2160) ?: bestMuxedUrl
                ),
                DownloadFormat(
                    formatId = "ytdlp_1080p",
                    type = MediaType.VIDEO,
                    extension = "mp4",
                    resolutionLabel = VideoResolution.RES_1080P.label,
                    height = 1080,
                    width = 1920,
                    fps = 60,
                    vcodec = "h264",
                    acodec = "aac",
                    estimatedSizeBytes = estimateSizeForHeight(1080, (durationSec * 850_000L).coerceAtLeast(2_000_000L)),
                    isRecommended = true,
                    directUrl = findDirectMp4WithAudio(1080) ?: bestMuxedUrl
                ),
                DownloadFormat(
                    formatId = "ytdlp_720p",
                    type = MediaType.VIDEO,
                    extension = "mp4",
                    resolutionLabel = VideoResolution.RES_720P.label,
                    height = 720,
                    width = 1280,
                    fps = 30,
                    vcodec = "h264",
                    acodec = "aac",
                    estimatedSizeBytes = estimateSizeForHeight(720, (durationSec * 450_000L).coerceAtLeast(1_200_000L)),
                    isRecommended = false,
                    directUrl = findDirectMp4WithAudio(720) ?: bestMuxedUrl
                ),
                DownloadFormat(
                    formatId = "ytdlp_480p",
                    type = MediaType.VIDEO,
                    extension = "mp4",
                    resolutionLabel = VideoResolution.RES_480P.label,
                    height = 480,
                    width = 854,
                    fps = 30,
                    vcodec = "h264",
                    acodec = "aac",
                    estimatedSizeBytes = estimateSizeForHeight(480, (durationSec * 250_000L).coerceAtLeast(800_000L)),
                    isRecommended = false,
                    directUrl = findDirectMp4WithAudio(480) ?: bestMuxedUrl
                )
            )

            val bestAudioDirect = rawFormats
                .filter { fmt ->
                    val acodec = fmt.acodec.orEmpty().lowercase()
                    val vcodec = fmt.vcodec.orEmpty().lowercase()
                    !fmt.url.isNullOrBlank() && acodec != "none" && (vcodec == "none" || vcodec.isBlank())
                }
                .maxByOrNull { it.tbr }
                ?.url ?: bestMuxedUrl

            val audioFormats = listOf(
                DownloadFormat(
                    formatId = "ytdlp_audio_mp3",
                    type = MediaType.AUDIO,
                    extension = AudioFormat.MP3.extension,
                    audioBitrateKbps = 320,
                    estimatedSizeBytes = (durationSec * 40_000L).coerceAtLeast(500_000L),
                    isAudioOnly = true,
                    isRecommended = true,
                    directUrl = bestAudioDirect
                ),
                DownloadFormat(
                    formatId = "ytdlp_audio_m4a",
                    type = MediaType.AUDIO,
                    extension = AudioFormat.M4A.extension,
                    audioBitrateKbps = 256,
                    estimatedSizeBytes = (durationSec * 32_000L).coerceAtLeast(400_000L),
                    isAudioOnly = true,
                    directUrl = bestAudioDirect
                ),
                DownloadFormat(
                    formatId = "ytdlp_audio_wav",
                    type = MediaType.AUDIO,
                    extension = AudioFormat.WAV.extension,
                    audioBitrateKbps = 1411,
                    estimatedSizeBytes = (durationSec * 176_000L).coerceAtLeast(1_500_000L),
                    isAudioOnly = true,
                    directUrl = bestAudioDirect
                )
            )

            val imageFormats = if (thumbnail.isNotBlank()) {
                listOf(
                    DownloadFormat(
                        formatId = "img_maxres",
                        type = MediaType.IMAGE,
                        extension = "jpg",
                        resolutionLabel = "Original Video Thumbnail Cover",
                        estimatedSizeBytes = 350_000L,
                        directUrl = thumbnail
                    )
                )
            } else emptyList()

            ExtractedMediaMetadata(
                url = url,
                title = rawTitle,
                uploader = uploader,
                durationSeconds = durationSec,
                thumbnailUrl = thumbnail,
                platform = info.extractorKey?.takeIf { it.isNotBlank() } ?: platformName,
                formats = videoFormats + audioFormats + imageFormats,
                imageGalleryUrls = if (thumbnail.isNotBlank()) listOf(thumbnail) else emptyList()
            )
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Executes a real download using embedded `yt-dlp` and `FFmpeg` (merging separate video+audio streams
     * or converting audio to MP3/M4A/WAV).
     */
    suspend fun downloadMedia(
        context: Context,
        sourceUrl: String,
        format: DownloadFormat,
        destFile: File,
        processId: String,
        onProgress: (percent: Int, etaSec: Long, statusLine: String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        if (!ensureInitialized(context)) {
            return@withContext Result.failure(IllegalStateException("yt-dlp native runtime not initialized"))
        }

        val parentDir = destFile.parentFile ?: context.filesDir
        if (!parentDir.exists()) parentDir.mkdirs()
        if (destFile.exists()) destFile.delete()

        val basePathWithoutExt = destFile.absolutePath.substringBeforeLast(".")
        val targetExt = format.extension.lowercase().ifBlank { "mp4" }

        fun buildRequest(): YoutubeDLRequest {
            return YoutubeDLRequest(sourceUrl).apply {
                addOption("--no-playlist")
                addOption("--no-mtime")
                addOption("--no-warnings")
                addOption("--no-check-certificates")
                addOption("--geo-bypass")
                addOption("--socket-timeout", "30")
                addOption("--retries", "5")
                addOption("--fragment-retries", "5")
                addOption("--concurrent-fragments", "6")
                addOption("--extractor-args", "youtube:player_client=visionos,tv,ios,mweb")

                when (format.type) {
                    MediaType.AUDIO -> {
                        addOption("-x")
                        addOption("--audio-format", targetExt)
                        addOption("--audio-quality", "0")
                        addOption("-o", "$basePathWithoutExt.%(ext)s")
                    }
                    MediaType.VIDEO -> {
                        val h = (format.height ?: 1080).coerceAtLeast(360)
                        val selector = "bestvideo[height<=$h][ext=mp4]+bestaudio[ext=m4a]/bestvideo[height<=$h]+bestaudio/best[height<=$h]/best"
                        addOption("-f", selector)
                        addOption("--merge-output-format", "mp4")
                        addOption("-o", "$basePathWithoutExt.%(ext)s")
                    }
                    MediaType.IMAGE -> {
                        addOption("-o", destFile.absolutePath)
                    }
                }
            }
        }

        fun findProducedFile(): File? {
            val candidates = listOf(
                destFile,
                File("$basePathWithoutExt.$targetExt"),
                File("$basePathWithoutExt.mp4"),
                File("$basePathWithoutExt.mkv"),
                File("$basePathWithoutExt.webm"),
                File("$basePathWithoutExt.mp3"),
                File("$basePathWithoutExt.m4a"),
                File("$basePathWithoutExt.wav")
            )
            return candidates.firstOrNull { it.exists() && it.length() > 0L }
        }

        for (attempt in 1..2) {
            try {
                YoutubeDL.getInstance().execute(buildRequest(), "${processId}_$attempt") { progress, etaInSeconds, line ->
                    val pct = progress.toInt().coerceIn(1, 99)
                    val cleanEta = if (etaInSeconds >= 0L) etaInSeconds else 0L
                    onProgress(pct, cleanEta, line.orEmpty())
                }

                val actualFile = findProducedFile()
                if (actualFile != null) {
                    if (actualFile.absolutePath != destFile.absolutePath) {
                        actualFile.copyTo(destFile, overwrite = true)
                        actualFile.delete()
                    }
                    return@withContext Result.success(destFile)
                }
            } catch (t: Throwable) {
                if (attempt == 1 && !hasUpdatedThisSession) {
                    onProgress(5, 0L, "Updating extractor engine...")
                    updateYtDlpIfPossible(context)
                    continue
                }
                return@withContext Result.failure(IllegalStateException(extractCleanErrorMessage(t)))
            }
        }

        Result.failure(IllegalStateException("yt-dlp completed but output file was not found"))
    }
}
