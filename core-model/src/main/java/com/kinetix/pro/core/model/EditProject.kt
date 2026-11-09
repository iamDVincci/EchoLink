package com.kinetix.pro.core.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class EditProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Untitled Edit",
    val sourceUri: String = "",
    val sourceDurationMs: Long = 0L,
    val sourceWidth: Int = 1920,
    val sourceHeight: Int = 1080,
    val clips: List<ClipSection> = listOf(ClipSection(0L, 0L)),
    val activeClipIndex: Int = 0,
    val canvas: CanvasConfig = CanvasConfig(),
    val filter: FilterConfig = FilterConfig(),
    val adjustment: AdjustmentParams = AdjustmentParams(),
    val texts: List<TextOverlay> = emptyList(),
    val overlays: List<PipOverlay> = emptyList(),
    val adjustmentLayers: List<AdjustmentLayer> = emptyList(),
    val voice: VoiceConfig = VoiceConfig(),
    val music: MusicConfig = MusicConfig(),
    val speed: Float = 1.0f,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val lastModifiedEpochMs: Long = System.currentTimeMillis()
)

@Serializable
data class ClipSection(
    val startMs: Long = 0L,
    val endMs: Long = 0L,
    val label: String = "Part 1",
    val speed: Float = 1.0f,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val isFreezeFrame: Boolean = false,
    val isReversed: Boolean = false,
    val reversedSourceUri: String? = null,
    val cropWidthPercent: Float = 100f,  // 20% to 100%
    val cropHeightPercent: Float = 100f, // 20% to 100%
    val cropOffsetX: Float = 0f,         // -0.5f to 0.5f normalized horizontal crop center offset
    val cropOffsetY: Float = 0f,         // -0.5f to 0.5f normalized vertical crop center offset
    val cropPreset: String = "Free",
    val transition: ClipTransition = ClipTransition.NONE,
    val transitionDurationMs: Long = 500L
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(0L)
}

@Serializable
data class PipOverlay(
    val id: String = UUID.randomUUID().toString(),
    val uri: String = "",
    val label: String = "Overlay 1",
    val startMs: Long = 0L,
    val endMs: Long = 5000L,
    val positionNormX: Float = 0.0f, // -0.5 to 0.5
    val positionNormY: Float = 0.0f, // -0.5 to 0.5
    val scale: Float = 0.45f,         // 0.1 to 1.5
    val rotationDegrees: Float = 0f,
    val opacity: Float = 1.0f,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val blendMode: String = "Normal"
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(0L)
}

@Serializable
data class AdjustmentLayer(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "Adjustment 1",
    val startMs: Long = 0L,
    val endMs: Long = 5000L,
    val filter: FilterConfig = FilterConfig(),
    val adjustment: AdjustmentParams = AdjustmentParams()
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(0L)
}

@Serializable
enum class ClipTransition(val label: String, val category: String) {
    NONE("None", "None"),
    CROSS_DISSOLVE("Cross Dissolve", "Basic"),
    FADE_TO_BLACK("Fade to Black", "Basic"),
    FADE_TO_WHITE("Fade to White", "Basic"),
    SLIDE_LEFT("Slide Left", "Slide"),
    SLIDE_RIGHT("Slide Right", "Slide"),
    PUSH_UP("Push Up", "Slide"),
    PUSH_DOWN("Push Down", "Slide"),
    ZOOM_IN("Zoom In", "Camera"),
    ZOOM_OUT("Zoom Out", "Camera"),
    SPIN_CW("Spin Clockwise", "Camera"),
    BLUR_DISSOLVE("Blur Dissolve", "Camera")
}

@Serializable
enum class TailRemainderRule(val label: String) {
    SEPARATE_CLIP("Keep tail as separate clip"),
    MERGE_INTO_LAST("Merge tail into last clip"),
    TRIM_OFF("Trim off tail")
}

