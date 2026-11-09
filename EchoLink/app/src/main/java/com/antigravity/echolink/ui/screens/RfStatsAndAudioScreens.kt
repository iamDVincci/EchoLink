package com.antigravity.echolink.ui.screens

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SpatialAudio
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.echolink.model.PacketTelemetry
import com.antigravity.echolink.model.TransportTier
import com.antigravity.echolink.ui.components.LiveWaveformStrip
import com.antigravity.echolink.ui.components.VisualSignalBars
import com.antigravity.echolink.ui.theme.EchoLinkColors
import com.antigravity.echolink.ui.theme.EchoLinkTypography
import kotlin.math.PI
import kotlin.math.sin

/**
 * Visual Signal & Link Health Screen — replaces raw hex/packet dumps with live waveforms,
 * radial gauges, and visual range meters.
 */
@Composable
fun RfStatsScreen(
    telemetry: PacketTelemetry,
    localIpAddress: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "signalOscilloscope")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "wavePhase"
    )

    val signalStrengthFraction = when {
        telemetry.rssiDbm == 0 -> 0.88f
        telemetry.rssiDbm > -52 -> 0.95f
        telemetry.rssiDbm > -65 -> 0.78f
        telemetry.rssiDbm > -75 -> 0.58f
        else -> 0.40f
    }

    val stabilityFraction = when {
        telemetry.pingMs == 0 -> 0.92f
        telemetry.pingMs < 15 -> 0.96f
        telemetry.pingMs < 35 -> 0.82f
        else -> 0.65f
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Live RF Signal Oscilloscope Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(EchoLinkColors.SurfaceContainerLow)
                .border(1.dp, EchoLinkColors.ArmorStroke, RoundedCornerShape(24.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiTethering,
                        contentDescription = null,
                        tint = EchoLinkColors.CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Live Signal Wave",
                        style = EchoLinkTypography.HeadlineSm,
                        color = EchoLinkColors.TextOnSurface
                    )
                }
                VisualSignalBars(
                    strength0To4 = if (signalStrengthFraction > 0.75f) 4 else 3,
                    activeColor = EchoLinkColors.EmeraldPulse
                )
            }

            // Smooth Oscilloscope Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(EchoLinkColors.SurfaceContainerLowest)
                    .padding(12.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val midY = h / 2f

                    val primaryPath = Path()
                    val harmonicPath = Path()
                    val steps = 72
                    for (i in 0..steps) {
                        val x = (i.toFloat() / steps) * w
                        val angle = (i.toFloat() / steps) * 2.5f * PI.toFloat()
                        val y1 = midY + sin(angle + wavePhase) * (h * 0.34f * signalStrengthFraction)
                        val y2 = midY + sin(angle * 1.6f - wavePhase) * (h * 0.22f * stabilityFraction)
                        if (i == 0) {
                            primaryPath.moveTo(x, y1)
                            harmonicPath.moveTo(x, y2)
                        } else {
                            primaryPath.lineTo(x, y1)
                            harmonicPath.lineTo(x, y2)
                        }
                    }

                    drawPath(
                        path = harmonicPath,
                        color = EchoLinkColors.EmeraldPulse.copy(alpha = 0.35f),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawPath(
                        path = primaryPath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                EchoLinkColors.CyberCyan.copy(alpha = 0.4f),
                                EchoLinkColors.CyberCyan,
                                EchoLinkColors.EmeraldPulse
                            )
                        ),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }

        // 2. Visual Radial Gauges (Signal Strength, Responsiveness, Encryption)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            VisualArcGaugeCard(
                title = "Signal",
                subtitle = "Strong",
                fraction = signalStrengthFraction,
                accentColor = EchoLinkColors.CyberCyan,
                modifier = Modifier.weight(1f)
            )
            VisualArcGaugeCard(
                title = "Stability",
                subtitle = "Smooth",
                fraction = stabilityFraction,
                accentColor = EchoLinkColors.EmeraldPulse,
                modifier = Modifier.weight(1f)
            )
            VisualArcGaugeCard(
                title = "Security",
                subtitle = "Encrypted",
                fraction = 1.0f,
                accentColor = EchoLinkColors.PrimaryLight,
                modifier = Modifier.weight(1f)
            )
        }

        // 3. Visual Wireless Range Matrix (Bars instead of raw numbers)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(EchoLinkColors.SurfaceContainerLow)
                .border(1.dp, EchoLinkColors.ArmorStroke, RoundedCornerShape(24.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Active Radio Channels",
                style = EchoLinkTypography.HeadlineSm,
                color = EchoLinkColors.TextOnSurface
            )

            val tierRangeFractions = listOf(0.95f, 0.85f, 0.72f, 0.50f)
            TransportTier.entries.forEachIndexed { idx, tier ->
                val fillFrac = tierRangeFractions.getOrElse(idx) { 0.7f }
                val barColor = Color(tier.colorHex)
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tier.fullName,
                            style = EchoLinkTypography.BodySm.copy(fontWeight = FontWeight.Medium),
                            color = EchoLinkColors.TextOnSurface
                        )
                        VisualSignalBars(
                            strength0To4 = 4 - (idx / 2),
                            activeColor = barColor
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(CircleShape)
                            .background(EchoLinkColors.SurfaceContainerHighest)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fillFrac)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(barColor)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VisualArcGaugeCard(
    title: String,
    subtitle: String,
    fraction: Float,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val animFrac by animateFloatAsState(
        targetValue = fraction.coerceIn(0.1f, 1.0f),
        animationSpec = tween(500),
        label = "gaugeFrac"
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(EchoLinkColors.SurfaceContainerLow)
            .border(1.dp, EchoLinkColors.ArmorStroke, RoundedCornerShape(20.dp))
            .padding(vertical = 14.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier.size(64.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 6.dp.toPx()
                drawArc(
                    color = EchoLinkColors.SurfaceContainerHighest,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round),
                    size = Size(size.width, size.height)
                )
                drawArc(
                    color = accentColor,
                    startAngle = 135f,
                    sweepAngle = 270f * animFrac,
                    useCenter = false,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round),
                    size = Size(size.width, size.height)
                )
            }
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = EchoLinkTypography.BodySm.copy(fontWeight = FontWeight.SemiBold),
                color = EchoLinkColors.TextOnSurface
            )
            Text(
                text = subtitle,
                style = EchoLinkTypography.BodySm.copy(fontSize = 11.sp),
                color = accentColor
            )
        }
    }
}

