package com.kinetix.pro.editor.effects

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.opengl.GLES20
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BaseGlShaderProgram
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.Brightness
import androidx.media3.effect.Contrast
import androidx.media3.effect.Crop
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram
import androidx.media3.effect.HslAdjustment
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.RgbFilter
import androidx.media3.effect.RgbMatrix
import androidx.media3.effect.ScaleAndRotateTransformation
import com.google.common.collect.ImmutableList
import com.kinetix.pro.core.model.AspectRatio
import com.kinetix.pro.core.model.ClipSection
import com.kinetix.pro.core.model.EditProject
import com.kinetix.pro.core.model.FilterPreset
import com.kinetix.pro.core.model.FitMode
import com.kinetix.pro.core.model.TextOverlay

/**
 * Custom GPU [RgbMatrix] that implements CapCut-grade Color Balance,
 * Temperature (warm/cool), Tint (green/magenta), Whites, Blacks, and Matte Film Fade.
 */
@OptIn(UnstableApi::class)
private class ColorGradingMatrix(
    private val redScale: Float,
    private val greenScale: Float,
    private val blueScale: Float,
    private val redOffset: Float,
    private val greenOffset: Float,
    private val blueOffset: Float
) : RgbMatrix {
    override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray {
        // Column-major 4x4 matrix for OpenGL shader
        return floatArrayOf(
            redScale, 0f, 0f, 0f,
            0f, greenScale, 0f, 0f,
            0f, 0f, blueScale, 0f,
            redOffset, greenOffset, blueOffset, 1f
        )
    }
}

/**
 * OpenGL ES 2.0 fragment shader effect (inspired by GPUMp4Composer-android & CyberAgent GPUImage)
 * that executes 5-tap Unsharp Mask convolution sharpening, midtone clarity, procedural 35mm film grain,
 * highlight/shadow tone curve recovery, radial lens vignette, RGB chromatic split, and Sepia toning.
 */
@OptIn(UnstableApi::class)
private class CapCutDetailAndGrainGlEffect(
    private val sharpenAmount: Float,
    private val clarityAmount: Float,
    private val grainAmount: Float,
    private val vignetteAmount: Float,
    private val highlights: Float,
    private val shadows: Float,
    private val isSepia: Boolean,
    private val isRgbSplit: Boolean
) : GlEffect {
    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram {
        return CapCutDetailAndGrainShaderProgram(
            useHdr = useHdr,
            sharpenAmount = sharpenAmount,
            clarityAmount = clarityAmount,
            grainAmount = grainAmount,
            vignetteAmount = vignetteAmount,
            highlights = highlights,
            shadows = shadows,
            isSepia = isSepia,
            isRgbSplit = isRgbSplit
        )
    }
}