@Serializable
enum class AspectRatio(val ratioX: Float, val ratioY: Float, val label: String) {
    RATIO_9_16(9f, 16f, "9:16 (Shorts/TikTok)"),
    RATIO_16_9(16f, 9f, "16:9 (YouTube)"),
    RATIO_1_1(1f, 1f, "1:1 (Square)"),
    RATIO_4_5(4f, 5f, "4:5 (Portrait)"),
    RATIO_4_3(4f, 3f, "4:3 (Classic)"),
    RATIO_3_4(3f, 4f, "3:4 (Retro)"),
    RATIO_21_9(21f, 9f, "2.35:1 (Cinematic)"),
    RATIO_ORIGINAL(0f, 0f, "Original");

    val aspectRatio: Float
        get() = if (ratioY > 0f) ratioX / ratioY else 1.0f
}

@Serializable
enum class FitMode(val label: String, val description: String) {
    FIT_BLUR_BG("Fit (Blurred Background)", "Preserves full video frame with aesthetic Gaussian blur behind"),
    FIT_SOLID_BG("Fit (Color Background)", "Preserves full video frame with custom canvas background color"),
    FILL_CROP("Fill / Crop", "Scales video to cover canvas; pan to adjust position"),
    ORIGINAL_PADDING("Original with Padding", "Centered on canvas with uniform borders")
}

@Serializable
data class NormalizedRect(
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 1f,
    val height: Float = 1f
)

@Serializable
data class CanvasConfig(
    val ratio: AspectRatio = AspectRatio.RATIO_9_16,
    val fitMode: FitMode = FitMode.FIT_BLUR_BG,
    val backgroundColorHex: String = "#0D0E11",
    val blurRadiusDp: Float = 24f,
    val cropRect: NormalizedRect = NormalizedRect(),
    val rotationDegrees: Float = 0f,
    val isFlippedHorizontal: Boolean = false,
    val panOffsetNormX: Float = 0f,
    val panOffsetNormY: Float = 0f,
    val scaleFactor: Float = 1.0f,
    val gradientColorsHex: List<String> = emptyList()
)

@Serializable
enum class ExportResolution(val width: Int, val height: Int, val label: String) {
    RES_480P(854, 480, "480p (SD)"),
    RES_720P(1280, 720, "720p (HD)"),
    RES_1080P(1920, 1080, "1080p (FHD)"),
    RES_4K(3840, 2160, "4K (UHD)")
}

@Serializable
enum class ExportFps(val fps: Int, val label: String) {
    FPS_24(24, "24 fps (Cinema)"),
    FPS_25(25, "25 fps (PAL)"),
    FPS_30(30, "30 fps (Standard)"),
    FPS_50(50, "50 fps"),
    FPS_60(60, "60 fps (Smooth)")
}

@Serializable
enum class ExportBitrate(val mbps: Float, val label: String) {
    LOW(8f, "Low"),
    RECOMMENDED(16f, "Recommended"),
    HIGH(32f, "High")
}

@Serializable
data class FilterConfig(
    val filterId: String = "none",
    val filterName: String = "Normal",
    val intensity: Float = 0.8f // 0.0 to 1.0
)

