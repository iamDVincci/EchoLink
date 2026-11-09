package com.kinetix.pro.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class MediaType {
    VIDEO,
    AUDIO,
    IMAGE
}

@Serializable
enum class VideoResolution(val label: String, val height: Int, val isHdOrHigher: Boolean) {
    RES_4320P("8K UHD (4320p)", 4320, true),
    RES_2160P("4K UHD (2160p)", 2160, true),
    RES_1440P("2K QHD (1440p)", 1440, true),
    RES_1080P("1080p Full HD", 1080, true),
    RES_720P("720p HD", 720, true),
    RES_480P("480p SD", 480, false),
    RES_360P("360p", 360, false),
    RES_240P("240p", 240, false),
    RES_144P("144p", 144, false);

    companion object {
        fun fromHeight(height: Int): VideoResolution {
            return entries.firstOrNull { height >= it.height } ?: RES_360P
        }
    }
}

@Serializable
enum class AudioFormat(val extension: String, val mimeType: String, val defaultBitrateKbps: Int) {
    MP3("mp3", "audio/mpeg", 320),
    M4A("m4a", "audio/mp4", 256),
    WAV("wav", "audio/wav", 1411),
    FLAC("flac", "audio/flac", 1000),
    OPUS("opus", "audio/opus", 160)
}

@Serializable
data class DownloadFormat(
    val formatId: String,
    val type: MediaType,
    val extension: String,
    val resolutionLabel: String? = null,
    val height: Int = 0,
    val width: Int = 0,
    val fps: Int = 30,
    val vcodec: String? = null,
    val acodec: String? = null,
    val audioBitrateKbps: Int = 0,
    val estimatedSizeBytes: Long = 0L,
    val isVideoOnly: Boolean = false,
    val isAudioOnly: Boolean = false,
    val isRecommended: Boolean = false,
    val directUrl: String? = null
)

@Serializable
data class ExtractedMediaMetadata(
    val url: String,
    val title: String,
    val uploader: String? = null,
    val durationSeconds: Long = 0L,
    val thumbnailUrl: String? = null,
    val platform: String = "Universal",
    val formats: List<DownloadFormat> = emptyList(),
    val subtitles: List<SubtitleTrack> = emptyList(),
    val imageGalleryUrls: List<String> = emptyList()
)

@Serializable
data class SubtitleTrack(
    val languageCode: String,
    val languageName: String,
    val url: String,
    val format: String = "vtt"
)
