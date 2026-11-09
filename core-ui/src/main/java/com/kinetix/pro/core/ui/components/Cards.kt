package com.kinetix.pro.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kinetix.pro.core.ui.theme.DarkCardGradient
import com.kinetix.pro.core.ui.theme.ObsidianCardStroke
import com.kinetix.pro.core.ui.theme.ObsidianSheetStroke
import com.kinetix.pro.core.ui.theme.ObsidianSurface1
import com.kinetix.pro.core.ui.theme.ObsidianSurface2
import com.kinetix.pro.core.ui.theme.PrimarySunset

@Composable
fun ObsidianCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    backgroundColor: Color = ObsidianSurface1,
    borderColor: Color = ObsidianCardStroke,
    isHighlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val scale = if (onClick != null) rememberPressScale(interactionSource, 0.98f) else 1f
    val activeBorder = if (isHighlighted) PrimarySunset.copy(alpha = 0.45f) else borderColor

    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = rememberRipple(color = PrimarySunset),
            onClick = onClick
        )
    } else Modifier

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(if (backgroundColor == ObsidianSurface1) DarkCardGradient else androidx.compose.ui.graphics.SolidColor(backgroundColor))
            .border(1.dp, activeBorder, shape)
            .then(clickModifier)
            .padding(18.dp),
        content = content
    )
}

@Composable
fun ObsidianInnerCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    backgroundColor: Color = ObsidianSurface2,
    borderColor: Color = ObsidianSheetStroke,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape)
            .padding(14.dp),
        content = content
    )
}
