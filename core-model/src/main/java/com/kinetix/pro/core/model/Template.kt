package com.kinetix.pro.core.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Template(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "My Brand Look",
    val description: String = "Reusable CapCut-grade styling recipe",
    val schemaVersion: Int = 1,
    val coverFrameUrl: String? = null,
    val includes: List<TemplateComponent> = listOf(
        TemplateComponent.FILTER,
        TemplateComponent.ADJUSTMENTS,
        TemplateComponent.CANVAS,
        TemplateComponent.TEXTS,
        TemplateComponent.VOICE,
        TemplateComponent.MUSIC
    ),
    val canvas: CanvasConfig = CanvasConfig(),
    val filter: FilterConfig = FilterConfig(),
    val adjustment: AdjustmentParams = AdjustmentParams(),
    val texts: List<TextOverlay> = emptyList(),
    val voice: VoiceConfig = VoiceConfig(),
    val music: MusicConfig = MusicConfig(),
    val speed: Float = 1.0f,
    val author: String = "Kinetix Pro User",
    val createdAtEpochMs: Long = System.currentTimeMillis()
) {
    fun applyToProject(baseProject: EditProject, videoTitle: String = "My Video"): EditProject {
        val resolvedTexts = texts.map { overlay ->
            val resolvedText = overlay.text
                .replace("{{title}}", videoTitle)
                .replace("{{date}}", java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date()))
            overlay.copy(text = resolvedText)
        }

        return baseProject.copy(
            canvas = if (includes.contains(TemplateComponent.CANVAS)) canvas else baseProject.canvas,
            filter = if (includes.contains(TemplateComponent.FILTER)) filter else baseProject.filter,
            adjustment = if (includes.contains(TemplateComponent.ADJUSTMENTS)) adjustment else baseProject.adjustment,
            texts = if (includes.contains(TemplateComponent.TEXTS)) resolvedTexts else baseProject.texts,
            voice = if (includes.contains(TemplateComponent.VOICE)) voice else baseProject.voice,
            music = if (includes.contains(TemplateComponent.MUSIC)) music else baseProject.music,
            speed = if (includes.contains(TemplateComponent.SPEED)) speed else baseProject.speed,
            lastModifiedEpochMs = System.currentTimeMillis()
        )
    }
}

@Serializable
enum class TemplateComponent(val label: String) {
    CANVAS("Canvas & Ratio"),
    FILTER("Color Filter & LUT"),
    ADJUSTMENTS("Color Adjustments"),
    TEXTS("Text Overlays"),
    VOICE("Voice Effect"),
    MUSIC("Background Music"),
    SPEED("Speed Setting")
}