@OptIn(UnstableApi::class)
private class CapCutDetailAndGrainShaderProgram(
    useHdr: Boolean,
    private val sharpenAmount: Float,
    private val clarityAmount: Float,
    private val grainAmount: Float,
    private val vignetteAmount: Float,
    private val highlights: Float,
    private val shadows: Float,
    private val isSepia: Boolean,
    private val isRgbSplit: Boolean
) : BaseGlShaderProgram(useHdr, 1) {

    private val glProgram: GlProgram
    private var texelWidth = 1f / 1080f
    private var texelHeight = 1f / 1920f

    init {
        try {
            glProgram = GlProgram(VERTEX_SHADER, FRAGMENT_SHADER)
            glProgram.setBufferAttribute(
                "aFramePosition",
                GlUtil.getNormalizedCoordinateBounds(),
                GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE
            )
        } catch (e: GlUtil.GlException) {
            throw VideoFrameProcessingException(e)
        }
    }

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        texelWidth = 1f / inputWidth.coerceAtLeast(1).toFloat()
        texelHeight = 1f / inputHeight.coerceAtLeast(1).toFloat()
        return Size(inputWidth, inputHeight)
    }

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        try {
            glProgram.use()
            glProgram.setSamplerTexIdUniform("uTexSampler", inputTexId, 0)
            glProgram.setFloatsUniform("uTexelSize", floatArrayOf(texelWidth, texelHeight))
            glProgram.setFloatUniform("uSharpen", sharpenAmount)
            glProgram.setFloatUniform("uClarity", clarityAmount)
            glProgram.setFloatUniform("uGrain", grainAmount)
            glProgram.setFloatUniform("uVignette", vignetteAmount)
            glProgram.setFloatUniform("uHighlights", highlights)
            glProgram.setFloatUniform("uShadows", shadows)
            glProgram.setFloatUniform("uSepia", if (isSepia) 1.0f else 0.0f)
            glProgram.setFloatUniform("uRgbSplit", if (isRgbSplit) 1.0f else 0.0f)
            val timeSec = ((presentationTimeUs / 41666L) % 1000L).toFloat() * 0.137f
            glProgram.setFloatUniform("uTimeSeed", timeSec)
            glProgram.bindAttributesAndUniforms()
            GLES20.glClearColor(0f, 0f, 0f, 0f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            GlUtil.checkGlError()
        } catch (e: GlUtil.GlException) {
            throw VideoFrameProcessingException(e, presentationTimeUs)
        }
    }

    override fun release() {
        super.release()
        try {
            glProgram.delete()
        } catch (e: GlUtil.GlException) {
            throw VideoFrameProcessingException(e)
        }
    }

    companion object {
        private const val VERTEX_SHADER = """
            attribute vec4 aFramePosition;
            varying vec2 vTexSamplingCoord;
            void main() {
                gl_Position = aFramePosition;
                vTexSamplingCoord = vec2(aFramePosition.x * 0.5 + 0.5, aFramePosition.y * 0.5 + 0.5);
            }
        """

        private const val FRAGMENT_SHADER = """
            precision highp float;
            varying vec2 vTexSamplingCoord;
            uniform sampler2D uTexSampler;
            uniform vec2 uTexelSize;
            uniform float uSharpen;
            uniform float uClarity;
            uniform float uGrain;
            uniform float uVignette;
            uniform float uHighlights;
            uniform float uShadows;
            uniform float uSepia;
            uniform float uRgbSplit;
            uniform float uTimeSeed;

            void main() {
                vec2 uv = vTexSamplingCoord;
                vec4 baseColor = texture2D(uTexSampler, uv);
                vec3 rgb = baseColor.rgb;

                // 1. Chromatic Aberration / RGB Split
                if (uRgbSplit > 0.5) {
                    vec2 shift = vec2(uTexelSize.x * 8.0, uTexelSize.y * 3.0);
                    float rChan = texture2D(uTexSampler, clamp(uv + shift, 0.0, 1.0)).r;
                    float bChan = texture2D(uTexSampler, clamp(uv - shift, 0.0, 1.0)).b;
                    rgb = vec3(rChan, rgb.g, bChan);
                }

                // 2. 5-Tap Unsharp Mask Convolution Sharpening & Local Midtone Clarity
                float totalSharp = uSharpen * 1.8 + uClarity * 0.65;
                if (totalSharp > 0.01) {
                    vec3 n = texture2D(uTexSampler, clamp(uv + vec2(0.0, -uTexelSize.y * 1.4), 0.0, 1.0)).rgb;
                    vec3 s = texture2D(uTexSampler, clamp(uv + vec2(0.0, uTexelSize.y * 1.4), 0.0, 1.0)).rgb;
                    vec3 e = texture2D(uTexSampler, clamp(uv + vec2(uTexelSize.x * 1.4, 0.0), 0.0, 1.0)).rgb;
                    vec3 w = texture2D(uTexSampler, clamp(uv + vec2(-uTexelSize.x * 1.4, 0.0), 0.0, 1.0)).rgb;
                    vec3 blur = (n + s + e + w) * 0.25;
                    vec3 detail = rgb - blur;
                    rgb = clamp(rgb + detail * totalSharp, 0.0, 1.0);
                }

                // 3. Highlights & Shadows Tone Curve Recovery
                float luma = dot(rgb, vec3(0.299, 0.587, 0.114));
                if (abs(uShadows) > 0.01) {
                    float shadowMask = clamp(1.0 - smoothstep(0.0, 0.55, luma), 0.0, 1.0);
                    rgb = clamp(rgb + vec3(uShadows * 0.28 * shadowMask), 0.0, 1.0);
                }
                if (abs(uHighlights) > 0.01) {
                    float hiMask = clamp(smoothstep(0.45, 1.0, luma), 0.0, 1.0);
                    rgb = clamp(rgb + vec3(uHighlights * 0.28 * hiMask), 0.0, 1.0);
                }

                // 4. Sepia Antique Toning
                if (uSepia > 0.5) {
                    vec3 sepiaColor = vec3(
                        dot(rgb, vec3(0.393, 0.769, 0.189)),
                        dot(rgb, vec3(0.349, 0.686, 0.168)),
                        dot(rgb, vec3(0.272, 0.534, 0.131))
                    );
                    rgb = mix(rgb, clamp(sepiaColor, 0.0, 1.0), 0.85);
                }

                // 5. 35mm Celluloid Film Grain (Midtone-Weighted High-Frequency Noise)
                if (uGrain > 0.01) {
                    float noise = fract(sin(dot(uv * vec2(640.0, 360.0) + vec2(uTimeSeed, uTimeSeed * 1.7), vec2(12.9898, 78.233))) * 43758.5453);
                    float centeredNoise = (noise - 0.5) * 2.0;
                    float midWeight = 1.0 - abs(luma - 0.5) * 1.2;
                    rgb = clamp(rgb + vec3(centeredNoise * uGrain * 0.22 * max(midWeight, 0.35)), 0.0, 1.0);
                }

                // 6. Radial Smoothstep Lens Vignette
                if (uVignette > 0.01) {
                    vec2 distVec = (uv - vec2(0.5)) * vec2(1.15, 1.15);
                    float dist = length(distVec);
                    float vigFactor = smoothstep(0.32, 0.88, dist);
                    rgb *= clamp(1.0 - vigFactor * uVignette * 0.85, 0.0, 1.0);
                }

                gl_FragColor = vec4(rgb, baseColor.a);
            }
        """
    }
}

