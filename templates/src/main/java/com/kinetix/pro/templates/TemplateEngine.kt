package com.kinetix.pro.templates

import com.kinetix.pro.core.model.AdjustmentParams
import com.kinetix.pro.core.model.AspectRatio
import com.kinetix.pro.core.model.CanvasConfig
import com.kinetix.pro.core.model.EditProject
import com.kinetix.pro.core.model.FilterConfig
import com.kinetix.pro.core.model.FitMode
import com.kinetix.pro.core.model.MusicConfig
import com.kinetix.pro.core.model.Template
import com.kinetix.pro.core.model.TemplateComponent
import com.kinetix.pro.core.model.TextAnimation
import com.kinetix.pro.core.model.TextOverlay
import com.kinetix.pro.core.model.TimingAnchor
import com.kinetix.pro.core.model.VoiceConfig
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class TemplateEngine {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Converts an active EditProject into a portable, reusable Template
     */
    fun createTemplateFromProject(
        project: EditProject,
        name: String,
        description: String,
        includedComponents: List<TemplateComponent>
    ): Template {
        return Template(
            name = name,
            description = description,
            includes = includedComponents,
            canvas = project.canvas,
            filter = project.filter,
            adjustment = project.adjustment,
            texts = project.texts,
            voice = project.voice,
            music = project.music,
            speed = project.speed
        )
    }

    /**
     * Exports a template to JSON string (.kxtemplate / .agpreset)
     */
    fun serializeTemplate(template: Template): String {
        return json.encodeToString(template)
    }

    /**
     * Parses a template from JSON string
     */
    fun deserializeTemplate(jsonString: String): Template {
        return json.decodeFromString(jsonString)
    }

    /**
     * Persists a custom or imported template as a .kxtemplate file in the given directory
     */
    fun saveCustomTemplate(directory: File, template: Template): File {
        if (!directory.exists()) directory.mkdirs()
        val safeId = template.id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val outFile = File(directory, "$safeId.kxtemplate")
        outFile.writeText(serializeTemplate(template))
        return outFile
    }

    /**
     * Loads all saved .kxtemplate files from the given directory
     */
    fun loadCustomTemplates(directory: File): List<Template> {
        if (!directory.exists()) return emptyList()
        return directory.listFiles { f -> f.isFile && (f.name.endsWith(".kxtemplate") || f.name.endsWith(".json")) }
            ?.sortedByDescending { it.lastModified() }
            ?.mapNotNull { file ->
                runCatching { deserializeTemplate(file.readText()) }.getOrNull()
            }
            ?: emptyList()
    }

    /**
     * Resolves a template by ID across both factory templates and custom saved templates
     */
    fun findTemplateById(directory: File?, id: String): Template? {
        FACTORY_TEMPLATES.firstOrNull { it.id == id }?.let { return it }
        if (directory != null) {
            return loadCustomTemplates(directory).firstOrNull { it.id == id }
        }
        return null
    }

    companion object {
        /**
         * 5 high-converting, professional factory templates bundled out of the box
         */
        val FACTORY_TEMPLATES = listOf(
            Template(
                id = "factory_viral_shorts",
                name = "Viral Shorts 9:16",
                description = "9:16 aspect ratio with blurred background, Teal & Orange color grade, and pop-up header text",
                canvas = CanvasConfig(
                    ratio = AspectRatio.RATIO_9_16,
                    fitMode = FitMode.FIT_BLUR_BG
                ),
                filter = FilterConfig(
                    filterId = "teal_orange",
                    filterName = "Teal & Orange",
                    intensity = 0.85f
                ),
                adjustment = AdjustmentParams(
                    contrast = 1.15f,
                    saturation = 1.1f,
                    vibrance = 0.2f
                ),
                texts = listOf(
                    TextOverlay(
                        text = "{{title}}",
                        fontSizeNorm = 0.055f,
                        positionNormY = 0.7f,
                        textColorHex = "#FFFFFF",
                        hasBackgroundBox = true,
                        boxColorHex = "#990D0E11",
                        inAnimation = TextAnimation.POP_BOUNCE,
                        outAnimation = TextAnimation.FADE_OUT
                    )
                ),
                music = MusicConfig(
                    musicVolume = 0.35f,
                    autoDuckOriginalSpeech = true,
                    duckingAmountDb = 12f
                )
            ),
            Template(
                id = "factory_cinematic_scope",
                name = "Cinematic Scope 2.39:1",
                description = "Ultra-wide anamorphic letterbox with Vintage 35mm film emulation and analog grain",
                canvas = CanvasConfig(
                    ratio = AspectRatio.RATIO_21_9,
                    fitMode = FitMode.FIT_SOLID_BG,
                    backgroundColorHex = "#000000"
                ),
                filter = FilterConfig(
                    filterId = "vintage_35mm",
                    filterName = "Vintage 35mm",
                    intensity = 0.8f
                ),
                adjustment = AdjustmentParams(
                    grain = 0.25f,
                    vignette = 0.35f,
                    contrast = 1.1f
                ),
                texts = listOf(
                    TextOverlay(
                        text = "{{title}}",
                        fontSizeNorm = 0.04f,
                        positionNormY = 0.82f,
                        textColorHex = "#FFE3B3",
                        hasBackgroundBox = false,
                        strokeWidthDp = 1.0f,
                        inAnimation = TextAnimation.FADE_IN,
                        outAnimation = TextAnimation.FADE_OUT
                    )
                )
            ),
            Template(
                id = "factory_retro_vhs",
                name = "Retro 90s Tape",
                description = "Authentic nostalgic VHS tape saturation, chromatic glitch dispersion, and timestamp",
                canvas = CanvasConfig(
                    ratio = AspectRatio.RATIO_4_5,
                    fitMode = FitMode.FIT_BLUR_BG
                ),
                filter = FilterConfig(
                    filterId = "vhs_90s",
                    filterName = "VHS 90s",
                    intensity = 0.9f
                ),
                adjustment = AdjustmentParams(
                    grain = 0.35f,
                    saturation = 1.25f
                ),
                voice = VoiceConfig(
                    presetId = "RETRO_RADIO",
                    pitchSemitones = 0.5f,
                    strength = 0.6f
                ),
                texts = listOf(
                    TextOverlay(
                        text = "REC ● {{date}}",
                        fontSizeNorm = 0.035f,
                        positionNormX = -0.6f,
                        positionNormY = -0.85f,
                        textColorHex = "#FF5252",
                        hasBackgroundBox = false,
                        inAnimation = TextAnimation.NONE
                    )
                )
            ),
            Template(
                id = "factory_golden_hour",
                name = "Golden Hour Glow",
                description = "Warm amber sunburst illumination, gentle skin softness, and rich travel tones",
                canvas = CanvasConfig(
                    ratio = AspectRatio.RATIO_9_16,
                    fitMode = FitMode.FIT_BLUR_BG
                ),
                filter = FilterConfig(
                    filterId = "golden_hour",
                    filterName = "Golden Hour",
                    intensity = 0.9f
                ),
                adjustment = AdjustmentParams(
                    temperature = 0.35f,
                    brightness = 0.05f,
                    saturation = 1.2f
                )
            ),
            Template(
                id = "factory_studio_tutorial",
                name = "Clean Studio",
                description = "Crisp balanced 16:9 presentation layout with subtitle box and high-contrast legibility",
                canvas = CanvasConfig(
                    ratio = AspectRatio.RATIO_16_9,
                    fitMode = FitMode.ORIGINAL_PADDING
                ),
                filter = FilterConfig(
                    filterId = "studio_clean",
                    filterName = "Studio Clean",
                    intensity = 0.75f
                ),
                adjustment = AdjustmentParams(
                    contrast = 1.05f,
                    exposure = 0.05f
                )
            )
        )
    }
}
