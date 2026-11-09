package com.antigravity.echolink.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Stitch & PRD Antigravity Tactical Color Palette
object EchoLinkColors {
    val ObsidianCanvas = Color(0xFF080A0F)
    val SurfaceDim = Color(0xFF111319)
    val SurfaceContainerLowest = Color(0xFF0C0E13)
    val SurfaceContainerLow = Color(0xFF191B21)
    val SurfaceContainer = Color(0xFF1E1F25)
    val SurfaceContainerHigh = Color(0xFF282A30)
    val SurfaceContainerHighest = Color(0xFF33353A)
    val HullCard = Color(0xFF121722)
    val ArmorStroke = Color(0xFF1F293D)
    val OutlineVariant = Color(0xFF3B494B)

    val CyberCyan = Color(0xFF00F0FF)
    val PrimaryLight = Color(0xFFDBFCFF)
    val PrimaryFixedDim = Color(0xFF00DBE9)
    val OnPrimaryContainer = Color(0xFF00363A)

    val EmeraldPulse = Color(0xFF4EDEA3)
    val EmeraldSecondary = Color(0xFF10B981)
    val SecondaryContainer = Color(0xFF00A572)
    val OnSecondaryContainer = Color(0xFF00311F)

    val AmberWarning = Color(0xFFFFB95F)
    val CombatCrimson = Color(0xFFEF4444)
    val ErrorLight = Color(0xFFFFB4AB)

    val TextPrimary = Color(0xFFF8FAFC)
    val TextOnSurface = Color(0xFFE2E2E9)
    val TextSecondary = Color(0xFF94A3B8)
    val TextOnSurfaceVariant = Color(0xFFB9CACB)
}

object EchoLinkTypography {
    val SpaceGrotesk = FontFamily.SansSerif
    val Inter = FontFamily.SansSerif
    val JetBrainsMono = FontFamily.Monospace

    val HeadlineXlMobile = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp
    )

    val HeadlineSm = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.3.sp
    )

    val BodyMd = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )

    val BodySm = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )

    val TelemetryMd = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp
    )

    val TelemetrySm = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.2.sp
    )

    val LabelCaps = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.9.sp
    )
}

private val TacticalColorScheme = darkColorScheme(
    primary = EchoLinkColors.CyberCyan,
    onPrimary = EchoLinkColors.OnPrimaryContainer,
    primaryContainer = EchoLinkColors.CyberCyan,
    onPrimaryContainer = EchoLinkColors.OnPrimaryContainer,
    secondary = EchoLinkColors.EmeraldPulse,
    onSecondary = EchoLinkColors.OnSecondaryContainer,
    secondaryContainer = EchoLinkColors.SecondaryContainer,
    onSecondaryContainer = EchoLinkColors.OnSecondaryContainer,
    tertiary = EchoLinkColors.AmberWarning,
    background = EchoLinkColors.SurfaceDim,
    onBackground = EchoLinkColors.TextOnSurface,
    surface = EchoLinkColors.SurfaceDim,
    onSurface = EchoLinkColors.TextOnSurface,
    surfaceVariant = EchoLinkColors.SurfaceContainer,
    onSurfaceVariant = EchoLinkColors.TextOnSurfaceVariant,
    error = EchoLinkColors.CombatCrimson
)

@Composable
fun EchoLinkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TacticalColorScheme,
        typography = Typography(),
        content = content
    )
}
