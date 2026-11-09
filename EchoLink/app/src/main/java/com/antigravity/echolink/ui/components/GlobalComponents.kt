package com.antigravity.echolink.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.echolink.model.EarbudStatus
import com.antigravity.echolink.model.NavTab
import com.antigravity.echolink.ui.theme.EchoLinkColors
import com.antigravity.echolink.ui.theme.EchoLinkTypography

/**
 * Custom Vector Antigravity EchoLink Emblem.
 */
@Composable
fun AntigravityEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(EchoLinkColors.SurfaceContainerLowest)
            .border(1.dp, EchoLinkColors.CyberCyan.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.68f)) {
            val w = this.size.width
            val h = this.size.height
            val path = Path().apply {
                moveTo(w * 0.18f, h * 0.80f)
                lineTo(w * 0.50f, h * 0.16f)
                lineTo(w * 0.82f, h * 0.80f)
            }
            drawPath(
                path = path,
                color = EchoLinkColors.CyberCyan,
                style = Stroke(width = w * 0.13f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawCircle(
                color = EchoLinkColors.EmeraldPulse,
                radius = w * 0.09f,
                center = Offset(w * 0.50f, h * 0.56f)
            )
        }
    }
}

/**
 * Stylized Tactical Operative Avatar drawn with Compose Canvas.
 */
