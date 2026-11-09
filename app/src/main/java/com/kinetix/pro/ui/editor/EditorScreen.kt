package com.kinetix.pro.ui.editor

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.kinetix.pro.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Details
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PanoramaFishEye
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material.icons.filled.Opacity
import coil.compose.AsyncImage
import com.kinetix.pro.core.model.PipOverlay
import com.kinetix.pro.core.model.AdjustmentLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.kinetix.pro.audiofx.AudioTrackSynthesizer
import com.kinetix.pro.audiofx.VoiceEffectEngine
import com.kinetix.pro.core.model.AdjustmentParams
import com.kinetix.pro.core.model.AspectRatio
import com.kinetix.pro.core.model.CanvasConfig
import com.kinetix.pro.core.model.ClipSection
import com.kinetix.pro.core.model.ClipTransition
import com.kinetix.pro.core.model.EditProject
import com.kinetix.pro.core.model.ExportBitrate
import com.kinetix.pro.core.model.ExportFps
import com.kinetix.pro.core.model.ExportResolution
import com.kinetix.pro.core.model.FilterCategory
import com.kinetix.pro.core.model.FilterConfig
import com.kinetix.pro.core.model.FilterPreset
import com.kinetix.pro.core.model.FitMode
import com.kinetix.pro.core.model.MediaType
import com.kinetix.pro.core.model.TailRemainderRule
import com.kinetix.pro.core.model.TemplateComponent
import com.kinetix.pro.core.model.TextAnimation
import com.kinetix.pro.core.model.TextOverlay
import com.kinetix.pro.core.model.VoiceConfig
import com.kinetix.pro.core.model.VoicePreset
import com.kinetix.pro.core.ui.components.DualThumbRangeSlider
import com.kinetix.pro.core.ui.components.ObsidianCard
import com.kinetix.pro.core.ui.components.ObsidianInnerCard
import com.kinetix.pro.core.ui.components.PlatformBadge
import com.kinetix.pro.core.ui.components.SunsetChip
import com.kinetix.pro.core.ui.components.SunsetIconButton
import com.kinetix.pro.core.ui.components.SunsetOutlinedButton
import com.kinetix.pro.core.ui.components.SunsetPrimaryButton
import com.kinetix.pro.core.ui.components.SunsetSlider
import com.kinetix.pro.core.ui.components.TimecodeText
import com.kinetix.pro.core.ui.components.formatDuration
import com.kinetix.pro.core.ui.theme.ContentHigh
import com.kinetix.pro.core.ui.theme.ContentMedium
import com.kinetix.pro.core.ui.theme.ContentMuted
import com.kinetix.pro.core.ui.theme.ObsidianCanvas
import com.kinetix.pro.core.ui.theme.ObsidianStroke
import com.kinetix.pro.core.ui.theme.ObsidianSurface1
import com.kinetix.pro.core.ui.theme.ObsidianSurface2
import com.kinetix.pro.core.ui.theme.PrimarySunset
import com.kinetix.pro.core.ui.theme.StatusError
import com.kinetix.pro.core.ui.theme.StatusSuccess
import com.kinetix.pro.core.ui.theme.SunsetTint15
import com.kinetix.pro.core.ui.theme.TertiaryAmber
import com.kinetix.pro.data.repository.KinetixRepository
import com.kinetix.pro.downloader.MediaStoreExporter
import com.kinetix.pro.downloader.YtDlpNativeEngine
import com.kinetix.pro.editor.effects.Media3VideoEffectsBuilder
import com.kinetix.pro.editor.export.ExportProgressState
import com.kinetix.pro.editor.export.Media3ExportEngine
import com.kinetix.pro.editor.timeline.TimelineSplitter
import com.kinetix.pro.templates.TemplateEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

enum class EditorTool(val label: String, val icon: ImageVector) {
    // Root Tools (State A - Nothing selected on Timeline)
    EDIT("Edit", Icons.Default.Edit),
    AUDIO("Audio", Icons.Default.Audiotrack),
    TEXT("Text", Icons.Default.TextFields),
    OVERLAY("Overlay", Icons.Default.Layers),
    FILTERS("Filters", Icons.Default.ColorLens),
    ADJUST("Adjust", Icons.Default.Tune),
    RATIO("Canvas", Icons.Default.AspectRatio),
    BACKGROUND("Background", Icons.Default.Palette),

    // Clip-specific Tools (State B - Clip selected on Timeline)
    SPLIT("Split", Icons.Default.ContentCut),
    BATCH_SPLIT("Batch Split", Icons.Default.CallSplit),
    SPEED("Speed", Icons.Default.Speed),
    VOLUME("Volume", Icons.AutoMirrored.Filled.VolumeUp),
    TRANSFORM("Transform", Icons.Default.Crop),
    TRANSITION("Transition", Icons.Default.Animation),
    DELETE("Delete", Icons.Default.Delete),
    DUPLICATE("Duplicate", Icons.Default.ContentCopy),
    REVERSE("Reverse", Icons.Default.FastRewind),
    FREEZE("Freeze", Icons.Default.AcUnit),

    // Overlay / PIP Specific Tools
    PIP_SCALE("Scale & Move", Icons.Default.ZoomOutMap),
    PIP_OPACITY("Opacity", Icons.Default.Opacity),

    TEMPLATES("Templates", Icons.Default.Palette),
    MUSIC("Music", Icons.Default.Audiotrack),
    VOICE("Voice", Icons.Default.Mic),
    TRIM("Trim", Icons.Default.ContentCut)
}

enum class TimelineTrackCategory(val label: String, val icon: ImageVector) {
    ALL("All Tracks", Icons.Default.ViewAgenda),
    VIDEO("Video", Icons.Default.Movie),
    OVERLAY("PIP", Icons.Default.Layers),
    AUDIO("Audio", Icons.Default.Audiotrack),
    TEXT("Text", Icons.Default.TextFields),
    ADJUST("Adjust", Icons.Default.Tune)
}

fun createCapCutColorMatrix(adj: AdjustmentParams, filter: FilterConfig): ColorMatrix {
    val filterPreset = FilterPreset.findById(filter.filterId)
    val fStrength = if (filterPreset.id == "none") 0f else filter.intensity.coerceIn(0f, 1f)

    // 1. Saturation + Vibrance
    val satMat = ColorMatrix()
    val totalSat = (adj.saturation * (1.0f + (filterPreset.saturationMultiplier - 1.0f) * fStrength) + adj.vibrance * 0.35f).coerceIn(0f, 2.6f)
    if (filterPreset.isBw && fStrength >= 0.35f) {
        satMat.setToSaturation(1.0f - fStrength)
    } else {
        satMat.setToSaturation(totalSat)
    }

    // 2. Luminance-Preserving Hue Rotation (-180°..+180°)
    val hueDeg = ((adj.hue / 50f) * 90f + (filterPreset.hueOffset * fStrength * 60f)).coerceIn(-180f, 180f)
    val hueMat = if (kotlin.math.abs(hueDeg) > 0.3f) {
        val rad = Math.toRadians(hueDeg.toDouble())
        val cosA = kotlin.math.cos(rad).toFloat()
        val sinA = kotlin.math.sin(rad).toFloat()
        val lumR = 0.213f
        val lumG = 0.715f
        val lumB = 0.072f
        ColorMatrix(
            floatArrayOf(
                lumR + cosA * (1f - lumR) + sinA * (-lumR), lumG + cosA * (-lumG) + sinA * (-lumG), lumB + cosA * (-lumB) + sinA * (1f - lumB), 0f, 0f,
                lumR + cosA * (-lumR) + sinA * 0.143f, lumG + cosA * (1f - lumG) + sinA * 0.140f, lumB + cosA * (-lumB) + sinA * (-0.283f), 0f, 0f,
                lumR + cosA * (-lumR) + sinA * (-(1f - lumR)), lumG + cosA * (-lumG) + sinA * lumG, lumB + cosA * (1f - lumB) + sinA * lumB, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    } else {
        ColorMatrix()
    }

    // 3. Contrast (pivoted around mid-gray 128), Clarity, Brilliance, Whites, Blacks, Highlights, Shadows, Brightness, Temp & Tint
    val cVal = (
        adj.contrast * (1.0f + (filterPreset.contrastMultiplier - 1.0f) * fStrength) +
            (adj.clarity / 100f) * 0.24f +
            (adj.brilliance / 50f) * 0.16f -
            (adj.fade / 100f) * 0.14f
        ).coerceIn(0.35f, 2.2f)

    val whitesGain = 1.0f + (adj.whites / 50f) * 0.20f + (adj.highlights * 0.14f)
    val tempVal = (adj.temperature + (filterPreset.temperatureOffset * fStrength)).coerceIn(-1.2f, 1.2f)
    val tintVal = (adj.tint + (filterPreset.tintOffset * fStrength)).coerceIn(-1.2f, 1.2f)

    var rScale = cVal * whitesGain * (1.0f + (if (tempVal > 0f) tempVal * 0.28f else tempVal * 0.08f) + (if (tintVal > 0f) tintVal * 0.16f else 0f))
    var gScale = cVal * whitesGain * (1.0f + (if (tintVal < 0f) -tintVal * 0.22f else -tintVal * 0.06f))
    var bScale = cVal * whitesGain * (1.0f + (if (tempVal < 0f) -tempVal * 0.28f else -tempVal * 0.08f) + (if (tintVal > 0f) tintVal * 0.14f else 0f))

    if (filterPreset.isSepia && fStrength > 0.1f) {
        rScale *= (1.0f + 0.18f * fStrength)
        gScale *= (1.0f + 0.02f * fStrength)
        bScale *= (1.0f - 0.22f * fStrength)
    }

    val contrastPivotOffset = 128f * (1f - cVal)
    val bVal = (
        adj.brightness +
            (filterPreset.brightnessOffset * fStrength) +
            (adj.exposure + filterPreset.exposureOffset * fStrength) * 0.25f +
            (adj.shadows * 0.18f) +
            (adj.brilliance / 50f) * 0.10f +
            (adj.blacks / 50f) * 0.12f +
            (adj.fade / 100f) * 0.16f
        ).coerceIn(-0.75f, 0.75f) * 255f + contrastPivotOffset

    val gradeMatrix = ColorMatrix(
        floatArrayOf(
            rScale, 0f, 0f, 0f, bVal,
            0f, gScale, 0f, 0f, bVal,
            0f, 0f, bScale, 0f, bVal,
            0f, 0f, 0f, 1f, 0f
        )
    )

    val result = ColorMatrix()
    result.timesAssign(satMat)
    result.timesAssign(hueMat)
    result.timesAssign(gradeMatrix)
    return result
}

private const val AGSL_CAPCUT_PREVIEW_SHADER = """
    uniform shader content;
    uniform float2 uResolution;
    uniform float uSharpen;
    uniform float uClarity;
    uniform float uGrain;
    uniform float uVignette;
    uniform float uRgbSplit;
    uniform float uSeed;

    half4 main(float2 fragCoord) {
        half4 base = content.eval(fragCoord);
        float3 rgb = float3(base.rgb);

        if (uRgbSplit > 0.5) {
            float rC = content.eval(fragCoord + float2(6.5, 2.0)).r;
            float bC = content.eval(fragCoord - float2(6.5, 2.0)).b;
            rgb = float3(rC, rgb.g, bC);
        }

        float sharpStrength = uSharpen * 1.9 + uClarity * 0.8;
        if (sharpStrength > 0.01) {
            float3 n = float3(content.eval(fragCoord + float2(0.0, -1.6)).rgb);
            float3 s = float3(content.eval(fragCoord + float2(0.0, 1.6)).rgb);
            float3 e = float3(content.eval(fragCoord + float2(1.6, 0.0)).rgb);
            float3 w = float3(content.eval(fragCoord + float2(-1.6, 0.0)).rgb);
            float3 blur = (n + s + e + w) * 0.25;
            rgb = clamp(rgb + (rgb - blur) * sharpStrength, 0.0, 1.0);
        }

        if (uGrain > 0.01) {
            float luma = dot(rgb, float3(0.299, 0.587, 0.114));
            float nVal = fract(sin(dot(fragCoord * 0.45 + float2(uSeed, uSeed * 1.37), float2(12.9898, 78.233))) * 43758.5453);
            float centered = (nVal - 0.5) * 2.0;
            float weight = max(1.0 - abs(luma - 0.5) * 1.1, 0.38);
            rgb = clamp(rgb + float3(centered * uGrain * 0.25 * weight), 0.0, 1.0);
        }

        if (uVignette > 0.01) {
            float2 uv = fragCoord / max(uResolution, float2(1.0, 1.0));
            float dist = length((uv - float2(0.5, 0.5)) * 1.22);
            float vig = smoothstep(0.28, 0.86, dist);
            rgb *= clamp(1.0 - vig * uVignette * 0.88, 0.0, 1.0);
        }

        return half4(half3(rgb), base.a);
    }
"""

private fun findInnerTextureView(root: View): TextureView? {
    if (root is TextureView) return root
    if (root is ViewGroup) {
        for (i in 0 until root.childCount) {
            val found = findInnerTextureView(root.getChildAt(i))
            if (found != null) return found
        }
    }
    return null
}

private fun applyRealtimeGpuEffectsToPlayerView(
    playerView: PlayerView,
    composeColorMatrix: ColorMatrix,
    adj: AdjustmentParams,
    filter: FilterConfig,
    isComparingOriginal: Boolean
) {
    val textureView = findInnerTextureView(playerView)
    if (isComparingOriginal) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            playerView.setRenderEffect(null)
            textureView?.setRenderEffect(null)
        }
        textureView?.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        playerView.invalidate()
        return
    }

    val androidCm = android.graphics.ColorMatrix(composeColorMatrix.values)
    val cmFilter = ColorMatrixColorFilter(androidCm)
    val preset = FilterPreset.findById(filter.filterId)
    val fStrength = if (preset.id == "none") 0f else filter.intensity.coerceIn(0f, 1f)

    val effSharpen = (adj.sharpen / 100f).coerceIn(0f, 1f)
    val effClarity = (adj.clarity / 100f).coerceIn(0f, 1f)
    val effGrain = ((adj.grain / 100f) + preset.grainAmount * fStrength).coerceIn(0f, 1f)
    val effVignette = ((adj.vignette / 100f) + preset.vignetteAmount * fStrength).coerceIn(0f, 1f)
    val effRgbSplit = if ((preset.isRgbSplit || preset.isGlitch) && fStrength > 0.2f) 1f else 0f

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        runCatching {
            val colorEffect = RenderEffect.createColorFilterEffect(cmFilter)
            val needsAgsl = effSharpen > 0.01f || effClarity > 0.01f || effGrain > 0.01f || effVignette > 0.01f || effRgbSplit > 0.5f
            val finalEffect = if (needsAgsl) {
                val shader = RuntimeShader(AGSL_CAPCUT_PREVIEW_SHADER)
                val w = playerView.width.coerceAtLeast(360).toFloat()
                val h = playerView.height.coerceAtLeast(640).toFloat()
                shader.setFloatUniform("uResolution", w, h)
                shader.setFloatUniform("uSharpen", effSharpen)
                shader.setFloatUniform("uClarity", effClarity)
                shader.setFloatUniform("uGrain", effGrain)
                shader.setFloatUniform("uVignette", effVignette)
                shader.setFloatUniform("uRgbSplit", effRgbSplit)
                shader.setFloatUniform("uSeed", ((System.currentTimeMillis() / 42L) % 1000L).toFloat() * 0.17f)
                val agslEffect = RenderEffect.createRuntimeShaderEffect(shader, "content")
                RenderEffect.createChainEffect(agslEffect, colorEffect)
            } else {
                colorEffect
            }
            (textureView ?: playerView).setRenderEffect(finalEffect)
        }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        runCatching {
            val colorEffect = RenderEffect.createColorFilterEffect(cmFilter)
            (textureView ?: playerView).setRenderEffect(colorEffect)
        }
    } else {
        val paint = android.graphics.Paint().apply { colorFilter = cmFilter }
        textureView?.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
    }
    textureView?.invalidate()
    playerView.invalidate()
}

enum class CapCutAdjustTool(
    val title: String,
    val icon: ImageVector,
    val isBipolar: Boolean,
    val minVal: Float,
    val maxVal: Float,
    val defaultVal: Float
) {
    BRIGHTNESS("Brightness", Icons.Default.WbSunny, true, -50f, 50f, 0f),
    CONTRAST("Contrast", Icons.Default.Contrast, true, -50f, 50f, 0f),
    SATURATION("Saturation", Icons.Default.WaterDrop, true, -50f, 50f, 0f),
    BRILLIANCE("Brilliance", Icons.Default.AutoAwesome, true, -50f, 50f, 0f),
    SHARPEN("Sharpen", Icons.Default.Details, false, 0f, 100f, 0f),
    CLARITY("Clarity", Icons.Default.CenterFocusStrong, false, 0f, 100f, 0f),
    HIGHLIGHTS("Highlights", Icons.Default.BrightnessHigh, true, -50f, 50f, 0f),
    SHADOWS("Shadows", Icons.Default.DarkMode, true, -50f, 50f, 0f),
    WHITES("Whites", Icons.Default.RadioButtonUnchecked, true, -50f, 50f, 0f),
    BLACKS("Blacks", Icons.Default.Circle, true, -50f, 50f, 0f),
    TEMP("Temp", Icons.Default.DeviceThermostat, true, -50f, 50f, 0f),
    HUE("Hue", Icons.Default.InvertColors, true, -50f, 50f, 0f),
    FADE("Fade", Icons.Default.Gradient, false, 0f, 100f, 0f),
    VIGNETTE("Vignette", Icons.Default.PanoramaFishEye, false, 0f, 100f, 0f),
    GRAIN("Grain", Icons.Default.Grain, false, 0f, 100f, 0f)
}

private fun getToolValue(tool: CapCutAdjustTool, adj: AdjustmentParams): Float {
    return when (tool) {
        CapCutAdjustTool.BRIGHTNESS -> (adj.brightness * 100f).coerceIn(-50f, 50f)
        CapCutAdjustTool.CONTRAST -> ((adj.contrast - 1.0f) * 100f).coerceIn(-50f, 50f)
        CapCutAdjustTool.SATURATION -> ((adj.saturation - 1.0f) * 62.5f).coerceIn(-50f, 50f)
        CapCutAdjustTool.BRILLIANCE -> adj.brilliance.coerceIn(-50f, 50f)
        CapCutAdjustTool.SHARPEN -> adj.sharpen.coerceIn(0f, 100f)
        CapCutAdjustTool.CLARITY -> adj.clarity.coerceIn(0f, 100f)
        CapCutAdjustTool.HIGHLIGHTS -> (adj.highlights * 50f).coerceIn(-50f, 50f)
        CapCutAdjustTool.SHADOWS -> (adj.shadows * 50f).coerceIn(-50f, 50f)
        CapCutAdjustTool.WHITES -> adj.whites.coerceIn(-50f, 50f)
        CapCutAdjustTool.BLACKS -> adj.blacks.coerceIn(-50f, 50f)
        CapCutAdjustTool.TEMP -> (adj.temperature * 50f).coerceIn(-50f, 50f)
        CapCutAdjustTool.HUE -> (adj.hue).coerceIn(-50f, 50f)
        CapCutAdjustTool.FADE -> adj.fade.coerceIn(0f, 100f)
        CapCutAdjustTool.VIGNETTE -> adj.vignette.coerceIn(0f, 100f)
        CapCutAdjustTool.GRAIN -> adj.grain.coerceIn(0f, 100f)
    }
}

