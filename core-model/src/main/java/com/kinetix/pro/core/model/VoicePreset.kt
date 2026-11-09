package com.kinetix.pro.core.model

import kotlinx.serialization.Serializable

@Serializable
data class VoicePreset(
    val id: String,
    val name: String,
    val description: String,
    val pitchSemitones: Float,
    val formantRatio: Float,
    val reverbAmount: Float = 0.0f,
    val isRobotized: Boolean = false,
    val iconName: String = "Mic"
) {
    companion object {
        val PRESETS = listOf(
            VoicePreset("NORMAL", "Original Voice", "No vocal transformation", 0.0f, 1.0f, 0.0f, false),
            VoicePreset("MALE_TO_FEMALE", "Male → Female", "Higher natural pitch (+5 semitones) and shifted vocal tract formant", 5.2f, 1.16f, 0.05f, false),
            VoicePreset("FEMALE_TO_MALE", "Female → Male", "Deeper chest resonance (-5.5 semitones) with widened vocal cavity", -5.5f, 0.86f, 0.05f, false),
            VoicePreset("CHIPMUNK", "Helium Chipmunk", "High-frequency cheerful pitch acceleration", 9.0f, 1.35f, 0.0f, false),
            VoicePreset("DEEP_GIANT", "Deep Giant / Demon", "Sub-bass chest growl with ominous sub-harmonics", -8.0f, 0.75f, 0.15f, false),
            VoicePreset("ROBOT_SYNTH", "Cyber Robot", "Ring-modulated 60Hz robotic carrier synthesizer", 0.0f, 1.0f, 0.0f, true),
            VoicePreset("CONCERT_HALL", "Concert Echo", "Spacious cathedral reverberation and stereo delay", 0.0f, 1.0f, 0.45f, false),
            VoicePreset("RETRO_RADIO", "Vintage Radio", "Band-pass 300Hz-3400Hz telephone / walkie-talkie speaker", 0.5f, 1.0f, 0.0f, false),
            VoicePreset("WHISPER_GHOST", "Whisper", "Noise-excited breathy vocal envelope", 2.0f, 1.1f, 0.25f, false),
            VoicePreset("ALIEN_SPEC", "Alien Morph", "Dual-frequency frequency-modulated resonant harmonics", -3.0f, 1.25f, 0.2f, true)
        )

        fun findById(id: String): VoicePreset {
            return PRESETS.firstOrNull { it.id == id } ?: PRESETS.first()
        }
    }
}
