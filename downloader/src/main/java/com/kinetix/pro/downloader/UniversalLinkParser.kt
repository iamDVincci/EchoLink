package com.kinetix.pro.downloader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URL
import java.util.regex.Pattern

enum class SupportedPlatform(val displayName: String, val badgeColorHex: String) {
    YOUTUBE("YouTube", "#FF0000"),
    TIKTOK("TikTok", "#00F2FE"),
    INSTAGRAM("Instagram", "#E1306C"),
    X_TWITTER("X / Twitter", "#1DA1F2"),
    REDDIT("Reddit", "#FF4500"),
    FACEBOOK("Facebook", "#1877F2"),
    VIMEO("Vimeo", "#1AB7EA"),
    DIRECT_MEDIA("Direct File", "#EA6113"),
    GENERIC_WEB("Web Extractor", "#9E9EB0")
}

data class DirectFileHeaderInfo(
    val url: String,
    val contentType: String?,
    val contentLength: Long,
    val suggestedFileName: String?,
    val isSeekable: Boolean,
    val isAudio: Boolean,
    val isVideo: Boolean,
    val isImage: Boolean
)

class UniversalLinkParser(private val okHttpClient: OkHttpClient = OkHttpClient()) {

    fun detectPlatform(url: String): SupportedPlatform {
        val lower = url.lowercase().trim()
        return when {
            lower.contains("youtube.com") || lower.contains("youtu.be") -> SupportedPlatform.YOUTUBE
            lower.contains("tiktok.com") -> SupportedPlatform.TIKTOK
            lower.contains("instagram.com") -> SupportedPlatform.INSTAGRAM
            lower.contains("twitter.com") || lower.contains("x.com") -> SupportedPlatform.X_TWITTER
            lower.contains("reddit.com") || lower.contains("redd.it") -> SupportedPlatform.REDDIT
            lower.contains("facebook.com") || lower.contains("fb.watch") -> SupportedPlatform.FACEBOOK
            lower.contains("vimeo.com") -> SupportedPlatform.VIMEO
            isDirectFileUrl(lower) -> SupportedPlatform.DIRECT_MEDIA
            else -> SupportedPlatform.GENERIC_WEB
        }
    }

    fun isDirectFileUrl(url: String): Boolean {
        val cleanUrl = url.split("?").first().lowercase()
        val directExtensions = listOf(
            ".mp4", ".mkv", ".webm", ".mov", ".avi", ".flv",
            ".mp3", ".m4a", ".wav", ".flac", ".ogg", ".opus", ".aac",
            ".jpg", ".jpeg", ".png", ".webp", ".gif"
        )
        return directExtensions.any { cleanUrl.endsWith(it) }
    }

    suspend fun inspectDirectFile(url: String): DirectFileHeaderInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .head()
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile) KinetixPro/1.0")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val contentType = response.header("Content-Type")?.lowercase()
            val contentLength = response.header("Content-Length")?.toLongOrNull() ?: -1L
            val acceptRanges = response.header("Accept-Ranges")
            val isSeekable = acceptRanges?.equals("bytes", ignoreCase = true) == true

            val contentDisposition = response.header("Content-Disposition")
            var filename: String? = null
            if (contentDisposition != null) {
                val matcher = Pattern.compile("filename=[\"']?([^\"';]+)[\"']?").matcher(contentDisposition)
                if (matcher.find()) {
                    filename = matcher.group(1)
                }
            }
            if (filename == null) {
                filename = URL(url).path.split("/").lastOrNull()?.takeIf { it.isNotBlank() } ?: "media_download"
            }

            val isAudio = contentType?.startsWith("audio/") == true || filename.endsWith(".mp3", true) || filename.endsWith(".m4a", true)
            val isVideo = contentType?.startsWith("video/") == true || filename.endsWith(".mp4", true) || filename.endsWith(".webm", true)
            val isImage = contentType?.startsWith("image/") == true || filename.endsWith(".jpg", true) || filename.endsWith(".png", true)

            DirectFileHeaderInfo(
                url = url,
                contentType = contentType,
                contentLength = contentLength,
                suggestedFileName = filename,
                isSeekable = isSeekable,
                isAudio = isAudio,
                isVideo = isVideo,
                isImage = isImage
            )
        } catch (e: Exception) {
            null
        }
    }
}