/**
 * Builds a unified list of Media3 GPU [Effect] instances from an [EditProject].
 * Passed to both [androidx.media3.exoplayer.ExoPlayer.setVideoEffects] for 60fps real-time preview
 * and [androidx.media3.transformer.EditedMediaItem.Builder.setEffects] for hardware-accelerated MP4 export.
 */
object Media3VideoEffectsBuilder {

    @OptIn(UnstableApi::class)
    fun buildEffects(
        project: EditProject,
        clip: ClipSection? = project.clips.getOrNull(project.activeClipIndex.coerceIn(0, (project.clips.size - 1).coerceAtLeast(0))),
        isComparingOriginal: Boolean = false,
        includePresentationAspectRatio: Boolean = false
    ): List<Effect> {
        if (isComparingOriginal) {
            return emptyList()
        }

        val effects = mutableListOf<Effect>()
        val canvas = project.canvas
        val filter = FilterPreset.findById(project.filter.filterId)
        val filterStrength = if (filter.id == "none") 0f else project.filter.intensity.coerceIn(0f, 1f)
        val adj = project.adjustment

        // 0. Clip Source Crop (applied during hardware MP4 export so custom/preset crops match preview 1:1)
        if (includePresentationAspectRatio && clip != null) {
            val wFrac = (clip.cropWidthPercent / 100f).coerceIn(0.2f, 1.0f)
            val hFrac = (clip.cropHeightPercent / 100f).coerceIn(0.2f, 1.0f)
            if (wFrac < 0.995f || hFrac < 0.995f || kotlin.math.abs(clip.cropOffsetX) > 0.005f || kotlin.math.abs(clip.cropOffsetY) > 0.005f) {
                val maxShiftX = (1f - wFrac).coerceAtLeast(0f)
                val maxShiftY = (1f - hFrac).coerceAtLeast(0f)
                val cx = (clip.cropOffsetX * 2f).coerceIn(-maxShiftX, maxShiftX)
                val cy = (-clip.cropOffsetY * 2f).coerceIn(-maxShiftY, maxShiftY)
                val left = (cx - wFrac).coerceIn(-1f, 0.85f)
                val right = (cx + wFrac).coerceIn(left + 0.15f, 1f)
                val bottom = (cy - hFrac).coerceIn(-1f, 0.85f)
                val top = (cy + hFrac).coerceIn(bottom + 0.15f, 1f)
                effects.add(Crop(left, right, bottom, top))
            }
        }

        // 1. Scale / Flip / Rotate transform (applied in Media3 during hardware export; handled interactively by Compose graphicsLayer in live preview)
        if (includePresentationAspectRatio && (canvas.rotationDegrees != 0f || canvas.isFlippedHorizontal || canvas.scaleFactor != 1.0f)) {
            val scaleX = (if (canvas.isFlippedHorizontal) -1f else 1f) * canvas.scaleFactor
            val scaleY = canvas.scaleFactor
            effects.add(
                ScaleAndRotateTransformation.Builder()
                    .setScale(scaleX, scaleY)
                    .setRotationDegrees(canvas.rotationDegrees)
                    .build()
            )
        }

        // 2. Aspect Ratio Presentation transform (applied during hardware MP4 export)
        if (includePresentationAspectRatio && canvas.ratio != AspectRatio.RATIO_ORIGINAL) {
            val layoutMode = if (canvas.fitMode == FitMode.FILL_CROP) {
                Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
            } else {
                Presentation.LAYOUT_SCALE_TO_FIT
            }
            effects.add(
                Presentation.createForAspectRatio(
                    canvas.ratio.aspectRatio.coerceIn(0.25f, 4.0f),
                    layoutMode
                )
            )
        }

        // 3. Grayscale / Monochrome LUT
        if (filter.isBw && filterStrength >= 0.4f) {
            effects.add(RgbFilter.createGrayscaleFilter())
        }

        // 4. Combined Brightness & Exposure [-1.0f .. 1.0f]
        val filterBrightness = (filter.brightnessOffset + filter.exposureOffset * 0.25f) * filterStrength
        val userBrightness = adj.brightness + (adj.exposure * 0.22f)
        val totalBrightness = (filterBrightness + userBrightness).coerceIn(-0.85f, 0.85f)
        if (kotlin.math.abs(totalBrightness) > 0.005f) {
            effects.add(Brightness(totalBrightness))
        }

        // 5. Combined Contrast [-1.0f .. 1.0f] (includes Clarity, Brilliance, and Film Fade)
        val filterContrastDelta = (filter.contrastMultiplier - 1.0f) * filterStrength
        val userContrastDelta = adj.contrast - 1.0f
        val clarityBonus = (adj.clarity / 100f) * 0.22f
        val brillianceContrast = (adj.brilliance / 50f) * 0.15f
        val fadeContrastReduction = (adj.fade / 100f) * -0.12f
        val totalContrast = (filterContrastDelta + userContrastDelta + clarityBonus + brillianceContrast + fadeContrastReduction).coerceIn(-0.85f, 0.85f)
        if (kotlin.math.abs(totalContrast) > 0.005f) {
            effects.add(Contrast(totalContrast))
        }

        // 6. Combined HSL (Hue, Saturation, Lightness)
        val filterSatDelta = (filter.saturationMultiplier - 1.0f) * filterStrength
        val userSatDelta = (adj.saturation - 1.0f) + (adj.vibrance * 0.5f)
        val satAdjustment = ((filterSatDelta + userSatDelta) * 80f).coerceIn(-100f, 100f)

        val hueAdjustment = ((adj.hue / 50f) * 35f + (adj.hue * 15f) + (filter.hueOffset * filterStrength * 60f)).coerceIn(-180f, 180f)

        val highlightShift = (adj.highlights * 18f)
        val shadowShift = (adj.shadows * 18f)
        val brillianceLightness = (adj.brilliance / 50f) * 12f
        val lightnessAdjustment = (highlightShift + shadowShift + brillianceLightness).coerceIn(-50f, 50f)

        if (kotlin.math.abs(satAdjustment) > 0.5f ||
            kotlin.math.abs(hueAdjustment) > 0.5f ||
            kotlin.math.abs(lightnessAdjustment) > 0.5f
        ) {
            effects.add(
                HslAdjustment.Builder()
                    .adjustHue(hueAdjustment)
                    .adjustSaturation(satAdjustment)
                    .adjustLightness(lightnessAdjustment)
                    .build()
            )
        }

        // 7. GPU Color Balance Matrix (Temperature, Tint, Whites, Blacks, Fade)
        val tempVal = adj.temperature + (filter.temperatureOffset * filterStrength)
        val tintVal = adj.tint + (filter.tintOffset * filterStrength)
        val whitesGain = 1.0f + (adj.whites / 50f) * 0.18f
        val blacksOffset = (adj.blacks / 50f) * 0.08f
        val fadeLift = (adj.fade / 100f) * 0.14f

        val rScale = (whitesGain * (1.0f + (if (tempVal > 0f) tempVal * 0.22f else 0f) + (if (tintVal > 0f) tintVal * 0.12f else 0f))).coerceIn(0.2f, 2.0f)
        val gScale = (whitesGain * (1.0f + (if (tintVal < 0f) -tintVal * 0.18f else -tintVal * 0.08f))).coerceIn(0.2f, 2.0f)
        val bScale = (whitesGain * (1.0f + (if (tempVal < 0f) -tempVal * 0.22f else 0f) + (if (tintVal > 0f) tintVal * 0.10f else 0f))).coerceIn(0.2f, 2.0f)

        val rOffset = (blacksOffset + fadeLift).coerceIn(-0.5f, 0.5f)
        val gOffset = (blacksOffset + fadeLift).coerceIn(-0.5f, 0.5f)
        val bOffset = (blacksOffset + fadeLift).coerceIn(-0.5f, 0.5f)

        val hasMatrixGrading = kotlin.math.abs(rScale - 1f) > 0.005f ||
                kotlin.math.abs(gScale - 1f) > 0.005f ||
                kotlin.math.abs(bScale - 1f) > 0.005f ||
                kotlin.math.abs(rOffset) > 0.005f ||
                kotlin.math.abs(gOffset) > 0.005f ||
                kotlin.math.abs(bOffset) > 0.005f

        if (hasMatrixGrading) {
            effects.add(
                ColorGradingMatrix(
                    redScale = rScale,
                    greenScale = gScale,
                    blueScale = bScale,
                    redOffset = rOffset,
                    greenOffset = gOffset,
                    blueOffset = bOffset
                )
            )
        }

        // 8. Custom OpenGL ES 2.0 Shader for Sharpen (Unsharp Mask), Clarity, Film Grain, Vignette, Highlights/Shadows, Sepia & RGB Split
        val effectiveSharpen = (adj.sharpen / 100f).coerceIn(0f, 1f)
        val effectiveClarity = (adj.clarity / 100f).coerceIn(0f, 1f)
        val effectiveGrain = ((adj.grain / 100f) + filter.grainAmount * filterStrength).coerceIn(0f, 1f)
        val effectiveVignette = ((adj.vignette / 100f) + filter.vignetteAmount * filterStrength).coerceIn(0f, 1f)
        val effectiveHighlights = adj.highlights.coerceIn(-1f, 1f)
        val effectiveShadows = adj.shadows.coerceIn(-1f, 1f)
        val applySepia = filter.isSepia && filterStrength > 0.2f
        val applyRgbSplit = (filter.isRgbSplit || filter.isGlitch) && filterStrength > 0.2f

        if (effectiveSharpen > 0.01f ||
            effectiveClarity > 0.01f ||
            effectiveGrain > 0.01f ||
            effectiveVignette > 0.01f ||
            kotlin.math.abs(effectiveHighlights) > 0.02f ||
            kotlin.math.abs(effectiveShadows) > 0.02f ||
            applySepia ||
            applyRgbSplit
        ) {
            effects.add(
                CapCutDetailAndGrainGlEffect(
                    sharpenAmount = effectiveSharpen,
                    clarityAmount = effectiveClarity,
                    grainAmount = effectiveGrain,
                    vignetteAmount = effectiveVignette,
                    highlights = effectiveHighlights,
                    shadows = effectiveShadows,
                    isSepia = applySepia,
                    isRgbSplit = applyRgbSplit
                )
            )
        }

        // 9. Bake Text Overlays into exported video frames
        if (includePresentationAspectRatio && project.texts.isNotEmpty()) {
            runCatching {
                val overlayBmp = renderTextOverlaysBitmap(project.texts)
                if (overlayBmp != null) {
                    val bmpOverlay = BitmapOverlay.createStaticBitmapOverlay(overlayBmp)
                    effects.add(OverlayEffect(ImmutableList.of(bmpOverlay)))
                }
            }
        }

        return effects
    }

