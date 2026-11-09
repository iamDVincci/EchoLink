package com.kinetix.pro.audiofx

import com.kinetix.pro.core.model.VoiceConfig
import com.kinetix.pro.core.model.VoicePreset
import kotlin.math.PI
import kotlin.math.sin

class VoiceEffectEngine {

    /**
     * Applies vocal DSP modulation to 16-bit PCM buffer (interleaved stereo or mono).
     */
    fun processPcmBuffer(
        buffer: ShortArray,
        sampleRate: Int,
        channels: Int,
        config: VoiceConfig
    ): ShortArray {
        if (config.presetId == "NORMAL" && config.pitchSemitones == 0f && !config.isRobotized && config.reverbAmount == 0f) {
            return buffer // Untouched
        }

        val output = ShortArray(buffer.size)
        val strength = config.strength.coerceIn(0f, 1f)

        // Delay buffer for echo
        val delaySamples = (sampleRate * 0.25).toInt() * channels
        val delayBuffer = FloatArray(delaySamples)
        var delayIdx = 0

        // Robot carrier frequency (75Hz)
        val carrierFreq = 75.0
        var phase = 0.0
        val phaseIncrement = (2.0 * PI * carrierFreq) / sampleRate

        for (i in buffer.indices) {
            var sample = buffer[i].toFloat()

            // 1. Robot synth ring modulation
            if (config.isRobotized) {
                val modulation = sin(phase).toFloat()
                sample *= modulation
                phase += phaseIncrement
                if (phase >= 2.0 * PI) phase -= 2.0 * PI
            }

            // 2. Reverb / Echo delay feedback
            if (config.reverbAmount > 0.05f) {
                val delayedSample = delayBuffer[delayIdx]
                delayBuffer[delayIdx] = sample + delayedSample * 0.45f
                sample += delayedSample * config.reverbAmount
                delayIdx = (delayIdx + 1) % delaySamples
            }

            // 3. Interpolate with dry signal based on effect strength
            val dry = buffer[i].toFloat()
            val blended = dry * (1f - strength) + sample * strength

            output[i] = blended.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        return output
    }

    /**
     * Converts semitones to playback speed / pitch ratio.
     * Ratio = 2^(semitones / 12)
     */
    fun semitonesToPitchRatio(semitones: Float): Float {
        return Math.pow(2.0, (semitones / 12.0).toDouble()).toFloat()
    }
}
