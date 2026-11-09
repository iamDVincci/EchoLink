package com.kinetix.pro.data.local

import androidx.room.TypeConverter
import com.kinetix.pro.core.model.AdjustmentParams
import com.kinetix.pro.core.model.CanvasConfig
import com.kinetix.pro.core.model.ClipSection
import com.kinetix.pro.core.model.FilterConfig
import com.kinetix.pro.core.model.MusicConfig
import com.kinetix.pro.core.model.TemplateComponent
import com.kinetix.pro.core.model.TextOverlay
import com.kinetix.pro.core.model.VoiceConfig
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class KinetixTypeConverters {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @TypeConverter
    fun fromClipList(value: List<ClipSection>): String = json.encodeToString(value)

    @TypeConverter
    fun toClipList(value: String): List<ClipSection> =
        if (value.isBlank()) emptyList() else json.decodeFromString(value)

    @TypeConverter
    fun fromCanvasConfig(value: CanvasConfig): String = json.encodeToString(value)

    @TypeConverter
    fun toCanvasConfig(value: String): CanvasConfig =
        if (value.isBlank()) CanvasConfig() else json.decodeFromString(value)

    @TypeConverter
    fun fromFilterConfig(value: FilterConfig): String = json.encodeToString(value)

    @TypeConverter
    fun toFilterConfig(value: String): FilterConfig =
        if (value.isBlank()) FilterConfig() else json.decodeFromString(value)

    @TypeConverter
    fun fromAdjustmentParams(value: AdjustmentParams): String = json.encodeToString(value)

    @TypeConverter
    fun toAdjustmentParams(value: String): AdjustmentParams =
        if (value.isBlank()) AdjustmentParams() else json.decodeFromString(value)

    @TypeConverter
    fun fromTextOverlayList(value: List<TextOverlay>): String = json.encodeToString(value)

    @TypeConverter
    fun toTextOverlayList(value: String): List<TextOverlay> =
        if (value.isBlank()) emptyList() else json.decodeFromString(value)

    @TypeConverter
    fun fromVoiceConfig(value: VoiceConfig): String = json.encodeToString(value)

    @TypeConverter
    fun toVoiceConfig(value: String): VoiceConfig =
        if (value.isBlank()) VoiceConfig() else json.decodeFromString(value)

    @TypeConverter
    fun fromMusicConfig(value: MusicConfig): String = json.encodeToString(value)

    @TypeConverter
    fun toMusicConfig(value: String): MusicConfig =
        if (value.isBlank()) MusicConfig() else json.decodeFromString(value)

    @TypeConverter
    fun fromTemplateComponentList(value: List<TemplateComponent>): String = json.encodeToString(value)

    @TypeConverter
    fun toTemplateComponentList(value: String): List<TemplateComponent> =
        if (value.isBlank()) emptyList() else json.decodeFromString(value)
}