@Serializable
data class AdjustmentParams(
    val exposure: Float = 0.0f,     // -50 to +50
    val brightness: Float = 0.0f,   // -50 to +50
    val contrast: Float = 1.0f,     // 0.5 to 1.5 (default 1.0f, UI: -50 to +50)
    val saturation: Float = 1.0f,   // 0.0 to 2.0 (default 1.0f, UI: -50 to +50)
    val brilliance: Float = 0.0f,   // -50 to +50 (dynamic tone micro-contrast)
    val sharpen: Float = 0.0f,      // 0 to 100 (unsharp edge crispness)
    val clarity: Float = 0.0f,      // 0 to 100 (midtone contrast)
    val highlights: Float = 0.0f,   // -50 to +50 (highlight recovery)
    val shadows: Float = 0.0f,      // -50 to +50 (shadow lift/depth)
    val whites: Float = 0.0f,       // -50 to +50 (white point clipping)
    val blacks: Float = 0.0f,       // -50 to +50 (black point floor)
    val temperature: Float = 0.0f,  // -50 to +50 (cool ↔ warm)
    val tint: Float = 0.0f,         // -50 to +50 (green ↔ magenta)
    val hue: Float = 0.0f,          // -50 to +50 (chroma shift)
    val fade: Float = 0.0f,         // 0 to 100 (matte film lift)
    val vignette: Float = 0.0f,     // 0 to 100 (radial falloff)
    val grain: Float = 0.0f,        // 0 to 100 (film grain noise)
    val vibrance: Float = 0.0f      // -50 to +50
) {
    val isModified: Boolean
        get() = exposure != 0f || brightness != 0f || contrast != 1f ||
                saturation != 1f || brilliance != 0f || sharpen != 0f ||
                clarity != 0f || highlights != 0f || shadows != 0f ||
                whites != 0f || blacks != 0f || temperature != 0f ||
                tint != 0f || hue != 0f || fade != 0f ||
                vignette != 0f || grain != 0f || vibrance != 0f
}

@Serializable
data class TextOverlay(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "Double tap to edit",
    val fontName: String = "Plus Jakarta Sans",
    val fontSizeNorm: Float = 0.05f, // percentage of screen height
    val fontSizeSp: Float = 20f,
    val textColorHex: String = "#FFFFFF",
    val textOpacity: Float = 1.0f,
    val isBold: Boolean = true,
    val isItalic: Boolean = false,
    val isUnderlined: Boolean = false,
    val strokeWidthDp: Float = 1.5f,
    val strokeColorHex: String = "#000000",
    val hasBackgroundBox: Boolean = true,
    val boxColorHex: String = "#800D0E11",
    val backgroundColorHex: String = "#800D0E11",
    val boxCornerRadiusDp: Float = 8f,
    val hasShadow: Boolean = true,
    val shadowColorHex: String = "#CC000000",
    // Normalized position [-1.0 .. 1.0] where (0,0) is screen center
    val positionNormX: Float = 0.0f,
    val positionNormY: Float = 0.65f,
    val rotationDegrees: Float = 0.0f,
    val inAnimation: TextAnimation = TextAnimation.POP_BOUNCE,
    val outAnimation: TextAnimation = TextAnimation.FADE_OUT,
    val timing: OverlayTiming = OverlayTiming()
)

@Serializable
enum class TextAnimation(val label: String) {
    NONE("None"),
    FADE_IN("Fade In"),
    FADE_OUT("Fade Out"),
    POP_BOUNCE("Pop & Bounce"),
    SLIDE_UP("Slide Up"),
    TYPEWRITER("Typewriter")
}

@Serializable
enum class TimingAnchor {
    START,
    END,
    FULL_CLIP
}

@Serializable
data class OverlayTiming(
    val anchor: TimingAnchor = TimingAnchor.FULL_CLIP,
    val offsetMs: Long = 0L,
    val durationMs: Long = 5000L
)

@Serializable
data class VoiceConfig(
    val presetId: String = "NORMAL",
    val pitchSemitones: Float = 0.0f,   // -12.0 to +12.0
    val formantRatio: Float = 1.0f,     // 0.7 to 1.4
    val strength: Float = 1.0f,         // 0.0 to 1.0
    val reverbAmount: Float = 0.0f,     // 0.0 to 1.0
    val isRobotized: Boolean = false
)

@Serializable
data class MusicConfig(
    val uri: String? = null,
    val title: String = "No Background Music",
    val originalVideoVolume: Float = 1.0f, // 0.0 to 2.0
    val musicVolume: Float = 0.40f,        // 0.0 to 2.0
    val fadeInMs: Long = 500L,
    val fadeOutMs: Long = 1500L,
    val loopToFit: Boolean = true,
    val autoDuckOriginalSpeech: Boolean = true,
    val duckingAmountDb: Float = 12.0f
)
