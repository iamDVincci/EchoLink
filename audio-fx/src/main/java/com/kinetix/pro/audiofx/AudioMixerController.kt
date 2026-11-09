package com.kinetix.pro.audiofx

import com.kinetix.pro.core.model.MusicConfig
import kotlin.math.sqrt

class AudioMixerController {

    /**
     * Calculates the dynamic ducking gain applied to background music.
     * When primary voice RMS energy exceeds threshold, music is ducked by -12dB (0.25x gain).
     */
    fun computeDuckingGain(
        voicePcmChunk: ShortArray,
        currentGain: Float,
        config: MusicConfig,
        thresholdRms: Float = 1200f,
        attackCoef: Float = 0.15f,
        releaseCoef: Float = 0.03f
    ): Float {
        if (!config.autoDuckOriginalSpeech || voicePcmChunk.isEmpty()) {
            return 1.0f
        }

        // Compute Root Mean Square (RMS) energy of primary voice
        var sumSquares = 0.0
        for (sample in voicePcmChunk) {
            sumSquares += (sample.toDouble() * sample.toDouble())
        }
        val rms = sqrt(sumSquares / voicePcmChunk.size).toFloat()

        // Target gain: If voice detected, drop to -12dB (~0.25x), else 1.0x
        val targetGain = if (rms > thresholdRms) 0.25f else 1.0f

        // Smooth gain transition to avoid audible pumping
        val coef = if (targetGain < currentGain) attackCoef else releaseCoef
        return currentGain + (targetGain - currentGain) * coef
    }

    /**
     * Computes fade factor [0.0 .. 1.0] for music intro/outro
     */
    fun computeFadeMultiplier(
        currentPositionMs: Long,
        totalDurationMs: Long,
        fadeInMs: Long,
        fadeOutMs: Long
    ): Float {
        var multiplier = 1.0f

        if (fadeInMs > 0 && currentPositionMs < fadeInMs) {
            multiplier *= (currentPositionMs.toFloat() / fadeInMs.toFloat()).coerceIn(0f, 1f)
        }

        val remainingMs = totalDurationMs - currentPositionMs
        if (fadeOutMs > 0 && remainingMs < fadeOutMs) {
            multiplier *= (remainingMs.toFloat() / fadeOutMs.toFloat()).coerceIn(0f, 1f)
        }

        return multiplier
    }
}
