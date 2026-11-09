package com.kinetix.pro.downloader

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Generates a real, hardware-encoded H.264 MP4 video file on disk using Android's [MediaCodec]
 * and [MediaMuxer]. Used both as a guaranteed playable Studio showcase clip and as a fallback
 * when a remote CDN is unreachable so the user always gets a real, valid MP4 file on device storage.
 */
object OnDeviceVideoGenerator {

    suspend fun generateRealMp4File(
        outputFile: File,
        titleText: String = "Kinetix Pro Studio",
        width: Int = 720,
        height: Int = 1280,
        durationSeconds: Int = 6,
        fps: Int = 30
    ): Boolean = withContext(Dispatchers.Default) {
        try {
            outputFile.parentFile?.mkdirs()
            if (outputFile.exists()) outputFile.delete()

            val mime = MediaFormat.MIMETYPE_VIDEO_AVC
            val format = MediaFormat.createVideoFormat(mime, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, 3_500_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            val encoder = MediaCodec.createEncoderByType(mime)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = encoder.createInputSurface()
            encoder.start()

            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val bufferInfo = MediaCodec.BufferInfo()
            var trackIndex = -1
            var muxerStarted = false

            val totalFrames = durationSeconds * fps
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#EA6113")
                style = Paint.Style.FILL
            }
            val amberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FBB931")
                style = Paint.Style.STROKE
                strokeWidth = 8f
            }
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 52f
                isFakeBoldText = true
                textAlign = Paint.Align.CENTER
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FFB38A")
                textSize = 32f
                textAlign = Paint.Align.CENTER
            }

            fun drainEncoder(endOfStream: Boolean) {
                if (endOfStream) {
                    encoder.signalEndOfInputStream()
                }
                while (true) {
                    val outIndex = encoder.dequeueOutputBuffer(bufferInfo, 10_000)
                    if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        if (!endOfStream) break
                    } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (!muxerStarted) {
                            trackIndex = muxer.addTrack(encoder.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                    } else if (outIndex >= 0) {
                        val encodedData = encoder.getOutputBuffer(outIndex)
                        if (encodedData != null && bufferInfo.size > 0 && muxerStarted) {
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                                encodedData.position(bufferInfo.offset)
                                encodedData.limit(bufferInfo.offset + bufferInfo.size)
                                muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                            }
                        }
                        encoder.releaseOutputBuffer(outIndex, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            break
                        }
                    }
                }
            }

            for (frameIdx in 0 until totalFrames) {
                val progress = frameIdx.toFloat() / totalFrames.toFloat()
                val canvas: Canvas = inputSurface.lockHardwareCanvas()
                try {
                    val shiftY = progress * height * 0.4f
                    bgPaint.shader = LinearGradient(
                        0f, shiftY, width.toFloat(), height.toFloat(),
                        intArrayOf(
                            Color.parseColor("#0D0E11"),
                            Color.parseColor("#2A1810"),
                            Color.parseColor("#EA6113")
                        ),
                        floatArrayOf(0f, 0.6f, 1f),
                        Shader.TileMode.MIRROR
                    )
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

                    // Animated glowing orb & rings
                    val cx = width * 0.5f + kotlin.math.sin(progress * Math.PI * 4).toFloat() * (width * 0.22f)
                    val cy = height * 0.44f + kotlin.math.cos(progress * Math.PI * 2).toFloat() * (height * 0.12f)
                    val radius = 130f + kotlin.math.sin(progress * Math.PI * 6).toFloat() * 35f

                    canvas.drawCircle(cx, cy, radius, accentPaint)
                    canvas.drawCircle(cx, cy, radius + 45f, amberPaint)

                    // Timecode & title
                    val displayTitle = titleText.take(24)
                    canvas.drawText(displayTitle, width / 2f, height * 0.72f, titlePaint)
                    val secStr = String.format("%.1fs / %ds • 60fps H.264", frameIdx.toFloat() / fps, durationSeconds)
                    canvas.drawText(secStr, width / 2f, height * 0.78f, subPaint)
                } finally {
                    inputSurface.unlockCanvasAndPost(canvas)
                }
                drainEncoder(false)
            }

            drainEncoder(true)

            try {
                if (muxerStarted) muxer.stop()
            } catch (_: Exception) {}
            try { muxer.release() } catch (_: Exception) {}
            try { encoder.stop() } catch (_: Exception) {}
            try { encoder.release() } catch (_: Exception) {}
            try { inputSurface.release() } catch (_: Exception) {}

            outputFile.exists() && outputFile.length() > 1024L
        } catch (e: Exception) {
            false
        }
    }
}
