package com.kinetix.pro.data.local

import com.kinetix.pro.core.model.AdjustmentParams
import com.kinetix.pro.core.model.AspectRatio
import com.kinetix.pro.core.model.CanvasConfig
import com.kinetix.pro.core.model.ClipSection
import com.kinetix.pro.core.model.FilterConfig
import com.kinetix.pro.core.model.FitMode
import com.kinetix.pro.core.model.MusicConfig
import com.kinetix.pro.core.model.TemplateComponent
import com.kinetix.pro.core.model.TextOverlay
import com.kinetix.pro.core.model.VoiceConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class KinetixTypeConvertersTest {

    private val converters = KinetixTypeConverters()

    @Test
    fun `round-trip clip list and canvas config`() {
        val clips = listOf(
            ClipSection(0L, 5000L, "Part 01"),
            ClipSection(5000L, 10000L, "Part 02")
        )
        val restoredClips = converters.toClipList(converters.fromClipList(clips))
        assertEquals(clips, restoredClips)

        val canvas = CanvasConfig(ratio = AspectRatio.RATIO_9_16, fitMode = FitMode.FIT_BLUR_BG, blurRadiusDp = 32f)
        val restoredCanvas = converters.toCanvasConfig(converters.fromCanvasConfig(canvas))
        assertEquals(canvas, restoredCanvas)
    }

    @Test
    fun `round-trip filter, adjustment, text, voice, music and template components`() {
        val filter = FilterConfig(filterId = "teal_orange", filterName = "Teal & Orange", intensity = 0.88f)
        assertEquals(filter, converters.toFilterConfig(converters.fromFilterConfig(filter)))

        val adjust = AdjustmentParams(exposure = 0.25f, contrast = 1.2f, saturation = 1.15f, temperature = 0.1f)
        assertEquals(adjust, converters.toAdjustmentParams(converters.fromAdjustmentParams(adjust)))

        val texts = listOf(TextOverlay(text = "Kinetix Pro", textColorHex = "#EA6113", positionNormY = 0.5f))
        assertEquals(texts, converters.toTextOverlayList(converters.fromTextOverlayList(texts)))

        val voice = VoiceConfig(presetId = "deep_studio", pitchSemitones = -3.0f, formantRatio = 0.9f)
        assertEquals(voice, converters.toVoiceConfig(converters.fromVoiceConfig(voice)))

        val music = MusicConfig(title = "Synthwave", musicVolume = 0.6f, autoDuckOriginalSpeech = true)
        assertEquals(music, converters.toMusicConfig(converters.fromMusicConfig(music)))

        val comps = TemplateComponent.entries.toList()
        assertEquals(comps, converters.toTemplateComponentList(converters.fromTemplateComponentList(comps)))
    }
}