/**
 * Phone-Optimized Audio Studio Screen: Uses live waveforms, smooth sliders, and visual toggles
 * instead of raw technical numbers.
 */
@Composable
fun AudioEngineSettingsScreen(
    sampleRateHz: Int,
    frameSizeMs: Int,
    opusBitrateKbps: Int,
    loopbackActive: Boolean,
    proximityGateActive: Boolean,
    spatialAudioActive: Boolean,
    spectrumBands: List<Float>,
    peerRxGainPercent: Int,
    onUpdateConfig: (Int, Int, Int) -> Unit,
    onUpdateRxGain: (Float) -> Unit,
    onToggleLoopback: () -> Unit,
    onToggleProximityGate: () -> Unit,
    onToggleSpatialAudio: () -> Unit
) {
    val claritySliderValue = when {
        sampleRateHz >= 48000 -> 1.0f
        sampleRateHz >= 24000 -> 0.5f
        else -> 0.0f
    }
    val speedSliderValue = if (frameSizeMs <= 10) 1.0f else 0.0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Live Microphone Waveform Monitor Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(EchoLinkColors.SurfaceContainerLow)
                .border(1.dp, EchoLinkColors.ArmorStroke, RoundedCornerShape(24.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = if (loopbackActive) EchoLinkColors.EmeraldPulse else EchoLinkColors.CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (loopbackActive) "Listening to Mic..." else "Voice Monitor",
                        style = EchoLinkTypography.HeadlineSm,
                        color = EchoLinkColors.TextOnSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (loopbackActive) EchoLinkColors.EmeraldPulse.copy(alpha = 0.18f)
                            else EchoLinkColors.SurfaceContainerHighest
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (loopbackActive) "LIVE" else "STANDBY",
                        style = EchoLinkTypography.BodySm.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = if (loopbackActive) EchoLinkColors.EmeraldPulse else EchoLinkColors.TextOnSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(EchoLinkColors.SurfaceContainerLowest)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                LiveWaveformStrip(
                    bands = spectrumBands,
                    height = 46.dp
                )
            }
        }

        // 2. Interactive Visual Sliders Card (Clarity, Response Speed, Output Gain)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(EchoLinkColors.SurfaceContainerLow)
                .border(1.dp, EchoLinkColors.ArmorStroke, RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Voice Clarity Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = EchoLinkColors.CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Voice Clarity",
                            style = EchoLinkTypography.BodyMd.copy(fontWeight = FontWeight.SemiBold),
                            color = EchoLinkColors.TextOnSurface
                        )
                    }
                    Text(
                        text = when {
                            sampleRateHz >= 48000 -> "Studio HD"
                            sampleRateHz >= 24000 -> "Balanced"
                            else -> "Battery Saver"
                        },
                        style = EchoLinkTypography.BodySm,
                        color = EchoLinkColors.CyberCyan
                    )
                }
                Slider(
                    value = claritySliderValue,
                    onValueChange = { v ->
                        val nextSr = when {
                            v > 0.66f -> 48000
                            v > 0.33f -> 24000
                            else -> 16000
                        }
                        onUpdateConfig(nextSr, frameSizeMs, opusBitrateKbps)
                    },
                    steps = 1,
                    colors = SliderDefaults.colors(
                        thumbColor = EchoLinkColors.CyberCyan,
                        activeTrackColor = EchoLinkColors.CyberCyan,
                        inactiveTrackColor = EchoLinkColors.SurfaceContainerHighest
                    )
                )
            }

            // Response Speed Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = EchoLinkColors.EmeraldPulse,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Response Speed",
                            style = EchoLinkTypography.BodyMd.copy(fontWeight = FontWeight.SemiBold),
                            color = EchoLinkColors.TextOnSurface
                        )
                    }
                    Text(
                        text = if (frameSizeMs <= 10) "Ultra-Fast" else "Smooth",
                        style = EchoLinkTypography.BodySm,
                        color = EchoLinkColors.EmeraldPulse
                    )
                }
                Slider(
                    value = speedSliderValue,
                    onValueChange = { v ->
                        val nextMs = if (v >= 0.5f) 10 else 20
                        onUpdateConfig(sampleRateHz, nextMs, opusBitrateKbps)
                    },
                    steps = 0,
                    colors = SliderDefaults.colors(
                        thumbColor = EchoLinkColors.EmeraldPulse,
                        activeTrackColor = EchoLinkColors.EmeraldPulse,
                        inactiveTrackColor = EchoLinkColors.SurfaceContainerHighest
                    )
                )
            }

            // Output Volume Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = EchoLinkColors.PrimaryLight,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Output Boost",
                            style = EchoLinkTypography.BodyMd.copy(fontWeight = FontWeight.SemiBold),
                            color = EchoLinkColors.TextOnSurface
                        )
                    }
                    Text(
                        text = if (peerRxGainPercent > 110) "Boosted" else "Standard",
                        style = EchoLinkTypography.BodySm,
                        color = EchoLinkColors.PrimaryLight
                    )
                }
                Slider(
                    value = (peerRxGainPercent / 150f).coerceIn(0f, 1f),
                    onValueChange = onUpdateRxGain,
                    colors = SliderDefaults.colors(
                        thumbColor = EchoLinkColors.PrimaryLight,
                        activeTrackColor = EchoLinkColors.PrimaryLight,
                        inactiveTrackColor = EchoLinkColors.SurfaceContainerHighest
                    )
                )
            }
        }

        // 3. Acoustic Enhancement Toggles (Noise Gate & Spatial Audio)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CompactAudioFeatureToggle(
                title = "Noise Gate",
                subtitle = if (proximityGateActive) "On" else "Off",
                icon = Icons.Default.Hearing,
                active = proximityGateActive,
                accentColor = EchoLinkColors.EmeraldPulse,
                onClick = onToggleProximityGate,
                modifier = Modifier.weight(1f)
            )
            CompactAudioFeatureToggle(
                title = "3D Spatial",
                subtitle = if (spatialAudioActive) "On" else "Off",
                icon = Icons.Default.SpatialAudio,
                active = spatialAudioActive,
                accentColor = EchoLinkColors.CyberCyan,
                onClick = onToggleSpatialAudio,
                modifier = Modifier.weight(1f)
            )
        }

        // 4. Hardware Mic Test Button
        Button(
            onClick = onToggleLoopback,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (loopbackActive) EchoLinkColors.EmeraldPulse else EchoLinkColors.SurfaceContainerHigh,
                contentColor = if (loopbackActive) EchoLinkColors.ObsidianCanvas else EchoLinkColors.PrimaryLight
            )
        ) {
            Text(
                text = if (loopbackActive) "Stop Microphone Test" else "Test Microphone & Waveform",
                style = EchoLinkTypography.HeadlineSm.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp)
            )
        }
    }
}

@Composable
private fun CompactAudioFeatureToggle(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(EchoLinkColors.SurfaceContainerLow)
            .border(
                1.dp,
                if (active) accentColor.copy(alpha = 0.5f) else EchoLinkColors.ArmorStroke,
                RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (active) accentColor else EchoLinkColors.TextOnSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = title,
                    style = EchoLinkTypography.BodySm.copy(fontWeight = FontWeight.SemiBold),
                    color = EchoLinkColors.TextOnSurface
                )
                Text(
                    text = subtitle,
                    style = EchoLinkTypography.BodySm.copy(fontSize = 11.sp),
                    color = if (active) accentColor else EchoLinkColors.TextOnSurfaceVariant
                )
            }
        }
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(20.dp)
                .clip(CircleShape)
                .background(if (active) accentColor.copy(alpha = 0.25f) else EchoLinkColors.SurfaceContainerHighest)
                .padding(2.dp),
            contentAlignment = if (active) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (active) accentColor else EchoLinkColors.TextSecondary)
            )
        }
    }
}
