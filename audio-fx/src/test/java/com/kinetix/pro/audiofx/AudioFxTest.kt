package com.kinetix.pro.audiofx

import com.kinetix.pro.core.model.MusicConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioFxTest {

    private val voiceEngine = VoiceEffectEngine()
    private val audioMixer = AudioMixerController()

    @Test
    fun testSemitonePitchRatioConversion() {
        // Octave up (+12 semitones) = 2.0x pitch
        val octaveUp = voiceEngine.semitonesToPitchRatio(12f)
        assertEquals(2.0f, octaveUp, 0.01f)

        // Octave down (-12 semitones) = 0.5x pitch
        val octaveDown = voiceEngine.semitonesToPitchRatio(-12f)
        assertEquals(0.5f, octaveDown, 0.01f)

        // Zero semitones = 1.0x pitch
        val neutral = voiceEngine.semitonesToPitchRatio(0f)
        assertEquals(1.0f, neutral, 0.001f)
    }

    @Test
    fun testDuckingGainAttenuatesWhenVoicePresent() {
        val loudVoiceChunk = ShortArray(100) { 8000.toShort() } // Loud RMS
        val config = MusicConfig(autoDuckOriginalSpeech = true, duckingAmountDb = 12f)

        val duckedGain = audioMixer.computeDuckingGain(
            voicePcmChunk = loudVoiceChunk,
            currentGain = 1.0f,
            config = config,
            thresholdRms = 1000f
        )

        // Gain should ramp down towards 0.25 (-12dB)
        assertTrue("Ducked gain should be lower than 1.0", duckedGain < 1.0f)
    }

    @Test
    fun testFadeMultiplierCalculatesLinearFades() {
        // Intro midpoint
        val introHalf = audioMixer.computeFadeMultiplier(
            currentPositionMs = 250L,
            totalDurationMs = 10000L,
            fadeInMs = 500L,
            fadeOutMs = 1500L
        )
        assertEquals(0.5f, introHalf, 0.05f)

        // Outro midpoint
        val outroHalf = audioMixer.computeFadeMultiplier(
            currentPositionMs = 9250L,
            totalDurationMs = 10000L,
            fadeInMs = 500L,
            fadeOutMs = 1500L
        )
        assertEquals(0.5f, outroHalf, 0.05f)
    }
}