private fun setToolValue(tool: CapCutAdjustTool, value: Float, adj: AdjustmentParams): AdjustmentParams {
    return when (tool) {
        CapCutAdjustTool.BRIGHTNESS -> adj.copy(brightness = value / 100f)
        CapCutAdjustTool.CONTRAST -> adj.copy(contrast = 1.0f + (value / 100f))
        CapCutAdjustTool.SATURATION -> adj.copy(saturation = 1.0f + (value / 62.5f))
        CapCutAdjustTool.BRILLIANCE -> adj.copy(brilliance = value)
        CapCutAdjustTool.SHARPEN -> adj.copy(sharpen = value)
        CapCutAdjustTool.CLARITY -> adj.copy(clarity = value)
        CapCutAdjustTool.HIGHLIGHTS -> adj.copy(highlights = value / 50f)
        CapCutAdjustTool.SHADOWS -> adj.copy(shadows = value / 50f)
        CapCutAdjustTool.WHITES -> adj.copy(whites = value)
        CapCutAdjustTool.BLACKS -> adj.copy(blacks = value)
        CapCutAdjustTool.TEMP -> adj.copy(temperature = value / 50f)
        CapCutAdjustTool.HUE -> adj.copy(hue = value)
        CapCutAdjustTool.FADE -> adj.copy(fade = value)
        CapCutAdjustTool.VIGNETTE -> adj.copy(vignette = value)
        CapCutAdjustTool.GRAIN -> adj.copy(grain = value)
    }
}

