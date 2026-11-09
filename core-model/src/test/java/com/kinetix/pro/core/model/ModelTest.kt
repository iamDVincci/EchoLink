package com.kinetix.pro.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelTest {

    @Test
    fun testTemplateApplicationReplacesPlaceholders() {
        val template = Template(
            name = "Brand Test",
            texts = listOf(
                TextOverlay(text = "Episode: {{title}}")
            )
        )

        val project = EditProject(name = "Initial Project")
        val updated = template.applyToProject(project, videoTitle = "My Epic Journey")

        assertEquals(1, updated.texts.size)
        assertEquals("Episode: My Epic Journey", updated.texts[0].text)
    }

    @Test
    fun testFilterCatalogPresetLookup() {
        assertTrue("Expected at least 40 filters at launch", FilterPreset.ALL_PRESETS.size >= 40)
        val preset = FilterPreset.findById("teal_orange")
        assertNotNull(preset)
        assertEquals("Teal & Orange", preset.name)
        assertEquals(FilterCategory.TRENDING, preset.category)
        assertTrue(preset.contrastMultiplier > 1.0f)
    }

    @Test
    fun testVoicePresetLookup() {
        assertTrue("Expected at least 10 voice presets", VoicePreset.PRESETS.size >= 10)
        val maleToFemale = VoicePreset.findById("MALE_TO_FEMALE")
        assertNotNull(maleToFemale)
        assertTrue(maleToFemale.pitchSemitones > 0f)
        assertTrue(maleToFemale.formantRatio > 1.0f)

        val femaleToMale = VoicePreset.findById("FEMALE_TO_MALE")
        assertNotNull(femaleToMale)
        assertTrue(femaleToMale.pitchSemitones < 0f)
        assertTrue(femaleToMale.formantRatio < 1.0f)
    }
}
