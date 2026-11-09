package com.kinetix.pro.core.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinetix.pro.core.ui.theme.ContentHigh
import com.kinetix.pro.core.ui.theme.ContentMedium
import com.kinetix.pro.core.ui.theme.ContentMuted
import com.kinetix.pro.core.ui.theme.ObsidianStroke
import com.kinetix.pro.core.ui.theme.ObsidianSurface1
import com.kinetix.pro.core.ui.theme.ObsidianSurface2
import com.kinetix.pro.core.ui.theme.PrimarySunset
import com.kinetix.pro.core.ui.theme.SunsetGlow
import com.kinetix.pro.core.ui.theme.SunsetGradient

/** Springy 0.96x press feedback shared by every tappable surface. */
@Composable
fun rememberPressScale(interactionSource: MutableInteractionSource, pressedScale: Float = 0.96f): Float {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale"
    )
    return scale
}

@Composable
fun SunsetPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(14.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource)
    val haptics = LocalHapticFeedback.current
    val active = enabled && !isLoading

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(
                if (enabled) Modifier.shadow(14.dp, shape, ambientColor = SunsetGlow, spotColor = SunsetGlow)
                else Modifier
            )
            .defaultMinSize(minHeight = 48.dp)
            .height(50.dp)
            .clip(shape)
            .background(if (enabled) SunsetGradient else SolidColor(ObsidianSurface2))
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(color = Color.White),
                enabled = active,
                role = Role.Button
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = ContentHigh,
                strokeWidth = 2.5.dp
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) ContentHigh else ContentMuted,
                        modifier = Modifier.padding(end = 8.dp).size(20.dp)
                    )
                }
                Text(
                    text = text,
                    color = if (enabled) ContentHigh else ContentMuted,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun SunsetOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    selected: Boolean = false
) {
    val shape = RoundedCornerShape(12.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource)

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .height(48.dp)
            .clip(shape)
            .background(if (selected) SunsetGradient else SolidColor(ObsidianSurface2))
            .border(1.dp, if (selected) PrimarySunset else ObsidianStroke, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(color = PrimarySunset),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = when {
                        !enabled -> ContentMuted
                        selected -> ContentHigh
                        else -> PrimarySunset
                    },
                    modifier = Modifier.padding(end = 8.dp).size(18.dp)
                )
            }
            Text(
                text = text,
                color = if (enabled) ContentHigh else ContentMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun SunsetIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = ContentHigh,
    backgroundColor: Color = ObsidianSurface2,
    size: Int = 42,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource, 0.9f)
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .size(size.coerceAtLeast(36).dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(1.dp, ObsidianStroke, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(color = PrimarySunset),
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else ContentMuted,
            modifier = Modifier.size((size * 0.52).dp)
        )
    }
}

/** 36dp selection chip from DESIGN.MD: gradient when active, hairline surface when idle. */
@Composable
fun SunsetChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val shape = RoundedCornerShape(12.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource)
    val haptics = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .height(36.dp)
            .clip(shape)
            .background(if (selected) SunsetGradient else SolidColor(ObsidianSurface1))
            .border(1.dp, if (selected) Color.Transparent else ObsidianStroke, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(color = PrimarySunset),
                role = Role.Tab
            ) {
                if (!selected) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    icon, null,
                    tint = if (selected) ContentHigh else ContentMedium,
                    modifier = Modifier.padding(end = 6.dp).size(16.dp)
                )
            }
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) ContentHigh else ContentMedium,
                maxLines = 1
            )
        }
    }
}