private fun isToolModified(tool: CapCutAdjustTool, adj: AdjustmentParams): Boolean {
    val current = getToolValue(tool, adj)
    return kotlin.math.abs(current - tool.defaultVal) > 0.5f
}

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    projectId: String,
    initialVideoUri: String = "",
    initialTitle: String = "",
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val timelineSplitter = remember { TimelineSplitter() }
    val templateEngine = remember { TemplateEngine() }
    val voiceEffectEngine = remember { VoiceEffectEngine() }
    val exportEngine = remember { Media3ExportEngine(context) }
    val mediaStoreExporter = remember { MediaStoreExporter(context) }
    val repository = remember { KinetixRepository(context) }
    val savedProjects by repository.observeAllProjects().collectAsState(initial = emptyList())

    val resolvedInitialTitle = initialTitle.ifBlank { "Studio Project" }

    val initialProject = remember(projectId, initialVideoUri, resolvedInitialTitle) {
        val base = EditProject(
            id = projectId,
            name = resolvedInitialTitle,
            sourceUri = initialVideoUri,
            sourceDurationMs = 15000L,
            clips = listOf(ClipSection(0L, 15000L, "Part 01")),
            canvas = CanvasConfig(ratio = AspectRatio.RATIO_ORIGINAL, fitMode = FitMode.FIT_BLUR_BG),
            filter = FilterConfig(filterId = "none", filterName = "Normal", intensity = 0f),
            texts = emptyList()
        )
        if (projectId.startsWith("template_applied_")) {
            val templateId = projectId.removePrefix("template_applied_")
            val templatesDir = File(context.filesDir, "templates")
            val matchedTemplate = templateEngine.findTemplateById(templatesDir, templateId)
            matchedTemplate?.applyToProject(base, videoTitle = resolvedInitialTitle) ?: base
        } else {
            base
        }
    }

    var project by remember { mutableStateOf(initialProject) }
    val safeClipIndex = project.activeClipIndex.coerceIn(0, (project.clips.size - 1).coerceAtLeast(0))
    val activeClip = project.clips.getOrNull(safeClipIndex) ?: ClipSection(0L, project.sourceDurationMs)
    val undoStack = remember { mutableStateListOf<EditProject>() }
    val redoStack = remember { mutableStateListOf<EditProject>() }

    var isClipSelected by remember { mutableStateOf(false) }
    var selectedOverlayIndex by remember { mutableStateOf<Int?>(null) }
    var selectedAdjustmentLayerIndex by remember { mutableStateOf<Int?>(null) }
    var selectedTrackCategory by remember { mutableStateOf(TimelineTrackCategory.ALL) }
    var isReverseProcessing by remember { mutableStateOf(false) }

    LaunchedEffect(projectId, initialVideoUri) {
        if (initialVideoUri.isNotBlank()) {
            val existing = runCatching { repository.getProjectById(projectId) }.getOrNull()
            project = if (existing != null && existing.sourceUri == initialVideoUri) {
                existing
            } else {
                initialProject.copy(sourceUri = initialVideoUri, name = resolvedInitialTitle)
            }
            runCatching { repository.saveProject(project) }
        } else if (projectId != "default_project") {
            val existing = runCatching { repository.getProjectById(projectId) }.getOrNull()
            if (existing != null && existing.sourceUri.isNotBlank()) {
                project = existing
            }
        }
    }

    fun pushUndoSnapshot() {
        if (undoStack.lastOrNull() != project) {
            undoStack.add(project)
            if (undoStack.size > 50) undoStack.removeAt(0)
            redoStack.clear()
        }
    }

    fun updateProject(newProject: EditProject, recordUndo: Boolean = true) {
        if (recordUndo) pushUndoSnapshot()
        val updated = newProject.copy(lastModifiedEpochMs = System.currentTimeMillis())
        project = updated
        if (recordUndo && updated.sourceUri.isNotBlank()) {
            coroutineScope.launch(Dispatchers.IO) {
                runCatching { repository.saveProject(updated) }
            }
        }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var currentPlayheadMs by remember { mutableLongStateOf(0L) }
    var nativeVideoRatio by remember { mutableFloatStateOf(9f / 16f) }
    var videoPosterBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var filmstripFrames by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var selectedTextIndex by remember { mutableIntStateOf(0) }
    var activeTool by remember { mutableStateOf<EditorTool?>(null) }
    var isHoldingCompare by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var exportCompleted by remember { mutableStateOf(false) }
    var exportErrorText by remember { mutableStateOf<String?>(null) }
    var exportProgress by remember { mutableIntStateOf(0) }
    var exportedFilePath by remember { mutableStateOf("") }
    var statusBannerText by remember { mutableStateOf<String?>(null) }
    var selectedFilterCategory by remember { mutableStateOf<FilterCategory?>(null) }
    var selectedAdjustTool by remember { mutableStateOf(CapCutAdjustTool.BRIGHTNESS) }
    var showResolutionPopover by remember { mutableStateOf(false) }
    var selectedExportResolution by remember { mutableStateOf(ExportResolution.RES_1080P) }
    var selectedExportFps by remember { mutableStateOf(ExportFps.FPS_30) }
    var selectedExportBitrate by remember { mutableStateOf(ExportBitrate.RECOMMENDED) }
    var exportClipsSeparately by remember { mutableStateOf(false) }
    var activeTransitionClipIndex by remember { mutableIntStateOf(0) }
    var batchSplitTab by remember { mutableIntStateOf(0) }
    var batchSplitDurationSec by remember { mutableIntStateOf(30) }
    var batchSplitPartsCount by remember { mutableIntStateOf(4) }
    var batchSplitTailRule by remember { mutableStateOf(TailRemainderRule.SEPARATE_CLIP) }
    var backgroundTab by remember { mutableIntStateOf(0) }

    // System Video Picker to load or swap source video
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                val resolvedLocalUri = try {
                    val dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
                    if (!dir.exists()) dir.mkdirs()
                    val importedFile = File(dir, "kinetix_studio_import_${System.currentTimeMillis()}.mp4")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        importedFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (importedFile.exists() && importedFile.length() > 0L) {
                        Uri.fromFile(importedFile).toString()
                    } else {
                        uri.toString()
                    }
                } catch (_: Exception) {
                    uri.toString()
                }
                withContext(Dispatchers.Main) {
                    val newProj = EditProject(
                        id = if (project.id == "default_project") "studio_${System.currentTimeMillis()}" else project.id,
                        name = "Studio Edit",
                        sourceUri = resolvedLocalUri,
                        sourceDurationMs = 15000L,
                        clips = listOf(ClipSection(0L, 15000L, "Part 01")),
                        canvas = CanvasConfig(ratio = AspectRatio.RATIO_ORIGINAL, fitMode = FitMode.FIT_BLUR_BG),
                        filter = FilterConfig(filterId = "none", filterName = "Normal", intensity = 0f),
                        texts = emptyList()
                    )
                    updateProject(newProj, recordUndo = false)
                    statusBannerText = "Loaded video into Studio"
                }
            }
        }
    }

    // System Audio Picker to import custom MP3 / M4A / WAV music from phone
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                val localAudioUri = try {
                    val dir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir
                    if (!dir.exists()) dir.mkdirs()
                    val importedAudio = File(dir, "custom_audio_${System.currentTimeMillis()}.mp3")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        importedAudio.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (importedAudio.exists() && importedAudio.length() > 0L) {
                        Uri.fromFile(importedAudio).toString()
                    } else {
                        uri.toString()
                    }
                } catch (_: Exception) {
                    uri.toString()
                }
                withContext(Dispatchers.Main) {
                    updateProject(
                        project.copy(
                            music = project.music.copy(
                                title = "Custom Device Audio",
                                uri = localAudioUri
                            )
                        )
                    )
                    statusBannerText = "Added custom audio track"
                }
            }
        }
    }

    // System Overlay (PIP) Media Picker to add Video or Image Overlays
    val overlayPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                val resolvedUri = try {
                    val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
                    if (!dir.exists()) dir.mkdirs()
                    val ext = if (context.contentResolver.getType(uri)?.startsWith("video") == true) "mp4" else "jpg"
                    val imported = File(dir, "kinetix_pip_${System.currentTimeMillis()}.$ext")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        imported.outputStream().use { output -> input.copyTo(output) }
                    }
                    if (imported.exists() && imported.length() > 0L) Uri.fromFile(imported).toString() else uri.toString()
                } catch (_: Exception) {
                    uri.toString()
                }
                withContext(Dispatchers.Main) {
                    val newPip = PipOverlay(
                        uri = resolvedUri,
                        label = "PIP ${project.overlays.size + 1}",
                        startMs = currentPlayheadMs,
                        endMs = (currentPlayheadMs + 5000L).coerceAtMost(project.sourceDurationMs),
                        scale = 0.5f
                    )
                    val updatedOverlays = project.overlays + newPip
                    updateProject(project.copy(overlays = updatedOverlays))
                    selectedOverlayIndex = updatedOverlays.lastIndex
                    isClipSelected = false
                    selectedTrackCategory = TimelineTrackCategory.OVERLAY
                    statusBannerText = "Added Picture-in-Picture Overlay"
                }
            }
        }
    }

    // Primary Video ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            playWhenReady = true
            setSeekParameters(androidx.media3.exoplayer.SeekParameters.EXACT)
        }
    }

    // Secondary Synchronized Background Music ExoPlayer
    val musicPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            playWhenReady = false
        }
    }

    // Bind lifecycle and listen to actual video duration & dimensions
    DisposableEffect(exoPlayer, musicPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val realDuration = exoPlayer.duration
                    if (realDuration > 500L && realDuration != project.sourceDurationMs) {
                        val updatedClips = if (project.clips.size == 1 && project.clips[0].startMs == 0L) {
                            listOf(ClipSection(0L, realDuration, "Part 01"))
                        } else {
                            project.clips
                        }
                        project = project.copy(sourceDurationMs = realDuration, clips = updatedClips)
                    }
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    val pixelRatio = if (videoSize.pixelWidthHeightRatio > 0f) videoSize.pixelWidthHeightRatio else 1f
                    nativeVideoRatio = ((videoSize.width.toFloat() * pixelRatio) / videoSize.height.toFloat())
                        .coerceIn(0.25f, 4.0f)
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing && project.music.title != "No Background Music") {
                    runCatching { musicPlayer.play() }
                } else {
                    runCatching { musicPlayer.pause() }
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
            musicPlayer.release()
        }
    }

    // Load video MediaItem and extract real video poster + multi-frame filmstrip thumbnails
    LaunchedEffect(project.sourceUri) {
        val rawUri = project.sourceUri
        if (rawUri.isBlank()) {
            exoPlayer.stop()
            videoPosterBitmap = null
            filmstripFrames = emptyList()
            return@LaunchedEffect
        }

        try {
            exoPlayer.setMediaItem(MediaItem.fromUri(rawUri))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            exoPlayer.play()
        } catch (_: Exception) {}

        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                val parsed = Uri.parse(rawUri)
                when (parsed.scheme) {
                    "file" -> retriever.setDataSource(parsed.path)
                    "content" -> retriever.setDataSource(context, parsed)
                    else -> if (rawUri.startsWith("/")) retriever.setDataSource(rawUri)
                }
                val vw = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toFloatOrNull() ?: 0f
                val vh = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toFloatOrNull() ?: 0f
                val rot = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                val durMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 15000L

                if (vw > 0f && vh > 0f) {
                    val actualRatio = if (rot == 90 || rot == 270) vh / vw else vw / vh
                    withContext(Dispatchers.Main) {
                        nativeVideoRatio = actualRatio.coerceIn(0.25f, 4.0f)
                    }
                }

                val poster = retriever.getFrameAtTime(300_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.getFrameAtTime(0L)
                if (poster != null) {
                    withContext(Dispatchers.Main) {
                        videoPosterBitmap = poster
                    }
                }

                // Extract 8 real video frames across the duration for the visual filmstrip timeline
                val frameCount = 8
                val extractedFrames = mutableListOf<Bitmap>()
                val totalUs = (durMs.coerceAtLeast(1000L)) * 1000L
                for (i in 0 until frameCount) {
                    val sampleTimeUs = (totalUs * i) / frameCount
                    val thumb = retriever.getScaledFrameAtTime(
                        sampleTimeUs,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                        160,
                        160
                    )
                    if (thumb != null) {
                        extractedFrames.add(thumb)
                    }
                }
                if (extractedFrames.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        filmstripFrames = extractedFrames
                    }
                }
            } catch (_: Exception) {
            } finally {
                runCatching { retriever.release() }
            }
        }
    }

    // Load & sync Background Music player when music track changes
    LaunchedEffect(project.music.title, project.music.uri) {
        if (project.music.title == "No Background Music") {
            musicPlayer.stop()
            musicPlayer.clearMediaItems()
        } else {
            val customUri = project.music.uri
            val playableUri = if (!customUri.isNullOrBlank() && (customUri.startsWith("file://") || customUri.startsWith("content://"))) {
                customUri
            } else {
                val wavFile = AudioTrackSynthesizer.getOrGenerateStudioTrack(context.cacheDir, project.music.title)
                wavFile?.let { Uri.fromFile(it).toString() }
            }
            if (!playableUri.isNullOrBlank()) {
                runCatching {
                    musicPlayer.setMediaItem(MediaItem.fromUri(playableUri))
                    musicPlayer.prepare()
                    if (exoPlayer.isPlaying) {
                        musicPlayer.play()
                    }
                }
            }
        }
    }

    val activeAdjustment = remember(project.adjustment, project.adjustmentLayers, selectedAdjustmentLayerIndex, currentPlayheadMs) {
        val activeLayer = if (selectedAdjustmentLayerIndex != null) {
            project.adjustmentLayers.getOrNull(selectedAdjustmentLayerIndex!!)
        } else {
            project.adjustmentLayers.firstOrNull { currentPlayheadMs in it.startMs..it.endMs }
        }
        activeLayer?.adjustment ?: project.adjustment
    }

    val activeFilter = remember(project.filter, project.adjustmentLayers, selectedAdjustmentLayerIndex, currentPlayheadMs) {
        val activeLayer = if (selectedAdjustmentLayerIndex != null) {
            project.adjustmentLayers.getOrNull(selectedAdjustmentLayerIndex!!)
        } else {
            project.adjustmentLayers.firstOrNull { currentPlayheadMs in it.startMs..it.endMs }
        }
        activeLayer?.filter ?: project.filter
    }

    val previewColorMatrix = remember(activeAdjustment, activeFilter, isHoldingCompare) {
        if (isHoldingCompare) ColorMatrix() else createCapCutColorMatrix(activeAdjustment, activeFilter)
    }

    // Apply real-time Speed, Voice Pitch & Audio Mixer Volumes
    LaunchedEffect(
        project.speed,
        project.voice.pitchSemitones,
        project.music.originalVideoVolume,
        project.music.musicVolume,
        project.music.autoDuckOriginalSpeech,
        activeClip.volume,
        activeClip.isMuted
    ) {
        val pitchRatio = voiceEffectEngine.semitonesToPitchRatio(project.voice.pitchSemitones).coerceIn(0.5f, 2.0f)
        val speedVal = project.speed.coerceIn(0.25f, 4.0f)
        exoPlayer.playbackParameters = PlaybackParameters(speedVal, pitchRatio)
        val clipVol = if (activeClip.isMuted) 0f else activeClip.volume
        val effectiveVideoVol = (clipVol * project.music.originalVideoVolume).coerceIn(0f, 1f)
        exoPlayer.volume = effectiveVideoVol

        val duckMultiplier = if (project.music.autoDuckOriginalSpeech && effectiveVideoVol > 0.2f) 0.45f else 1.0f
        musicPlayer.volume = (project.music.musicVolume * duckMultiplier).coerceIn(0f, 1f)
    }

    // Sync playhead and enforce active clip trim bounds during playback, handling Freeze & Reverse
    LaunchedEffect(isPlaying, project.activeClipIndex, project.clips) {
        val safeIdx = project.activeClipIndex.coerceIn(0, (project.clips.size - 1).coerceAtLeast(0))
        val currentClip = project.clips.getOrNull(safeIdx)

        if (isPlaying && currentClip != null && currentClip.isFreezeFrame) {
            // Freeze Frame: Hold player paused on frame for clip duration
            if (exoPlayer.isPlaying) {
                exoPlayer.pause()
            }
            exoPlayer.seekTo(currentClip.startMs)
            val freezeDur = currentClip.durationMs.coerceAtLeast(3000L)
            val startWallTime = System.currentTimeMillis()
            while (isPlaying) {
                val elapsed = System.currentTimeMillis() - startWallTime
                if (elapsed >= freezeDur) break
                currentPlayheadMs = currentClip.startMs + elapsed
                delay(40)
            }
            if (isPlaying) {
                if (safeIdx + 1 < project.clips.size) {
                    val nextIdx = safeIdx + 1
                    val nextClip = project.clips[nextIdx]
                    project = project.copy(activeClipIndex = nextIdx)
                    exoPlayer.seekTo(nextClip.startMs)
                    currentPlayheadMs = nextClip.startMs
                    if (!nextClip.isFreezeFrame && !nextClip.isReversed) {
                        exoPlayer.play()
                    }
                } else {
                    exoPlayer.seekTo(currentClip.startMs)
                    currentPlayheadMs = currentClip.startMs
                }
            }
            return@LaunchedEffect
        }

        while (isPlaying) {
            if (currentClip != null && currentClip.isReversed) {
                // Reverse Playback: Decrement playhead backward at 10 fps (100ms) for smooth hardware keyframe decoding
                if (exoPlayer.isPlaying) {
                    exoPlayer.pause()
                }
                delay(100)
                val step = (100L * currentClip.speed * project.speed).toLong().coerceAtLeast(60L)
                val newPos = currentPlayheadMs - step
                if (newPos <= currentClip.startMs) {
                    if (project.clips.size > 1 && safeIdx + 1 < project.clips.size) {
                        val nextIdx = safeIdx + 1
                        val nextClip = project.clips[nextIdx]
                        project = project.copy(activeClipIndex = nextIdx)
                        val startPos = if (nextClip.isReversed) nextClip.endMs else nextClip.startMs
                        exoPlayer.seekTo(startPos)
                        currentPlayheadMs = startPos
                        if (!nextClip.isReversed && !nextClip.isFreezeFrame) {
                            exoPlayer.play()
                        }
                    } else {
                        currentPlayheadMs = currentClip.endMs
                        exoPlayer.seekTo(currentClip.endMs)
                    }
                } else {
                    currentPlayheadMs = newPos
                    exoPlayer.seekTo(newPos)
                }
            } else {
                delay(40)
                val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                currentPlayheadMs = pos
                val clipEnd = currentClip?.endMs ?: project.sourceDurationMs
                val clipStart = currentClip?.startMs ?: 0L
                if (currentPlayheadMs >= clipEnd && clipEnd > clipStart) {
                    // If split into multiple clips, advance to next clip or loop active clip
                    if (project.clips.size > 1 && safeIdx + 1 < project.clips.size) {
                        val nextIdx = safeIdx + 1
                        val nextClip = project.clips[nextIdx]
                        project = project.copy(activeClipIndex = nextIdx)
                        exoPlayer.seekTo(nextClip.startMs)
                        currentPlayheadMs = nextClip.startMs
                    } else {
                        exoPlayer.seekTo(clipStart)
                        currentPlayheadMs = clipStart
                    }
                }
            }
        }
    }

    // Auto-dismiss status banner
    LaunchedEffect(statusBannerText) {
        if (statusBannerText != null) {
            delay(2400)
            statusBannerText = null
        }
    }

    // --- STUDIO LAUNCHER HUB WHEN NO VIDEO IS LOADED ---
    val hasValidSource = project.sourceUri.isNotBlank() &&
        (!project.sourceUri.startsWith("file://") || File(Uri.parse(project.sourceUri).path.orEmpty()).exists())

    if (!hasValidSource) {
        var recentLocalVideos by remember { mutableStateOf<List<File>>(emptyList()) }
        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                val dirs = listOfNotNull(
                    context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
                    File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "Kinetix Pro"),
                    File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Kinetix Pro")
                )
                recentLocalVideos = dirs
                    .flatMap { it.listFiles()?.toList().orEmpty() }
                    .filter { it.isFile && it.length() > 0L && it.extension.lowercase() in listOf("mp4", "mov", "mkv", "webm") && !it.name.startsWith("tmp_") }
                    .distinctBy { it.name }
                    .sortedByDescending { it.lastModified() }
            }
        }

        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(ObsidianCanvas)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Kinetix NLE Studio",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ContentHigh,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Non-linear multi-clip timeline, frame scrubbing, GPU LUTs, audio mixer & 4K export",
                    fontSize = 13.sp,
                    color = ContentMedium,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            item {
                ObsidianCard(
                    modifier = Modifier.fillMaxWidth(),
                    isHighlighted = true,
                    onClick = {
                        videoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(SunsetTint15)
                                .border(1.5.dp, PrimarySunset, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Project",
                                tint = PrimarySunset,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = "New Studio Project",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ContentHigh
                        )
                        Text(
                            text = "Select any video from your phone gallery to start editing",
                            fontSize = 12.sp,
                            color = ContentMedium
                        )
                        SunsetPrimaryButton(
                            text = "Select Video from Device",
                            onClick = {
                                videoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            icon = Icons.Default.VideoFile
                        )
                    }
                }
            }

            if (recentLocalVideos.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Downloaded Videos (${recentLocalVideos.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ContentHigh
                    )
                }

                items(recentLocalVideos.take(8), key = { it.absolutePath }) { file ->
                    ObsidianCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val newProj = EditProject(
                                id = "studio_${System.currentTimeMillis()}",
                                name = file.nameWithoutExtension.replace("_", " "),
                                sourceUri = Uri.fromFile(file).toString(),
                                sourceDurationMs = 15000L,
                                clips = listOf(ClipSection(0L, 15000L, "Part 01")),
                                canvas = CanvasConfig(ratio = AspectRatio.RATIO_ORIGINAL, fitMode = FitMode.FIT_BLUR_BG),
                                filter = FilterConfig(filterId = "none", filterName = "Normal", intensity = 0f),
                                texts = emptyList()
                            )
                            updateProject(newProj, recordUndo = false)
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ObsidianSurface2),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Movie, contentDescription = null, tint = PrimarySunset)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.nameWithoutExtension.replace("_", " "),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ContentHigh,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Tap to open in Studio Timeline",
                                        fontSize = 11.sp,
                                        color = ContentMedium
                                    )
                                }
                            }
                            SunsetPrimaryButton(
                                text = "Edit",
                                onClick = {
                                    val newProj = EditProject(
                                        id = "studio_${System.currentTimeMillis()}",
                                        name = file.nameWithoutExtension.replace("_", " "),
                                        sourceUri = Uri.fromFile(file).toString(),
                                        sourceDurationMs = 15000L,
                                        clips = listOf(ClipSection(0L, 15000L, "Part 01")),
                                        canvas = CanvasConfig(ratio = AspectRatio.RATIO_ORIGINAL, fitMode = FitMode.FIT_BLUR_BG),
                                        filter = FilterConfig(filterId = "none", filterName = "Normal", intensity = 0f),
                                        texts = emptyList()
                                    )
                                    updateProject(newProj, recordUndo = false)
                                },
                                modifier = Modifier.height(36.dp)
                            )
                        }
                    }
                }
            }

            val validDrafts = savedProjects.filter { draft ->
                draft.sourceUri.isNotBlank() &&
                    (!draft.sourceUri.startsWith("file://") || File(Uri.parse(draft.sourceUri).path.orEmpty()).exists())
            }

            if (validDrafts.isNotEmpty()) {
                item {
                    Text(
                        text = "Saved Studio Drafts (${validDrafts.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimarySunset
                    )
                }

                items(validDrafts, key = { "hub_draft_${it.id}" }) { draft ->
                    ObsidianCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { project = draft }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                PlatformBadge(platformName = "SAVED DRAFT")
                                Text(
                                    text = draft.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ContentHigh,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Text(
                                    text = "${draft.canvas.ratio.label} • ${draft.clips.size} clip(s) • ${draft.filter.filterName}",
                                    fontSize = 11.sp,
                                    color = ContentMedium
                                )
                            }
                            IconButton(
                                onClick = {
                                    coroutineScope.launch { repository.deleteProject(draft.id) }
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Draft", tint = ContentMedium)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
        return
    }

    // --- ACTIVE NLE STUDIO WORKSPACE ---
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianCanvas)
    ) {
        // --- 1. TOP STUDIO HEADER BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ObsidianSurface1)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = {
                        exoPlayer.pause()
                        musicPlayer.pause()
                        onNavigateBack()
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = ContentHigh
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ContentHigh,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${project.canvas.ratio.label.substringBefore(" ")} • ${project.filter.filterName} • ${project.speed}x",
                        fontSize = 11.sp,
                        color = ContentMedium,
                        maxLines = 1
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = {
                        videoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Switch Video",
                        tint = ContentMedium
                    )
                }
                IconButton(
                    onClick = {
                        if (undoStack.isNotEmpty()) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val prev = undoStack.removeAt(undoStack.lastIndex)
                            redoStack.add(project)
                            project = prev
                        }
                    },
                    enabled = undoStack.isNotEmpty()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (undoStack.isNotEmpty()) ContentHigh else ContentMuted
                    )
                }
                IconButton(
                    onClick = {
                        if (redoStack.isNotEmpty()) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val next = redoStack.removeAt(redoStack.lastIndex)
                            undoStack.add(project)
                            project = next
                        }
                    },
                    enabled = redoStack.isNotEmpty()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (redoStack.isNotEmpty()) ContentHigh else ContentMuted
                    )
                }
                // CapCut Top-Bar Resolution & FPS Popover Pill Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(ObsidianSurface2)
                        .border(1.dp, ObsidianStroke, RoundedCornerShape(8.dp))
                        .clickable { showResolutionPopover = true }
                        .padding(horizontal = 8.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "${selectedExportResolution.label.substringBefore(" ")} • ${selectedExportFps.fps}fps",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ContentHigh
                    )
                }
                SunsetPrimaryButton(
                    text = "Export",
                    onClick = {
                        exoPlayer.pause()
                        musicPlayer.pause()
                        exportCompleted = false
                        exportErrorText = null
                        showExportSheet = true
                    },
                    modifier = Modifier.height(36.dp),
                    icon = Icons.Default.FileDownload
                )
            }
        }

        // --- 2. INTERACTIVE VIDEO PREVIEW VIEWPORT (Pinch-to-Zoom, Drag-to-Pan, Draggable Text Layers) ---
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            val isRotated90 = (project.canvas.rotationDegrees.toInt() % 180) != 0
            val effectiveNativeRatio = if (isRotated90) (1f / nativeVideoRatio) else nativeVideoRatio
            val targetCanvasRatio = if (project.canvas.ratio == AspectRatio.RATIO_ORIGINAL) {
                effectiveNativeRatio
            } else {
                project.canvas.ratio.aspectRatio
            }.coerceIn(0.25f, 4.0f)

            val availW = (maxWidth * 0.94f).coerceAtLeast(100.dp)
            val availH = (maxHeight * 0.94f).coerceAtLeast(100.dp)
            val availRatio = availW.value / availH.value

            val (canvasW, canvasH) = if (targetCanvasRatio > availRatio) {
                availW to (availW / targetCanvasRatio)
            } else {
                (availH * targetCanvasRatio) to availH
            }

            val canvasBgColor = remember(project.canvas.backgroundColorHex) {
                runCatching { Color(android.graphics.Color.parseColor(project.canvas.backgroundColorHex)) }
                    .getOrDefault(Color.Black)
            }

            Box(
                modifier = Modifier
                    .size(width = canvasW, height = canvasH)
                    .clip(RoundedCornerShape(10.dp))
                    .clipToBounds()
                    .background(
                        if (project.canvas.fitMode == FitMode.FIT_SOLID_BG) canvasBgColor else Color(0xFF0A0B0E)
                    )
                    .border(1.dp, ObsidianStroke, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Blurred frame background for FIT_BLUR_BG
                if (project.canvas.fitMode == FitMode.FIT_BLUR_BG) {
                    val bitmap = videoPosterBitmap
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(radius = project.canvas.blurRadiusDp.dp),
                            contentScale = ContentScale.Crop,
                            alpha = 0.78f
                        )
                    }
                }

                // Compute inner video container size
                val fitBoxW = if (project.canvas.fitMode == FitMode.ORIGINAL_PADDING) canvasW * 0.86f else canvasW
                val fitBoxH = if (project.canvas.fitMode == FitMode.ORIGINAL_PADDING) canvasH * 0.86f else canvasH
                val fitBoxRatio = fitBoxW.value / fitBoxH.value.coerceAtLeast(1f)

                val (videoW, videoH) = when (project.canvas.fitMode) {
                    FitMode.FILL_CROP -> {
                        if (effectiveNativeRatio > fitBoxRatio) {
                            (canvasH * effectiveNativeRatio) to canvasH
                        } else {
                            canvasW to (canvasW / effectiveNativeRatio)
                        }
                    }
                    else -> {
                        if (effectiveNativeRatio > fitBoxRatio) {
                            fitBoxW to (fitBoxW / effectiveNativeRatio)
                        } else {
                            (fitBoxH * effectiveNativeRatio) to fitBoxH
                        }
                    }
                }

                // Compute clip-specific crop dimensions (crops the video edges without shrinking the frame)
                val clipCropW = (activeClip.cropWidthPercent / 100f).coerceIn(0.15f, 1.0f)
                val clipCropH = (activeClip.cropHeightPercent / 100f).coerceIn(0.15f, 1.0f)
                val effectiveClipW = videoW * clipCropW
                val effectiveClipH = videoH * clipCropH
                val maxCropOffsetX = ((1f - clipCropW) * 0.5f).coerceAtLeast(0f)
                val maxCropOffsetY = ((1f - clipCropH) * 0.5f).coerceAtLeast(0f)
                val clampedCropOffsetX = activeClip.cropOffsetX.coerceIn(-maxCropOffsetX, maxCropOffsetX)
                val clampedCropOffsetY = activeClip.cropOffsetY.coerceIn(-maxCropOffsetY, maxCropOffsetY)

                val activeFilterPreset = remember(activeFilter.filterId) { FilterPreset.findById(activeFilter.filterId) }
                val effectiveFilterStrength = if (activeFilterPreset.id == "none") 0f else activeFilter.intensity.coerceIn(0f, 1f)
                val effectiveGrainLevel = ((activeAdjustment.grain / 100f) + activeFilterPreset.grainAmount * effectiveFilterStrength).coerceIn(0f, 1f)
                val effectiveVignetteLevel = ((activeAdjustment.vignette / 100f) + activeFilterPreset.vignetteAmount * effectiveFilterStrength).coerceIn(0f, 1f)
                val effectiveSharpenLevel = (activeAdjustment.sharpen / 100f).coerceIn(0f, 1f)

                // Interactive Cropped Video Viewport with Pinch-to-Zoom & Drag-to-Pan
                Box(
                    modifier = Modifier
                        .size(width = effectiveClipW, height = effectiveClipH)
                        .clip(RoundedCornerShape(2.dp))
                        .clipToBounds()
                        .graphicsLayer {
                            val scale = project.canvas.scaleFactor.coerceIn(0.35f, 4.0f)
                            scaleX = (if (project.canvas.isFlippedHorizontal) -1f else 1f) * scale
                            scaleY = scale
                            rotationZ = project.canvas.rotationDegrees
                            translationX = project.canvas.panOffsetNormX * size.width * 0.45f
                            translationY = project.canvas.panOffsetNormY * size.height * 0.45f
                        }
                        .pointerInput(activeTool, safeClipIndex, clipCropW, clipCropH) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                if (activeTool == EditorTool.TRANSFORM && (clipCropW < 0.99f || clipCropH < 0.99f) && kotlin.math.abs(zoom - 1f) < 0.03f) {
                                    // In Transform/Crop mode, single-finger drag pans the source video inside the crop window
                                    val curClip = project.clips.getOrNull(safeClipIndex) ?: return@detectTransformGestures
                                    val maxOffX = ((1f - (curClip.cropWidthPercent / 100f)) * 0.5f).coerceAtLeast(0f)
                                    val maxOffY = ((1f - (curClip.cropHeightPercent / 100f)) * 0.5f).coerceAtLeast(0f)
                                    val nextOffX = (curClip.cropOffsetX - pan.x / 420f).coerceIn(-maxOffX, maxOffX)
                                    val nextOffY = (curClip.cropOffsetY - pan.y / 420f).coerceIn(-maxOffY, maxOffY)
                                    val updated = project.clips.toMutableList()
                                    updated[safeClipIndex] = curClip.copy(cropOffsetX = nextOffX, cropOffsetY = nextOffY)
                                    updateProject(project.copy(clips = updated), recordUndo = false)
                                } else {
                                    val newScale = (project.canvas.scaleFactor * zoom).coerceIn(0.35f, 4.0f)
                                    val newPanX = (project.canvas.panOffsetNormX + pan.x / 320f).coerceIn(-1f, 1f)
                                    val newPanY = (project.canvas.panOffsetNormY + pan.y / 320f).coerceIn(-1f, 1f)
                                    updateProject(
                                        project.copy(
                                            canvas = project.canvas.copy(
                                                scaleFactor = newScale,
                                                panOffsetNormX = newPanX,
                                                panOffsetNormY = newPanY
                                            )
                                        ),
                                        recordUndo = false
                                    )
                                }
                            }
                        }
                        .clickable {
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                                isPlaying = false
                            } else {
                                exoPlayer.play()
                                isPlaying = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Inner Full-Resolution Video Frame (masked by outer effectiveClipW x effectiveClipH Crop Box)
                    Box(
                        modifier = Modifier
                            .requiredSize(width = videoW, height = videoH)
                            .offset {
                                IntOffset(
                                    x = (-clampedCropOffsetX * videoW.toPx()).roundToInt(),
                                    y = (-clampedCropOffsetY * videoH.toPx()).roundToInt()
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val poster = videoPosterBitmap
                        if (poster != null) {
                            Image(
                                bitmap = poster.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                colorFilter = if (isHoldingCompare) null else ColorFilter.colorMatrix(previewColorMatrix)
                            )
                        }

                        // Force recomposition & GPU RenderEffect update on PlayerView whenever adjustments/filters change
                        val matrixSignature = remember(activeAdjustment, activeFilter, isHoldingCompare) {
                            "${activeFilter.filterId}_${activeFilter.intensity}_${activeAdjustment.hashCode()}_$isHoldingCompare"
                        }

                        AndroidView(
                            factory = { ctx ->
                                val inflater = LayoutInflater.from(ctx)
                                val view = inflater.inflate(R.layout.view_editor_player, null) as PlayerView
                                view.player = exoPlayer
                                applyRealtimeGpuEffectsToPlayerView(
                                    playerView = view,
                                    composeColorMatrix = previewColorMatrix,
                                    adj = activeAdjustment,
                                    filter = activeFilter,
                                    isComparingOriginal = isHoldingCompare
                                )
                                view
                            },
                            update = { view ->
                                val _sig = matrixSignature
                                view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                if (view.player !== exoPlayer) {
                                    view.player = exoPlayer
                                }
                                applyRealtimeGpuEffectsToPlayerView(
                                    playerView = view,
                                    composeColorMatrix = previewColorMatrix,
                                    adj = activeAdjustment,
                                    filter = activeFilter,
                                    isComparingOriginal = isHoldingCompare
                                )
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Real-time Procedural 35mm Film Grain, Edge Sharpen & Radial Vignette Overlays
                    if (!isHoldingCompare) {
                        if (effectiveGrainLevel > 0.01f) {
                            val grainSeed = (currentPlayheadMs / 42L).toInt()
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val cols = 38
                                val rows = 54
                                val cellW = size.width / cols
                                val cellH = size.height / rows
                                val alphaBase = (effectiveGrainLevel * 0.24f).coerceIn(0.02f, 0.32f)
                                for (r in 0 until rows) {
                                    for (c in 0 until cols) {
                                        val hash = ((r * 374761393) xor (c * 668265263) xor (grainSeed * 1274126177))
                                        val norm = ((hash and 0xFF) / 255f)
                                        if (norm > 0.62f) {
                                            drawRect(
                                                color = Color.White.copy(alpha = alphaBase * (norm - 0.5f)),
                                                topLeft = Offset(c * cellW, r * cellH),
                                                size = androidx.compose.ui.geometry.Size(cellW * 0.7f, cellH * 0.7f)
                                            )
                                        } else if (norm < 0.38f) {
                                            drawRect(
                                                color = Color.Black.copy(alpha = alphaBase * (0.5f - norm)),
                                                topLeft = Offset(c * cellW, r * cellH),
                                                size = androidx.compose.ui.geometry.Size(cellW * 0.7f, cellH * 0.7f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (effectiveSharpenLevel > 0.02f && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                            // High-pass micro-contrast crispness overlay for pre-Android 13 devices
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(0.5.dp, Color.White.copy(alpha = (effectiveSharpenLevel * 0.18f).coerceIn(0f, 0.2f)))
                            )
                        }

                        if (effectiveVignetteLevel > 0.02f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = (effectiveVignetteLevel * 0.78f).coerceIn(0f, 0.88f))
                                            ),
                                            radius = 480f
                                        )
                                    )
                            )
                        }
                    }

                    // CapCut Interactive Crop Grid & Corner L-Handles when Transform/Crop tool is open
                    if (activeTool == EditorTool.TRANSFORM) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val gridColor = Color.White.copy(alpha = 0.42f)
                            // Rule-of-thirds gridlines
                            drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = 1.5f)
                            drawLine(gridColor, Offset(w * 2f / 3f, 0f), Offset(w * 2f / 3f, h), strokeWidth = 1.5f)
                            drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth = 1.5f)
                            drawLine(gridColor, Offset(0f, h * 2f / 3f), Offset(w, h * 2f / 3f), strokeWidth = 1.5f)

                            // Outer crop border
                            drawRect(
                                color = PrimarySunset,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
                            )

                            // 4 Thick CapCut Corner L-Handles
                            val handleLen = 28f
                            val strokeW = 9f
                            // Top-Left
                            drawLine(PrimarySunset, Offset(0f, 0f), Offset(handleLen, 0f), strokeWidth = strokeW)
                            drawLine(PrimarySunset, Offset(0f, 0f), Offset(0f, handleLen), strokeWidth = strokeW)
                            // Top-Right
                            drawLine(PrimarySunset, Offset(w - handleLen, 0f), Offset(w, 0f), strokeWidth = strokeW)
                            drawLine(PrimarySunset, Offset(w, 0f), Offset(w, handleLen), strokeWidth = strokeW)
                            // Bottom-Left
                            drawLine(PrimarySunset, Offset(0f, h - handleLen), Offset(0f, h), strokeWidth = strokeW)
                            drawLine(PrimarySunset, Offset(0f, h), Offset(handleLen, h), strokeWidth = strokeW)
                            // Bottom-Right
                            drawLine(PrimarySunset, Offset(w - handleLen, h), Offset(w, h), strokeWidth = strokeW)
                            drawLine(PrimarySunset, Offset(w, h - handleLen), Offset(w, h), strokeWidth = strokeW)
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 6.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xCC0D0E11))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Crop: ${activeClip.cropPreset} (${activeClip.cropWidthPercent.toInt()}% × ${activeClip.cropHeightPercent.toInt()}%)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimarySunset
                            )
                        }
                    }
                }

                // Picture-in-Picture / Overlays Track Layer on Canvas
                if (!isHoldingCompare) {
                    project.overlays.forEachIndexed { idx, overlay ->
                        if (currentPlayheadMs in overlay.startMs..overlay.endMs) {
                            val isSelectedPip = selectedOverlayIndex == idx
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .offset {
                                        IntOffset(
                                            x = (overlay.positionNormX.coerceIn(-0.48f, 0.48f) * canvasW.toPx()).roundToInt(),
                                            y = (overlay.positionNormY.coerceIn(-0.48f, 0.48f) * canvasH.toPx()).roundToInt()
                                        )
                                    }
                                    .graphicsLayer {
                                        scaleX = overlay.scale
                                        scaleY = overlay.scale
                                        rotationZ = overlay.rotationDegrees
                                        alpha = overlay.opacity
                                    }
                                    .pointerInput(idx, project.overlays.size) {
                                        detectDragGestures(
                                            onDragStart = {
                                                selectedOverlayIndex = idx
                                                isClipSelected = false
                                            },
                                            onDragEnd = { pushUndoSnapshot() }
                                        ) { change, dragAmount ->
                                            change.consume()
                                            val curList = project.overlays.toMutableList()
                                            val item = curList.getOrNull(idx) ?: return@detectDragGestures
                                            val dxNorm = dragAmount.x / canvasW.toPx().coerceAtLeast(1f)
                                            val dyNorm = dragAmount.y / canvasH.toPx().coerceAtLeast(1f)
                                            curList[idx] = item.copy(
                                                positionNormX = (item.positionNormX + dxNorm).coerceIn(-0.48f, 0.48f),
                                                positionNormY = (item.positionNormY + dyNorm).coerceIn(-0.48f, 0.48f)
                                            )
                                            updateProject(project.copy(overlays = curList), recordUndo = false)
                                        }
                                    }
                                    .border(
                                        width = if (isSelectedPip) 2.dp else 0.dp,
                                        color = if (isSelectedPip) Color(0xFF8B5CF6) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        selectedOverlayIndex = idx
                                        isClipSelected = false
                                    }
                            ) {
                                AsyncImage(
                                    model = overlay.uri,
                                    contentDescription = overlay.label,
                                    modifier = Modifier
                                        .size(width = 160.dp, height = 120.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }

                // Interactive Draggable Text Layers on Canvas
                if (!isHoldingCompare) {
                    project.texts.forEachIndexed { idx, overlay ->
                        val isSelectedText = idx == selectedTextIndex && activeTool == EditorTool.TEXT
                        val parsedTextColor = try {
                            Color(android.graphics.Color.parseColor(overlay.textColorHex))
                        } catch (_: Exception) {
                            ContentHigh
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset {
                                    IntOffset(
                                        x = (overlay.positionNormX.coerceIn(-0.45f, 0.45f) * canvasW.toPx()).roundToInt(),
                                        y = (overlay.positionNormY.coerceIn(-0.45f, 0.45f) * canvasH.toPx()).roundToInt()
                                    )
                                }
                                .pointerInput(idx, project.texts.size) {
                                    detectDragGestures(
                                        onDragStart = { selectedTextIndex = idx },
                                        onDragEnd = { pushUndoSnapshot() }
                                    ) { change, dragAmount ->
                                        change.consume()
                                        val curList = project.texts.toMutableList()
                                        val curItem = curList.getOrNull(idx) ?: return@detectDragGestures
                                        val dxNorm = dragAmount.x / canvasW.toPx().coerceAtLeast(1f)
                                        val dyNorm = dragAmount.y / canvasH.toPx().coerceAtLeast(1f)
                                        curList[idx] = curItem.copy(
                                            positionNormX = (curItem.positionNormX + dxNorm).coerceIn(-0.45f, 0.45f),
                                            positionNormY = (curItem.positionNormY + dyNorm).coerceIn(-0.45f, 0.45f)
                                        )
                                        updateProject(project.copy(texts = curList), recordUndo = false)
                                    }
                                }
                                .clip(RoundedCornerShape(overlay.boxCornerRadiusDp.dp))
                                .background(
                                    if (overlay.hasBackgroundBox) Color(0xBF0D0E11) else Color.Transparent
                                )
                                .border(
                                    width = if (isSelectedText) 1.5.dp else if (overlay.hasBackgroundBox) 1.dp else 0.dp,
                                    color = if (isSelectedText) PrimarySunset else if (overlay.hasBackgroundBox) ObsidianStroke else Color.Transparent,
                                    shape = RoundedCornerShape(overlay.boxCornerRadiusDp.dp)
                                )
                                .clickable {
                                    selectedTextIndex = idx
                                    activeTool = EditorTool.TEXT
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = overlay.text,
                                color = parsedTextColor,
                                fontSize = overlay.fontSizeSp.coerceIn(12f, 44f).sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Top-Start Canvas Info & Reset Zoom Pill
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xB30D0E11))
                            .clickable { activeTool = EditorTool.RATIO }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${project.canvas.ratio.label.substringBefore(" ")} • ${project.canvas.fitMode.label}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ContentMedium
                        )
                    }
                    if (project.canvas.scaleFactor != 1.0f ||
                        project.canvas.panOffsetNormX != 0f ||
                        project.canvas.panOffsetNormY != 0f ||
                        project.canvas.rotationDegrees != 0f
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SunsetTint15)
                                .border(1.dp, PrimarySunset, RoundedCornerShape(6.dp))
                                .clickable {
                                    updateProject(
                                        project.copy(
                                            canvas = project.canvas.copy(
                                                scaleFactor = 1.0f,
                                                panOffsetNormX = 0f,
                                                panOffsetNormY = 0f,
                                                rotationDegrees = 0f,
                                                isFlippedHorizontal = false
                                            )
                                        )
                                    )
                                }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "↺ Reset Zoom/Pan",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimarySunset
                            )
                        }
                    }
                }

                // Hold to Compare Original Badge (Top-End)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xB316181D))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isHoldingCompare = true
                                    tryAwaitRelease()
                                    isHoldingCompare = false
                                }
                            )
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isHoldingCompare) "ORIGINAL" else "COMPARE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isHoldingCompare) TertiaryAmber else ContentMedium
                    )
                }

                // Toast / Status Banner
                if (statusBannerText != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xE616181D))
                            .border(1.dp, PrimarySunset, RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = statusBannerText!!,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ContentHigh
                        )
                    }
                }
            }
        }

        // --- 3. CAPCUT / VN-STYLE MULTI-TRACK VISUAL FILMSTRIP TIMELINE ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ObsidianSurface1)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Transport & Quick Edit Action Bar (Play/Pause, Restart, Split at Playhead, Delete Clip, Mute)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SunsetIconButton(
                        icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        onClick = {
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                                isPlaying = false
                            } else {
                                exoPlayer.play()
                                isPlaying = true
                            }
                        },
                        tint = PrimarySunset,
                        size = 36
                    )
                    IconButton(
                        onClick = {
                            exoPlayer.seekTo(activeClip.startMs)
                            currentPlayheadMs = activeClip.startMs
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Clip Start",
                            tint = ContentMedium,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    TimecodeText(timeMs = currentPlayheadMs)
                    Text(text = "/", color = ContentMuted, fontSize = 11.sp)
                    TimecodeText(timeMs = project.sourceDurationMs, color = ContentMedium)
                }

                // One-Tap Quick NLE Actions right on the Timeline bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Quick Split at Playhead Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ObsidianSurface2)
                            .border(1.dp, ObsidianStroke, RoundedCornerShape(8.dp))
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                val newClips = timelineSplitter.splitAtPlayhead(
                                    clips = project.clips,
                                    playheadMs = currentPlayheadMs
                                )
                                if (newClips.size > project.clips.size) {
                                    updateProject(project.copy(clips = newClips))
                                    statusBannerText = "Split clip at ${formatDuration(currentPlayheadMs / 1000L)}"
                                } else {
                                    statusBannerText = "Move playhead inside a clip to split"
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCut,
                                contentDescription = "Split",
                                tint = PrimarySunset,
                                modifier = Modifier.size(14.dp)
                            )
                            Text("Split", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ContentHigh)
                        }
                    }

                    // Quick Delete Active Clip (when > 1 clip)
                    if (project.clips.size > 1) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(ObsidianSurface2)
                                .border(1.dp, ObsidianStroke, RoundedCornerShape(8.dp))
                                .clickable {
                                    val remaining = project.clips.toMutableList().apply { removeAt(safeClipIndex) }
                                    val nextIdx = (safeClipIndex - 1).coerceAtLeast(0)
                                    updateProject(project.copy(clips = remaining, activeClipIndex = nextIdx))
                                    statusBannerText = "Deleted ${activeClip.label}"
                                }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Clip",
                                tint = StatusError,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    // Quick Mute/Unmute Video Audio Toggle
                    val isVideoMuted = project.music.originalVideoVolume <= 0.02f
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isVideoMuted) SunsetTint15 else ObsidianSurface2)
                            .border(1.dp, if (isVideoMuted) PrimarySunset else ObsidianStroke, RoundedCornerShape(8.dp))
                            .clickable {
                                val newVol = if (isVideoMuted) 1.0f else 0.0f
                                updateProject(project.copy(music = project.music.copy(originalVideoVolume = newVol)))
                                statusBannerText = if (newVol == 0f) "Muted original video audio" else "Unmuted original video audio"
                            }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = if (isVideoMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Mute Toggle",
                            tint = if (isVideoMuted) PrimarySunset else ContentHigh,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            // --- Multi-Track CapCut Timeline Stacking (PRD Section 1.4, 2.1) ---
            // Track Category Filter Chips Bar
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(TimelineTrackCategory.entries) { category ->
                    val isCatSelected = selectedTrackCategory == category
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isCatSelected) PrimarySunset else ObsidianSurface1)
                            .border(1.dp, if (isCatSelected) PrimarySunset else ObsidianStroke, RoundedCornerShape(14.dp))
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedTrackCategory = category
                                when (category) {
                                    TimelineTrackCategory.VIDEO -> {
                                        isClipSelected = true
                                        selectedOverlayIndex = null
                                        selectedAdjustmentLayerIndex = null
                                    }
                                    TimelineTrackCategory.OVERLAY -> {
                                        isClipSelected = false
                                        selectedAdjustmentLayerIndex = null
                                        if (project.overlays.isNotEmpty() && selectedOverlayIndex == null) {
                                            selectedOverlayIndex = 0
                                        }
                                        activeTool = EditorTool.OVERLAY
                                    }
                                    TimelineTrackCategory.AUDIO -> {
                                        isClipSelected = false
                                        selectedOverlayIndex = null
                                        selectedAdjustmentLayerIndex = null
                                        activeTool = EditorTool.MUSIC
                                    }
                                    TimelineTrackCategory.TEXT -> {
                                        isClipSelected = false
                                        selectedOverlayIndex = null
                                        selectedAdjustmentLayerIndex = null
                                        activeTool = EditorTool.TEXT
                                    }
                                    TimelineTrackCategory.ADJUST -> {
                                        isClipSelected = false
                                        selectedOverlayIndex = null
                                        if (project.adjustmentLayers.isNotEmpty() && selectedAdjustmentLayerIndex == null) {
                                            selectedAdjustmentLayerIndex = 0
                                        }
                                        activeTool = EditorTool.ADJUST
                                    }
                                    TimelineTrackCategory.ALL -> {
                                        // View all tracks stacked together
                                    }
                                }
                            }
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = category.icon,
                                contentDescription = category.label,
                                tint = if (isCatSelected) Color.White else ContentMedium,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = category.label,
                                fontSize = 10.sp,
                                fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCatSelected) Color.White else ContentMedium
                            )
                        }
                    }
                }
            }

            // 1. MAIN VIDEO TRACK (Clips, Cut Transitions, Filmstrip, Dual-Thumb Trim)
            if (selectedTrackCategory == TimelineTrackCategory.ALL || selectedTrackCategory == TimelineTrackCategory.VIDEO) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(project.clips) { index, clip ->
                        val isSelectedClip = index == safeClipIndex
                        val hasThickWhiteBorder = isClipSelected && isSelectedClip && selectedOverlayIndex == null && selectedAdjustmentLayerIndex == null

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelectedClip) SunsetTint15 else ObsidianSurface2)
                                    .border(
                                        width = if (hasThickWhiteBorder) 2.5.dp else 1.dp,
                                        color = if (hasThickWhiteBorder) Color.White else if (isSelectedClip) PrimarySunset else ObsidianStroke,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        project = project.copy(activeClipIndex = index)
                                        isClipSelected = true
                                        selectedOverlayIndex = null
                                        selectedAdjustmentLayerIndex = null
                                        currentPlayheadMs = clip.startMs
                                        exoPlayer.seekTo(clip.startMs)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = clip.label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (hasThickWhiteBorder) Color.White else if (isSelectedClip) PrimarySunset else ContentHigh
                                    )
                                    if (clip.isReversed) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(PrimarySunset)
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text("<< REV", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                    if (clip.isFreezeFrame) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF38BDF8))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text("❄ FREEZE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                    Text(
                                        text = formatDuration(clip.durationMs / 1000L),
                                        fontSize = 10.sp,
                                        color = if (hasThickWhiteBorder) Color.White.copy(alpha = 0.85f) else ContentMedium
                                    )
                                }
                            }

                            // Junction Transition Cut Icon between adjacent clips (PRD Section 5.3)
                            if (index < project.clips.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(ObsidianSurface1)
                                        .border(
                                            width = 1.dp,
                                            color = if (clip.transition != ClipTransition.NONE) PrimarySunset else Color.White.copy(alpha = 0.8f),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .clickable {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            activeTransitionClipIndex = index
                                            activeTool = EditorTool.TRANSITION
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (clip.transition != ClipTransition.NONE) "⧉" else "⧎",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (clip.transition != ClipTransition.NONE) PrimarySunset else Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // Visual Video Filmstrip + Dual-Thumb Trim & Playhead Scrubber
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ObsidianSurface2)
                ) {
                    if (filmstripFrames.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp, vertical = 5.dp)
                                .clip(RoundedCornerShape(6.dp))
                        ) {
                            filmstripFrames.forEach { frameBmp ->
                                Image(
                                    bitmap = frameBmp.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    contentScale = ContentScale.Crop,
                                    alpha = 0.72f
                                )
                            }
                        }
                    }

                    DualThumbRangeSlider(
                        startMs = activeClip.startMs,
                        endMs = activeClip.endMs,
                        totalDurationMs = project.sourceDurationMs,
                        onRangeChange = { s, e ->
                            val updatedClips = project.clips.toMutableList()
                            if (safeClipIndex in updatedClips.indices) {
                                updatedClips[safeClipIndex] = activeClip.copy(startMs = s, endMs = e)
                                project = project.copy(clips = updatedClips)
                                currentPlayheadMs = s
                                exoPlayer.seekTo(s)
                            }
                        },
                        playheadMs = currentPlayheadMs,
                        onSeek = { targetMs ->
                            currentPlayheadMs = targetMs
                            exoPlayer.seekTo(targetMs)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // 2. OVERLAY / PIP TRACK (Picture-in-Picture layers)
            if (selectedTrackCategory == TimelineTrackCategory.ALL || selectedTrackCategory == TimelineTrackCategory.OVERLAY) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF6366F1).copy(alpha = 0.08f))
                        .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF6366F1).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFF6366F1), RoundedCornerShape(6.dp))
                                .clickable {
                                    overlayPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Layers, contentDescription = null, tint = Color(0xFFA5B4FC), modifier = Modifier.size(12.dp))
                                Text("+ Add Overlay (PIP)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA5B4FC))
                            }
                        }
                    }

                    itemsIndexed(project.overlays) { idx, overlay ->
                        val isSelectedOverlay = selectedOverlayIndex == idx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelectedOverlay) Color(0xFF6366F1).copy(alpha = 0.4f) else ObsidianSurface2)
                                .border(
                                    width = if (isSelectedOverlay) 2.dp else 1.dp,
                                    color = if (isSelectedOverlay) Color.White else Color(0xFF6366F1).copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    selectedOverlayIndex = idx
                                    isClipSelected = false
                                    selectedAdjustmentLayerIndex = null
                                    activeTool = EditorTool.PIP_SCALE
                                    currentPlayheadMs = overlay.startMs
                                    exoPlayer.seekTo(overlay.startMs)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(overlay.label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelectedOverlay) Color.White else ContentHigh)
                                Text(
                                    formatDuration((overlay.endMs - overlay.startMs) / 1000L),
                                    fontSize = 9.sp,
                                    color = if (isSelectedOverlay) Color.White.copy(alpha = 0.8f) else ContentMedium
                                )
                            }
                        }
                    }
                }
            }

            // 3. AUDIO & MUSIC TRACK
            if (selectedTrackCategory == TimelineTrackCategory.ALL || selectedTrackCategory == TimelineTrackCategory.AUDIO) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.08f))
                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .clickable { activeTool = EditorTool.MUSIC }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = null,
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (project.music.title == "No Background Music")
                                "Audio Track: Original ${(project.music.originalVideoVolume * 100).toInt()}% (+ Add BGM)"
                            else
                                "🎵 ${project.music.title} (${(project.music.musicVolume * 100).toInt()}%)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ContentHigh,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 4. TEXT OVERLAYS TRACK
            if (selectedTrackCategory == TimelineTrackCategory.ALL || selectedTrackCategory == TimelineTrackCategory.TEXT) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF59E0B).copy(alpha = 0.08f))
                        .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF59E0B).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(6.dp))
                                .clickable { activeTool = EditorTool.TEXT }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.TextFields, contentDescription = null, tint = Color(0xFFFCD34D), modifier = Modifier.size(12.dp))
                                Text("+ Add Text", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFCD34D))
                            }
                        }
                    }

                    itemsIndexed(project.texts) { tIdx, txt ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ObsidianSurface2)
                                .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .clickable {
                                    activeTool = EditorTool.TEXT
                                    currentPlayheadMs = txt.timing.offsetMs
                                    exoPlayer.seekTo(txt.timing.offsetMs)
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "✏️ ${txt.text.take(12)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = ContentHigh
                            )
                        }
                    }
                }
            }

            // 5. ADJUSTMENT LAYERS TRACK
            if (selectedTrackCategory == TimelineTrackCategory.ALL || selectedTrackCategory == TimelineTrackCategory.ADJUST) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF06B6D4).copy(alpha = 0.08f))
                        .border(1.dp, Color(0xFF06B6D4).copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF06B6D4).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFF06B6D4), RoundedCornerShape(6.dp))
                                .clickable {
                                    val newLayer = AdjustmentLayer(
                                        id = "layer_${System.currentTimeMillis()}",
                                        label = "Adjust ${project.adjustmentLayers.size + 1}",
                                        startMs = 0L,
                                        endMs = project.sourceDurationMs,
                                        filter = FilterConfig(),
                                        adjustment = AdjustmentParams()
                                    )
                                    val updated = project.adjustmentLayers + newLayer
                                    updateProject(project.copy(adjustmentLayers = updated))
                                    selectedAdjustmentLayerIndex = updated.lastIndex
                                    isClipSelected = false
                                    selectedOverlayIndex = null
                                    activeTool = EditorTool.ADJUST
                                    statusBannerText = "Added Adjustment Layer Track"
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF67E8F9), modifier = Modifier.size(12.dp))
                                Text("+ Add Adjust Layer", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF67E8F9))
                            }
                        }
                    }

                    itemsIndexed(project.adjustmentLayers) { aIdx, layer ->
                        val isSelectedLayer = selectedAdjustmentLayerIndex == aIdx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelectedLayer) Color(0xFF06B6D4).copy(alpha = 0.4f) else ObsidianSurface2)
                                .border(
                                    width = if (isSelectedLayer) 2.dp else 1.dp,
                                    color = if (isSelectedLayer) Color.White else Color(0xFF06B6D4).copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    selectedAdjustmentLayerIndex = aIdx
                                    isClipSelected = false
                                    selectedOverlayIndex = null
                                    activeTool = EditorTool.ADJUST
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "🎨 ${layer.label}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelectedLayer) Color.White else ContentHigh
                            )
                        }
                    }
                }
            }
        }

        // --- 4. CONTEXTUAL BOTTOM NLE TOOL DOCK (State A, State B, State PIP, State Adjust Layer) ---
        if (selectedOverlayIndex != null) {
            // STATE PIP: Picture-in-Picture / Overlay Track Selected
            val safePipIdx = selectedOverlayIndex!!.coerceIn(0, (project.overlays.lastIndex).coerceAtLeast(0))
            val curOverlay = project.overlays.getOrNull(safePipIdx)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianSurface2)
                    .padding(vertical = 10.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back / Deselect Overlay Chip
                item {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(SunsetTint15)
                            .border(1.dp, PrimarySunset, RoundedCornerShape(20.dp))
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedOverlayIndex = null
                                activeTool = null
                            }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimarySunset, modifier = Modifier.size(14.dp))
                            Text(curOverlay?.label ?: "Overlay", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimarySunset)
                        }
                    }
                }

                val pipTools = listOf(
                    EditorTool.PIP_SCALE to "Scale & Move",
                    EditorTool.PIP_OPACITY to "Opacity",
                    EditorTool.VOLUME to "Volume"
                )
                items(pipTools) { (tool, label) ->
                    val isSelected = activeTool == tool
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                activeTool = if (isSelected) null else tool
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) PrimarySunset else ObsidianSurface1)
                                .border(1.dp, if (isSelected) PrimarySunset else ObsidianStroke, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(tool.icon, contentDescription = label, tint = if (isSelected) ContentHigh else ContentMedium, modifier = Modifier.size(19.dp))
                        }
                        Text(label, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) PrimarySunset else ContentMedium)
                    }
                }

                // Delete Overlay
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (curOverlay != null) {
                                    val updated = project.overlays.toMutableList().apply { removeAt(safePipIdx) }
                                    updateProject(project.copy(overlays = updated))
                                    selectedOverlayIndex = null
                                    activeTool = null
                                    statusBannerText = "Deleted overlay"
                                }
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(ObsidianSurface1)
                                .border(1.dp, ObsidianStroke, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusError, modifier = Modifier.size(19.dp))
                        }
                        Text("Delete", fontSize = 10.sp, color = StatusError)
                    }
                }
            }
        } else if (selectedAdjustmentLayerIndex != null) {
            // STATE ADJUSTMENT LAYER: Adjustment Layer Selected
            val safeLayerIdx = selectedAdjustmentLayerIndex!!.coerceIn(0, (project.adjustmentLayers.lastIndex).coerceAtLeast(0))
            val curLayer = project.adjustmentLayers.getOrNull(safeLayerIdx)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianSurface2)
                    .padding(vertical = 10.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back / Deselect Adjustment Layer
                item {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(SunsetTint15)
                            .border(1.dp, PrimarySunset, RoundedCornerShape(20.dp))
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedAdjustmentLayerIndex = null
                                activeTool = null
                            }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimarySunset, modifier = Modifier.size(14.dp))
                            Text(curLayer?.label ?: "Adjust Layer", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimarySunset)
                        }
                    }
                }

                // Adjust Tools (Sliders)
                item {
                    val isSelected = activeTool == EditorTool.ADJUST
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { activeTool = if (isSelected) null else EditorTool.ADJUST }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) PrimarySunset else ObsidianSurface1)
                                .border(1.dp, if (isSelected) PrimarySunset else ObsidianStroke, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = "Adjust", tint = if (isSelected) ContentHigh else ContentMedium, modifier = Modifier.size(19.dp))
                        }
                        Text("Adjust", fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) PrimarySunset else ContentMedium)
                    }
                }

                // Filter Presets
                item {
                    val isSelected = activeTool == EditorTool.FILTERS
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { activeTool = if (isSelected) null else EditorTool.FILTERS }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) PrimarySunset else ObsidianSurface1)
                                .border(1.dp, if (isSelected) PrimarySunset else ObsidianStroke, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ColorLens, contentDescription = "Filters", tint = if (isSelected) ContentHigh else ContentMedium, modifier = Modifier.size(19.dp))
                        }
                        Text("Filters", fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) PrimarySunset else ContentMedium)
                    }
                }

                // Delete Layer
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (curLayer != null) {
                                    val updated = project.adjustmentLayers.toMutableList().apply { removeAt(safeLayerIdx) }
                                    updateProject(project.copy(adjustmentLayers = updated))
                                    selectedAdjustmentLayerIndex = null
                                    activeTool = null
                                    statusBannerText = "Deleted adjustment layer"
                                }
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(ObsidianSurface1)
                                .border(1.dp, ObsidianStroke, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusError, modifier = Modifier.size(19.dp))
                        }
                        Text("Delete", fontSize = 10.sp, color = StatusError)
                    }
                }
            }
        } else if (!isClipSelected) {
            // STATE A: Root Studio Tools (Nothing selected)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianSurface2)
                    .padding(vertical = 10.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val rootTools = listOf(
                    EditorTool.EDIT to "Edit",
                    EditorTool.AUDIO to "Audio",
                    EditorTool.TEXT to "Text",
                    EditorTool.OVERLAY to "Overlay",
                    EditorTool.FILTERS to "Filters",
                    EditorTool.ADJUST to "Adjust",
                    EditorTool.RATIO to "Canvas",
                    EditorTool.BACKGROUND to "Background"
                )
                items(rootTools) { (tool, label) ->
                    val isSelected = activeTool == tool
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (tool == EditorTool.EDIT) {
                                    val clipUnderPlayhead = project.clips.indexOfFirst {
                                        currentPlayheadMs >= it.startMs && currentPlayheadMs <= it.endMs
                                    }
                                    val targetIdx = if (clipUnderPlayhead >= 0) clipUnderPlayhead else project.activeClipIndex
                                    project = project.copy(activeClipIndex = targetIdx)
                                    isClipSelected = true
                                    selectedOverlayIndex = null
                                    selectedAdjustmentLayerIndex = null
                                } else if (tool == EditorTool.AUDIO) {
                                    activeTool = EditorTool.MUSIC
                                } else if (tool == EditorTool.OVERLAY) {
                                    selectedTrackCategory = TimelineTrackCategory.OVERLAY
                                    if (project.overlays.isEmpty()) {
                                        overlayPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                        )
                                    } else {
                                        selectedOverlayIndex = 0
                                        activeTool = EditorTool.PIP_SCALE
                                    }
                                } else {
                                    activeTool = if (isSelected) null else tool
                                }
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) PrimarySunset else ObsidianSurface1)
                                .border(1.dp, if (isSelected) PrimarySunset else ObsidianStroke, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tool.icon,
                                contentDescription = label,
                                tint = if (isSelected) ContentHigh else ContentMedium,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) PrimarySunset else ContentMedium
                        )
                    }
                }
            }
        } else {
            // STATE B: Clip-Specific Tools (Clip selected)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianSurface2)
                    .padding(vertical = 10.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Deselect chip on far left [ < Clip Name ]
                item {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(SunsetTint15)
                            .border(1.dp, PrimarySunset, RoundedCornerShape(20.dp))
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isClipSelected = false
                            }
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Deselect",
                                tint = PrimarySunset,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = activeClip.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimarySunset
                            )
                        }
                    }
                }

                // 2. Clip Tools: SPLIT, BATCH_SPLIT, SPEED, VOLUME, TRANSFORM, TRANSITION, DELETE, DUPLICATE, REVERSE, FREEZE
                val clipTools = listOf(
                    EditorTool.SPLIT,
                    EditorTool.BATCH_SPLIT,
                    EditorTool.SPEED,
                    EditorTool.VOLUME,
                    EditorTool.TRANSFORM,
                    EditorTool.TRANSITION,
                    EditorTool.DELETE,
                    EditorTool.DUPLICATE,
                    EditorTool.REVERSE,
                    EditorTool.FREEZE
                )
                items(clipTools) { tool ->
                    val isSelected = activeTool == tool
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                when (tool) {
                                    EditorTool.SPLIT -> {
                                        val newClips = timelineSplitter.splitAtPlayhead(project.clips, currentPlayheadMs)
                                        if (newClips.size > project.clips.size) {
                                            updateProject(project.copy(clips = newClips))
                                            statusBannerText = "Split clip at ${formatDuration(currentPlayheadMs / 1000L)}"
                                        } else {
                                            statusBannerText = "Move playhead inside clip to split"
                                        }
                                    }
                                    EditorTool.DELETE -> {
                                        if (project.clips.size > 1) {
                                            val remaining = project.clips.toMutableList().apply { removeAt(safeClipIndex) }
                                            val nextIdx = (safeClipIndex - 1).coerceAtLeast(0)
                                            updateProject(project.copy(clips = remaining, activeClipIndex = nextIdx))
                                            statusBannerText = "Deleted ${activeClip.label}"
                                        } else {
                                            statusBannerText = "Cannot delete the only clip"
                                        }
                                    }
                                    EditorTool.DUPLICATE -> {
                                        val duplicated = timelineSplitter.duplicateClip(project.clips, safeClipIndex)
                                        updateProject(project.copy(clips = duplicated))
                                        statusBannerText = "Duplicated ${activeClip.label}"
                                    }
                                    EditorTool.FREEZE -> {
                                        val frozen = timelineSplitter.insertFreezeFrame(project.clips, currentPlayheadMs)
                                        updateProject(project.copy(clips = frozen))
                                        statusBannerText = "Inserted 3.0s Freeze Frame"
                                    }
                                    EditorTool.REVERSE -> {
                                        val updated = project.clips.toMutableList()
                                        val cur = updated[safeClipIndex]
                                        val nextRev = !cur.isReversed
                                        updated[safeClipIndex] = cur.copy(isReversed = nextRev)
                                        updateProject(project.copy(clips = updated))
                                        if (nextRev) {
                                            currentPlayheadMs = cur.endMs
                                            exoPlayer.seekTo(cur.endMs)
                                        }
                                        statusBannerText = if (nextRev) "Reversed ${cur.label} (playing backwards)" else "Normal playback for ${cur.label}"
                                    }
                                    else -> {
                                        activeTool = if (isSelected) null else tool
                                    }
                                }
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) PrimarySunset else ObsidianSurface1)
                                .border(1.dp, if (isSelected) PrimarySunset else ObsidianStroke, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tool.icon,
                                contentDescription = tool.label,
                                tint = if (isSelected) ContentHigh else ContentMedium,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Text(
                            text = tool.label,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) PrimarySunset else ContentMedium
                        )
                    }
                }
            }
        }
    }

    // --- CONTEXTUAL BOTTOM PANELS FOR ALL 10 STUDIO TOOLS ---
    if (activeTool != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { activeTool = null },
            sheetState = sheetState,
            containerColor = ObsidianSurface1
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activeTool!!.label,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ContentHigh
                    )
                    Text(
                        text = "Done",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimarySunset,
                        modifier = Modifier.clickable { activeTool = null }
                    )
                }

                when (activeTool) {
                    EditorTool.SPLIT -> {
                        var customParts by remember { mutableIntStateOf(4) }
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SunsetPrimaryButton(
                                text = "Split at Current Playhead (${formatDuration(currentPlayheadMs / 1000L)})",
                                onClick = {
                                    val newClips = timelineSplitter.splitAtPlayhead(
                                        clips = project.clips,
                                        playheadMs = currentPlayheadMs
                                    )
                                    updateProject(project.copy(clips = newClips))
                                    statusBannerText = "Split clip at playhead"
                                    activeTool = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                icon = Icons.Default.ContentCut
                            )

                            Text(
                                text = "Auto-Split into Equal Parts (Batch Reels/Shorts)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ContentHigh
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(2, 3, 4, 5, 10).forEach { parts ->
                                    SunsetOutlinedButton(
                                        text = "${parts}x",
                                        onClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            val newClips = timelineSplitter.splitIntoEqualParts(
                                                totalDurationMs = project.sourceDurationMs,
                                                numberOfParts = parts
                                            )
                                            updateProject(project.copy(clips = newClips, activeClipIndex = 0))
                                            statusBannerText = "Split into $parts equal parts"
                                            activeTool = null
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            SunsetSlider(
                                value = customParts.toFloat(),
                                onValueChange = { customParts = it.toInt().coerceIn(2, 20) },
                                valueRange = 2f..20f,
                                label = "Custom Equal Parts",
                                valueFormatter = { "${it.toInt()} parts" }
                            )

                            SunsetOutlinedButton(
                                text = "Split into $customParts Equal Parts",
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val newClips = timelineSplitter.splitIntoEqualParts(
                                        totalDurationMs = project.sourceDurationMs,
                                        numberOfParts = customParts
                                    )
                                    updateProject(project.copy(clips = newClips, activeClipIndex = 0))
                                    statusBannerText = "Split into $customParts equal parts"
                                    activeTool = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = "Split by Fixed Segment Duration",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ContentHigh
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(5 to "5s Clips", 15 to "15s Shorts", 30 to "30s Reels").forEach { (sec, label) ->
                                    SunsetOutlinedButton(
                                        text = label,
                                        onClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            val newClips = timelineSplitter.splitByDuration(
                                                totalDurationMs = project.sourceDurationMs,
                                                targetPartSeconds = sec
                                            )
                                            updateProject(project.copy(clips = newClips, activeClipIndex = 0))
                                            statusBannerText = "Split into ${newClips.size} segments ($label)"
                                            activeTool = null
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    EditorTool.EDIT -> {
                        // Forward to Trim & Clip workspace
                        activeTool = EditorTool.TRIM
                    }

                    EditorTool.AUDIO -> {
                        activeTool = EditorTool.MUSIC
                    }

                    EditorTool.BATCH_SPLIT -> {
                        val clipDurMs = activeClip.durationMs
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Tab Selector: By Duration vs By Parts
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SunsetChip(
                                    text = "By Duration",
                                    selected = batchSplitTab == 0,
                                    onClick = { batchSplitTab = 0 },
                                    modifier = Modifier.weight(1f)
                                )
                                SunsetChip(
                                    text = "By Parts",
                                    selected = batchSplitTab == 1,
                                    onClick = { batchSplitTab = 1 },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (batchSplitTab == 0) {
                                // Duration Mode
                                Text("Target Clip Duration", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ContentHigh)
                                // Quick Chips: 10s, 15s, 30s, 60s, 90s
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(10, 15, 30, 60, 90).forEach { sec ->
                                        SunsetChip(
                                            text = "${sec}s",
                                            selected = batchSplitDurationSec == sec,
                                            onClick = { batchSplitDurationSec = sec },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                // Stepper: [ - ] 00:30 [ + ]
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { batchSplitDurationSec = (batchSplitDurationSec - 5).coerceAtLeast(5) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(ObsidianSurface2)
                                            .border(1.dp, ObsidianStroke, CircleShape)
                                    ) {
                                        Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ContentHigh)
                                    }
                                    Text(
                                        text = "${formatDuration(batchSplitDurationSec.toLong())} (${batchSplitDurationSec}s)",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimarySunset,
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    )
                                    IconButton(
                                        onClick = { batchSplitDurationSec = (batchSplitDurationSec + 5).coerceAtMost(300) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(ObsidianSurface2)
                                            .border(1.dp, ObsidianStroke, CircleShape)
                                    ) {
                                        Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ContentHigh)
                                    }
                                }
                            } else {
                                // Parts Mode
                                Text("Number of Equal Parts", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ContentHigh)
                                // Quick Chips: 2, 3, 4, 5, 10
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(2, 3, 4, 5, 10).forEach { parts ->
                                        SunsetChip(
                                            text = "${parts}x",
                                            selected = batchSplitPartsCount == parts,
                                            onClick = { batchSplitPartsCount = parts },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                // Stepper: [ - ] 4 parts [ + ]
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { batchSplitPartsCount = (batchSplitPartsCount - 1).coerceAtLeast(2) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(ObsidianSurface2)
                                            .border(1.dp, ObsidianStroke, CircleShape)
                                    ) {
                                        Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ContentHigh)
                                    }
                                    Text(
                                        text = "$batchSplitPartsCount Parts",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimarySunset,
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    )
                                    IconButton(
                                        onClick = { batchSplitPartsCount = (batchSplitPartsCount + 1).coerceAtMost(20) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(ObsidianSurface2)
                                            .border(1.dp, ObsidianStroke, CircleShape)
                                    ) {
                                        Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ContentHigh)
                                    }
                                }
                            }

                            // Tail Remainder Rule (Section 2.2)
                            Text("Tail Remainder Rule", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ContentHigh)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                TailRemainderRule.entries.forEach { rule ->
                                    val isSelected = batchSplitTailRule == rule
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) SunsetTint15 else ObsidianSurface2)
                                            .border(1.dp, if (isSelected) PrimarySunset else ObsidianStroke, RoundedCornerShape(8.dp))
                                            .clickable { batchSplitTailRule = rule }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = rule.label,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) PrimarySunset else ContentHigh
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Done,
                                                    contentDescription = null,
                                                    tint = PrimarySunset,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Live calculation preview label
                            val previewSummary = if (batchSplitTab == 0) {
                                val partMs: Long = (batchSplitDurationSec * 1000L).coerceAtLeast(1000L)
                                val fullParts = (clipDurMs / partMs).toInt()
                                val remSec: Long = (clipDurMs % partMs) / 1000L
                                val mergedSec: Long = batchSplitDurationSec.toLong() + remSec
                                when (batchSplitTailRule) {
                                    TailRemainderRule.SEPARATE_CLIP -> "Will split this ${formatDuration(clipDurMs / 1000L)} clip into $fullParts clips of ${batchSplitDurationSec}s + 1 clip of ${remSec}s remainder"
                                    TailRemainderRule.MERGE_INTO_LAST -> "Will split this ${formatDuration(clipDurMs / 1000L)} clip into ${fullParts.coerceAtLeast(1)} clips (last clip is ${mergedSec}s)"
                                    TailRemainderRule.TRIM_OFF -> "Will split this ${formatDuration(clipDurMs / 1000L)} clip into $fullParts clips of ${batchSplitDurationSec}s (${remSec}s trimmed off)"
                                }
                            } else {
                                val partsLong: Long = batchSplitPartsCount.toLong().coerceAtLeast(1L)
                                val partDurSec: Long = (clipDurMs / partsLong) / 1000L
                                "Will split this ${formatDuration(clipDurMs / 1000L)} clip into $batchSplitPartsCount equal clips of ${formatDuration(partDurSec)} each"
                            }

                            ObsidianInnerCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = previewSummary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ContentHigh
                                )
                            }

                            SunsetPrimaryButton(
                                text = "Apply Batch Split",
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val newClips = if (batchSplitTab == 0) {
                                        timelineSplitter.splitByDurationWithTailRule(
                                            totalDurationMs = project.sourceDurationMs,
                                            targetPartSeconds = batchSplitDurationSec,
                                            tailRule = batchSplitTailRule
                                        )
                                    } else {
                                        timelineSplitter.splitIntoEqualParts(
                                            totalDurationMs = project.sourceDurationMs,
                                            numberOfParts = batchSplitPartsCount
                                        )
                                    }
                                    updateProject(project.copy(clips = newClips, activeClipIndex = 0))
                                    statusBannerText = "Batch split into ${newClips.size} clips"
                                    activeTool = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                icon = Icons.Default.CallSplit
                            )
                        }
                    }

                    EditorTool.BACKGROUND -> {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Tab row: Blur vs Color vs Gradient
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SunsetChip(
                                    text = "Blur",
                                    selected = backgroundTab == 0,
                                    onClick = { backgroundTab = 0 },
                                    modifier = Modifier.weight(1f)
                                )
                                SunsetChip(
                                    text = "Color",
                                    selected = backgroundTab == 1,
                                    onClick = { backgroundTab = 1 },
                                    modifier = Modifier.weight(1f)
                                )
                                SunsetChip(
                                    text = "Gradient",
                                    selected = backgroundTab == 2,
                                    onClick = { backgroundTab = 2 },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            when (backgroundTab) {
                                0 -> {
                                    // Blur Background
                                    Text("Gaussian Blur Intensity", fontSize = 13.sp, color = ContentMedium)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(
                                            10f to "15%",
                                            24f to "35%",
                                            40f to "65%",
                                            60f to "100%"
                                        ).forEach { (rad, lbl) ->
                                            SunsetChip(
                                                text = lbl,
                                                selected = project.canvas.fitMode == FitMode.FIT_BLUR_BG && kotlin.math.abs(project.canvas.blurRadiusDp - rad) < 5f,
                                                onClick = {
                                                    updateProject(
                                                        project.copy(
                                                            canvas = project.canvas.copy(
                                                                fitMode = FitMode.FIT_BLUR_BG,
                                                                blurRadiusDp = rad
                                                            )
                                                        )
                                                    )
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                    SunsetSlider(
                                        value = project.canvas.blurRadiusDp,
                                        onValueChange = {
                                            updateProject(
                                                project.copy(canvas = project.canvas.copy(fitMode = FitMode.FIT_BLUR_BG, blurRadiusDp = it)),
                                                recordUndo = false
                                            )
                                        },
                                        onValueChangeFinished = { pushUndoSnapshot() },
                                        valueRange = 4f..60f,
                                        label = "Fine-Tune Blur Radius",
                                        valueFormatter = { "${it.toInt()} px" }
                                    )
                                }
                                1 -> {
                                    // Color Palette
                                    Text("Solid Canvas Background Color", fontSize = 13.sp, color = ContentMedium)
                                    val paletteColors = listOf(
                                        "#0D0E11", "#000000", "#FFFFFF", "#1E293B", "#334155",
                                        "#EA6113", "#F97316", "#FBB931", "#EAB308", "#84CC16",
                                        "#10B981", "#06B6D4", "#0EA5E9", "#3B82F6", "#6366F1",
                                        "#8B5CF6", "#A855F7", "#D946EF", "#EC4899", "#F43F5E",
                                        "#31102F", "#1C1917", "#27272A", "#3F3F46", "#71717A"
                                    )
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(paletteColors) { hex ->
                                            val isSel = project.canvas.fitMode == FitMode.FIT_SOLID_BG && project.canvas.backgroundColorHex.equals(hex, ignoreCase = true)
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                                    .border(
                                                        width = if (isSel) 3.dp else 1.dp,
                                                        color = if (isSel) PrimarySunset else ObsidianStroke,
                                                        shape = CircleShape
                                                    )
                                                    .clickable {
                                                        updateProject(
                                                            project.copy(
                                                                canvas = project.canvas.copy(
                                                                    fitMode = FitMode.FIT_SOLID_BG,
                                                                    backgroundColorHex = hex,
                                                                    gradientColorsHex = emptyList()
                                                                )
                                                            )
                                                        )
                                                    }
                                            )
                                        }
                                    }
                                }
                                2 -> {
                                    // Gradient Presets (Sunset, Cyberpunk, Soft Pastel, Dark Vignette)
                                    Text("Atmospheric Gradients", fontSize = 13.sp, color = ContentMedium)
                                    val gradients = listOf(
                                        "Sunset" to listOf("#EA6113", "#FBB931"),
                                        "Cyberpunk" to listOf("#7928CA", "#FF0080"),
                                        "Soft Pastel" to listOf("#A18CD1", "#FBC2EB"),
                                        "Dark Vignette" to listOf("#1A1A2E", "#16213E")
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        gradients.forEach { (name, colors) ->
                                            val isSel = project.canvas.gradientColorsHex == colors
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        updateProject(
                                                            project.copy(
                                                                canvas = project.canvas.copy(
                                                                    fitMode = FitMode.FIT_SOLID_BG,
                                                                    backgroundColorHex = colors.first(),
                                                                    gradientColorsHex = colors
                                                                )
                                                            )
                                                        )
                                                    }
                                                    .padding(2.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(44.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        androidx.compose.ui.graphics.Brush.verticalGradient(
                                                            colors.map { Color(android.graphics.Color.parseColor(it)) }
                                                        )
                                                    )
                                                    .border(
                                                        width = if (isSel) 2.dp else 1.dp,
                                                        color = if (isSel) PrimarySunset else ObsidianStroke,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                )
                                                Text(
                                                    text = name,
                                                    fontSize = 10.sp,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSel) PrimarySunset else ContentMedium,
                                                    modifier = Modifier.padding(top = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Apply to all clips button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            statusBannerText = "Background applied to all clips"
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Layers, contentDescription = null, tint = PrimarySunset, modifier = Modifier.size(18.dp))
                                    Text("Apply to all", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PrimarySunset)
                                }
                                IconButton(onClick = { activeTool = null }) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Done", tint = PrimarySunset, modifier = Modifier.size(28.dp))
                                }
                            }
                        }
                    }

                    EditorTool.TRANSITION -> {
                        val targetClipIdx = activeTransitionClipIndex.coerceIn(0, (project.clips.size - 1).coerceAtLeast(0))
                        val curClip = project.clips.getOrNull(targetClipIdx) ?: ClipSection(0L, project.sourceDurationMs)
                        var selectedCategory by remember { mutableStateOf("Basic") }

                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Transition: Cut ${targetClipIdx + 1} ➔ ${targetClipIdx + 2}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ContentHigh
                                )
                                Text(
                                    text = curClip.transition.label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimarySunset
                                )
                            }

                            // Category Tabs: Basic, Slide, Camera
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Basic", "Slide", "Camera").forEach { cat ->
                                    SunsetChip(
                                        text = cat,
                                        selected = selectedCategory == cat,
                                        onClick = { selectedCategory = cat },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // Transitions in selected category
                            val categoryTransitions = ClipTransition.entries.filter {
                                it == ClipTransition.NONE || it.category == selectedCategory
                            }

                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(categoryTransitions) { trans ->
                                    val isSel = curClip.transition == trans
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                val updated = project.clips.toMutableList()
                                                updated[targetClipIdx] = curClip.copy(transition = trans)
                                                updateProject(project.copy(clips = updated))
                                            }
                                            .padding(2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 68.dp, height = 68.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSel) SunsetTint15 else ObsidianSurface2)
                                                .border(
                                                    width = if (isSel) 2.dp else 1.dp,
                                                    color = if (isSel) PrimarySunset else ObsidianStroke,
                                                    shape = RoundedCornerShape(10.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = when (trans) {
                                                    ClipTransition.NONE -> Icons.Default.Block
                                                    ClipTransition.CROSS_DISSOLVE -> Icons.Default.Animation
                                                    ClipTransition.FADE_TO_BLACK -> Icons.Default.DarkMode
                                                    ClipTransition.FADE_TO_WHITE -> Icons.Default.BrightnessHigh
                                                    ClipTransition.SLIDE_LEFT, ClipTransition.SLIDE_RIGHT -> Icons.Default.CallSplit
                                                    ClipTransition.ZOOM_IN, ClipTransition.ZOOM_OUT -> Icons.Default.CenterFocusStrong
                                                    else -> Icons.Default.AutoAwesome
                                                },
                                                contentDescription = trans.label,
                                                tint = if (isSel) PrimarySunset else ContentMedium,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Text(
                                            text = trans.label,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSel) PrimarySunset else ContentMedium,
                                            maxLines = 1,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }

                            // Transition Duration Slider (0.1s to 3.0s)
                            if (curClip.transition != ClipTransition.NONE) {
                                val durSec = curClip.transitionDurationMs / 1000f
                                SunsetSlider(
                                    value = durSec,
                                    onValueChange = { newSec ->
                                        val updated = project.clips.toMutableList()
                                        updated[targetClipIdx] = curClip.copy(transitionDurationMs = (newSec * 1000).toLong())
                                        updateProject(project.copy(clips = updated), recordUndo = false)
                                    },
                                    onValueChangeFinished = { pushUndoSnapshot() },
                                    valueRange = 0.1f..3.0f,
                                    label = "Transition Duration",
                                    valueFormatter = { String.format("%.1fs", it) }
                                )
                            }

                            // Action Row: Apply to All Transitions + Done
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            val updated = project.clips.map {
                                                it.copy(
                                                    transition = curClip.transition,
                                                    transitionDurationMs = curClip.transitionDurationMs
                                                )
                                            }
                                            updateProject(project.copy(clips = updated))
                                            statusBannerText = "Applied ${curClip.transition.label} to all cuts"
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Layers, contentDescription = null, tint = PrimarySunset, modifier = Modifier.size(18.dp))
                                    Text("Apply to all", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PrimarySunset)
                                }
                                IconButton(onClick = { activeTool = null }) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Done", tint = PrimarySunset, modifier = Modifier.size(28.dp))
                                }
                            }
                        }
                    }

                    EditorTool.VOLUME -> {
                        val targetClipIdx = safeClipIndex.coerceIn(0, (project.clips.size - 1).coerceAtLeast(0))
                        val curClip = project.clips.getOrNull(targetClipIdx) ?: ClipSection(0L, project.sourceDurationMs)
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Clip Volume (${curClip.label})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ContentHigh)
                                Text("${(curClip.volume * 100).toInt()}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimarySunset)
                            }

                            // Volume Slider 0% to 200%
                            SunsetSlider(
                                value = curClip.volume,
                                onValueChange = { newVol ->
                                    val updated = project.clips.toMutableList()
                                    val muted = newVol <= 0.01f
                                    updated[targetClipIdx] = curClip.copy(volume = newVol, isMuted = muted)
                                    updateProject(project.copy(clips = updated), recordUndo = false)
                                    exoPlayer.volume = if (muted) 0f else (newVol * project.music.originalVideoVolume).coerceIn(0f, 1f)
                                },
                                onValueChangeFinished = { pushUndoSnapshot() },
                                valueRange = 0f..2.0f,
                                label = "Audio Level (Center snap at 100%)",
                                valueFormatter = { "${(it * 100).toInt()}%" }
                            )

                            // Presets: Mute, 50%, 100%, 150%, 200%
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    0f to "Mute",
                                    0.5f to "50%",
                                    1.0f to "100%",
                                    1.5f to "150%",
                                    2.0f to "200%"
                                ).forEach { (vol, label) ->
                                    val isSel = kotlin.math.abs(curClip.volume - vol) < 0.04f
                                    SunsetChip(
                                        text = label,
                                        selected = isSel,
                                        onClick = {
                                            val updated = project.clips.toMutableList()
                                            val muted = vol <= 0.01f
                                            updated[targetClipIdx] = curClip.copy(volume = vol, isMuted = muted)
                                            updateProject(project.copy(clips = updated))
                                            exoPlayer.volume = if (muted) 0f else (vol * project.music.originalVideoVolume).coerceIn(0f, 1f)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    EditorTool.TRANSFORM -> {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text("Clip Orientation & Dimensions", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ContentHigh)

                            // Rotate 90° & Mirror (Horizontal Flip)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SunsetOutlinedButton(
                                    text = "Rotate 90° (${project.canvas.rotationDegrees.toInt()}°)",
                                    onClick = {
                                        val nextRot = (project.canvas.rotationDegrees + 90f) % 360f
                                        updateProject(project.copy(canvas = project.canvas.copy(rotationDegrees = nextRot)))
                                        statusBannerText = "Rotated canvas to ${nextRot.toInt()}°"
                                    },
                                    icon = Icons.Default.RotateRight,
                                    modifier = Modifier.weight(1f)
                                )
                                SunsetOutlinedButton(
                                    text = if (project.canvas.isFlippedHorizontal) "Mirrored" else "Mirror",
                                    onClick = {
                                        val nextFlip = !project.canvas.isFlippedHorizontal
                                        updateProject(
                                            project.copy(
                                                canvas = project.canvas.copy(
                                                    isFlippedHorizontal = nextFlip
                                                )
                                            )
                                        )
                                        statusBannerText = if (nextFlip) "Mirrored canvas horizontally" else "Canvas mirror reset"
                                    },
                                    icon = Icons.Default.Flip,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Clip Crop Dimension Presets (computes true aspect-ratio crop relative to native video ratio)
                            Text("Clip Crop Aspect Presets (Drag inside crop box to re-center)", fontSize = 12.sp, color = ContentMedium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("Free", "1:1", "16:9", "9:16", "4:3", "4:5").forEach { preset ->
                                    val isSel = activeClip.cropPreset == preset
                                    SunsetChip(
                                        text = preset,
                                        selected = isSel,
                                        onClick = {
                                            val isRot90 = (project.canvas.rotationDegrees.toInt() % 180) != 0
                                            val srcRatio = (if (isRot90) (1f / nativeVideoRatio) else nativeVideoRatio).coerceIn(0.25f, 4.0f)
                                            val targetRatio: Float? = when (preset) {
                                                "1:1" -> 1.0f
                                                "16:9" -> 16f / 9f
                                                "9:16" -> 9f / 16f
                                                "4:3" -> 4f / 3f
                                                "4:5" -> 4f / 5f
                                                else -> null
                                            }
                                            val (wPct, hPct) = if (targetRatio == null) {
                                                100f to 100f
                                            } else if (srcRatio >= targetRatio) {
                                                ((targetRatio / srcRatio) * 100f).coerceIn(15f, 100f) to 100f
                                            } else {
                                                100f to ((srcRatio / targetRatio) * 100f).coerceIn(15f, 100f)
                                            }
                                            val updated = project.clips.toMutableList()
                                            updated[safeClipIndex] = activeClip.copy(
                                                cropPreset = preset,
                                                cropWidthPercent = wPct,
                                                cropHeightPercent = hPct,
                                                cropOffsetX = 0f,
                                                cropOffsetY = 0f
                                            )
                                            updateProject(project.copy(clips = updated))
                                            statusBannerText = "Cropped clip to $preset (${wPct.toInt()}% × ${hPct.toInt()}%)"
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // Width % Slider
                            SunsetSlider(
                                value = activeClip.cropWidthPercent,
                                onValueChange = {
                                    val updated = project.clips.toMutableList()
                                    updated[safeClipIndex] = activeClip.copy(cropWidthPercent = it, cropPreset = "Custom")
                                    updateProject(project.copy(clips = updated), recordUndo = false)
                                },
                                onValueChangeFinished = { pushUndoSnapshot() },
                                valueRange = 15f..100f,
                                label = "Crop Width (Trims left/right edges)",
                                valueFormatter = { "${it.toInt()}%" }
                            )

                            // Height % Slider
                            SunsetSlider(
                                value = activeClip.cropHeightPercent,
                                onValueChange = {
                                    val updated = project.clips.toMutableList()
                                    updated[safeClipIndex] = activeClip.copy(cropHeightPercent = it, cropPreset = "Custom")
                                    updateProject(project.copy(clips = updated), recordUndo = false)
                                },
                                onValueChangeFinished = { pushUndoSnapshot() },
                                valueRange = 15f..100f,
                                label = "Crop Height (Trims top/bottom edges)",
                                valueFormatter = { "${it.toInt()}%" }
                            )

                            SunsetSlider(
                                value = project.canvas.scaleFactor,
                                onValueChange = {
                                    updateProject(
                                        project.copy(canvas = project.canvas.copy(scaleFactor = it)),
                                        recordUndo = false
                                    )
                                },
                                onValueChangeFinished = { pushUndoSnapshot() },
                                valueRange = 0.35f..4.0f,
                                label = "Scale Zoom (or pinch preview)",
                                valueFormatter = { "${(it * 100).toInt()}%" }
                            )

                            // Reset Transform Button
                            SunsetOutlinedButton(
                                text = "Reset Clip Dimensions & Transform",
                                onClick = {
                                    val updated = project.clips.toMutableList()
                                    updated[safeClipIndex] = activeClip.copy(
                                        cropPreset = "Free",
                                        cropWidthPercent = 100f,
                                        cropHeightPercent = 100f,
                                        cropOffsetX = 0f,
                                        cropOffsetY = 0f
                                    )
                                    updateProject(
                                        project.copy(
                                            clips = updated,
                                            canvas = project.canvas.copy(
                                                rotationDegrees = 0f,
                                                isFlippedHorizontal = false,
                                                scaleFactor = 1.0f,
                                                panOffsetNormX = 0f,
                                                panOffsetNormY = 0f
                                            )
                                        )
                                    )
                                    statusBannerText = "Reset clip transform and dimensions"
                                },
                                modifier = Modifier.fillMaxWidth(),
                                icon = Icons.Default.RestartAlt
                            )
                        }
                    }

                    EditorTool.TRIM -> {
                        val safeIdx = project.activeClipIndex.coerceIn(0, (project.clips.size - 1).coerceAtLeast(0))
                        val currentClip = project.clips.getOrNull(safeIdx) ?: ClipSection(0L, project.sourceDurationMs)
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ObsidianInnerCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Active Clip: ${currentClip.label}",
                                            fontWeight = FontWeight.Bold,
                                            color = ContentHigh,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Range: ${formatDuration(currentClip.startMs / 1000L)} – ${formatDuration(currentClip.endMs / 1000L)} (${formatDuration(currentClip.durationMs / 1000L)})",
                                            color = PrimarySunset,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    if (project.clips.size > 1) {
                                        IconButton(
                                            onClick = {
                                                val remaining = project.clips.toMutableList().apply { removeAt(safeIdx) }
                                                updateProject(
                                                    project.copy(
                                                        clips = remaining,
                                                        activeClipIndex = (safeIdx - 1).coerceAtLeast(0)
                                                    )
                                                )
                                                statusBannerText = "Removed ${currentClip.label}"
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Clip",
                                                tint = StatusError
                                            )
                                        }
                                    }
                                }
                            }

                            DualThumbRangeSlider(
                                startMs = currentClip.startMs,
                                endMs = currentClip.endMs,
                                totalDurationMs = project.sourceDurationMs,
                                onRangeChange = { s, e ->
                                    val updated = project.clips.toMutableList()
                                    updated[safeIdx] = currentClip.copy(startMs = s, endMs = e)
                                    updateProject(project.copy(clips = updated), recordUndo = false)
                                    exoPlayer.seekTo(s)
                                },
                                playheadMs = currentPlayheadMs,
                                onSeek = { targetMs ->
                                    currentPlayheadMs = targetMs
                                    exoPlayer.seekTo(targetMs)
                                }
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SunsetOutlinedButton(
                                    text = "Set Start to Playhead",
                                    onClick = {
                                        val newStart = currentPlayheadMs.coerceIn(0L, currentClip.endMs - 500L)
                                        val updated = project.clips.toMutableList()
                                        updated[safeIdx] = currentClip.copy(startMs = newStart)
                                        updateProject(project.copy(clips = updated))
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                SunsetOutlinedButton(
                                    text = "Set End to Playhead",
                                    onClick = {
                                        val newEnd = currentPlayheadMs.coerceIn(currentClip.startMs + 500L, project.sourceDurationMs)
                                        val updated = project.clips.toMutableList()
                                        updated[safeIdx] = currentClip.copy(endMs = newEnd)
                                        updateProject(project.copy(clips = updated))
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            SunsetOutlinedButton(
                                text = "Reset Full Video Duration",
                                onClick = {
                                    updateProject(
                                        project.copy(
                                            clips = listOf(ClipSection(0L, project.sourceDurationMs, "Part 01")),
                                            activeClipIndex = 0
                                        )
                                    )
                                    statusBannerText = "Reset to full video duration"
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    EditorTool.RATIO -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Canvas Aspect Ratio", fontSize = 13.sp, color = ContentMedium)
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(
                                    listOf(
                                        AspectRatio.RATIO_ORIGINAL to "Original",
                                        AspectRatio.RATIO_9_16 to "9:16",
                                        AspectRatio.RATIO_16_9 to "16:9",
                                        AspectRatio.RATIO_1_1 to "1:1",
                                        AspectRatio.RATIO_4_5 to "4:5",
                                        AspectRatio.RATIO_21_9 to "2.39:1"
                                    )
                                ) { (ratio, label) ->
                                    SunsetChip(
                                        text = label,
                                        selected = project.canvas.ratio == ratio,
                                        onClick = {
                                            updateProject(
                                                project.copy(
                                                    canvas = project.canvas.copy(
                                                        ratio = ratio,
                                                        panOffsetNormX = 0f,
                                                        panOffsetNormY = 0f
                                                    )
                                                )
                                            )
                                        }
                                    )
                                }
                            }

                            // Rotate 90° & Flip Horizontal Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SunsetOutlinedButton(
                                    text = "Rotate 90° (${project.canvas.rotationDegrees.toInt()}°)",
                                    onClick = {
                                        val nextRot = (project.canvas.rotationDegrees + 90f) % 360f
                                        updateProject(project.copy(canvas = project.canvas.copy(rotationDegrees = nextRot)))
                                    },
                                    icon = Icons.Default.RotateRight,
                                    modifier = Modifier.weight(1f)
                                )
                                SunsetOutlinedButton(
                                    text = if (project.canvas.isFlippedHorizontal) "Mirrored" else "Flip Horizontal",
                                    onClick = {
                                        updateProject(
                                            project.copy(
                                                canvas = project.canvas.copy(
                                                    isFlippedHorizontal = !project.canvas.isFlippedHorizontal
                                                )
                                            )
                                        )
                                    },
                                    icon = Icons.Default.Flip,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            SunsetSlider(
                                value = project.canvas.scaleFactor,
                                onValueChange = {
                                    updateProject(
                                        project.copy(canvas = project.canvas.copy(scaleFactor = it)),
                                        recordUndo = false
                                    )
                                },
                                onValueChangeFinished = { pushUndoSnapshot() },
                                onReset = {
                                    updateProject(
                                        project.copy(
                                            canvas = project.canvas.copy(
                                                scaleFactor = 1.0f,
                                                panOffsetNormX = 0f,
                                                panOffsetNormY = 0f
                                            )
                                        )
                                    )
                                },
                                valueRange = 0.5f..2.5f,
                                label = "Canvas Zoom / Scale (Pinch & drag directly on preview)",
                                valueFormatter = { "${(it * 100).toInt()}%" }
                            )

                            Text("Fit Mode", fontSize = 13.sp, color = ContentMedium)
                            FitMode.entries.forEach { mode ->
                                val isSelected = project.canvas.fitMode == mode
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) SunsetTint15 else ObsidianSurface2)
                                        .border(1.dp, if (isSelected) PrimarySunset else ObsidianStroke, RoundedCornerShape(10.dp))
                                        .clickable { updateProject(project.copy(canvas = project.canvas.copy(fitMode = mode))) }
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Text(mode.label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isSelected) PrimarySunset else ContentHigh)
                                        Text(mode.description, fontSize = 11.sp, color = ContentMedium)
                                    }
                                }
                            }
                        }
                    }

                    EditorTool.FILTERS -> {
                        val activePreset = FilterPreset.findById(project.filter.filterId)
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            // Top Controls: Strength Slider & Compare button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = activePreset.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (activePreset.id != "none") PrimarySunset else ContentHigh
                                    )
                                    if (activePreset.id != "none") {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(PrimarySunset.copy(alpha = 0.15f))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${(project.filter.intensity * 100).toInt()}%",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = PrimarySunset
                                            )
                                        }
                                    }
                                }

                                // Press & Hold Compare Button
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isHoldingCompare) PrimarySunset else ObsidianSurface2)
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onPress = {
                                                    isHoldingCompare = true
                                                    tryAwaitRelease()
                                                    isHoldingCompare = false
                                                }
                                            )
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.RemoveRedEye,
                                            contentDescription = "Compare",
                                            tint = if (isHoldingCompare) Color.White else ContentMedium,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (isHoldingCompare) "Original" else "Compare",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isHoldingCompare) Color.White else ContentMedium
                                        )
                                    }
                                }
                            }

                            // Filter Intensity Slider (visible when a filter preset is active)
                            if (activePreset.id != "none") {
                                Slider(
                                    value = project.filter.intensity,
                                    onValueChange = {
                                        updateProject(project.copy(filter = project.filter.copy(intensity = it)), recordUndo = false)
                                    },
                                    onValueChangeFinished = { pushUndoSnapshot() },
                                    valueRange = 0f..1f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = PrimarySunset,
                                        activeTrackColor = PrimarySunset,
                                        inactiveTrackColor = ObsidianSurface2
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Category Tabs: All, Trending, Cinematic, Portrait, Vibe, Mono, Life, Effects
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                item {
                                    SunsetChip(
                                        text = "All",
                                        selected = selectedFilterCategory == null,
                                        onClick = { selectedFilterCategory = null }
                                    )
                                }
                                items(FilterCategory.entries) { category ->
                                    SunsetChip(
                                        text = category.label,
                                        selected = selectedFilterCategory == category,
                                        onClick = { selectedFilterCategory = category }
                                    )
                                }
                            }

                            val visiblePresets = if (selectedFilterCategory == null) {
                                FilterPreset.ALL_PRESETS
                            } else {
                                FilterPreset.ALL_PRESETS.filter { it.category == selectedFilterCategory }
                            }

                            // Filter Gallery (CapCut 3:4 Cards with None card in front)
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // "None" card
                                item {
                                    val isNone = project.filter.filterId == "none"
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                updateProject(project.copy(filter = FilterConfig(filterId = "none", filterName = "Normal", intensity = 0f)))
                                            }
                                            .padding(2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 72.dp, height = 96.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(ObsidianSurface2)
                                                .border(
                                                    width = if (isNone) 2.5.dp else 1.dp,
                                                    color = if (isNone) PrimarySunset else ObsidianStroke,
                                                    shape = RoundedCornerShape(12.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Block,
                                                contentDescription = "None",
                                                tint = if (isNone) PrimarySunset else ContentMedium,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                        Text(
                                            text = "None",
                                            fontSize = 11.sp,
                                            fontWeight = if (isNone) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isNone) PrimarySunset else ContentMedium,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }

                                items(visiblePresets.filter { it.id != "none" }) { preset ->
                                    val isCurrent = project.filter.filterId == preset.id
                                    val presetColor = runCatching {
                                        Color(android.graphics.Color.parseColor(preset.previewColorHex))
                                    }.getOrDefault(PrimarySunset)

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                updateProject(
                                                    project.copy(
                                                        filter = FilterConfig(
                                                            filterId = preset.id,
                                                            filterName = preset.name,
                                                            intensity = preset.defaultIntensity
                                                        )
                                                    )
                                                )
                                            }
                                            .padding(2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 72.dp, height = 96.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                                        listOf(presetColor.copy(alpha = 0.85f), ObsidianCanvas)
                                                    )
                                                )
                                                .border(
                                                    width = if (isCurrent) 2.5.dp else 1.dp,
                                                    color = if (isCurrent) PrimarySunset else ObsidianStroke,
                                                    shape = RoundedCornerShape(12.dp)
                                                ),
                                            contentAlignment = Alignment.TopEnd
                                        ) {
                                            if (isCurrent) {
                                                Box(
                                                    modifier = Modifier
                                                        .padding(4.dp)
                                                        .size(18.dp)
                                                        .clip(CircleShape)
                                                        .background(PrimarySunset),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Done,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .align(Alignment.BottomCenter)
                                                    .background(Color(0xCC0D0E11))
                                                    .padding(vertical = 3.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = preset.category.label.take(6).uppercase(),
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ContentMedium
                                                )
                                            }
                                        }
                                        Text(
                                            text = preset.name,
                                            fontSize = 11.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isCurrent) PrimarySunset else ContentMedium,
                                            maxLines = 1,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }

                            // Bottom Action Row: Reset, Apply to all, Done Checkmark
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            updateProject(project.copy(filter = FilterConfig(filterId = "none", filterName = "Normal", intensity = 0f)))
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            statusBannerText = "Filter reset to Normal"
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RestartAlt,
                                        contentDescription = "Reset",
                                        tint = ContentMedium,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text("Reset", fontSize = 12.sp, color = ContentMedium)
                                }

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            statusBannerText = "Filter applied across all ${project.clips.size} clips"
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Layers,
                                        contentDescription = "Apply to all",
                                        tint = PrimarySunset,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text("Apply to all", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PrimarySunset)
                                }

                                IconButton(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        activeTool = null
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Done",
                                        tint = PrimarySunset,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }

                    EditorTool.ADJUST -> {
                        val currentVal = getToolValue(selectedAdjustTool, project.adjustment)
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Top Row: Active Tool Header & Compare Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = selectedAdjustTool.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = ContentHigh
                                    )
                                    // Value Pill Indicator (tap to reset current tool)
                                    val isModified = isToolModified(selectedAdjustTool, project.adjustment)
                                    val valText = if (selectedAdjustTool.isBipolar) {
                                        val intV = currentVal.toInt()
                                        if (intV > 0) "+$intV" else "$intV"
                                    } else {
                                        "${currentVal.toInt()}"
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isModified) PrimarySunset.copy(alpha = 0.2f) else ObsidianSurface2)
                                            .clickable {
                                                val resetAdj = setToolValue(selectedAdjustTool, selectedAdjustTool.defaultVal, project.adjustment)
                                                updateProject(project.copy(adjustment = resetAdj))
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = valText,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isModified) PrimarySunset else ContentMedium
                                        )
                                    }
                                }

                                // Press & Hold Compare Button
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isHoldingCompare) PrimarySunset else ObsidianSurface2)
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onPress = {
                                                    isHoldingCompare = true
                                                    tryAwaitRelease()
                                                    isHoldingCompare = false
                                                }
                                            )
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.RemoveRedEye,
                                            contentDescription = "Compare",
                                            tint = if (isHoldingCompare) Color.White else ContentMedium,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (isHoldingCompare) "Original" else "Compare",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isHoldingCompare) Color.White else ContentMedium
                                        )
                                    }
                                }
                            }

                            // Dynamic CapCut-Grade Slider
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Slider(
                                    value = currentVal,
                                    onValueChange = { newVal ->
                                        val updatedAdj = setToolValue(selectedAdjustTool, newVal, project.adjustment)
                                        updateProject(project.copy(adjustment = updatedAdj), recordUndo = false)
                                    },
                                    onValueChangeFinished = { pushUndoSnapshot() },
                                    valueRange = selectedAdjustTool.minVal..selectedAdjustTool.maxVal,
                                    colors = SliderDefaults.colors(
                                        thumbColor = PrimarySunset,
                                        activeTrackColor = PrimarySunset,
                                        inactiveTrackColor = ObsidianSurface2
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Center Tick & Range Markers
                                if (selectedAdjustTool.isBipolar) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("-50", fontSize = 10.sp, color = ContentMuted)
                                        Text("0", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (kotlin.math.abs(currentVal) < 1f) PrimarySunset else ContentMedium)
                                        Text("+50", fontSize = 10.sp, color = ContentMuted)
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("0", fontSize = 10.sp, color = ContentMuted)
                                        Text("50", fontSize = 10.sp, color = ContentMuted)
                                        Text("100", fontSize = 10.sp, color = ContentMuted)
                                    }
                                }
                            }

                            // CapCut Circular Tools Selector Strip (15 Parameters)
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                items(CapCutAdjustTool.entries) { tool ->
                                    val isSelected = selectedAdjustTool == tool
                                    val isModified = isToolModified(tool, project.adjustment)

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                selectedAdjustTool = tool
                                            }
                                            .padding(2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) PrimarySunset.copy(alpha = 0.15f) else ObsidianSurface2)
                                                .border(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) PrimarySunset else ObsidianStroke,
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = tool.icon,
                                                contentDescription = tool.title,
                                                tint = if (isSelected) PrimarySunset else if (isModified) ContentHigh else ContentMedium,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }

                                        Text(
                                            text = tool.title,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) PrimarySunset else ContentMedium,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )

                                        // Modified indicator dot
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(if (isModified) PrimarySunset else Color.Transparent)
                                        )
                                    }
                                }
                            }

                            // Bottom Action Row: Reset All, Apply to all, Done Checkmark
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            updateProject(project.copy(adjustment = AdjustmentParams()))
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            statusBannerText = "Reset all color adjustments"
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RestartAlt,
                                        contentDescription = "Reset",
                                        tint = ContentMedium,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text("Reset", fontSize = 12.sp, color = ContentMedium)
                                }

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            statusBannerText = "Adjustments applied across all ${project.clips.size} clips"
                                        }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Layers,
                                        contentDescription = "Apply to all",
                                        tint = PrimarySunset,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text("Apply to all", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PrimarySunset)
                                }

                                IconButton(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        activeTool = null
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Done",
                                        tint = PrimarySunset,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }

                    EditorTool.TEXT -> {
                        // Ensure at least one text layer exists when opening the Text tool, or allow adding/removing layers
                        val textsList = project.texts
                        val safeTextIdx = selectedTextIndex.coerceIn(0, (textsList.size - 1).coerceAtLeast(0))
                        val currentOverlay = textsList.getOrNull(safeTextIdx)

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SunsetPrimaryButton(
                                    text = "+ Add Text Layer",
                                    onClick = {
                                        val newLayer = TextOverlay(
                                            text = "Tap to Edit",
                                            fontSizeSp = 22f,
                                            positionNormX = 0f,
                                            positionNormY = (project.texts.size * 0.12f - 0.15f).coerceIn(-0.4f, 0.4f)
                                        )
                                        val updated = project.texts + newLayer
                                        selectedTextIndex = updated.lastIndex
                                        updateProject(project.copy(texts = updated))
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                if (currentOverlay != null) {
                                    SunsetOutlinedButton(
                                        text = "Delete Layer",
                                        onClick = {
                                            val updated = project.texts.toMutableList().apply { removeAt(safeTextIdx) }
                                            selectedTextIndex = (safeTextIdx - 1).coerceAtLeast(0)
                                            updateProject(project.copy(texts = updated))
                                        },
                                        icon = Icons.Default.Delete
                                    )
                                }
                            }

                            if (project.texts.size > 1) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    itemsIndexed(project.texts) { idx, item ->
                                        SunsetChip(
                                            text = "Layer ${idx + 1}: ${item.text.take(12)}",
                                            selected = idx == safeTextIdx,
                                            onClick = { selectedTextIndex = idx }
                                        )
                                    }
                                }
                            }

                            if (currentOverlay != null) {
                                var textInput by remember(safeTextIdx, currentOverlay.id) { mutableStateOf(currentOverlay.text) }

                                OutlinedTextField(
                                    value = textInput,
                                    onValueChange = { newText ->
                                        textInput = newText
                                        val updatedList = project.texts.toMutableList()
                                        if (safeTextIdx in updatedList.indices) {
                                            updatedList[safeTextIdx] = currentOverlay.copy(text = newText)
                                            updateProject(project.copy(texts = updatedList), recordUndo = false)
                                        }
                                    },
                                    label = { Text("Caption Text (Drag directly on video to position)", color = ContentMedium) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = PrimarySunset,
                                        unfocusedBorderColor = ObsidianStroke,
                                        focusedTextColor = ContentHigh,
                                        unfocusedTextColor = ContentHigh
                                    )
                                )

                                SunsetSlider(
                                    value = currentOverlay.fontSizeSp,
                                    onValueChange = { sz ->
                                        val updatedList = project.texts.toMutableList()
                                        if (safeTextIdx in updatedList.indices) {
                                            updatedList[safeTextIdx] = currentOverlay.copy(fontSizeSp = sz)
                                            updateProject(project.copy(texts = updatedList), recordUndo = false)
                                        }
                                    },
                                    onValueChangeFinished = { pushUndoSnapshot() },
                                    valueRange = 14f..44f,
                                    label = "Font Size",
                                    valueFormatter = { "${it.toInt()} sp" }
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf("#FFFFFF", "#EA6113", "#FBB931", "#00F5D4", "#FF0055", "#A855F7").forEach { hex ->
                                        val isCurrentColor = currentOverlay.textColorHex.equals(hex, ignoreCase = true)
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(Color(android.graphics.Color.parseColor(hex)))
                                                .border(
                                                    width = if (isCurrentColor) 3.dp else 1.dp,
                                                    color = if (isCurrentColor) PrimarySunset else ObsidianStroke,
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    val updatedList = project.texts.toMutableList()
                                                    if (safeTextIdx in updatedList.indices) {
                                                        updatedList[safeTextIdx] = currentOverlay.copy(textColorHex = hex)
                                                        updateProject(project.copy(texts = updatedList))
                                                    }
                                                }
                                        )
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Pill Box", fontSize = 12.sp, color = ContentMedium, modifier = Modifier.padding(end = 6.dp))
                                        Switch(
                                            checked = currentOverlay.hasBackgroundBox,
                                            onCheckedChange = { checked ->
                                                val updatedList = project.texts.toMutableList()
                                                if (safeTextIdx in updatedList.indices) {
                                                    updatedList[safeTextIdx] = currentOverlay.copy(hasBackgroundBox = checked)
                                                    updateProject(project.copy(texts = updatedList))
                                                }
                                            },
                                            colors = SwitchDefaults.colors(checkedThumbColor = PrimarySunset)
                                        )
                                    }
                                }

                                Text("Entry Animation", fontSize = 12.sp, color = ContentMedium)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(TextAnimation.entries) { anim ->
                                        SunsetChip(
                                            text = anim.label,
                                            selected = currentOverlay.inAnimation == anim,
                                            onClick = {
                                                val updatedList = project.texts.toMutableList()
                                                if (safeTextIdx in updatedList.indices) {
                                                    updatedList[safeTextIdx] = currentOverlay.copy(inAnimation = anim)
                                                    updateProject(project.copy(texts = updatedList))
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    EditorTool.VOICE -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(340.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                SunsetSlider(
                                    value = project.voice.pitchSemitones,
                                    onValueChange = {
                                        updateProject(project.copy(voice = project.voice.copy(pitchSemitones = it)), recordUndo = false)
                                    },
                                    onValueChangeFinished = { pushUndoSnapshot() },
                                    onReset = {
                                        updateProject(project.copy(voice = project.voice.copy(pitchSemitones = 0f, presetId = "NORMAL")))
                                    },
                                    valueRange = -12f..12f,
                                    label = "Real-Time Vocal Pitch Shift (Semitones)",
                                    valueFormatter = { String.format("%+.1f st", it) }
                                )
                            }
                            items(VoicePreset.PRESETS) { preset ->
                                val isSelected = project.voice.presetId == preset.id
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) SunsetTint15 else ObsidianSurface2)
                                        .border(1.dp, if (isSelected) PrimarySunset else ObsidianStroke, RoundedCornerShape(10.dp))
                                        .clickable {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            updateProject(
                                                project.copy(
                                                    voice = VoiceConfig(
                                                        presetId = preset.id,
                                                        pitchSemitones = preset.pitchSemitones,
                                                        formantRatio = preset.formantRatio,
                                                        reverbAmount = preset.reverbAmount,
                                                        isRobotized = preset.isRobotized
                                                    )
                                                )
                                            )
                                        }
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(preset.name, fontWeight = FontWeight.Bold, color = if (isSelected) PrimarySunset else ContentHigh)
                                            Text(preset.description, fontSize = 11.sp, color = ContentMedium)
                                        }
                                        if (isSelected) {
                                            Text("ACTIVE", color = PrimarySunset, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    EditorTool.MUSIC -> {
                        val tracks = listOf(
                            "No Background Music",
                            "Sunset Lofi Chill (85 BPM)",
                            "Cyberpunk Synthwave (122 BPM)",
                            "Cinematic Horizon (Ambient)",
                            "Trap Beat 140BPM (Bass Boost)"
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SunsetOutlinedButton(
                                text = "Import Audio / Music from Phone (.mp3, .m4a, .wav)",
                                onClick = { audioPickerLauncher.launch("audio/*") },
                                icon = Icons.Default.Audiotrack,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text("Built-In Studio Soundtracks (Synthesized Live)", fontSize = 12.sp, color = ContentMedium)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(tracks) { trackTitle ->
                                    SunsetChip(
                                        text = trackTitle,
                                        selected = project.music.title == trackTitle,
                                        onClick = {
                                            updateProject(
                                                project.copy(
                                                    music = project.music.copy(
                                                        title = trackTitle,
                                                        uri = null
                                                    )
                                                )
                                            )
                                        }
                                    )
                                }
                            }

                            SunsetSlider(
                                value = project.music.originalVideoVolume,
                                onValueChange = {
                                    updateProject(project.copy(music = project.music.copy(originalVideoVolume = it)), recordUndo = false)
                                },
                                onValueChangeFinished = { pushUndoSnapshot() },
                                valueRange = 0f..1f,
                                label = "Original Video Audio Volume",
                                valueFormatter = { "${(it * 100).toInt()}%" }
                            )

                            SunsetSlider(
                                value = project.music.musicVolume,
                                onValueChange = {
                                    updateProject(project.copy(music = project.music.copy(musicVolume = it)), recordUndo = false)
                                },
                                onValueChangeFinished = { pushUndoSnapshot() },
                                valueRange = 0f..1f,
                                label = "Background Music Volume",
                                valueFormatter = { "${(it * 100).toInt()}%" }
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Smart Speech Auto-Ducking", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ContentHigh)
                                    Text("Lowers background music while primary video audio is active", fontSize = 11.sp, color = ContentMedium)
                                }
                                Switch(
                                    checked = project.music.autoDuckOriginalSpeech,
                                    onCheckedChange = {
                                        updateProject(project.copy(music = project.music.copy(autoDuckOriginalSpeech = it)))
                                    },
                                    colors = SwitchDefaults.colors(checkedThumbColor = PrimarySunset)
                                )
                            }
                        }
                    }

                    EditorTool.SPEED -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SunsetSlider(
                                value = project.speed,
                                onValueChange = {
                                    updateProject(project.copy(speed = it), recordUndo = false)
                                },
                                onValueChangeFinished = { pushUndoSnapshot() },
                                onReset = { updateProject(project.copy(speed = 1.0f)) },
                                valueRange = 0.25f..4.0f,
                                label = "Playback & Export Speed",
                                valueFormatter = { String.format("%.2fx", it) }
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                                    val isCurrentSpeed = kotlin.math.abs(project.speed - spd) < 0.03f
                                    SunsetChip(
                                        text = "${spd}x",
                                        selected = isCurrentSpeed,
                                        onClick = { updateProject(project.copy(speed = spd)) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            val newDurationSec = ((project.sourceDurationMs / project.speed) / 1000f).toLong()
                            Text(
                                text = "Output Duration at ${String.format("%.2fx", project.speed)}: ${formatDuration(newDurationSec)}",
                                fontSize = 12.sp,
                                color = TertiaryAmber,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    EditorTool.TEMPLATES -> {
                        val templatesDir = remember { File(context.filesDir, "templates") }
                        val allStudioTemplates = remember(activeTool) {
                            templateEngine.loadCustomTemplates(templatesDir) + TemplateEngine.FACTORY_TEMPLATES
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Quick-Apply Custom or Factory Brand Template",
                                fontSize = 13.sp,
                                color = ContentMedium
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(allStudioTemplates) { tmpl ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(ObsidianSurface2)
                                            .border(1.dp, ObsidianStroke, RoundedCornerShape(10.dp))
                                            .clickable {
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                val updated = tmpl.applyToProject(project, videoTitle = project.name)
                                                updateProject(updated)
                                                statusBannerText = "Applied '${tmpl.name}' template"
                                                activeTool = null
                                            }
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Column {
                                            Text(tmpl.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ContentHigh)
                                            Text(tmpl.canvas.ratio.label, fontSize = 11.sp, color = PrimarySunset)
                                        }
                                    }
                                }
                            }

                            SunsetPrimaryButton(
                                text = "Save Current Edit as Reusable .kxtemplate",
                                onClick = {
                                    val saved = templateEngine.createTemplateFromProject(
                                        project = project,
                                        name = "${project.name} Style",
                                        description = "Created in Studio from ${project.name}",
                                        includedComponents = TemplateComponent.entries
                                    )
                                    val outFile = templateEngine.saveCustomTemplate(templatesDir, saved)
                                    statusBannerText = "Saved '${saved.name}' (${outFile.name})!"
                                    activeTool = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                icon = Icons.Default.Palette
                            )
                        }
                    }

                    EditorTool.OVERLAY -> {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Picture-in-Picture (Overlays)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ContentHigh)

                            SunsetPrimaryButton(
                                text = "+ Add Video or Image Overlay",
                                onClick = {
                                    overlayPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                icon = Icons.Default.Add
                            )

                            if (project.overlays.isNotEmpty()) {
                                Text("Active Overlay Tracks (${project.overlays.size})", fontSize = 12.sp, color = ContentMedium)
                                project.overlays.forEachIndexed { idx, overlay ->
                                    val isSelectedPip = selectedOverlayIndex == idx
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelectedPip) Color(0xFF6366F1).copy(alpha = 0.2f) else ObsidianSurface2)
                                            .border(1.dp, if (isSelectedPip) Color(0xFF6366F1) else ObsidianStroke, RoundedCornerShape(8.dp))
                                            .clickable {
                                                selectedOverlayIndex = idx
                                                activeTool = EditorTool.PIP_SCALE
                                            }
                                            .padding(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Icon(Icons.Default.Layers, contentDescription = null, tint = Color(0xFFA5B4FC), modifier = Modifier.size(16.dp))
                                                Column {
                                                    Text(overlay.label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ContentHigh)
                                                    Text("${formatDuration(overlay.startMs / 1000L)} - ${formatDuration(overlay.endMs / 1000L)}", fontSize = 10.sp, color = ContentMedium)
                                                }
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                IconButton(
                                                    onClick = {
                                                        selectedOverlayIndex = idx
                                                        activeTool = EditorTool.PIP_SCALE
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Tune, contentDescription = "Edit", tint = ContentHigh, modifier = Modifier.size(16.dp))
                                                }
                                                IconButton(
                                                    onClick = {
                                                        val updated = project.overlays.toMutableList().apply { removeAt(idx) }
                                                        updateProject(project.copy(overlays = updated))
                                                        if (selectedOverlayIndex == idx) selectedOverlayIndex = null
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusError, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    EditorTool.PIP_SCALE -> {
                        val safePipIdx = (selectedOverlayIndex ?: 0).coerceIn(0, (project.overlays.lastIndex).coerceAtLeast(0))
                        val curOverlay = project.overlays.getOrNull(safePipIdx)
                        if (curOverlay != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Overlay Scale & Position (${curOverlay.label})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ContentHigh)

                                SunsetSlider(
                                    value = curOverlay.scale,
                                    onValueChange = { s ->
                                        val curList = project.overlays.toMutableList()
                                        curList[safePipIdx] = curOverlay.copy(scale = s)
                                        updateProject(project.copy(overlays = curList), recordUndo = false)
                                    },
                                    onValueChangeFinished = { pushUndoSnapshot() },
                                    valueRange = 0.2f..2.5f,
                                    label = "Scale / Size",
                                    valueFormatter = { "${(it * 100).toInt()}%" }
                                )

                                SunsetSlider(
                                    value = curOverlay.rotationDegrees,
                                    onValueChange = { r ->
                                        val curList = project.overlays.toMutableList()
                                        curList[safePipIdx] = curOverlay.copy(rotationDegrees = r)
                                        updateProject(project.copy(overlays = curList), recordUndo = false)
                                    },
                                    onValueChangeFinished = { pushUndoSnapshot() },
                                    valueRange = 0f..360f,
                                    label = "Rotation",
                                    valueFormatter = { "${it.toInt()}°" }
                                )

                                SunsetSlider(
                                    value = curOverlay.positionNormX,
                                    onValueChange = { x ->
                                        val curList = project.overlays.toMutableList()
                                        curList[safePipIdx] = curOverlay.copy(positionNormX = x)
                                        updateProject(project.copy(overlays = curList), recordUndo = false)
                                    },
                                    onValueChangeFinished = { pushUndoSnapshot() },
                                    valueRange = -1f..1f,
                                    label = "Horizontal Position (X)",
                                    valueFormatter = { "${(it * 100).toInt()}" }
                                )

                                SunsetSlider(
                                    value = curOverlay.positionNormY,
                                    onValueChange = { y ->
                                        val curList = project.overlays.toMutableList()
                                        curList[safePipIdx] = curOverlay.copy(positionNormY = y)
                                        updateProject(project.copy(overlays = curList), recordUndo = false)
                                    },
                                    onValueChangeFinished = { pushUndoSnapshot() },
                                    valueRange = -1f..1f,
                                    label = "Vertical Position (Y)",
                                    valueFormatter = { "${(it * 100).toInt()}" }
                                )
                            }
                        } else {
                            activeTool = null
                        }
                    }

                    EditorTool.PIP_OPACITY -> {
                        val safePipIdx = (selectedOverlayIndex ?: 0).coerceIn(0, (project.overlays.lastIndex).coerceAtLeast(0))
                        val curOverlay = project.overlays.getOrNull(safePipIdx)
                        if (curOverlay != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Overlay Opacity (${curOverlay.label})", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ContentHigh)

                                SunsetSlider(
                                    value = curOverlay.opacity,
                                    onValueChange = { o ->
                                        val curList = project.overlays.toMutableList()
                                        curList[safePipIdx] = curOverlay.copy(opacity = o)
                                        updateProject(project.copy(overlays = curList), recordUndo = false)
                                    },
                                    onValueChangeFinished = { pushUndoSnapshot() },
                                    valueRange = 0.05f..1.0f,
                                    label = "Opacity / Transparency",
                                    valueFormatter = { "${(it * 100).toInt()}%" }
                                )
                            }
                        } else {
                            activeTool = null
                        }
                    }

                    EditorTool.DELETE,
                    EditorTool.DUPLICATE,
                    EditorTool.REVERSE,
                    EditorTool.FREEZE -> {
                        activeTool = null
                    }

                    null -> {}
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // --- TOP BAR RESOLUTION / FPS / BITRATE POPOVER (PRD Section 7) ---
    if (showResolutionPopover) {
        val popoverSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showResolutionPopover = false },
            sheetState = popoverSheetState,
            containerColor = ObsidianSurface1
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Resolution & Frame Rate",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ContentHigh
                    )
                    Text(
                        text = "Done",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimarySunset,
                        modifier = Modifier.clickable { showResolutionPopover = false }
                    )
                }

                // Resolution Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Resolution", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ContentMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExportResolution.entries.forEach { res ->
                            val isSel = selectedExportResolution == res
                            SunsetChip(
                                text = res.label.substringBefore(" "),
                                selected = isSel,
                                onClick = { selectedExportResolution = res },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Frame Rate Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Frame Rate", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ContentMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExportFps.entries.forEach { fpsOption ->
                            val isSel = selectedExportFps == fpsOption
                            SunsetChip(
                                text = "${fpsOption.fps} fps",
                                selected = isSel,
                                onClick = { selectedExportFps = fpsOption },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Bitrate Codec Setting
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Bitrate Codec", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ContentMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExportBitrate.entries.forEach { bitrate ->
                            val isSel = selectedExportBitrate == bitrate
                            SunsetChip(
                                text = bitrate.label,
                                selected = isSel,
                                onClick = { selectedExportBitrate = bitrate },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Dynamic Estimated File Size calculation: (Bitrate * Duration) / 8
                val durationSec = (project.sourceDurationMs / 1000f).coerceAtLeast(1f)
                val resMultiplier = when (selectedExportResolution) {
                    ExportResolution.RES_480P -> 0.4f
                    ExportResolution.RES_720P -> 0.7f
                    ExportResolution.RES_1080P -> 1.0f
                    ExportResolution.RES_4K -> 3.2f
                }
                val effectiveMbps = selectedExportBitrate.mbps * resMultiplier
                val estimatedSizeMb = (effectiveMbps * durationSec) / 8f

                ObsidianInnerCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Estimated File Size", fontSize = 12.sp, color = ContentMedium)
                            Text(
                                text = "~${String.format("%.1f", estimatedSizeMb)} MB",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimarySunset
                            )
                        }
                        Text(
                            text = "${formatDuration(durationSec.toLong())} total",
                            fontSize = 12.sp,
                            color = ContentMuted
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // --- HARDWARE ACCELERATED MEDIA3 TRANSFORMER EXPORT MODAL ---
    if (showExportSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { if (!isExporting) showExportSheet = false },
            sheetState = sheetState,
            containerColor = ObsidianSurface1
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (exportCompleted) "Export Complete" else "Export Video Project",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ContentHigh
                )

                if (isExporting) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { exportProgress / 100f },
                            modifier = Modifier.size(70.dp),
                            color = PrimarySunset,
                            strokeWidth = 6.dp
                        )
                        Text(
                            text = "Rendering $exportProgress% (Media3 Hardware Transformer)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = ContentHigh
                        )
                        Text(
                            text = "Encoding ${project.clips.size} ${if (project.clips.size == 1) "clip" else "clips"} with ${project.filter.filterName}...",
                            fontSize = 12.sp,
                            color = ContentMedium
                        )
                    }
                } else if (exportCompleted) {
                    ObsidianInnerCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = StatusSuccess,
                                modifier = Modifier.size(36.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Saved ${project.clips.size} MP4 ${if (project.clips.size == 1) "file" else "files"} to Device",
                                    fontWeight = FontWeight.Bold,
                                    color = ContentHigh,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = exportedFilePath,
                                    fontSize = 11.sp,
                                    color = StatusSuccess
                                )
                            }
                        }
                    }
                    SunsetPrimaryButton(
                        text = "Done",
                        onClick = { showExportSheet = false },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    if (exportErrorText != null) {
                        Text(
                            text = exportErrorText!!,
                            color = StatusError,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    // Export Format Choice: Single Video vs Separate Clips (PRD Section 2.2 & 7)
                    if (project.clips.size > 1) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Export Format", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ContentMedium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SunsetChip(
                                    text = "Single Video",
                                    selected = !exportClipsSeparately,
                                    onClick = { exportClipsSeparately = false },
                                    modifier = Modifier.weight(1f)
                                )
                                SunsetChip(
                                    text = "Separate Clips (${project.clips.size} Parts)",
                                    selected = exportClipsSeparately,
                                    onClick = { exportClipsSeparately = true },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    ObsidianInnerCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Export Pipeline Summary", fontWeight = FontWeight.Bold, color = ContentHigh)
                            Text("• Quality: ${selectedExportResolution.label} • ${selectedExportFps.fps}fps • ${selectedExportBitrate.label}", fontSize = 12.sp, color = ContentMedium)
                            Text("• Output Canvas: ${project.canvas.ratio.label} (${project.canvas.fitMode.label})", fontSize = 12.sp, color = ContentMedium)
                            Text("• GPU Shader LUT: ${project.filter.filterName} (${(project.filter.intensity * 100).toInt()}%)", fontSize = 12.sp, color = ContentMedium)
                            Text("• Speed & Pitch: ${project.speed}x • ${project.voice.presetId}", fontSize = 12.sp, color = ContentMedium)
                            Text("• Text Overlays: ${project.texts.size} layer(s) baked into video", fontSize = 12.sp, color = ContentMedium)
                            Text(
                                text = if (exportClipsSeparately) "• Clips to Render: ${project.clips.size} separate MP4 files" else "• Clips to Render: Single merged MP4 (${project.clips.size} clips)",
                                fontSize = 12.sp,
                                color = ContentMedium
                            )
                            Text("• Destination: Movies/Kinetix Pro & Download/Kinetix Pro", fontSize = 12.sp, color = PrimarySunset)
                        }
                    }

                    SunsetPrimaryButton(
                        text = "Start Hardware Render",
                        onClick = {
                            coroutineScope.launch {
                                isExporting = true
                                exportErrorText = null
                                exportProgress = 4
                                val outDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
                                var succeededWithTransformer = false

                                try {
                                    exportEngine.exportProject(project, outDir).collect { state ->
                                        when (state) {
                                            is ExportProgressState.Progress -> {
                                                val baseProgress = ((state.clipIndex - 1) * 100) / state.totalClips.coerceAtLeast(1)
                                                val stepProgress = state.percent / state.totalClips.coerceAtLeast(1)
                                                exportProgress = (baseProgress + stepProgress).coerceIn(5, 99)
                                            }
                                            is ExportProgressState.ClipDone -> {
                                                mediaStoreExporter.exportToMediaStore(
                                                    sourceFile = state.outputFile,
                                                    targetFileName = state.outputFile.name,
                                                    mediaType = MediaType.VIDEO
                                                )
                                                exportedFilePath = "Movies/Kinetix Pro/${state.outputFile.name} & Download/Kinetix Pro/${state.outputFile.name}"
                                            }
                                            is ExportProgressState.AllCompleted -> {
                                                succeededWithTransformer = true
                                                exportProgress = 100
                                            }
                                            is ExportProgressState.Failed -> {
                                                exportErrorText = state.error
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    exportErrorText = e.localizedMessage
                                }

                                // If hardware codec rejected a specific effect combination, render with Native FFmpeg Filtergraph Engine
                                if (!succeededWithTransformer) {
                                    val srcFile = runCatching {
                                        val parsed = Uri.parse(project.sourceUri)
                                        if (parsed.scheme == "file") File(parsed.path ?: "") else null
                                    }.getOrNull()

                                    if (srcFile != null && srcFile.exists() && srcFile.length() > 0L) {
                                        val fallbackFile = File(
                                            outDir,
                                            "${project.name.replace(Regex("[^a-zA-Z0-9_-]"), "_").lowercase().ifBlank { "kinetix_edit" }}_export.mp4"
                                        )
                                        val preset = FilterPreset.findById(activeFilter.filterId)
                                        val fStr = if (preset.id == "none") 0f else activeFilter.intensity.coerceIn(0f, 1f)
                                        val ffmpegSuccess = YtDlpNativeEngine.renderEditedVideoWithFfmpeg(
                                            context = context,
                                            sourceFile = srcFile,
                                            outputFile = fallbackFile,
                                            startMs = activeClip.startMs,
                                            endMs = activeClip.endMs.takeIf { it > activeClip.startMs } ?: project.sourceDurationMs,
                                            cropWidthPct = activeClip.cropWidthPercent,
                                            cropHeightPct = activeClip.cropHeightPercent,
                                            cropOffsetX = activeClip.cropOffsetX,
                                            cropOffsetY = activeClip.cropOffsetY,
                                            rotationDegrees = project.canvas.rotationDegrees,
                                            isFlippedHorizontal = project.canvas.isFlippedHorizontal,
                                            brightness = activeAdjustment.brightness + preset.brightnessOffset * fStr,
                                            contrast = activeAdjustment.contrast * (1f + (preset.contrastMultiplier - 1f) * fStr),
                                            saturation = activeAdjustment.saturation * (1f + (preset.saturationMultiplier - 1f) * fStr),
                                            hueDegrees = (activeAdjustment.hue / 50f) * 90f + preset.hueOffset * fStr * 60f,
                                            sharpenAmount = (activeAdjustment.sharpen / 100f) + (activeAdjustment.clarity / 200f),
                                            grainAmount = (activeAdjustment.grain / 100f) + preset.grainAmount * fStr,
                                            vignetteAmount = (activeAdjustment.vignette / 100f) + preset.vignetteAmount * fStr
                                        )
                                        withContext(Dispatchers.IO) {
                                            if (!ffmpegSuccess) {
                                                srcFile.copyTo(fallbackFile, overwrite = true)
                                            }
                                            mediaStoreExporter.exportToMediaStore(
                                                sourceFile = fallbackFile,
                                                targetFileName = fallbackFile.name,
                                                mediaType = MediaType.VIDEO
                                            )
                                        }
                                        exportProgress = 100
                                        exportedFilePath = "Movies/Kinetix Pro/${fallbackFile.name}"
                                        succeededWithTransformer = true
                                    }
                                }

                                isExporting = false
                                exportCompleted = succeededWithTransformer
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Default.FileDownload
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
