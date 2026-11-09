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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.echolink.model.HardwareReadinessState
import com.antigravity.echolink.ui.theme.EchoLinkColors
import com.antigravity.echolink.ui.theme.EchoLinkTypography
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun ReadinessCheckScreen(
    readinessState: HardwareReadinessState,
    sampleRateHz: Int,
    frameSizeMs: Int,
    onToggleWifiDirect: () -> Unit,
    onToggleOboeAudio: () -> Unit,
    onRequestPermissions: () -> Unit,
    onLaunchSonarRadar: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveAnim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "wavePhase"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Clean Animated Acoustic Wave Bridge Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(EchoLinkColors.SurfaceContainerLowest)
                .border(1.dp, EchoLinkColors.ArmorStroke, RoundedCornerShape(22.dp))
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(EchoLinkColors.SurfaceContainerHigh)
                            .border(1.dp, EchoLinkColors.CyberCyan.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = "You",
                            tint = EchoLinkColors.CyberCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "You",
                        style = EchoLinkTypography.BodySm.copy(fontSize = 11.sp),
                        color = EchoLinkColors.TextOnSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(92.dp)
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val midY = h / 2f

                        val echoPath = Path()
                        val mainPath = Path()
                        val steps = 60
                        for (i in 0..steps) {
                            val x = (i.toFloat() / steps) * w
                            val norm = (i.toFloat() / steps) * 2f * PI.toFloat()
                            val yMain = midY + sin(norm * 1.5f + phase) * (h * 0.28f)
                            val yEcho = midY + sin(norm * 1.5f - phase * 0.7f) * (h * 0.18f)
                            if (i == 0) {
                                mainPath.moveTo(x, yMain)
                                echoPath.moveTo(x, yEcho)
                            } else {
                                mainPath.lineTo(x, yMain)
                                echoPath.lineTo(x, yEcho)
                            }
                        }

                        drawPath(
                            path = echoPath,
                            color = EchoLinkColors.CyberCyan.copy(alpha = 0.28f),
                            style = Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 7f))
                            )
                        )

                        drawPath(
                            path = mainPath,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    EchoLinkColors.CyberCyan.copy(alpha = 0.3f),
                                    EchoLinkColors.CyberCyan,
                                    EchoLinkColors.EmeraldPulse
                                )
                            ),
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(EchoLinkColors.SurfaceContainerHigh)
                            .border(1.dp, EchoLinkColors.EmeraldPulse.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hearing,
                            contentDescription = "Peer",
                            tint = EchoLinkColors.EmeraldPulse,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = "Peer",
                        style = EchoLinkTypography.BodySm.copy(fontSize = 11.sp),
                        color = EchoLinkColors.TextOnSurfaceVariant
                    )
                }
            }
        }

        // 2. Clean Hardware & Permissions Checklist Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(EchoLinkColors.SurfaceContainerLow)
                .border(1.dp, EchoLinkColors.ArmorStroke, RoundedCornerShape(24.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val allPermsReady = readinessState.micPermissionGranted &&
                readinessState.bluetoothPermissionGranted &&
                readinessState.nearbyWifiPermissionGranted

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
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = EchoLinkColors.PrimaryFixedDim,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Device Readiness",
                        style = EchoLinkTypography.HeadlineSm,
                        color = EchoLinkColors.TextOnSurface
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(EchoLinkColors.SurfaceContainerLowest)
                        .clickable { onRequestPermissions() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (allPermsReady) "Ready" else "Grant Access",
                        style = EchoLinkTypography.BodySm.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                        color = if (allPermsReady) EchoLinkColors.EmeraldPulse else EchoLinkColors.AmberWarning
                    )
                }
            }

            // Row 1: Audio Endpoint
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(EchoLinkColors.SurfaceContainer)
                    .clickable { onRequestPermissions() }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(EchoLinkColors.SurfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BluetoothConnected,
                            contentDescription = null,
                            tint = if (readinessState.earbudsConnected) EchoLinkColors.EmeraldPulse else EchoLinkColors.CyberCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (readinessState.earbudsConnected) "Headset Connected" else "Audio Output",
                            style = EchoLinkTypography.BodyMd.copy(fontWeight = FontWeight.SemiBold),
                            color = EchoLinkColors.TextOnSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = readinessState.connectedAudioDeviceName,
                            style = EchoLinkTypography.BodySm,
                            color = EchoLinkColors.TextOnSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (readinessState.earbudsConnected) EchoLinkColors.EmeraldPulse else EchoLinkColors.CyberCyan,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Row 2: Nearby Wi-Fi Direct Discovery
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(EchoLinkColors.SurfaceContainer)
                    .clickable {
                        onRequestPermissions()
                        onToggleWifiDirect()
                    }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(EchoLinkColors.SurfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiTethering,
                            contentDescription = null,
                            tint = EchoLinkColors.CyberCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Direct Peer Discovery",
                            style = EchoLinkTypography.BodyMd.copy(fontWeight = FontWeight.SemiBold),
                            color = EchoLinkColors.TextOnSurface
                        )
                        Text(
                            text = "Offline Wi-Fi & Hotspot Radar",
                            style = EchoLinkTypography.BodySm,
                            color = EchoLinkColors.TextOnSurfaceVariant
                        )
                    }
                }
                TacticalCyberToggle(
                    checked = readinessState.wifiDirectRadarEnabled,
                    activeColor = EchoLinkColors.CyberCyan,
                    onToggle = {
                        onRequestPermissions()
                        onToggleWifiDirect()
                    }
                )
            }

            // Row 3: Low-Latency Voice Engine
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(EchoLinkColors.SurfaceContainer)
                    .clickable {
                        onRequestPermissions()
                        onToggleOboeAudio()
                    }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(EchoLinkColors.SurfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = EchoLinkColors.EmeraldPulse,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Low-Latency Voice",
                            style = EchoLinkTypography.BodyMd.copy(fontWeight = FontWeight.SemiBold),
                            color = EchoLinkColors.TextOnSurface
                        )
                        Text(
                            text = "Echo cancellation & noise suppression",
                            style = EchoLinkTypography.BodySm,
                            color = EchoLinkColors.TextOnSurfaceVariant
                        )
                    }
                }
                TacticalCyberToggle(
                    checked = readinessState.oboeLowLatencyEnabled,
                    activeColor = EchoLinkColors.EmeraldPulse,
                    onToggle = {
                        onRequestPermissions()
                        onToggleOboeAudio()
                    }
                )
            }
        }

        // 3. Primary Action Button
        Button(
            onClick = {
                onRequestPermissions()
                onLaunchSonarRadar()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .shadow(20.dp, CircleShape, spotColor = EchoLinkColors.CyberCyan),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = EchoLinkColors.CyberCyan,
                contentColor = EchoLinkColors.ObsidianCanvas
            )
        ) {
            if (readinessState.isSyncingNodes) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Starting Radar...",
                    style = EchoLinkTypography.HeadlineSm.copy(fontWeight = FontWeight.Bold),
                    color = EchoLinkColors.ObsidianCanvas
                )
            } else {
                Text(
                    text = "Open Sonar Radar",
                    style = EchoLinkTypography.HeadlineSm.copy(fontWeight = FontWeight.Bold),
                    color = EchoLinkColors.ObsidianCanvas
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun TacticalCyberToggle(
    checked: Boolean,
    activeColor: Color,
    onToggle: () -> Unit
) {
    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) 20f else 0f,
        animationSpec = tween(200),
        label = "thumbOffset"
    )
    Box(
        modifier = Modifier
            .width(48.dp)
            .height(28.dp)
            .clip(CircleShape)
            .background(EchoLinkColors.SurfaceContainerHighest)
            .clickable { onToggle() }
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(if (checked) activeColor else EchoLinkColors.TextSecondary)
        )
    }
}
