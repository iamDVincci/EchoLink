package com.kinetix.pro.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinetix.pro.core.ui.theme.ContentHigh
import com.kinetix.pro.core.ui.theme.ObsidianStroke
import com.kinetix.pro.core.ui.theme.ObsidianSurface2
import com.kinetix.pro.core.ui.theme.PrimarySunset
import com.kinetix.pro.core.ui.theme.TertiaryAmber

@Composable
fun PlatformBadge(
    platformName: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (platformName.lowercase()) {
        "youtube" -> Color(0x33FF0000) to Color(0xFFFF4D4D)
        "tiktok" -> Color(0x3300F2FE) to Color(0xFF00F2FE)
        "instagram" -> Color(0x33E1306C) to Color(0xFFFF548F)
        "x", "twitter" -> Color(0x331DA1F2) to Color(0xFF1DA1F2)
        "reddit" -> Color(0x33FF4500) to Color(0xFFFF6D3B)
        "facebook" -> Color(0x331877F2) to Color(0xFF4C9AFF)
        "vimeo" -> Color(0x331AB7EA) to Color(0xFF1AB7EA)
        else -> ObsidianSurface2 to PrimarySunset
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, textColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = platformName.uppercase(),
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun ResolutionBadge(
    resolutionLabel: String,
    modifier: Modifier = Modifier,
    isRecommended: Boolean = false
) {
    val bgColor = if (isRecommended) Color(0x33EA6113) else ObsidianSurface2
    val borderColor = if (isRecommended) PrimarySunset else ObsidianStroke
    val textColor = if (isRecommended) PrimarySunset else ContentHigh

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = resolutionLabel,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun RecommendedPill(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x33FBB931))
            .border(1.dp, TertiaryAmber, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = "RECOMMENDED",
            color = TertiaryAmber,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        )
    }
}
