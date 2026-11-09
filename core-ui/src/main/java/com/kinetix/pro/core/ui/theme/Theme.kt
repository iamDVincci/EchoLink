package com.kinetix.pro.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimarySunset,
    onPrimary = ContentHigh,
    primaryContainer = ObsidianSurface2,
    onPrimaryContainer = WarmHighlight,
    secondary = SecondaryOrange,
    onSecondary = ContentHigh,
    tertiary = TertiaryAmber,
    onTertiary = ObsidianCanvas,
    background = ObsidianCanvas,
    onBackground = ContentHigh,
    surface = ObsidianSurface1,
    onSurface = ContentHigh,
    surfaceVariant = ObsidianSurface2,
    onSurfaceVariant = ContentMedium,
    outline = ObsidianStroke,
    outlineVariant = ObsidianStrokeHigh,
    error = StatusError,
    onError = ContentHigh
)

@Composable
fun KinetixTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = ObsidianCanvas.toArgb()
                window.navigationBarColor = ObsidianCanvas.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ObsidianTypography,
        content = content
    )
}
