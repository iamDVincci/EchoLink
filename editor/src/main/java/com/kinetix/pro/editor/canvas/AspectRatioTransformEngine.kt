package com.kinetix.pro.editor.canvas

import com.kinetix.pro.core.model.AspectRatio
import com.kinetix.pro.core.model.CanvasConfig
import com.kinetix.pro.core.model.FitMode
import kotlin.math.max

data class CanvasRenderLayout(
    val outputWidth: Int,
    val outputHeight: Int,
    val videoDrawX: Int,
    val videoDrawY: Int,
    val videoDrawWidth: Int,
    val videoDrawHeight: Int,
    val backgroundScale: Float,
    val fitMode: FitMode,
    val backgroundColorHex: String
)

class AspectRatioTransformEngine {

    fun computeLayout(
        sourceWidth: Int,
        sourceHeight: Int,
        canvasConfig: CanvasConfig,
        targetBaseDimension: Int = 1920
    ): CanvasRenderLayout {
        val targetRatio = when (canvasConfig.ratio) {
            AspectRatio.RATIO_9_16 -> 9f / 16f
            AspectRatio.RATIO_16_9 -> 16f / 9f
            AspectRatio.RATIO_1_1 -> 1f / 1f
            AspectRatio.RATIO_4_5 -> 4f / 5f
            AspectRatio.RATIO_4_3 -> 4f / 3f
            AspectRatio.RATIO_3_4 -> 3f / 4f
            AspectRatio.RATIO_21_9 -> 21f / 9f
            AspectRatio.RATIO_ORIGINAL -> sourceWidth.toFloat() / sourceHeight.coerceAtLeast(1).toFloat()
        }

        val outWidth: Int
        val outHeight: Int
        if (targetRatio <= 1.0f) { // Vertical / portrait or square
            outHeight = targetBaseDimension
            outWidth = (targetBaseDimension * targetRatio).toInt()
        } else { // Landscape
            outWidth = targetBaseDimension
            outHeight = (targetBaseDimension / targetRatio).toInt()
        }

        val srcW = sourceWidth.toFloat()
        val srcH = sourceHeight.toFloat()
        val srcRatio = srcW / srcH.coerceAtLeast(1f)

        var drawX = 0
        var drawY = 0
        var drawW = outWidth
        var drawH = outHeight
        var bgScale = 1.0f

        when (canvasConfig.fitMode) {
            FitMode.FIT_BLUR_BG, FitMode.FIT_SOLID_BG, FitMode.ORIGINAL_PADDING -> {
                // Scale to fit inside canvas without cropping
                if (srcRatio > targetRatio) { // Source wider than target
                    drawW = outWidth
                    drawH = (outWidth / srcRatio).toInt()
                    drawX = 0
                    drawY = (outHeight - drawH) / 2
                } else { // Source taller than target
                    drawH = outHeight
                    drawW = (outHeight * srcRatio).toInt()
                    drawX = (outWidth - drawW) / 2
                    drawY = 0
                }
                // Background fills canvas completely
                bgScale = max(outWidth / srcW, outHeight / srcH)
            }
            FitMode.FILL_CROP -> {
                // Scale to cover entire canvas, crop excess
                if (srcRatio > targetRatio) {
                    drawH = outHeight
                    drawW = (outHeight * srcRatio).toInt()
                    drawX = (outWidth - drawW) / 2
                    drawY = 0
                } else {
                    drawW = outWidth
                    drawH = (outWidth / srcRatio).toInt()
                    drawX = 0
                    drawY = (outHeight - drawH) / 2
                }
                bgScale = 1.0f
            }
        }

        // Apply pan offsets (normalized -1.0 to 1.0)
        drawX += (canvasConfig.panOffsetNormX * outWidth).toInt()
        drawY += (canvasConfig.panOffsetNormY * outHeight).toInt()

        return CanvasRenderLayout(
            outputWidth = outWidth,
            outputHeight = outHeight,
            videoDrawX = drawX,
            videoDrawY = drawY,
            videoDrawWidth = drawW,
            videoDrawHeight = drawH,
            backgroundScale = bgScale,
            fitMode = canvasConfig.fitMode,
            backgroundColorHex = canvasConfig.backgroundColorHex
        )
    }
}
