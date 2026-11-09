package com.kinetix.pro.downloader

import android.content.Context
import com.kinetix.pro.core.model.AudioFormat
import com.kinetix.pro.core.model.DownloadFormat
import com.kinetix.pro.core.model.ExtractedMediaMetadata
import com.kinetix.pro.core.model.MediaType
import com.kinetix.pro.core.model.VideoResolution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

sealed class ExtractionResult {
    data class Success(val metadata: ExtractedMediaMetadata) : ExtractionResult()
    data class Error(val message: String, val canRetry: Boolean = true) : ExtractionResult()
}

private data class ScrapedPageMeta(
    val title: String? = null,
    val author: String? = null,
    val thumbnailUrl: String? = null,
    val directVideoUrl: String? = null,
    val stream2160pUrl: String? = null,
    val stream1440pUrl: String? = null,
    val stream1080pUrl: String? = null,
    val stream720pUrl: String? = null,
    val stream480pUrl: String? = null,
    val directAudioUrl: String? = null,
    val durationSec: Long? = null,
    val sizeByHeightBytes: Map<Int, Long> = emptyMap(),
    val audioSizeBytes: Long? = null
)

class DownloaderEngine(
    private val context: Context? = null,
    private val linkParser: UniversalLinkParser = UniversalLinkParser(),
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        // Separator used when a high-res adaptive video stream and audio stream must be muxed on device
        const val ADAPTIVE_MUX_SEPARATOR = "|||"

        // Retained only as compile-time constants for legacy unit test references; never used as a download fallback
        const val SAMPLE_STREAM_1080P = ""
        const val SAMPLE_STREAM_720P = ""
        const val SAMPLE_STREAM_480P = ""
    }

    suspend fun extractMetadata(rawUrl: String): ExtractionResult = withContext(Dispatchers.IO) {
        val cleanUrl = rawUrl.trim()
        if (cleanUrl.isBlank()) {
            return@withContext ExtractionResult.Error("Please enter or paste a valid URL.")
        }

        val platform = linkParser.detectPlatform(cleanUrl)

        // 1. Direct media file check via HTTP HEAD inspection
        if (platform == SupportedPlatform.DIRECT_MEDIA || linkParser.isDirectFileUrl(cleanUrl)) {
            val headerInfo = linkParser.inspectDirectFile(cleanUrl)
            val filename = headerInfo?.suggestedFileName
                ?: cleanUrl.substringAfterLast("/").substringBefore("?").ifBlank { "direct_media.mp4" }
            val mediaType = when {
                headerInfo?.isAudio == true || filename.endsWith(".mp3", true) || filename.endsWith(".m4a", true) -> MediaType.AUDIO
                headerInfo?.isImage == true || filename.endsWith(".jpg", true) || filename.endsWith(".png", true) -> MediaType.IMAGE
                else -> MediaType.VIDEO
            }
            val format = DownloadFormat(
                formatId = "direct_http",
                type = mediaType,
                extension = filename.substringAfterLast(".", "mp4"),
                resolutionLabel = if (mediaType == MediaType.VIDEO) "Original Direct Stream" else null,
                estimatedSizeBytes = (headerInfo?.contentLength ?: 0L).takeIf { it > 0L } ?: 4_200_000L,
                isRecommended = true,
                directUrl = cleanUrl
            )
            return@withContext ExtractionResult.Success(
                ExtractedMediaMetadata(
                    url = cleanUrl,
                    title = filename.substringBeforeLast("."),
                    uploader = "Direct HTTP Server",
                    durationSeconds = 15L,
                    thumbnailUrl = "",
                    platform = "Direct File",
                    formats = listOf(format)
                )
            )
        }

        // 2. Fast Native Platform API Extractors (YouTube InnerTube, TikTok API, X/Twitter FX API, Instagram Embed, Facebook, Reddit)
        try {
            val scraped = fetchOEmbedOrOpenGraph(cleanUrl, platform)
            if (!scraped.directVideoUrl.isNullOrBlank() || !scraped.stream1080pUrl.isNullOrBlank()) {
                return@withContext ExtractionResult.Success(
                    buildPlatformFormatMatrix(cleanUrl, platform, scraped)
                )
            }

            // 3. Embedded yt-dlp Native Runtime Extraction (supports 1,700+ sites)
            if (context != null) {
                val ytDlpMeta = YtDlpNativeEngine.extractMetadata(context, cleanUrl, platform.displayName)
                if (ytDlpMeta != null) {
                    return@withContext ExtractionResult.Success(ytDlpMeta)
                }
            }

            // 4. If oEmbed/OpenGraph succeeded with title/thumbnail, allow yt-dlp execution at download time
            if (!scraped.title.isNullOrBlank() || context != null) {
                return@withContext ExtractionResult.Success(
                    buildPlatformFormatMatrix(cleanUrl, platform, scraped)
                )
            }

            ExtractionResult.Error(
                message = "Could not extract a downloadable video stream from this ${platform.displayName} link. Ensure the post is public.",
                canRetry = true
            )
        } catch (e: Exception) {
            ExtractionResult.Error(
                message = "Failed to resolve link: ${e.localizedMessage ?: "Network error"}. Please check your connection and try again.",
                canRetry = true
            )
        }
    }

    private fun fetchOEmbedOrOpenGraph(url: String, platform: SupportedPlatform): ScrapedPageMeta {
        val resolvedUrl = resolveShortRedirectUrl(url)
        val encoded = try { URLEncoder.encode(resolvedUrl, "UTF-8") } catch (_: Exception) { "" }

        // 1. Platform-specific real stream APIs
        when (platform) {
            SupportedPlatform.YOUTUBE -> {
                extractYouTubeStream(resolvedUrl)?.let { return it }
            }
            SupportedPlatform.TIKTOK -> {
                extractTikTokStream(encoded)?.let { return it }
            }
            SupportedPlatform.INSTAGRAM -> {
                extractInstagramStream(resolvedUrl)?.let { return it }
            }
            SupportedPlatform.X_TWITTER -> {
                extractTwitterStream(resolvedUrl)?.let { return it }
            }
            SupportedPlatform.FACEBOOK -> {
                extractFacebookStream(resolvedUrl)?.let { return it }
            }
            SupportedPlatform.REDDIT -> {
                extractRedditStream(resolvedUrl)?.let { return it }
            }
            else -> {}
        }

        // 2. oEmbed metadata lookup
        var oembedMeta: ScrapedPageMeta? = null
        val oembedEndpoint = when (platform) {
            SupportedPlatform.YOUTUBE -> "https://www.youtube.com/oembed?url=$encoded&format=json"
            SupportedPlatform.TIKTOK -> "https://www.tiktok.com/oembed?url=$encoded"
            SupportedPlatform.VIMEO -> "https://vimeo.com/api/oembed.json?url=$encoded"
            SupportedPlatform.REDDIT -> "https://www.reddit.com/oembed?url=$encoded"
            else -> null
        }

        if (oembedEndpoint != null) {
            try {
                val req = Request.Builder()
                    .url(oembedEndpoint)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) KinetixPro/1.0")
                    .build()
                okHttpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val bodyStr = resp.body?.string().orEmpty()
                        val root = json.parseToJsonElement(bodyStr).jsonObject
                        val title = root["title"]?.jsonPrimitive?.content
                        val author = root["author_name"]?.jsonPrimitive?.content
                        val thumb = root["thumbnail_url"]?.jsonPrimitive?.content
                        val duration = root["duration"]?.jsonPrimitive?.content?.toLongOrNull()
                        if (!title.isNullOrBlank()) {
                            oembedMeta = ScrapedPageMeta(
                                title = title,
                                author = author,
                                thumbnailUrl = thumb,
                                durationSec = duration
                            )
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // 3. OpenGraph HTML tag & embedded <video> / MP4 inspection
        val ogMeta = try {
            val req = Request.Builder()
                .url(resolvedUrl)
                .header("User-Agent", "TelegramBot (like TwitterBot)")
                .build()
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return oembedMeta ?: ScrapedPageMeta()
                val html = resp.body?.string()?.take(200_000).orEmpty()
                val ogTitle = extractMetaProperty(html, "og:title") ?: extractTitleTag(html)
                val ogImage = extractMetaProperty(html, "og:image")
                val ogVideo = extractMetaProperty(html, "og:video:secure_url")
                    ?: extractMetaProperty(html, "og:video:url")
                    ?: extractMetaProperty(html, "og:video")
                    ?: Regex("""https?://[^\s"'<>\\]+\.mp4(?:\?[^\s"'<>\\]+)?""")
                        .find(html)?.value?.replace("\\u0026", "&")?.replace("\\/", "/")
                val ogSite = extractMetaProperty(html, "og:site_name")
                ScrapedPageMeta(
                    title = oembedMeta?.title ?: ogTitle,
                    author = oembedMeta?.author ?: ogSite,
                    thumbnailUrl = oembedMeta?.thumbnailUrl ?: ogImage,
                    directVideoUrl = ogVideo?.takeIf { it.startsWith("http") && !it.contains("/embed/") },
                    durationSec = oembedMeta?.durationSec
                )
            }
        } catch (_: Exception) {
            oembedMeta ?: ScrapedPageMeta()
        }

        return ogMeta
    }

    private fun resolveShortRedirectUrl(url: String): String {
        val lower = url.lowercase()
        if (lower.contains("vm.tiktok.com") ||
            lower.contains("vt.tiktok.com") ||
            lower.contains("fb.watch") ||
            lower.contains("facebook.com/share/") ||
            lower.contains("redd.it/") ||
            lower.contains("pin.it/")
        ) {
            return try {
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; SM-S918U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                    .build()
                okHttpClient.newCall(req).execute().use { resp ->
                    resp.request.url.toString()
                }
            } catch (_: Exception) {
                url
            }
        }
        return url
    }

    private fun extractYouTubeVideoId(url: String): String? {
        val patterns = listOf(
            Regex("""(?:v=|/shorts/|/live/|youtu\.be/|/embed/)([a-zA-Z0-9_-]{11})""")
        )
        for (p in patterns) {
            val m = p.find(url)
            if (m != null) return m.groupValues[1]
        }
        return null
    }

    private fun fetchYouTubeVisitorData(videoId: String): String {
        try {
            val req = Request.Builder()
                .url("https://www.youtube.com/embed/$videoId")
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                )
                .build()
            okHttpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val html = resp.body?.string().orEmpty()
                    val m = Regex(""""VISITOR_DATA"\s*:\s*"([^"]+)"""").find(html)
                    if (m != null && m.groupValues[1].isNotBlank()) {
                        return m.groupValues[1]
                    }
                }
            }
        } catch (_: Exception) {}

        try {
            val req = Request.Builder()
                .url("https://www.youtube.com/sw.js_data")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            okHttpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val text = resp.body?.string().orEmpty()
                    val m = Regex(""""(Cg[a-zA-Z0-9_%-]+)"""").find(text)
                    if (m != null && m.groupValues[1].isNotBlank()) {
                        return m.groupValues[1]
                    }
                }
            }
        } catch (_: Exception) {}
        return ""
    }

    private fun extractClenFromUrl(url: String): Long? {
        val m = Regex("""clen(?:%3D|=)(\d+)""").find(url)
        return m?.groupValues?.getOrNull(1)?.toLongOrNull()?.takeIf { it > 0L }
    }

    private fun extractYouTubeStream(url: String): ScrapedPageMeta? {
        val videoId = extractYouTubeVideoId(url) ?: return null
        val visitorData = fetchYouTubeVisitorData(videoId)
        val visitorField = if (visitorData.isNotBlank()) """, "visitorData": "$visitorData"""" else ""

        // 1. Primary Non-SABR HLS Client: VISIONOS (works on long & large videos of any duration without 403 cutoff)
        try {
            val visionUa = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15"
            val visionPayload = """
                {
                  "videoId": "$videoId",
                  "context": {
                    "client": {
                      "clientName": "VISIONOS",
                      "clientVersion": "0.1",
                      "deviceMake": "Apple",
                      "deviceModel": "RealityDevice14,1",
                      "osName": "visionOS",
                      "osVersion": "1.3.21O771",
                      "hl": "en",
                      "gl": "US"$visitorField
                    }
                  },
                  "contentCheckOk": true,
                  "racyCheckOk": true
                }
            """.trimIndent()
            val req = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/player?prettyPrint=false")
                .header("User-Agent", visionUa)
                .header("Content-Type", "application/json")
                .post(visionPayload.toRequestBody("application/json".toMediaType()))
                .build()

            okHttpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val bodyStr = resp.body?.string().orEmpty()
                    val root = json.parseToJsonElement(bodyStr).jsonObject
                    val videoDetails = root["videoDetails"]?.jsonObject
                    val title = videoDetails?.get("title")?.jsonPrimitive?.content
                    val author = videoDetails?.get("author")?.jsonPrimitive?.content
                    val lengthSec = videoDetails?.get("lengthSeconds")?.jsonPrimitive?.content?.toLongOrNull()
                    val hlsManifestUrl = root["streamingData"]?.jsonObject?.get("hlsManifestUrl")?.jsonPrimitive?.content

                    if (!hlsManifestUrl.isNullOrBlank()) {
                        val m3u8Req = Request.Builder()
                            .url(hlsManifestUrl)
                            .header("User-Agent", visionUa)
                            .build()
                        okHttpClient.newCall(m3u8Req).execute().use { m3u8Resp ->
                            if (m3u8Resp.isSuccessful) {
                                val m3u8Text = m3u8Resp.body?.string().orEmpty()
                                val lines = m3u8Text.lines()

                                val audioByGroup = mutableMapOf<String, String>()
                                val videoByHeight = mutableMapOf<Int, String>()
                                val sizeByHeight = mutableMapOf<Int, Long>()

                                for (i in lines.indices) {
                                    val line = lines[i].trim()
                                    if (line.startsWith("#EXT-X-MEDIA:") && line.contains("TYPE=AUDIO")) {
                                        val groupId = Regex("""GROUP-ID="([^"]+)"""").find(line)?.groupValues?.getOrNull(1)
                                        val uri = Regex("""URI="([^"]+)"""").find(line)?.groupValues?.getOrNull(1)
                                        if (!groupId.isNullOrBlank() && !uri.isNullOrBlank()) {
                                            audioByGroup[groupId] = uri
                                        }
                                    } else if (line.startsWith("#EXT-X-STREAM-INF:") && i + 1 < lines.size) {
                                        val resMatch = Regex("""RESOLUTION=(\d+)x(\d+)""").find(line)
                                        val height = resMatch?.groupValues?.getOrNull(2)?.toIntOrNull() ?: 0
                                        val isAvc = line.contains("avc1", ignoreCase = true)
                                        val nextLine = lines[i + 1].trim()
                                        if (height > 0 && nextLine.startsWith("http")) {
                                            // Prefer H.264 (avc1) for universal Android hardware muxer/player compatibility
                                            if (!videoByHeight.containsKey(height) || isAvc) {
                                                videoByHeight[height] = nextLine
                                                extractClenFromUrl(nextLine)?.let { sizeByHeight[height] = it }
                                            }
                                        }
                                    }
                                }

                                val bestAudioM3u8 = audioByGroup["234"] ?: audioByGroup["233"] ?: audioByGroup.values.firstOrNull()
                                val audioSize = bestAudioM3u8?.let { extractClenFromUrl(it) }

                                fun buildHlsPair(targetHeight: Int): String? {
                                    val bestVideoM3u8 = videoByHeight.entries
                                        .filter { it.key <= targetHeight }
                                        .maxByOrNull { it.key }
                                        ?.value ?: videoByHeight.entries.minByOrNull { it.key }?.value
                                    return if (bestVideoM3u8 != null && bestAudioM3u8 != null) {
                                        "$bestVideoM3u8$ADAPTIVE_MUX_SEPARATOR$bestAudioM3u8"
                                    } else {
                                        bestVideoM3u8
                                    }
                                }

                                val primary1080 = buildHlsPair(1080)
                                if (!primary1080.isNullOrBlank()) {
                                    return ScrapedPageMeta(
                                        title = title ?: "YouTube Video",
                                        author = author ?: "YouTube Creator",
                                        thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                                        directVideoUrl = primary1080,
                                        stream2160pUrl = buildHlsPair(2160),
                                        stream1440pUrl = buildHlsPair(1440),
                                        stream1080pUrl = primary1080,
                                        stream720pUrl = buildHlsPair(720),
                                        stream480pUrl = buildHlsPair(480),
                                        directAudioUrl = bestAudioM3u8,
                                        durationSec = lengthSec ?: 30L,
                                        sizeByHeightBytes = sizeByHeight,
                                        audioSizeBytes = audioSize
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. Fallback Client: ANDROID (with visitorData)
        try {
            val androidUa = "com.google.android.youtube/20.10.38 (Linux; U; Android 14) gzip"
            val payload = """
                {
                  "videoId": "$videoId",
                  "context": {
                    "client": {
                      "clientName": "ANDROID",
                      "clientVersion": "20.10.38",
                      "androidSdkVersion": 34,
                      "osName": "Android",
                      "osVersion": "14",
                      "hl": "en",
                      "gl": "US"$visitorField
                    }
                  },
                  "contentCheckOk": true,
                  "racyCheckOk": true
                }
            """.trimIndent()
            val req = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/player?prettyPrint=false")
                .header("User-Agent", androidUa)
                .header("Content-Type", "application/json")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()

            okHttpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val bodyStr = resp.body?.string().orEmpty()
                    val root = json.parseToJsonElement(bodyStr).jsonObject
                    val videoDetails = root["videoDetails"]?.jsonObject
                    val title = videoDetails?.get("title")?.jsonPrimitive?.content
                    val author = videoDetails?.get("author")?.jsonPrimitive?.content
                    val lengthSec = videoDetails?.get("lengthSeconds")?.jsonPrimitive?.content?.toLongOrNull()

                    val streamingData = root["streamingData"]?.jsonObject
                    val hasSabr = streamingData?.containsKey("serverAbrStreamingUrl") == true
                    val formats = streamingData?.get("formats")?.jsonArray
                    var muxedMp4Url: String? = null
                    if (formats != null) {
                        for (fmtElem in formats) {
                            val fmtObj = fmtElem.jsonObject
                            val mime = fmtObj["mimeType"]?.jsonPrimitive?.content.orEmpty()
                            val directUrl = fmtObj["url"]?.jsonPrimitive?.content
                            if (!directUrl.isNullOrBlank() && mime.startsWith("video/mp4")) {
                                muxedMp4Url = directUrl
                            }
                        }
                    }

                    val adaptiveFormats = streamingData?.get("adaptiveFormats")?.jsonArray
                    var bestAudioUrl: String? = null
                    val videoStreamsByHeight = mutableMapOf<Int, String>()
                    val sizeByHeight = mutableMapOf<Int, Long>()

                    if (adaptiveFormats != null && (!hasSabr || (lengthSec ?: 0L) <= 45L)) {
                        for (fmtElem in adaptiveFormats) {
                            val fmtObj = fmtElem.jsonObject
                            val mime = fmtObj["mimeType"]?.jsonPrimitive?.content.orEmpty()
                            val directUrl = fmtObj["url"]?.jsonPrimitive?.content ?: continue
                            val height = fmtObj["height"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                            val clen = fmtObj["contentLength"]?.jsonPrimitive?.content?.toLongOrNull()

                            if (mime.startsWith("audio/mp4") && bestAudioUrl == null) {
                                bestAudioUrl = directUrl
                            } else if (mime.startsWith("video/mp4") && height > 0) {
                                val isAvc = mime.contains("avc1")
                                if (!videoStreamsByHeight.containsKey(height) || isAvc) {
                                    videoStreamsByHeight[height] = directUrl
                                    if (clen != null && clen > 0L) sizeByHeight[height] = clen
                                }
                            }
                        }
                    }

                    fun buildAdaptiveOrMuxed(targetHeight: Int): String? {
                        val exactOrClosest = videoStreamsByHeight.entries
                            .filter { it.key <= targetHeight }
                            .maxByOrNull { it.key }
                            ?.value
                        return if (exactOrClosest != null && bestAudioUrl != null) {
                            "$exactOrClosest$ADAPTIVE_MUX_SEPARATOR$bestAudioUrl"
                        } else {
                            muxedMp4Url ?: exactOrClosest
                        }
                    }

                    val primaryUrl = buildAdaptiveOrMuxed(1080) ?: muxedMp4Url
                    if (!primaryUrl.isNullOrBlank()) {
                        return ScrapedPageMeta(
                            title = title ?: "YouTube Video",
                            author = author ?: "YouTube Creator",
                            thumbnailUrl = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                            directVideoUrl = primaryUrl,
                            stream2160pUrl = buildAdaptiveOrMuxed(2160),
                            stream1440pUrl = buildAdaptiveOrMuxed(1440),
                            stream1080pUrl = buildAdaptiveOrMuxed(1080),
                            stream720pUrl = buildAdaptiveOrMuxed(720),
                            stream480pUrl = muxedMp4Url ?: buildAdaptiveOrMuxed(480),
                            directAudioUrl = bestAudioUrl ?: muxedMp4Url,
                            durationSec = lengthSec ?: 15L,
                            sizeByHeightBytes = sizeByHeight
                        )
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun extractInstagramStream(url: String): ScrapedPageMeta? {
        // 1. Try Instagram official embed endpoint (/p/<code/embed/captioned/)
        val shortcode = Regex("""/(?:p|reel|reels|tv)/([A-Za-z0-9_-]+)""").find(url)?.groupValues?.getOrNull(1)
        if (!shortcode.isNullOrBlank()) {
            try {
                val embedUrl = "https://www.instagram.com/p/$shortcode/embed/captioned/"
                val req = Request.Builder()
                    .url(embedUrl)
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                    )
                    .build()
                okHttpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val html = resp.body?.string().orEmpty()
                        val videoUrlMatch = Regex(""""video_url"\s*:\s*"([^"]+)"""").find(html)?.groupValues?.getOrNull(1)
                            ?: Regex("""src=["'](https://[^"']+\.mp4[^"']*)["']""").find(html)?.groupValues?.getOrNull(1)
                        val cleanVideoUrl = videoUrlMatch
                            ?.replace("\\/", "/")
                            ?.replace("\\u0026", "&")
                            ?.replace("&amp;", "&")
                        val thumbMatch = Regex(""""display_url"\s*:\s*"([^"]+)"""").find(html)?.groupValues?.getOrNull(1)
                            ?.replace("\\/", "/")
                            ?.replace("\\u0026", "&")
                        val username = Regex(""""username"\s*:\s*"([^"]+)"""").find(html)?.groupValues?.getOrNull(1)
                        if (!cleanVideoUrl.isNullOrBlank() && cleanVideoUrl.startsWith("http")) {
                            return ScrapedPageMeta(
                                title = "Instagram Reel ($shortcode)",
                                author = username ?: "Instagram Creator",
                                thumbnailUrl = thumbMatch,
                                directVideoUrl = cleanVideoUrl
                            )
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // 2. Fallback to ddinstagram / vxinstagram proxy OpenGraph
        for (proxyDomain in listOf("www.ddinstagram.com", "www.vxinstagram.com")) {
            try {
                val proxyUrl = url
                    .replace("https://www.instagram.com/", "https://$proxyDomain/")
                    .replace("https://instagram.com/", "https://$proxyDomain/")
                val req = Request.Builder()
                    .url(proxyUrl)
                    .header("User-Agent", "TelegramBot (like TwitterBot)")
                    .build()
                okHttpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val html = resp.body?.string().orEmpty()
                        val ogVideo = extractMetaProperty(html, "og:video:secure_url")
                            ?: extractMetaProperty(html, "og:video")
                        val ogTitle = extractMetaProperty(html, "og:description")
                            ?: extractMetaProperty(html, "og:title")
                        val ogImage = extractMetaProperty(html, "og:image")
                        if (!ogVideo.isNullOrBlank()) {
                            val finalVideoUrl = if (ogVideo.startsWith("http")) ogVideo else "https://$proxyDomain$ogVideo"
                            return ScrapedPageMeta(
                                title = ogTitle?.take(60)?.ifBlank { "Instagram Reel" } ?: "Instagram Reel",
                                author = "Instagram Creator",
                                thumbnailUrl = ogImage,
                                directVideoUrl = finalVideoUrl
                            )
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return null
    }

    private fun extractTikTokStream(encodedUrl: String): ScrapedPageMeta? {
        return try {
            val req = Request.Builder()
                .url("https://www.tikwm.com/api/?url=$encodedUrl&hd=1")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) KinetixPro/1.0")
                .build()
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val bodyStr = resp.body?.string().orEmpty()
                val root = json.parseToJsonElement(bodyStr).jsonObject
                val data = root["data"]?.jsonObject ?: return null
                val title = data["title"]?.jsonPrimitive?.content
                val hdUrl = data["hdplay"]?.jsonPrimitive?.content?.let {
                    if (it.startsWith("/")) "https://www.tikwm.com$it" else it
                }
                val sdUrl = data["play"]?.jsonPrimitive?.content?.let {
                    if (it.startsWith("/")) "https://www.tikwm.com$it" else it
                }
                val playUrl = hdUrl ?: sdUrl
                val rawMusicUrl = data["music"]?.jsonPrimitive?.content
                val musicUrl = rawMusicUrl?.let {
                    if (it.startsWith("/")) "https://www.tikwm.com$it" else it
                }
                val coverUrl = data["origin_cover"]?.jsonPrimitive?.content
                    ?: data["cover"]?.jsonPrimitive?.content
                val duration = data["duration"]?.jsonPrimitive?.content?.toLongOrNull()
                val author = data["author"]?.jsonObject?.get("nickname")?.jsonPrimitive?.content
                if (!playUrl.isNullOrBlank()) {
                    ScrapedPageMeta(
                        title = title?.takeIf { it.isNotBlank() } ?: "TikTok Video",
                        author = author ?: "TikTok Creator",
                        thumbnailUrl = coverUrl,
                        directVideoUrl = playUrl,
                        stream1080pUrl = hdUrl ?: playUrl,
                        stream720pUrl = sdUrl ?: playUrl,
                        directAudioUrl = musicUrl ?: playUrl,
                        durationSec = duration
                    )
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractTwitterStream(url: String): ScrapedPageMeta? {
        // Use api.fxtwitter.com (no Cloudflare challenge)
        try {
            val apiUrl = url
                .replace("https://x.com/", "https://api.fxtwitter.com/")
                .replace("https://mobile.x.com/", "https://api.fxtwitter.com/")
                .replace("https://twitter.com/", "https://api.fxtwitter.com/")
                .replace("https://mobile.twitter.com/", "https://api.fxtwitter.com/")
                .substringBefore("?")
            val req = Request.Builder()
                .url(apiUrl)
                .header("User-Agent", "KinetixPro/1.0 (Android 14)")
                .build()
            okHttpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val bodyStr = resp.body?.string().orEmpty()
                    val root = json.parseToJsonElement(bodyStr).jsonObject
                    val tweet = root["tweet"]?.jsonObject
                    val text = tweet?.get("text")?.jsonPrimitive?.content
                    val authorName = tweet?.get("author")?.jsonObject?.get("name")?.jsonPrimitive?.content
                    val mediaUrl = Regex(""""url"\s*:\s*"(https://video\.twimg\.com/[^"]+\.mp4[^"]*)"""")
                        .find(bodyStr)?.groupValues?.getOrNull(1)
                    val thumbUrl = Regex(""""thumbnail_url"\s*:\s*"(https://[^"]+)"""")
                        .find(bodyStr)?.groupValues?.getOrNull(1)
                    if (!mediaUrl.isNullOrBlank()) {
                        return ScrapedPageMeta(
                            title = text?.take(60)?.ifBlank { "X Video Post" } ?: "X Video Post",
                            author = authorName ?: "X Creator",
                            thumbnailUrl = thumbUrl,
                            directVideoUrl = mediaUrl
                        )
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun extractFacebookStream(url: String): ScrapedPageMeta? {
        return try {
            val req = Request.Builder()
                .url(url)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                )
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val html = resp.body?.string().orEmpty()
                val hdUrl = (
                    Regex(""""browser_native_hd_url"\s*:\s*"([^"]+)"""").find(html)?.groupValues?.getOrNull(1)
                        ?: Regex(""""playable_url_quality_hd"\s*:\s*"([^"]+)"""").find(html)?.groupValues?.getOrNull(1)
                        ?: Regex("""hd_src\s*:\s*"([^"]+)"""").find(html)?.groupValues?.getOrNull(1)
                    )?.replace("\\/", "/")?.replace("\\u0025", "%")?.replace("\\u0026", "&")

                val sdUrl = (
                    Regex(""""browser_native_sd_url"\s*:\s*"([^"]+)"""").find(html)?.groupValues?.getOrNull(1)
                        ?: Regex(""""playable_url"\s*:\s*"([^"]+)"""").find(html)?.groupValues?.getOrNull(1)
                        ?: Regex("""sd_src\s*:\s*"([^"]+)"""").find(html)?.groupValues?.getOrNull(1)
                    )?.replace("\\/", "/")?.replace("\\u0025", "%")?.replace("\\u0026", "&")

                val bestUrl = hdUrl ?: sdUrl
                val title = extractMetaProperty(html, "og:title") ?: extractTitleTag(html) ?: "Facebook Video"
                val thumb = extractMetaProperty(html, "og:image")?.replace("&amp;", "&")
                if (!bestUrl.isNullOrBlank() && bestUrl.startsWith("http")) {
                    ScrapedPageMeta(
                        title = title,
                        author = "Facebook Creator",
                        thumbnailUrl = thumb,
                        directVideoUrl = bestUrl,
                        stream1080pUrl = hdUrl ?: bestUrl,
                        stream720pUrl = sdUrl ?: bestUrl
                    )
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractRedditStream(url: String): ScrapedPageMeta? {
        return try {
            val clean = url.substringBefore("?").trimEnd('/') + ".json"
            val req = Request.Builder()
                .url(clean)
                .header("User-Agent", "android:com.kinetix.pro:v1.0.0 (by /u/kinetixpro)")
                .build()
            okHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val bodyStr = resp.body?.string().orEmpty()
                val fallbackUrl = Regex(""""fallback_url"\s*:\s*"(https://v\.redd\.it/[^"]+)"""")
                    .find(bodyStr)?.groupValues?.getOrNull(1)?.replace("&amp;", "&")
                val title = Regex(""""title"\s*:\s*"([^"]+)"""")
                    .find(bodyStr)?.groupValues?.getOrNull(1)
                val thumb = Regex(""""thumbnail"\s*:\s*"(https://[^"]+)"""")
                    .find(bodyStr)?.groupValues?.getOrNull(1)?.replace("&amp;", "&")
                if (!fallbackUrl.isNullOrBlank()) {
                    val baseIdUrl = fallbackUrl.substringBeforeLast("/")
                    val audioCandidateUrl = "$baseIdUrl/DASH_AUDIO_128.mp4"
                    val muxedStream = "$fallbackUrl$ADAPTIVE_MUX_SEPARATOR$audioCandidateUrl"
                    ScrapedPageMeta(
                        title = title ?: "Reddit Video Clip",
                        author = "Reddit Community",
                        thumbnailUrl = thumb,
                        directVideoUrl = muxedStream,
                        directAudioUrl = audioCandidateUrl
                    )
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractMetaProperty(html: String, property: String): String? {
        val regex = Regex(
            """<meta[^>]+(?:property|name)=["']$property["'][^>]+content=["']([^"']+)["']""",
            RegexOption.IGNORE_CASE
        )
        return regex.find(html)?.groupValues?.getOrNull(1)?.trim()
    }

    private fun extractTitleTag(html: String): String? {
        val regex = Regex("""<title[^>]*>([^<]+)</title>""", RegexOption.IGNORE_CASE)
        return regex.find(html)?.groupValues?.getOrNull(1)?.trim()
    }

    private fun buildPlatformFormatMatrix(
        url: String,
        platform: SupportedPlatform,
        scraped: ScrapedPageMeta
    ): ExtractedMediaMetadata {
        val domain = platform.displayName
        val resolvedTitle = scraped.title?.takeIf { it.isNotBlank() } ?: inferTitleFromUrl(url, platform)
        val resolvedUploader = scraped.author?.takeIf { it.isNotBlank() } ?: "$domain Creator"
        val resolvedThumb = scraped.thumbnailUrl?.takeIf { it.isNotBlank() } ?: ""

        // Notice: NO fallback to generic sample streams! If null, yt-dlp executes on url directly at download time.
        val primaryStream = scraped.directVideoUrl
        val stream2160 = scraped.stream2160pUrl ?: primaryStream
        val stream1440 = scraped.stream1440pUrl ?: primaryStream
        val stream1080 = scraped.stream1080pUrl ?: primaryStream
        val stream720 = scraped.stream720pUrl ?: primaryStream
        val stream480 = scraped.stream480pUrl ?: primaryStream
        val audioStream = scraped.directAudioUrl ?: primaryStream?.substringBefore(ADAPTIVE_MUX_SEPARATOR)

        val durationSec = (scraped.durationSec ?: 30L).coerceAtLeast(5L)
        val audioBytes = scraped.audioSizeBytes ?: (durationSec * 16_000L)

        fun resolveVideoSize(targetHeight: Int, fallbackPerSec: Long, minBytes: Long): Long {
            val exact = scraped.sizeByHeightBytes[targetHeight]
                ?: scraped.sizeByHeightBytes.entries
                    .filter { it.key <= targetHeight }
                    .maxByOrNull { it.key }
                    ?.value
            return if (exact != null && exact > 0L) {
                exact + audioBytes
            } else {
                (durationSec * fallbackPerSec).coerceAtLeast(minBytes)
            }
        }

        val videoFormats = listOf(
            DownloadFormat(
                formatId = "v_2160p",
                type = MediaType.VIDEO,
                extension = "mp4",
                resolutionLabel = VideoResolution.RES_2160P.label,
                height = 2160,
                width = 3840,
                fps = 60,
                vcodec = "avc1 / hevc",
                acodec = "mp4a.40.2",
                estimatedSizeBytes = resolveVideoSize(2160, 1_800_000L, 6_400_000L),
                isVideoOnly = false,
                isRecommended = false,
                directUrl = stream2160
            ),
            DownloadFormat(
                formatId = "v_1440p",
                type = MediaType.VIDEO,
                extension = "mp4",
                resolutionLabel = VideoResolution.RES_1440P.label,
                height = 1440,
                width = 2560,
                fps = 60,
                vcodec = "avc1",
                acodec = "mp4a.40.2",
                estimatedSizeBytes = resolveVideoSize(1440, 1_100_000L, 4_200_000L),
                isRecommended = false,
                directUrl = stream1440
            ),
            DownloadFormat(
                formatId = "v_1080p",
                type = MediaType.VIDEO,
                extension = "mp4",
                resolutionLabel = VideoResolution.RES_1080P.label,
                height = 1080,
                width = 1920,
                fps = 60,
                vcodec = "h264",
                acodec = "aac",
                estimatedSizeBytes = resolveVideoSize(1080, 650_000L, 2_490_000L),
                isRecommended = true,
                directUrl = stream1080
            ),
            DownloadFormat(
                formatId = "v_720p",
                type = MediaType.VIDEO,
                extension = "mp4",
                resolutionLabel = VideoResolution.RES_720P.label,
                height = 720,
                width = 1280,
                fps = 30,
                vcodec = "h264",
                acodec = "aac",
                estimatedSizeBytes = resolveVideoSize(720, 380_000L, 1_500_000L),
                isRecommended = false,
                directUrl = stream720
            ),
            DownloadFormat(
                formatId = "v_480p",
                type = MediaType.VIDEO,
                extension = "mp4",
                resolutionLabel = VideoResolution.RES_480P.label,
                height = 480,
                width = 854,
                fps = 30,
                vcodec = "h264",
                acodec = "aac",
                estimatedSizeBytes = resolveVideoSize(480, 220_000L, 900_000L),
                isRecommended = false,
                directUrl = stream480
            )
        )

        val audioFormats = listOf(
            DownloadFormat(
                formatId = "a_mp3_320",
                type = MediaType.AUDIO,
                extension = AudioFormat.MP3.extension,
                audioBitrateKbps = 320,
                estimatedSizeBytes = (durationSec * 40_000L).coerceAtLeast(600_000L),
                isAudioOnly = true,
                isRecommended = true,
                directUrl = audioStream
            ),
            DownloadFormat(
                formatId = "a_m4a_256",
                type = MediaType.AUDIO,
                extension = AudioFormat.M4A.extension,
                audioBitrateKbps = 256,
                estimatedSizeBytes = (durationSec * 32_000L).coerceAtLeast(480_000L),
                isAudioOnly = true,
                directUrl = audioStream
            ),
            DownloadFormat(
                formatId = "a_wav_lossless",
                type = MediaType.AUDIO,
                extension = AudioFormat.WAV.extension,
                audioBitrateKbps = 1411,
                estimatedSizeBytes = (durationSec * 176_000L).coerceAtLeast(2_200_000L),
                isAudioOnly = true,
                directUrl = audioStream
            )
        )

        val imageFormats = if (resolvedThumb.isNotBlank()) {
            listOf(
                DownloadFormat(
                    formatId = "img_maxres",
                    type = MediaType.IMAGE,
                    extension = "jpg",
                    resolutionLabel = "Original Video Thumbnail Cover",
                    estimatedSizeBytes = 350_000L,
                    directUrl = resolvedThumb
                )
            )
        } else emptyList()

        return ExtractedMediaMetadata(
            url = url,
            title = resolvedTitle,
            uploader = resolvedUploader,
            durationSeconds = durationSec,
            thumbnailUrl = resolvedThumb,
            platform = domain,
            formats = videoFormats + audioFormats + imageFormats,
            imageGalleryUrls = if (resolvedThumb.isNotBlank()) listOf(resolvedThumb) else emptyList()
        )
    }

    private fun inferTitleFromUrl(url: String, platform: SupportedPlatform): String {
        return try {
            val path = URLDecoder.decode(url, "UTF-8")
                .substringBefore("?")
                .split("/")
                .lastOrNull { it.isNotBlank() } ?: "Media Stream"
            val sanitized = path.replace("-", " ").replace("_", " ").take(42)
            "${platform.displayName} • ${sanitized.replaceFirstChar { it.uppercase() }}"
        } catch (_: Exception) {
            "${platform.displayName} Video"
        }
    }
}