@Composable
fun TacticalAvatarBadge(
    name: String,
    accentColor: Color,
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    val initials = name.trim().take(2).uppercase()
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.32f),
                        EchoLinkColors.SurfaceContainerHigh,
                        EchoLinkColors.SurfaceContainerLowest
                    )
                )
            )
            .border(1.2.dp, accentColor.copy(alpha = 0.7f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = EchoLinkTypography.LabelCaps.copy(
                color = EchoLinkColors.TextPrimary,
                fontSize = (size.value * 0.34f).sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

/**
 * Visual 4-Bar RF Signal Quality Indicator (replaces raw dBm / MHz text dumps).
 */
@Composable
fun VisualSignalBars(
    strength0To4: Int,
    activeColor: Color = EchoLinkColors.EmeraldPulse,
    modifier: Modifier = Modifier
) {
    val clamped = strength0To4.coerceIn(1, 4)
    Row(
        modifier = modifier.height(14.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp)
    ) {
        for (i in 1..4) {
            val frac = i / 4f
            val isLit = i <= clamped
            Box(
                modifier = Modifier
                    .width(3.2.dp)
                    .fillMaxHeight(frac)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(if (isLit) activeColor else EchoLinkColors.OutlineVariant.copy(alpha = 0.45f))
            )
        }
    }
}

/**
 * Visual Bilateral / Multi-Bar Waveform Strip (replaces raw dB and frequency numbers).
 */
@Composable
fun LiveWaveformStrip(
    bands: List<Float>,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
    primaryColor: Color = EchoLinkColors.CyberCyan,
    secondaryColor: Color = EchoLinkColors.EmeraldPulse
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        bands.forEachIndexed { idx, fraction ->
            val animH by animateFloatAsState(
                targetValue = fraction.coerceIn(0.14f, 1.0f),
                animationSpec = tween(80),
                label = "wave_$idx"
            )
            val barColor = if (idx % 2 == 0) primaryColor else secondaryColor
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(animH)
                    .clip(CircleShape)
                    .background(barColor)
            )
        }
    }
}

/**
 * Sleek Single-Row Phone Top Bar respecting system status bar insets.
 */
@Composable
fun EchoLinkTopAppBar(
    title: String,
    subtitle: String,
    earbudStatus: EarbudStatus,
    onSelectRoute: (String) -> Unit,
    showBackButton: Boolean = false,
    showEmergencySquelch: Boolean = false,
    onBackClick: () -> Unit = {},
    onEmergencySquelchClick: () -> Unit = {}
) {
    var routeMenuExpanded by remember { mutableStateOf(false) }
    val pulseTransition = rememberInfiniteTransition(label = "earbudPulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "pulseAlpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(EchoLinkColors.SurfaceDim.copy(alpha = 0.95f))
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (showBackButton) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(EchoLinkColors.SurfaceContainerHigh)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = EchoLinkColors.TextOnSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                AntigravityEmblem(size = 34.dp)
            }

            Column {
                Text(
                    text = title,
                    style = EchoLinkTypography.HeadlineSm,
                    color = EchoLinkColors.TextOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = EchoLinkTypography.BodySm.copy(fontSize = 11.sp),
                    color = EchoLinkColors.TextOnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Compact Audio Output Selector Pill + Optional Mute Shortcut
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box {
                val dotColor = if (earbudStatus.isConnected) EchoLinkColors.EmeraldPulse else EchoLinkColors.CyberCyan
                val compactRouteName = when {
                    earbudStatus.isConnected -> earbudStatus.deviceName.take(14)
                    earbudStatus.routeType == "EARPIECE" -> "Earpiece"
                    else -> "Speaker"
                }

                Row(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(CircleShape)
                        .background(EchoLinkColors.SurfaceContainerHigh)
                        .border(1.dp, EchoLinkColors.ArmorStroke, CircleShape)
                        .clickable { routeMenuExpanded = true }
                        .padding(horizontal = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(dotColor.copy(alpha = pulseAlpha))
                    )
                    Icon(
                        imageVector = if (earbudStatus.isConnected) Icons.Default.Headphones else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = dotColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = compactRouteName,
                        style = EchoLinkTypography.BodySm.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                        color = EchoLinkColors.TextOnSurface,
                        maxLines = 1
                    )
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = "Audio Route",
                        tint = EchoLinkColors.TextOnSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }

                DropdownMenu(
                    expanded = routeMenuExpanded,
                    onDismissRequest = { routeMenuExpanded = false },
                    modifier = Modifier.background(EchoLinkColors.SurfaceContainerHigh)
                ) {
                    earbudStatus.availableRoutes.forEach { route ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = route,
                                    style = EchoLinkTypography.BodySm,
                                    color = EchoLinkColors.TextPrimary
                                )
                            },
                            onClick = {
                                onSelectRoute(route)
                                routeMenuExpanded = false
                            }
                        )
                    }
                }
            }

            if (showEmergencySquelch) {
                IconButton(
                    onClick = onEmergencySquelchClick,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(EchoLinkColors.ErrorLight.copy(alpha = 0.16f))
                ) {
                    Icon(
                        imageVector = Icons.Default.MicOff,
                        contentDescription = "Quick Mute",
                        tint = EchoLinkColors.ErrorLight,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

/**
 * Phone-Optimized Bottom Navigation Bar respecting system gesture navigation bar insets.
 */
@Composable
fun EchoLinkBottomNavBar(
    selectedTab: NavTab,
    onSelectTab: (NavTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(EchoLinkColors.SurfaceDim.copy(alpha = 0.96f))
            .border(width = 0.5.dp, color = EchoLinkColors.ArmorStroke)
            .navigationBarsPadding()
            .height(60.dp)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavTab.entries.forEach { tab ->
            val isSelected = (tab == selectedTab)
            val tint = if (isSelected) EchoLinkColors.CyberCyan else EchoLinkColors.TextOnSurfaceVariant
            val icon = when (tab) {
                NavTab.MESH -> Icons.Default.Hub
                NavTab.AUDIO -> Icons.Default.Equalizer
                NavTab.RF_STATS -> Icons.AutoMirrored.Filled.ShowChart
                NavTab.HUD -> Icons.Default.Radio
            }
            val cleanLabel = when (tab) {
                NavTab.MESH -> "Radar"
                NavTab.AUDIO -> "Audio"
                NavTab.RF_STATS -> "Signal"
                NavTab.HUD -> "Setup"
            }
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) EchoLinkColors.CyberCyan.copy(alpha = 0.12f) else Color.Transparent)
                    .clickable { onSelectTab(tab) }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = cleanLabel,
                    tint = tint,
                    modifier = Modifier.size(21.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = cleanLabel,
                    style = EchoLinkTypography.BodySm.copy(
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = tint
                )
            }
        }
    }
}
