package com.kinetix.pro.downloader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

sealed class DownloadProgressUpdate {
    data class Progress(
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val percent: Int,
        val speedBytesPerSec: Long,
        val etaSeconds: Long
    ) : DownloadProgressUpdate()
    data class Completed(val outputFile: File) : DownloadProgressUpdate()
    data class Failed(val error: String) : DownloadProgressUpdate()
}

class ChunkedHttpDownloader(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .dispatcher(
            Dispatcher().apply {
                maxRequests = 64
                maxRequestsPerHost = 24
            }
        )
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {

    companion object {
        private const val VISION_UA =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15"
        private const val ANDROID_UA =
            "com.google.android.youtube/20.10.38 (Linux; U; Android 14) gzip"
        private const val CHROME_UA =
            "Mozilla/5.0 (Linux; Android 14; SM-S918U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
    }

    fun downloadFile(
        url: String,
        destinationFile: File
    ): Flow<DownloadProgressUpdate> = flow {
        if (destinationFile.exists()) {
            destinationFile.delete()
        }
        val parent = destinationFile.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }

        val isHlsManifest = url.contains(".m3u8", ignoreCase = true) ||
            url.contains("/manifest/hls_playlist/", ignoreCase = true)

        if (isHlsManifest) {
            downloadHlsStream(url, destinationFile).collect { emit(it) }
            return@flow
        }

        val isGoogleVideo = url.contains("googlevideo.com/videoplayback", ignoreCase = true)
        if (isGoogleVideo) {
            downloadGoogleVideoChunked(url, destinationFile).collect { emit(it) }
            return@flow
        }

        downloadStandardHttpWithResume(url, destinationFile).collect { emit(it) }
    }.flowOn(Dispatchers.IO)

    private fun extractClen(url: String): Long {
        val m = Regex("""clen(?:%3D|=)(\d+)""").find(url)
        return m?.groupValues?.getOrNull(1)?.toLongOrNull() ?: -1L
    }

    /**
     * Strips leading RFC 8216 ID3 timestamp tags (e.g. `com.apple.streaming.transportStreamTimestamp`)
     * from packed AAC HLS audio segments so concatenated segments form a clean ADTS `.aac` stream.
     */
    private fun stripLeadingId3Tags(bytes: ByteArray): Pair<Int, Int> {
        var offset = 0
        val limit = bytes.size
        while (offset + 10 <= limit &&
            bytes[offset] == 'I'.code.toByte() &&
            bytes[offset + 1] == 'D'.code.toByte() &&
            bytes[offset + 2] == '3'.code.toByte()
        ) {
            val size = ((bytes[offset + 6].toInt() and 0x7F) shl 21) or
                ((bytes[offset + 7].toInt() and 0x7F) shl 14) or
                ((bytes[offset + 8].toInt() and 0x7F) shl 7) or
                (bytes[offset + 9].toInt() and 0x7F)
            val tagTotal = 10 + size
            if (offset + tagTotal > limit) break
            offset += tagTotal
        }
        return offset to (limit - offset)
    }

    private fun fetchBytesWithRetry(url: String, userAgent: String, rangeHeader: String? = null, maxAttempts: Int = 3): ByteArray? {
        for (attempt in 1..maxAttempts) {
            try {
                val reqBuilder = Request.Builder()
                    .url(url)
                    .header("User-Agent", userAgent)
                if (rangeHeader != null) {
                    reqBuilder.header("Range", rangeHeader)
                }
                client.newCall(reqBuilder.build()).execute().use { resp ->
                    if (resp.isSuccessful || resp.code == 206) {
                        val bytes = resp.body?.bytes()
                        if (bytes != null && bytes.isNotEmpty()) {
                            return bytes
                        }
                    } else if (resp.code == 403 && attempt == 1) {
                        // Fail fast on 403 if stream token is SABR-restricted
                        return null
                    }
                }
            } catch (_: Exception) {
                if (attempt == maxAttempts) return null
            }
            try {
                Thread.sleep(250L * attempt)
            } catch (_: InterruptedException) {
                return null
            }
        }
        return null
    }

    private fun downloadHlsStream(
        playlistUrl: String,
        destinationFile: File
    ): Flow<DownloadProgressUpdate> = flow {
        try {
            val initialBytes = fetchBytesWithRetry(playlistUrl, VISION_UA)
            if (initialBytes == null) {
                emit(DownloadProgressUpdate.Failed("Could not fetch HLS manifest"))
                return@flow
            }
            var playlistText = initialBytes.decodeToString()

            // If this is a master playlist, pick the best avc1 variant playlist
            if (playlistText.contains("#EXT-X-STREAM-INF")) {
                val lines = playlistText.lines()
                var chosenVariantUrl: String? = null
                var bestHeight = -1
                for (i in lines.indices) {
                    val line = lines[i].trim()
                    if (line.startsWith("#EXT-X-STREAM-INF:") && i + 1 < lines.size) {
                        val h = Regex("""RESOLUTION=(\d+)x(\d+)""").find(line)?.groupValues?.getOrNull(2)?.toIntOrNull() ?: 0
                        val isAvc = line.contains("avc1", ignoreCase = true)
                        val nextUrl = lines[i + 1].trim()
                        if (nextUrl.startsWith("http") && (h > bestHeight || (h == bestHeight && isAvc))) {
                            bestHeight = h
                            chosenVariantUrl = nextUrl
                        }
                    }
                }
                if (chosenVariantUrl != null) {
                    val subBytes = fetchBytesWithRetry(chosenVariantUrl, VISION_UA)
                    if (subBytes != null) {
                        playlistText = subBytes.decodeToString()
                    }
                }
            }

            val segmentUrls = playlistText.lineSequence()
                .map { it.trim() }
                .filter { it.startsWith("http://") || it.startsWith("https://") }
                .toList()

            if (segmentUrls.isEmpty()) {
                emit(DownloadProgressUpdate.Failed("HLS playlist contained no media segments"))
                return@flow
            }

            val clenEstimate = extractClen(playlistUrl).takeIf { it > 0L }
                ?: extractClen(segmentUrls.first()).takeIf { it > 0L }
                ?: -1L

            val outputStream = FileOutputStream(destinationFile, false)
            var totalDownloaded = 0L
            val startTimeMs = System.currentTimeMillis()
            var lastEmitTimeMs = startTimeMs
            var bytesSinceLastEmit = 0L

            val batchSize = 8
            val totalSegments = segmentUrls.size
            var completedSegments = 0

            try {
                for (batchStart in 0 until totalSegments step batchSize) {
                    val batchEnd = minOf(batchStart + batchSize, totalSegments)
                    val batchSlice = segmentUrls.subList(batchStart, batchEnd)

                    val segmentBuffers = coroutineScope {
                        batchSlice.map { segUrl ->
                            async(Dispatchers.IO) {
                                fetchBytesWithRetry(segUrl, VISION_UA, maxAttempts = 4)
                            }
                        }.awaitAll()
                    }

                    for (segBytes in segmentBuffers) {
                        if (segBytes == null || segBytes.isEmpty()) {
                            emit(DownloadProgressUpdate.Failed("Segment download timed out after retries"))
                            return@flow
                        }
                        val (offset, len) = stripLeadingId3Tags(segBytes)
                        if (len > 0) {
                            outputStream.write(segBytes, offset, len)
                            totalDownloaded += len
                            bytesSinceLastEmit += len
                        }
                        completedSegments++
                    }

                    val now = System.currentTimeMillis()
                    val elapsedSinceLast = (now - lastEmitTimeMs).coerceAtLeast(1L)
                    val overallElapsed = (now - startTimeMs).coerceAtLeast(1L)
                    val speed = if (elapsedSinceLast >= 150L) {
                        (bytesSinceLastEmit * 1000L) / elapsedSinceLast
                    } else {
                        (totalDownloaded * 1000L) / overallElapsed
                    }

                    val estimatedTotalBytes = if (clenEstimate > 0L) {
                        maxOf(clenEstimate, totalDownloaded)
                    } else {
                        val avgPerSeg = totalDownloaded / completedSegments.coerceAtLeast(1)
                        avgPerSeg * totalSegments
                    }

                    val percent = ((completedSegments * 100) / totalSegments).coerceIn(1, 99)
                    val remainingSegs = (totalSegments - completedSegments).coerceAtLeast(0)
                    val segsPerSec = completedSegments.toDouble() / (overallElapsed.toDouble() / 1000.0).coerceAtLeast(0.1)
                    val eta = if (segsPerSec > 0.0) (remainingSegs / segsPerSec).toLong().coerceAtLeast(1L) else 1L

                    emit(
                        DownloadProgressUpdate.Progress(
                            bytesDownloaded = totalDownloaded,
                            totalBytes = estimatedTotalBytes,
                            percent = percent,
                            speedBytesPerSec = speed,
                            etaSeconds = eta
                        )
                    )

                    lastEmitTimeMs = now
                    bytesSinceLastEmit = 0L
                }

                outputStream.flush()
                outputStream.fd.sync()
                if (destinationFile.exists() && destinationFile.length() > 0L) {
                    emit(DownloadProgressUpdate.Completed(destinationFile))
                } else {
                    emit(DownloadProgressUpdate.Failed("Downloaded HLS stream was 0 bytes"))
                }
            } finally {
                runCatching { outputStream.close() }
            }
        } catch (e: Exception) {
            emit(DownloadProgressUpdate.Failed(e.localizedMessage ?: "HLS stream download failed"))
        }
    }

    private fun downloadGoogleVideoChunked(
        url: String,
        destinationFile: File
    ): Flow<DownloadProgressUpdate> = flow {
        val ua = if (url.contains("c=ANDROID", ignoreCase = true)) ANDROID_UA else CHROME_UA
        var totalBytes = extractClen(url)

        val outputStream = FileOutputStream(destinationFile, false)
        val chunkSize = 4L * 1024L * 1024L // 4 MB per range request
        var downloaded = 0L
        val startTime = System.currentTimeMillis()

        try {
            // If clen wasn't in URL, probe first 1 MB to read Content-Range
            if (totalBytes <= 0L) {
                val probeReq = Request.Builder()
                    .url(url)
                    .header("User-Agent", ua)
                    .header("Range", "bytes=0-1048575")
                    .build()
                client.newCall(probeReq).execute().use { resp ->
                    if (!resp.isSuccessful && resp.code != 206) {
                        emit(DownloadProgressUpdate.Failed("HTTP Error ${resp.code}"))
                        return@flow
                    }
                    val contentRange = resp.header("Content-Range").orEmpty()
                    val totalFromRange = contentRange.substringAfter("/", "").toLongOrNull()
                    val bodyBytes = resp.body?.bytes()
                    if (bodyBytes == null || bodyBytes.isEmpty()) {
                        emit(DownloadProgressUpdate.Failed("Empty stream probe response"))
                        return@flow
                    }
                    outputStream.write(bodyBytes)
                    downloaded += bodyBytes.size
                    totalBytes = totalFromRange ?: bodyBytes.size.toLong()
                }
            }

            while (downloaded < totalBytes) {
                val endByte = minOf(downloaded + chunkSize - 1L, totalBytes - 1L)
                val chunkBytes = fetchBytesWithRetry(
                    url = url,
                    userAgent = ua,
                    rangeHeader = "bytes=$downloaded-$endByte",
                    maxAttempts = 3
                )
                if (chunkBytes == null || chunkBytes.isEmpty()) {
                    emit(DownloadProgressUpdate.Failed("Stream chunk blocked or interrupted at ${downloaded / 1024} KB"))
                    return@flow
                }
                outputStream.write(chunkBytes)
                downloaded += chunkBytes.size

                val elapsed = (System.currentTimeMillis() - startTime).coerceAtLeast(1L)
                val speed = (downloaded * 1000L) / elapsed
                val pct = ((downloaded * 100L) / totalBytes.coerceAtLeast(1L)).toInt().coerceIn(1, 99)
                val rem = (totalBytes - downloaded).coerceAtLeast(0L)
                val eta = if (speed > 0L) rem / speed else 1L

                emit(
                    DownloadProgressUpdate.Progress(
                        bytesDownloaded = downloaded,
                        totalBytes = totalBytes,
                        percent = pct,
                        speedBytesPerSec = speed,
                        etaSeconds = eta
                    )
                )
            }

            outputStream.flush()
            outputStream.fd.sync()
            if (destinationFile.exists() && destinationFile.length() > 0L) {
                emit(DownloadProgressUpdate.Completed(destinationFile))
            } else {
                emit(DownloadProgressUpdate.Failed("Downloaded file was 0 bytes"))
            }
        } catch (e: Exception) {
            emit(DownloadProgressUpdate.Failed(e.localizedMessage ?: "Chunked download failed"))
        } finally {
            runCatching { outputStream.close() }
        }
    }

    private fun downloadStandardHttpWithResume(
        url: String,
        destinationFile: File
    ): Flow<DownloadProgressUpdate> = flow {
        var totalReadSoFar = 0L
        var totalBytes = -1L
        var attempts = 0
        val maxResumeAttempts = 3

        val outputStream = FileOutputStream(destinationFile, false)
        val buffer = ByteArray(64 * 1024)
        var lastTime = System.currentTimeMillis()
        var bytesSinceLastTime = 0L

        try {
            while (attempts < maxResumeAttempts) {
                attempts++
                val reqBuilder = Request.Builder()
                    .url(url)
                    .header("User-Agent", CHROME_UA)
                if (totalReadSoFar > 0L) {
                    reqBuilder.header("Range", "bytes=$totalReadSoFar-")
                }

                try {
                    val response = client.newCall(reqBuilder.build()).execute()
                    if (!response.isSuccessful && response.code != 206) {
                        if (attempts >= maxResumeAttempts) {
                            emit(DownloadProgressUpdate.Failed("HTTP Error ${response.code}: ${response.message}"))
                            return@flow
                        }
                        delay(400L * attempts)
                        continue
                    }

                    val body = response.body
                    if (body == null) {
                        emit(DownloadProgressUpdate.Failed("Empty response body"))
                        return@flow
                    }

                    if (totalBytes <= 0L) {
                        val len = body.contentLength()
                        if (len > 0L) totalBytes = totalReadSoFar + len
                    }

                    body.byteStream().use { inputStream ->
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            totalReadSoFar += bytesRead
                            bytesSinceLastTime += bytesRead

                            val currentTime = System.currentTimeMillis()
                            val elapsed = currentTime - lastTime
                            if (elapsed >= 250) {
                                val speed = (bytesSinceLastTime * 1000) / elapsed.coerceAtLeast(1)
                                val percent = if (totalBytes > 0) {
                                    ((totalReadSoFar * 100) / totalBytes).toInt().coerceIn(1, 99)
                                } else {
                                    ((totalReadSoFar / 50_000L).toInt()).coerceIn(5, 95)
                                }

                                val remainingBytes = (totalBytes - totalReadSoFar).coerceAtLeast(0)
                                val eta = if (speed > 0 && totalBytes > 0) remainingBytes / speed else 1L

                                emit(
                                    DownloadProgressUpdate.Progress(
                                        bytesDownloaded = totalReadSoFar,
                                        totalBytes = totalBytes,
                                        percent = percent,
                                        speedBytesPerSec = speed,
                                        etaSeconds = eta
                                    )
                                )

                                lastTime = currentTime
                                bytesSinceLastTime = 0L
                            }
                        }
                    }

                    // Completed full stream
                    break
                } catch (e: Exception) {
                    if (attempts >= maxResumeAttempts || (totalBytes > 0L && totalReadSoFar >= totalBytes)) {
                        if (totalBytes > 0L && totalReadSoFar >= totalBytes) break
                        emit(DownloadProgressUpdate.Failed(e.localizedMessage ?: "Network read interrupted"))
                        return@flow
                    }
                    delay(500L * attempts)
                }
            }

            outputStream.flush()
            outputStream.fd.sync()
            if (destinationFile.exists() && destinationFile.length() > 0L) {
                emit(DownloadProgressUpdate.Completed(destinationFile))
            } else {
                emit(DownloadProgressUpdate.Failed("Downloaded file was 0 bytes"))
            }
        } finally {
            runCatching { outputStream.close() }
        }
    }
}
