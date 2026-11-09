package com.kinetix.pro.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class FilterCategory(val label: String) {
    TRENDING("Trending"),
    CINEMATIC("Cinematic"),
    PORTRAIT("Portrait"),
    VIBE("Vibe"),
    MONO("Mono"),
    LIFE("Life"),
    EFFECTS("Effects")
}

@Serializable
data class FilterPreset(
    val id: String,
    val name: String,
    val category: FilterCategory,
    val defaultIntensity: Float = 0.85f,
    val description: String = "",
    val previewColorHex: String = "#EA6113",
    // Shader parameter offsets applied by this filter
    val exposureOffset: Float = 0.0f,
    val brightnessOffset: Float = 0.0f,
    val contrastMultiplier: Float = 1.0f,
    val saturationMultiplier: Float = 1.0f,
    val temperatureOffset: Float = 0.0f,
    val tintOffset: Float = 0.0f,
    val hueOffset: Float = 0.0f,
    val vignetteAmount: Float = 0.0f,
    val grainAmount: Float = 0.0f,
    val isGlitch: Boolean = false,
    val isBw: Boolean = false,
    val isSepia: Boolean = false,
    val isRgbSplit: Boolean = false
) {
    companion object {
        val ALL_PRESETS = listOf(
            // --- TRENDING ---
            FilterPreset("none", "Normal", FilterCategory.TRENDING, 1.0f, "Original untouched colors", "#FFFFFF"),
            FilterPreset("teal_orange", "Teal & Orange", FilterCategory.TRENDING, 0.9f, "Iconic blockbuster contrast with cyan shadows and warm skin tones", "#008080", temperatureOffset = 0.15f, contrastMultiplier = 1.25f, saturationMultiplier = 1.15f),
            FilterPreset("golden_hour", "Golden Hour", FilterCategory.TRENDING, 0.85f, "Dreamy warm sunset glow with amber highlights", "#FBB931", temperatureOffset = 0.35f, brightnessOffset = 0.05f, saturationMultiplier = 1.2f),
            FilterPreset("cyberpunk_neon", "Cyberpunk", FilterCategory.TRENDING, 0.9f, "High-contrast electric magenta and cyan highlights", "#E000FF", contrastMultiplier = 1.35f, saturationMultiplier = 1.4f, tintOffset = 0.25f, hueOffset = 0.1f),
            FilterPreset("film_fade", "Film Fade", FilterCategory.TRENDING, 0.8f, "Lifted matte blacks and nostalgic organic warmth", "#D4A373", contrastMultiplier = 0.9f, brightnessOffset = 0.08f, temperatureOffset = 0.1f, grainAmount = 0.15f),
            FilterPreset("noir_contrast", "Noir High", FilterCategory.TRENDING, 0.95f, "Deep inky blacks and crisp silver highlights", "#222222", isBw = true, contrastMultiplier = 1.4f),

            // --- CINEMATIC ---
            FilterPreset("cine_teal", "Cine Teal", FilterCategory.CINEMATIC, 0.85f, "Clean cinematic teal grading for modern cinema", "#1A535C", contrastMultiplier = 1.2f, temperatureOffset = -0.15f, tintOffset = -0.1f),
            FilterPreset("vintage_35mm", "Vintage 35mm", FilterCategory.CINEMATIC, 0.8f, "Analog Kodak film emulsion with subtle grain", "#C27D38", contrastMultiplier = 1.1f, temperatureOffset = 0.2f, grainAmount = 0.25f),
            FilterPreset("moody_blue", "Moody Blue", FilterCategory.CINEMATIC, 0.85f, "Atmospheric cold tones for dramatic storytelling", "#2B4C7E", temperatureOffset = -0.3f, saturationMultiplier = 0.9f, vignetteAmount = 0.3f),
            FilterPreset("matrix_emerald", "Emerald", FilterCategory.CINEMATIC, 0.8f, "Sci-fi jade and green ambient grading", "#2A9D8F", tintOffset = -0.35f, contrastMultiplier = 1.15f),
            FilterPreset("desert_warmth", "Desert Dune", FilterCategory.CINEMATIC, 0.85f, "Bleached sun-drenched desert tones", "#E76F51", exposureOffset = 0.1f, temperatureOffset = 0.3f, saturationMultiplier = 0.95f),
            FilterPreset("bleach_bypass", "Bleach Bypass", FilterCategory.CINEMATIC, 0.9f, "Desaturated gritty war cinema look", "#6C757D", saturationMultiplier = 0.5f, contrastMultiplier = 1.45f),

            // --- PORTRAIT ---
            FilterPreset("soft_skin", "Soft Skin", FilterCategory.PORTRAIT, 0.75f, "Flattering radiant warmth and gentle tone curve", "#F4ACB7", brightnessOffset = 0.05f, temperatureOffset = 0.08f, contrastMultiplier = 0.95f),
            FilterPreset("warm_portrait", "Warm Peach", FilterCategory.PORTRAIT, 0.8f, "Soft peach complexion illumination", "#FFCAD4", temperatureOffset = 0.15f, tintOffset = 0.05f),
            FilterPreset("ivory_glow", "Ivory Glow", FilterCategory.PORTRAIT, 0.85f, "High-key editorial fashion luminance", "#E8E8E4", exposureOffset = 0.12f, brightnessOffset = 0.06f, contrastMultiplier = 1.05f),
            FilterPreset("sunset_bronze", "Sunset Bronze", FilterCategory.PORTRAIT, 0.85f, "Sun-kissed tanned golden profile", "#DDA15E", temperatureOffset = 0.28f, saturationMultiplier = 1.2f),
            FilterPreset("studio_clean", "Studio Clean", FilterCategory.PORTRAIT, 0.8f, "Pristine commercial balanced lighting", "#F8F9FA", contrastMultiplier = 1.1f, saturationMultiplier = 1.05f),

            // --- VIBE ---
            FilterPreset("vhs_90s", "VHS 90s", FilterCategory.VIBE, 0.85f, "Authentic tape saturation, chromatic split, and grain", "#8338EC", saturationMultiplier = 1.25f, contrastMultiplier = 1.1f, grainAmount = 0.35f, isRgbSplit = true),
            FilterPreset("retro_film", "Retro Fade", FilterCategory.VIBE, 0.8f, "Faded photo album with washed yellow-green cast", "#CCD5AE", contrastMultiplier = 0.85f, temperatureOffset = 0.18f, brightnessOffset = 0.08f),
            FilterPreset("dreamy_bloom", "Dreamy", FilterCategory.VIBE, 0.8f, "Ethereal luminous pastel highlights", "#E2AFFC", exposureOffset = 0.15f, contrastMultiplier = 0.9f, saturationMultiplier = 1.1f),
            FilterPreset("sunset_drive", "Sunset Drive", FilterCategory.VIBE, 0.9f, "Deep dusk purple shadows and orange skies", "#9B5DE5", temperatureOffset = 0.2f, tintOffset = 0.2f, vignetteAmount = 0.3f),
            FilterPreset("tokyo_drift", "Tokyo Night", FilterCategory.VIBE, 0.85f, "Vibrant street lights with cool shadow contrast", "#00F5D4", contrastMultiplier = 1.3f, saturationMultiplier = 1.3f, temperatureOffset = -0.15f),
            FilterPreset("pastel_pop", "Pastel Pop", FilterCategory.VIBE, 0.75f, "Sweet soft candyland palette", "#F15BB5", saturationMultiplier = 1.15f, contrastMultiplier = 0.95f, brightnessOffset = 0.08f),

            // --- MONO ---
            FilterPreset("classic_bw", "Classic B&W", FilterCategory.MONO, 1.0f, "Balanced neutral black and white conversion", "#495057", isBw = true),
            FilterPreset("silver_grain", "Silver 400", FilterCategory.MONO, 1.0f, "High-ISO street photography with gritty grain", "#343A40", isBw = true, contrastMultiplier = 1.25f, grainAmount = 0.3f),
            FilterPreset("sepia_vintage", "Sepia Antique", FilterCategory.MONO, 0.9f, "Warm nostalgic brown sepia tone", "#7F4F24", isSepia = true),
            FilterPreset("high_key_bw", "High Key B&W", FilterCategory.MONO, 1.0f, "Minimalist bright architectural monochrome", "#ADB5BD", isBw = true, exposureOffset = 0.2f, contrastMultiplier = 1.1f),
            FilterPreset("carbon_dark", "Carbon", FilterCategory.MONO, 1.0f, "Deep velvety dark moody monochrome", "#16181D", isBw = true, contrastMultiplier = 1.5f, vignetteAmount = 0.4f),

            // --- LIFE ---
            FilterPreset("fresh_nature", "Fresh Nature", FilterCategory.LIFE, 0.85f, "Rich verdant greens and vibrant natural colors", "#588157", saturationMultiplier = 1.3f, temperatureOffset = -0.05f, tintOffset = -0.15f),
            FilterPreset("food_delight", "Food Pop", FilterCategory.LIFE, 0.9f, "Appetizing warm hues with amplified red & yellow vibrancy", "#F77F00", saturationMultiplier = 1.35f, temperatureOffset = 0.2f, contrastMultiplier = 1.15f),
            FilterPreset("travel_wander", "Wanderlust", FilterCategory.LIFE, 0.85f, "Vivid skies and rich travel adventure colors", "#0077B6", saturationMultiplier = 1.25f, contrastMultiplier = 1.15f, temperatureOffset = 0.05f),
            FilterPreset("ocean_breeze", "Ocean Breeze", FilterCategory.LIFE, 0.85f, "Deep turquoise water and sunlit beaches", "#00B4D8", temperatureOffset = -0.2f, saturationMultiplier = 1.2f, tintOffset = -0.1f),
            FilterPreset("autumn_leaves", "Autumn Leaves", FilterCategory.LIFE, 0.9f, "Rich copper, russet, and golden amber foliage", "#BC4749", temperatureOffset = 0.35f, tintOffset = 0.1f, saturationMultiplier = 1.25f),

            // --- EFFECTS ---
            FilterPreset("cyber_glitch", "Cyber Glitch", FilterCategory.EFFECTS, 0.9f, "Digital chromatic dispersion with synthetic noise", "#FF0055", isGlitch = true, isRgbSplit = true),
            FilterPreset("rgb_shift", "RGB Shift", FilterCategory.EFFECTS, 0.85f, "Prismatic color lens fringing and channel split", "#00FF66", isRgbSplit = true),
            FilterPreset("heavy_vignette", "Cinema Lens", FilterCategory.EFFECTS, 0.85f, "Dramatic focal vignette and anamorphic depth", "#1F222A", vignetteAmount = 0.65f, contrastMultiplier = 1.15f),
            FilterPreset("film_grain_35", "Pure Grain", FilterCategory.EFFECTS, 0.8f, "Textured 35mm celluloid analog grain overlay", "#6C757D", grainAmount = 0.45f),
            FilterPreset("radiant_glow", "Radiant Bloom", FilterCategory.EFFECTS, 0.85f, "Diffused dreamy glow across highlight boundaries", "#FFE3B3", exposureOffset = 0.18f, contrastMultiplier = 0.92f, brightnessOffset = 0.1f),
            FilterPreset("sunset_ember", "Sunset Ember", FilterCategory.TRENDING, 0.9f, "Deep vermilion sunset warmth with glowing highlights", "#EA6113", temperatureOffset = 0.32f, contrastMultiplier = 1.18f, saturationMultiplier = 1.25f),
            FilterPreset("nordic_frost", "Nordic Frost", FilterCategory.CINEMATIC, 0.85f, "Crisp sub-zero Scandinavian blue and desaturated slate", "#48CAE4", temperatureOffset = -0.35f, saturationMultiplier = 0.82f, contrastMultiplier = 1.2f),
            FilterPreset("velvet_dusk", "Velvet Dusk", FilterCategory.VIBE, 0.85f, "Twilight magenta and indigo shadow grading", "#7209B7", tintOffset = 0.28f, temperatureOffset = -0.1f, vignetteAmount = 0.25f),
            FilterPreset("kodachrome_64", "Kodachrome 64", FilterCategory.LIFE, 0.9f, "Iconic archival slide film with rich primary saturation", "#D62828", contrastMultiplier = 1.22f, saturationMultiplier = 1.28f, temperatureOffset = 0.12f, grainAmount = 0.18f)
        )

        fun findById(id: String): FilterPreset {
            return ALL_PRESETS.firstOrNull { it.id == id } ?: ALL_PRESETS.first()
        }
    }
}
