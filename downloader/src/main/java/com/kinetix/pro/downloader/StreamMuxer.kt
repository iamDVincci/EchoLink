package com.kinetix.pro.downloader

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * Lossless hardware container muxer using Android's [MediaExtractor] and [MediaMuxer],
 * with automatic fallback to embedded FFmpeg container stream-copy.
 * Supports both fragmented MP4/M4A streams and HLS MPEG-TS (`.ts`) + ADTS AAC (`.aac`) streams.
 */
object StreamMuxer {

    private val AAC_SAMPLE_RATES = intArrayOf(
        96000, 88200, 64000, 48000, 44100, 32000,
        24000, 22050, 16000, 12000, 11025, 8000, 7350
    )

    private fun buildAacLcCsd0(sampleRate: Int, channelCount: Int): ByteBuffer {
        val freqIdx = AAC_SAMPLE_RATES.indexOfFirst { it == sampleRate }.let { if (it >= 0) it else 4 }
        val chanCfg = channelCount.coerceIn(1, 7)
        val b0 = ((2 shl 3) or (freqIdx shr 1)) and 0xFF
        val b1 = (((freqIdx and 0x01) shl 7) or (chanCfg shl 3)) and 0xFF
        return ByteBuffer.wrap(byteArrayOf(b0.toByte(), b1.toByte()))
    }

