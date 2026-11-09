package com.kinetix.pro.audiofx

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Generates real 44.1kHz 16-bit PCM WAV background music loops on disk for Studio soundtracks
 * so the user can preview and mix real background music in the NLE Editor without external assets.
 */
object AudioTrackSynthesizer {

    suspend fun getOrGenerateStudioTrack(
        cacheDir: File,
        trackTitle: String,
        durationSeconds: Int = 12
    ): File? = withContext(Dispatchers.IO) {
        if (trackTitle == "No Background Music" || trackTitle.isBlank()) return@withContext null

        val safeName = trackTitle.replace(Regex("[^a-zA-Z0-9]"), "_").lowercase()
        val outFile = File(cacheDir, "kinetix_bgm_$safeName.wav")
        if (outFile.exists() && outFile.length() > 44L) {
            return@withContext outFile
        }

        val sampleRate = 44100
        val totalSamples = sampleRate * durationSeconds
        val pcmBytes = ByteArray(totalSamples * 2) // 16-bit mono

        val bpm = when {
            trackTitle.contains("85") || trackTitle.contains("Lofi", true) -> 85.0
            trackTitle.contains("122") || trackTitle.contains("Cyberpunk", true) -> 122.0
            trackTitle.contains("140") || trackTitle.contains("Trap", true) -> 140.0
            else -> 72.0
        }
        val isAmbient = trackTitle.contains("Ambient", true) || trackTitle.contains("Cinematic", true)
        val isCyber = trackTitle.contains("Cyberpunk", true)
        val isTrap = trackTitle.contains("Trap", true)

        val beatDurationSec = 60.0 / bpm
        val chordFreqs = when {
            isCyber -> arrayOf(
                doubleArrayOf(110.0, 220.0, 261.63, 329.63), // Am
                doubleArrayOf(87.31, 174.61, 220.0, 261.63), // F
                doubleArrayOf(130.81, 261.63, 329.63, 392.00), // C
                doubleArrayOf(98.00, 196.00, 246.94, 293.66)  // G
            )
            isTrap -> arrayOf(
                doubleArrayOf(73.42, 146.83, 220.0, 293.66), // Dm
                doubleArrayOf(65.41, 130.81, 196.0, 261.63), // Cm
                doubleArrayOf(58.27, 116.54, 174.61, 233.08), // Bb
                doubleArrayOf(55.00, 110.00, 164.81, 220.00)  // A
            )
            else -> arrayOf(
                doubleArrayOf(130.81, 196.00, 246.94, 329.63), // Cmaj7
                doubleArrayOf(110.00, 164.81, 220.00, 261.63), // Am7
                doubleArrayOf(146.83, 220.00, 261.63, 349.23), // Dm7
                doubleArrayOf(98.00, 196.00, 246.94, 349.23)   // G7
            )
        }

        var byteIdx = 0
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate.toDouble()
            val beatIndex = (t / beatDurationSec).toInt()
            val beatPos = (t % beatDurationSec) / beatDurationSec
            val barIndex = (beatIndex / 4) % chordFreqs.size
            val chord = chordFreqs[barIndex]

            // Warm pad / chord synthesis
            var padSample = 0.0
            for (f in chord) {
                padSample += sin(2.0 * PI * f * t) * 0.14
                if (isCyber) {
                    padSample += sin(2.0 * PI * (f * 2.0) * t) * 0.05
                }
            }

            // Sub bass / kick drum on beats
            var drumSample = 0.0
            if (!isAmbient) {
                val isKickBeat = (beatIndex % 2 == 0) || (isTrap && beatIndex % 4 == 3)
                if (isKickBeat && beatPos < 0.35) {
                    val kickEnv = exp(-beatPos * 14.0)
                    val pitchDrop = 52.0 + 95.0 * exp(-beatPos * 28.0)
                    drumSample += sin(2.0 * PI * pitchDrop * t) * kickEnv * 0.45
                }
                // Snare / hi-hat pulse on off-beats
                val subBeatPos = (beatPos * 2.0) % 1.0
                if (subBeatPos < 0.08) {
                    val hatEnv = exp(-subBeatPos * 40.0)
                    val noise = ((i * 1103515245 + 12345) and 0x7fffffff).toDouble() / Int.MAX_VALUE.toDouble() - 0.5
                    drumSample += noise * hatEnv * 0.12
                }
            }

            // Gentle arpeggio melody
            val arpNote = chord[(beatIndex * 2 + (if (beatPos > 0.5) 1 else 0)) % chord.size] * 2.0
            val arpEnv = exp(-(beatPos % 0.5) * 6.0)
            val arpSample = sin(2.0 * PI * arpNote * t) * arpEnv * 0.10

            val combined = (padSample + drumSample + arpSample).coerceIn(-0.95, 0.95)
            val pcmShort = (combined * Short.MAX_VALUE).toInt().toShort()
            pcmBytes[byteIdx++] = (pcmShort.toInt() and 0xFF).toByte()
            pcmBytes[byteIdx++] = ((pcmShort.toInt() shr 8) and 0xFF).toByte()
        }

        FileOutputStream(outFile).use { fos ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray(Charsets.US_ASCII))
            header.putInt(36 + pcmBytes.size)
            header.put("WAVE".toByteArray(Charsets.US_ASCII))
            header.put("fmt ".toByteArray(Charsets.US_ASCII))
            header.putInt(16) // PCM chunk size
            header.putShort(1) // PCM format
            header.putShort(1) // Mono
            header.putInt(sampleRate)
            header.putInt(sampleRate * 2) // Byte rate
            header.putShort(2) // Block align
            header.putShort(16) // Bits per sample
            header.put("data".toByteArray(Charsets.US_ASCII))
            header.putInt(pcmBytes.size)
            fos.write(header.array())
            fos.write(pcmBytes)
            fos.fd.sync()
        }

        outFile
    }
}