    private fun renderTextOverlaysBitmap(texts: List<TextOverlay>): Bitmap? {
        val validTexts = texts.filter { it.text.isNotBlank() }
        if (validTexts.isEmpty()) return null

        val width = 1080
        val height = 1920
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        for (overlay in validTexts) {
            val fontSizePx = (overlay.fontSizeSp.coerceIn(12f, 64f) * 2.6f)
            textPaint.textSize = fontSizePx
            textPaint.color = runCatching { Color.parseColor(overlay.textColorHex) }.getOrDefault(Color.WHITE)

            val cx = (width * 0.5f + overlay.positionNormX.coerceIn(-0.45f, 0.45f) * width)
            val cy = (height * 0.5f + overlay.positionNormY.coerceIn(-0.45f, 0.45f) * height)

            val textWidth = textPaint.measureText(overlay.text)
            val fontMetrics = textPaint.fontMetrics
            val textHeight = fontMetrics.descent - fontMetrics.ascent

            if (overlay.hasBackgroundBox) {
                boxPaint.color = runCatching { Color.parseColor(overlay.backgroundColorHex) }
                    .getOrDefault(Color.argb(190, 13, 14, 17))
                val padX = 36f
                val padY = 20f
                val rect = RectF(
                    cx - textWidth / 2f - padX,
                    cy - textHeight / 2f - padY,
                    cx + textWidth / 2f + padX,
                    cy + textHeight / 2f + padY
                )
                val radius = overlay.boxCornerRadiusDp * 2.5f
                canvas.drawRoundRect(rect, radius, radius, boxPaint)
            } else {
                textPaint.setShadowLayer(8f, 0f, 3f, Color.argb(200, 0, 0, 0))
            }

            val baselineY = cy - (fontMetrics.ascent + fontMetrics.descent) / 2f
            canvas.drawText(overlay.text, cx, baselineY, textPaint)
            textPaint.clearShadowLayer()
        }

        return bmp
    }
}