    @SuppressLint("WrongConstant")
    suspend fun muxVideoAndAudio(
        videoFile: File,
        audioFile: File,
        outputFile: File,
        context: Context? = null
    ): Boolean = withContext(Dispatchers.IO) {
        if (!videoFile.exists() || videoFile.length() == 0L) return@withContext false
        if (!audioFile.exists() || audioFile.length() == 0L) {
            videoFile.copyTo(outputFile, overwrite = true)
            return@withContext outputFile.exists() && outputFile.length() > 0L
        }

        val videoExtractor = MediaExtractor()
        val audioExtractor = MediaExtractor()
        var muxer: MediaMuxer? = null

        try {
            if (outputFile.exists()) outputFile.delete()

            videoExtractor.setDataSource(videoFile.absolutePath)
            audioExtractor.setDataSource(audioFile.absolutePath)

            var videoTrackIndexInExtractor = -1
            var videoFormat: MediaFormat? = null
            for (i in 0 until videoExtractor.trackCount) {
                val format = videoExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("video/")) {
                    videoTrackIndexInExtractor = i
                    videoFormat = format
                    break
                }
            }

            var audioTrackIndexInExtractor = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until audioExtractor.trackCount) {
                val format = audioExtractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/")) {
                    audioTrackIndexInExtractor = i
                    audioFormat = format
                    break
                }
            }

            if (videoTrackIndexInExtractor < 0 || videoFormat == null) {
                return@withContext false
            }

            // Ensure AAC csd-0 is populated when reading from raw ADTS .aac stream
            if (audioFormat != null && !audioFormat.containsKey("csd-0")) {
                val mime = audioFormat.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.contains("mp4a") || mime.contains("aac")) {
                    val sr = runCatching { audioFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE) }.getOrDefault(44100)
                    val ch = runCatching { audioFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) }.getOrDefault(2)
                    audioFormat.setByteBuffer("csd-0", buildAacLcCsd0(sr, ch))
                }
            }

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerVideoTrack = muxer.addTrack(videoFormat)
            val muxerAudioTrack = if (audioTrackIndexInExtractor >= 0 && audioFormat != null) {
                runCatching { muxer.addTrack(audioFormat) }.getOrDefault(-1)
            } else {
                -1
            }

            muxer.start()

            val bufferSize = 4 * 1024 * 1024 // 4 MB buffer for high-bitrate 1080p/4K IDR frames
            val buffer = ByteBuffer.allocateDirect(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            // Copy video samples with monotonic timestamp protection
            videoExtractor.selectTrack(videoTrackIndexInExtractor)
            var videoDurationUs = 0L
            var firstVideoPtsUs = -1L
            var lastVideoPtsUs = 0L
            while (true) {
                val sampleSize = videoExtractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break
                val rawTimeUs = videoExtractor.sampleTime.coerceAtLeast(0L)
                if (firstVideoPtsUs < 0L) firstVideoPtsUs = rawTimeUs
                var normPtsUs = (rawTimeUs - firstVideoPtsUs).coerceAtLeast(0L)
                if (normPtsUs < lastVideoPtsUs) {
                    normPtsUs = lastVideoPtsUs + 1000L
                }
                lastVideoPtsUs = normPtsUs

                bufferInfo.offset = 0
                bufferInfo.size = sampleSize
                bufferInfo.presentationTimeUs = normPtsUs
                bufferInfo.flags = videoExtractor.sampleFlags and MediaCodec.BUFFER_FLAG_KEY_FRAME
                videoDurationUs = maxOf(videoDurationUs, normPtsUs)
                muxer.writeSampleData(muxerVideoTrack, buffer, bufferInfo)
                videoExtractor.advance()
            }

            // Copy audio samples up to video duration
            if (muxerAudioTrack >= 0) {
                audioExtractor.selectTrack(audioTrackIndexInExtractor)
                var firstAudioPtsUs = -1L
                var lastAudioPtsUs = 0L
                while (true) {
                    val sampleSize = audioExtractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break
                    val rawTimeUs = audioExtractor.sampleTime.coerceAtLeast(0L)
                    if (firstAudioPtsUs < 0L) firstAudioPtsUs = rawTimeUs
                    var normPtsUs = (rawTimeUs - firstAudioPtsUs).coerceAtLeast(0L)
                    if (normPtsUs <= lastAudioPtsUs && lastAudioPtsUs > 0L) {
                        normPtsUs = lastAudioPtsUs + 23219L // ~1024 samples @ 44.1kHz
                    }
                    lastAudioPtsUs = normPtsUs

                    if (videoDurationUs > 0L && normPtsUs > videoDurationUs + 1_000_000L) break

                    // Strip 7-byte or 9-byte ADTS header if left by extractor
                    var adtsOffset = 0
                    if (sampleSize > 7) {
                        val b0 = buffer.get(0).toInt() and 0xFF
                        val b1 = buffer.get(1).toInt() and 0xF0
                        if (b0 == 0xFF && b1 == 0xF0) {
                            val protectionAbsent = buffer.get(1).toInt() and 0x01
                            val hdrLen = if (protectionAbsent == 0) 9 else 7
                            if (sampleSize > hdrLen) {
                                adtsOffset = hdrLen
                            }
                        }
                    }

                    bufferInfo.offset = adtsOffset
                    bufferInfo.size = sampleSize - adtsOffset
                    bufferInfo.presentationTimeUs = normPtsUs
                    bufferInfo.flags = audioExtractor.sampleFlags and MediaCodec.BUFFER_FLAG_KEY_FRAME
                    if (bufferInfo.size > 0) {
                        muxer.writeSampleData(muxerAudioTrack, buffer, bufferInfo)
                    }
                    audioExtractor.advance()
                }
            }

            muxer.stop()
            if (outputFile.exists() && outputFile.length() > 0L) {
                return@withContext true
            }
        } catch (_: Exception) {
            // Fall through to FFmpeg muxer or direct copy fallback
        } finally {
            runCatching { videoExtractor.release() }
            runCatching { audioExtractor.release() }
            runCatching { muxer?.release() }
        }

        // Fallback 1: Use embedded FFmpeg binary if context is provided
        if (context != null) {
            val ffmpegOk = YtDlpNativeEngine.muxStreamsWithFfmpeg(context, videoFile, audioFile, outputFile)
            if (ffmpegOk && outputFile.exists() && outputFile.length() > 0L) {
                return@withContext true
            }
        }

        // Fallback 2: Copy video file directly so download still succeeds
        runCatching {
            if (videoFile.exists() && videoFile.length() > 0L) {
                videoFile.copyTo(outputFile, overwrite = true)
            }
        }
        outputFile.exists() && outputFile.length() > 0L
    }
}
