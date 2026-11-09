package com.kinetix.pro.core.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Obsidian Sunset Color Palette (as defined in DESIGN.MD)
val ObsidianCanvas = Color(0xFF0D0E11)
val ObsidianSurface1 = Color(0xFF16181D)
val ObsidianSurface2 = Color(0xFF1F222A)
val ObsidianSurface3 = Color(0xFF252934)
val ObsidianStroke = Color(0xFF2A2E39)
val ObsidianStrokeHigh = Color(0xFF3E4453)
val ObsidianCardStroke = Color(0xFF232630)     // Level 1 hairline (DESIGN.MD)
val ObsidianSheetStroke = Color(0xFF2E3340)    // Level 2 hairline (DESIGN.MD)
val ObsidianInputStroke = Color(0xFF252932)    // Form input border (DESIGN.MD)
val ContentPlaceholder = Color(0xFF5D6273)     // Input placeholder (DESIGN.MD)
val ControlInactiveStroke = Color(0xFF393D4B)  // Radio / checkbox inactive border
val SunsetTint15 = Color(0x26EA6113)           // rgba(234,97,19,0.15) badge / focus ring
val SunsetGlow = Color(0x59EA6113)             // rgba(234,97,19,0.35) CTA halo

val PrimarySunset = Color(0xFFEA6113)
val SecondaryOrange = Color(0xFFF88F22)
val TertiaryAmber = Color(0xFFFBB931)
val WarmHighlight = Color(0xFFFFE3B3)

val ContentHigh = Color(0xFFFFFFFF)
val ContentMedium = Color(0xFF9E9EB0)
val ContentMuted = Color(0xFF474A56)

val StatusSuccess = Color(0xFF22C55E)
val StatusError = Color(0xFFFF5252)
val StatusWarning = Color(0xFFF59E0B)
val StatusInfo = Color(0xFF38BDF8)

// Gradients
val SunsetGradient = Brush.horizontalGradient(
    colors = listOf(PrimarySunset, SecondaryOrange)
)

val SunsetAmberGradient = Brush.horizontalGradient(
    colors = listOf(PrimarySunset, TertiaryAmber)
)

val GlowShadowColor = Color(0x59EA6113)
val DarkCardGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF1A1C23), ObsidianSurface1)
)
