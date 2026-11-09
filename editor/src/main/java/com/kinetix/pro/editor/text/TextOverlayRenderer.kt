package com.kinetix.pro.editor.text

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import com.kinetix.pro.core.model.TextAnimation
import com.kinetix.pro.core.model.TextOverlay
import com.kinetix.pro.core.model.TimingAnchor
import kotlin.math.sin

class TextOverlayRenderer {

    fun renderOverlayBitmap(
        overlay: TextOverlay,
        canvasWidth: Int,
        canvasHeight: Int,
        presentationTimeMs: Long,
        clipDurationMs: Long
    ): Bitmap? {
        val timing = overlay.timing
        val (inMs, outMs) = when (timing.anchor) {
            TimingAnchor.START -> timing.offsetMs to (timing.offsetMs + timing.durationMs)
            TimingAnchor.END -> (clipDurationMs - timing.offsetMs - timing.durationMs).coerceAtLeast(0L) to (clipDurationMs - timing.offsetMs)
            TimingAnchor.FULL_CLIP -> 0L to clipDurationMs
        }

        if (presentationTimeMs < inMs || presentationTimeMs > outMs) {
            return null // Not visible at this timestamp
        }

        val elapsedInMs = presentationTimeMs - inMs
        val remainingOutMs = outMs - presentationTimeMs

        // Calculate animation factors
        var alpha = overlay.textOpacity
        var scale = 1.0f
        var translateY = 0f
        var textToDisplay = overlay.text

        // In animation (first 400ms)
        if (elapsedInMs < 400L) {
            val progress = (elapsedInMs / 400f).coerceIn(0f, 1f)
            when (overlay.inAnimation) {
                TextAnimation.FADE_IN -> alpha *= progress
                TextAnimation.POP_BOUNCE -> {
                    // Spring bounce curve
                    scale = 0.5f + 0.6f * progress + 0.15f * sin(progress * Math.PI.toFloat())
                    alpha *= progress
                }
                TextAnimation.SLIDE_UP -> translateY = (1f - progress) * 60f
                TextAnimation.TYPEWRITER -> {
                    val charCount = (progress * overlay.text.length).toInt().coerceIn(1, overlay.text.length)
                    textToDisplay = overlay.text.take(charCount)
                }
                TextAnimation.NONE -> {}
                else -> {}
            }
        }

        // Out animation (last 300ms)
        if (remainingOutMs < 300L) {
            val progress = (remainingOutMs / 300f).coerceIn(0f, 1f)
            when (overlay.outAnimation) {
                TextAnimation.FADE_OUT -> alpha *= progress
                TextAnimation.POP_BOUNCE -> scale *= progress
                TextAnimation.SLIDE_UP -> translateY = (1f - progress) * -40f
                TextAnimation.NONE -> {}
                else -> {}
            }
        }

        val bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val baseFontSizePx = canvasHeight * overlay.fontSizeNorm * scale
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = baseFontSizePx
            color = parseColorSafely(overlay.textColorHex, Color.WHITE)
            this.alpha = (alpha * 255).toInt().coerceIn(0, 255)
            typeface = if (overlay.isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
        }

        val bounds = Rect()
        textPaint.getTextBounds(textToDisplay, 0, textToDisplay.length, bounds)
        val textWidth = bounds.width().toFloat()
        val textHeight = bounds.height().toFloat()

        // Normalized position to screen coordinates
        val centerX = canvasWidth / 2f + (overlay.positionNormX * canvasWidth / 2f)
        val centerY = canvasHeight / 2f + (overlay.positionNormY * canvasHeight / 2f) + translateY

        canvas.save()
        canvas.rotate(overlay.rotationDegrees, centerX, centerY)

        // Draw background box capsule if enabled
        if (overlay.hasBackgroundBox) {
            val paddingH = baseFontSizePx * 0.45f
            val paddingV = baseFontSizePx * 0.25f
            val boxRect = RectF(
                centerX - textWidth / 2f - paddingH,
                centerY - textHeight / 2f - paddingV,
                centerX + textWidth / 2f + paddingH,
                centerY + textHeight / 2f + paddingV
            )
            val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = parseColorSafely(overlay.boxColorHex, Color.argb(180, 13, 14, 17))
                this.alpha = ((this.alpha / 255f) * alpha * 255).toInt().coerceIn(0, 255)
            }
            val radiusPx = overlay.boxCornerRadiusDp * 2.5f
            canvas.drawRoundRect(boxRect, radiusPx, radiusPx, boxPaint)
        }

        // Draw stroke if enabled
        if (overlay.strokeWidthDp > 0f) {
            val strokePaint = Paint(textPaint).apply {
                style = Paint.Style.STROKE
                strokeWidth = overlay.strokeWidthDp * 2.5f
                color = parseColorSafely(overlay.strokeColorHex, Color.BLACK)
                this.alpha = (alpha * 255).toInt().coerceIn(0, 255)
            }
            canvas.drawText(textToDisplay, centerX, centerY + textHeight / 3f, strokePaint)
        }

        // Draw text
        canvas.drawText(textToDisplay, centerX, centerY + textHeight / 3f, textPaint)
        canvas.restore()

        return bitmap
    }

    private fun parseColorSafely(hex: String, fallback: Int): Int {
        return try {
            Color.parseColor(hex)
        } catch (_: Exception) {
            fallback
        }
    }
}
