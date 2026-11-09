package com.kinetix.pro.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.kinetix.pro.core.ui.R

/**
 * Plus Jakarta Sans (SIL Open Font License) bundled as static weights so the
 * typography renders identically on every device, with no Play Services font provider.
 */
val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold)
)

// Type scale mapped 1:1 from DESIGN.MD (display-lg-mobile, headline-*, body-*, label-*).
val ObsidianTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = (-0.03).em, color = ContentHigh
    ),
    displayMedium = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = (-0.02).em, color = ContentHigh
    ),
    headlineLarge = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold,
        fontSize = 26.sp, lineHeight = 34.sp, letterSpacing = (-0.02).em, color = ContentHigh
    ),
    headlineMedium = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold,
        fontSize = 20.sp, lineHeight = 28.sp, letterSpacing = (-0.01).em, color = ContentHigh
    ),
    headlineSmall = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp, lineHeight = 24.sp, color = ContentHigh
    ),
    titleLarge = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold,
        fontSize = 20.sp, lineHeight = 28.sp, letterSpacing = (-0.01).em, color = ContentHigh
    ),
    titleMedium = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 22.sp, color = ContentHigh
    ),
    titleSmall = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp, color = ContentHigh
    ),
    bodyLarge = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp, color = ContentHigh
    ),
    bodyMedium = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp, color = ContentMedium
    ),
    bodySmall = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp, color = ContentMedium
    ),
    labelLarge = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.01.em, color = ContentHigh
    ),
    labelMedium = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.02.em, color = ContentHigh
    ),
    labelSmall = TextStyle(
        fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.04.em, color = ContentHigh
    )
)
