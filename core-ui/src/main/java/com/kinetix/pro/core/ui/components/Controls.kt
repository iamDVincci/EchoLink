package com.kinetix.pro.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinetix.pro.core.ui.theme.ContentHigh
import com.kinetix.pro.core.ui.theme.ContentMedium
import com.kinetix.pro.core.ui.theme.ObsidianStroke
import com.kinetix.pro.core.ui.theme.ObsidianSurface2
import com.kinetix.pro.core.ui.theme.PrimarySunset
import com.kinetix.pro.core.ui.theme.SecondaryOrange
import com.kinetix.pro.core.ui.theme.SunsetGradient
import kotlin.math.roundToInt

@Composable
fun SunsetSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    label: String? = null,
    valueFormatter: (Float) -> String = { String.format("%.2f", it) },
    onReset: (() -> Unit)? = null,
    onValueChangeFinished: (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (label != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = ContentMedium
                )
                Text(
                    text = valueFormatter(value),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimarySunset,
                    modifier = if (onReset != null) Modifier.clickable { onReset() } else Modifier
                )
            }
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = PrimarySunset,
                activeTrackColor = PrimarySunset,
                inactiveTrackColor = ObsidianSurface2,
                activeTickColor = SecondaryOrange,
                inactiveTickColor = ObsidianStroke
            )
        )
    }
}

/**
 * Precision dual-thumb range slider + interactive playhead scrubber for non-destructive trimming
 */
@Composable
fun DualThumbRangeSlider(
    startMs: Long,
    endMs: Long,
    totalDurationMs: Long,
    onRangeChange: (startMs: Long, endMs: Long) -> Unit,
    playheadMs: Long = startMs,
    onSeek: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(ObsidianSurface2)
            .border(1.dp, ObsidianStroke, RoundedCornerShape(8.dp))
    ) {
        val densityValue = LocalDensity.current.density
        val totalWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val validDuration = totalDurationMs.coerceAtLeast(1000L).toFloat()

        val startFraction = (startMs.toFloat() / validDuration).coerceIn(0f, 1f)
        val endFraction = (endMs.toFloat() / validDuration).coerceIn(startFraction, 1f)
        val playheadFraction = (playheadMs.toFloat() / validDuration).coerceIn(0f, 1f)

        val startX = startFraction * totalWidthPx
        val endX = endFraction * totalWidthPx
        val playheadX = playheadFraction * totalWidthPx

        val currentStartX by rememberUpdatedState(startX)
        val currentEndX by rememberUpdatedState(endX)
        val currentStartMs by rememberUpdatedState(startMs)
        val currentEndMs by rememberUpdatedState(endMs)
        val currentValidDuration by rememberUpdatedState(validDuration)
        val currentOnRangeChange by rememberUpdatedState(onRangeChange)
        val currentOnSeek by rememberUpdatedState(onSeek)

        // Track background tap/drag to scrub playhead
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(totalWidthPx) {
                    detectTapGestures { offset ->
                        val targetMs = ((offset.x / totalWidthPx).coerceIn(0f, 1f) * currentValidDuration).toLong()
                        currentOnSeek?.invoke(targetMs.coerceIn(currentStartMs, currentEndMs))
                    }
                }
                .pointerInput(totalWidthPx) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val targetMs = ((change.position.x / totalWidthPx).coerceIn(0f, 1f) * currentValidDuration).toLong()
                        currentOnSeek?.invoke(targetMs.coerceIn(currentStartMs, currentEndMs))
                    }
                }
        )

        // Active range highlight box
        Box(
            modifier = Modifier
                .offset { IntOffset(startX.roundToInt(), 0) }
                .width(((endX - startX).coerceAtLeast(0f) / densityValue).dp)
                .height(56.dp)
                .background(Color(0x33EA6113))
                .border(2.dp, PrimarySunset)
        )

        // Live White Playhead Line
        Box(
            modifier = Modifier
                .offset { IntOffset(playheadX.coerceIn(0f, totalWidthPx - 4f).roundToInt(), 0) }
                .width(3.dp)
                .height(56.dp)
                .background(ContentHigh)
        )

        // Left Trim Handle
        Box(
            modifier = Modifier
                .offset { IntOffset((startX - 10f).coerceAtLeast(0f).roundToInt(), 0) }
                .width(22.dp)
                .height(56.dp)
                .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                .background(PrimarySunset)
                .pointerInput(totalWidthPx) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newStartX = (currentStartX + dragAmount.x).coerceIn(0f, (currentEndX - 24f).coerceAtLeast(0f))
                        val newStartMs = ((newStartX / totalWidthPx) * currentValidDuration).toLong()
                        currentOnRangeChange(newStartMs, currentEndMs)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(24.dp)
                    .background(ContentHigh)
            )
        }

        // Right Trim Handle
        Box(
            modifier = Modifier
                .offset { IntOffset((endX - 12f).coerceIn(0f, (totalWidthPx - 22f).coerceAtLeast(0f)).roundToInt(), 0) }
                .width(22.dp)
                .height(56.dp)
                .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                .background(PrimarySunset)
                .pointerInput(totalWidthPx) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newEndX = (currentEndX + dragAmount.x).coerceIn((currentStartX + 24f).coerceAtMost(totalWidthPx), totalWidthPx)
                        val newEndMs = ((newEndX / totalWidthPx) * currentValidDuration).toLong()
                        currentOnRangeChange(currentStartMs, newEndMs)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(24.dp)
                    .background(ContentHigh)
            )
        }
    }
}
