package com.antigravity.echolink.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.echolink.model.PeerNode
import com.antigravity.echolink.model.RecentPeer
import com.antigravity.echolink.ui.components.TacticalAvatarBadge
import com.antigravity.echolink.ui.components.VisualSignalBars
import com.antigravity.echolink.ui.theme.EchoLinkColors
import com.antigravity.echolink.ui.theme.EchoLinkTypography

@Composable
fun SonarRadarHomeScreen(
    discoveredPeers: List<PeerNode>,
    recentPeers: List<RecentPeer>,
    localIp: String,
    wifiFrequencyMhz: Int,
    toastBanner: Pair<String, String>?,
    onTapPeerNode: (PeerNode) -> Unit,
    onTapRecentPeer: (RecentPeer) -> Unit,
    onRemoveRecentPeer: (String) -> Unit,
    onClearRecentPeers: () -> Unit,
    onRenamePeer: (String, String) -> Unit,
    onCreateCallLink: () -> Unit,
    onScanCallLink: () -> Unit,
    onDismissToast: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radarSweepTransition")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3400, easing = LinearEasing), RepeatMode.Restart),
        label = "sweepAngle"
    )
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.42f,
        animationSpec = infiniteRepeatable(tween(1300), RepeatMode.Reverse),
        label = "pulseRadius"
    )

    val signalBars = when {
        wifiFrequencyMhz >= 4900 -> 4
        wifiFrequencyMhz >= 2400 -> 3
        localIp != "0.0.0.0" -> 3
        else -> 2
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Clean Top Status Strip with Visual Signal Bars (No raw MHz / port numbers)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(EchoLinkColors.SurfaceContainerLow)
                    .border(1.dp, EchoLinkColors.ArmorStroke, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(EchoLinkColors.EmeraldPulse)
                )
                Text(
                    text = if (discoveredPeers.isEmpty()) "Radar Scanning" else "Peers Nearby",
                    style = EchoLinkTypography.BodySm.copy(fontWeight = FontWeight.Medium, fontSize = 12.sp),
                    color = EchoLinkColors.TextOnSurface
                )
            }

            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(EchoLinkColors.SurfaceContainerLow)
                    .border(1.dp, EchoLinkColors.ArmorStroke, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VisualSignalBars(
                    strength0To4 = signalBars,
                    activeColor = EchoLinkColors.CyberCyan
                )
                Text(
                    text = if (wifiFrequencyMhz >= 4900) "High-Speed P2P" else "Local Mesh",
                    style = EchoLinkTypography.BodySm.copy(fontSize = 12.sp),
                    color = EchoLinkColors.TextOnSurfaceVariant
                )
            }
        }

        // 2. Centered Phone-Proportioned Sonar Radar
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(268.dp)
                    .shadow(24.dp, CircleShape, spotColor = EchoLinkColors.CyberCyan.copy(alpha = 0.22f))
                    .clip(CircleShape)
                    .background(EchoLinkColors.SurfaceContainerLowest)
                    .border(1.dp, EchoLinkColors.ArmorStroke, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val maxR = size.minDimension / 2f
                    val gridColor = EchoLinkColors.OutlineVariant.copy(alpha = 0.4f)

                    drawLine(
                        color = gridColor,
                        start = Offset(c.x, 0f),
                        end = Offset(c.x, size.height),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, c.y),
                        end = Offset(size.width, c.y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )

                    drawCircle(
                        color = gridColor,
                        radius = maxR * 0.34f,
                        center = c,
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawCircle(
                        color = gridColor,
                        radius = maxR * 0.64f,
                        center = c,
                        style = Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 8f))
                        )
                    )
                    drawCircle(
                        color = gridColor,
                        radius = maxR * 0.90f,
                        center = c,
                        style = Stroke(width = 1.dp.toPx())
                    )

                    rotate(degrees = sweepAngle, pivot = c) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                0.0f to EchoLinkColors.CyberCyan.copy(alpha = 0.28f),
                                0.18f to EchoLinkColors.CyberCyan.copy(alpha = 0.04f),
                                0.25f to Color.Transparent,
                                1.0f to Color.Transparent,
                                center = c
                            ),
                            startAngle = 0f,
                            sweepAngle = 72f,
                            useCenter = true,
                            topLeft = Offset.Zero,
                            size = Size(size.width, size.height)
                        )
                    }

                    drawCircle(
                        color = EchoLinkColors.CyberCyan.copy(alpha = 0.14f),
                        radius = 28.dp.toPx() * pulseRadius,
                        center = c
                    )
                    drawCircle(
                        color = EchoLinkColors.CyberCyan.copy(alpha = 0.26f),
                        radius = 22.dp.toPx(),
                        center = c
                    )
                }

                // Local Center Node
                Box(contentAlignment = Alignment.BottomEnd) {
                    TacticalAvatarBadge(
                        name = "ME",
                        accentColor = EchoLinkColors.CyberCyan,
                        size = 46.dp
                    )
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(EchoLinkColors.SecondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = EchoLinkColors.OnSecondaryContainer,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }

                // Discovered Peer Nodes on Radar
                val nodeOffsets = listOf(
                    Pair(52.dp, 26.dp),
                    Pair((-36).dp, (-52).dp),
                    Pair((-40).dp, 76.dp)
                )
                discoveredPeers.take(3).forEachIndexed { idx, peer ->
                    val (ox, oy) = nodeOffsets.getOrElse(idx) { Pair(36.dp, (-36).dp) }
                    val accent = Color(peer.transport.colorHex)
                    Row(
                        modifier = Modifier
                            .offset(x = ox, y = oy)
                            .clip(CircleShape)
                            .clickable { onTapPeerNode(peer) }
                            .padding(3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TacticalAvatarBadge(
                            name = peer.name,
                            accentColor = accent,
                            size = 32.dp
                        )
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(EchoLinkColors.SurfaceContainerHigh.copy(alpha = 0.94f))
                                .border(0.8.dp, accent.copy(alpha = 0.5f), CircleShape)
                                .padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = peer.name,
                                style = EchoLinkTypography.BodySm.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                                color = EchoLinkColors.TextOnSurface
                            )
                            VisualSignalBars(
                                strength0To4 = if (peer.rssiDbm > -55) 4 else 3,
                                activeColor = accent
                            )
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Rename Device",
                                tint = EchoLinkColors.TextOnSurfaceVariant,
                                modifier = Modifier
                                    .size(13.dp)
                                    .clickable { onRenamePeer(peer.ipAddress, peer.name) }
                            )
                        }
                    }
                }
            }

            Text(
                text = if (discoveredPeers.isEmpty()) {
                    "Looking for nearby EchoLink devices..."
                } else {
                    "Tap a device on the radar to start talking"
                },
                style = EchoLinkTypography.BodySm,
                color = EchoLinkColors.TextOnSurfaceVariant
            )
        }

        // Optional Toast Notification Banner
        AnimatedVisibility(visible = toastBanner != null) {
            toastBanner?.let { (title, subtitle) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(EchoLinkColors.SurfaceContainerHigh)
                        .border(1.dp, EchoLinkColors.CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cable,
                            contentDescription = null,
                            tint = EchoLinkColors.EmeraldPulse
                        )
                        Column {
                            Text(
                                text = title,
                                style = EchoLinkTypography.BodyMd.copy(fontWeight = FontWeight.SemiBold),
                                color = EchoLinkColors.PrimaryLight,
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
                    IconButton(onClick = onDismissToast, modifier = Modifier.size(26.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = EchoLinkColors.TextOnSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 3. Clean Lower Action Dock (Host QR vs Scan QR + Optional Saved Peers)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(EchoLinkColors.SurfaceContainerLow)
                .border(1.dp, EchoLinkColors.ArmorStroke, RoundedCornerShape(24.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Host Room (QR)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(EchoLinkColors.SurfaceContainer)
                        .border(1.dp, EchoLinkColors.ArmorStroke, RoundedCornerShape(18.dp))
                        .clickable { onCreateCallLink() }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(EchoLinkColors.CyberCyan.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = null,
                            tint = EchoLinkColors.CyberCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Host Room",
                            style = EchoLinkTypography.HeadlineSm.copy(fontSize = 15.sp),
                            color = EchoLinkColors.TextOnSurface
                        )
                        Text(
                            text = "Show QR code",
                            style = EchoLinkTypography.BodySm.copy(fontSize = 12.sp),
                            color = EchoLinkColors.TextOnSurfaceVariant
                        )
                    }
                }

                // Join Room (Scan)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(EchoLinkColors.SurfaceContainer)
                        .border(1.dp, EchoLinkColors.ArmorStroke, RoundedCornerShape(18.dp))
                        .clickable { onScanCallLink() }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(EchoLinkColors.EmeraldPulse.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CenterFocusStrong,
                            contentDescription = null,
                            tint = EchoLinkColors.EmeraldPulse,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Join Room",
                            style = EchoLinkTypography.HeadlineSm.copy(fontSize = 15.sp),
                            color = EchoLinkColors.TextOnSurface
                        )
                        Text(
                            text = "Scan peer QR",
                            style = EchoLinkTypography.BodySm.copy(fontSize = 12.sp),
                            color = EchoLinkColors.TextOnSurfaceVariant
                        )
                    }
                }
            }

            // Only show Recent Peers if the user has actually connected to peers before
            if (recentPeers.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = EchoLinkColors.CyberCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Connected Devices",
                                style = EchoLinkTypography.BodySm.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                color = EchoLinkColors.TextOnSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(EchoLinkColors.ErrorLight.copy(alpha = 0.14f))
                                .clickable { onClearRecentPeers() }
                                .padding(horizontal = 9.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear History",
                                tint = EchoLinkColors.ErrorLight,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Clear History",
                                style = EchoLinkTypography.BodySm.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                color = EchoLinkColors.ErrorLight
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        recentPeers.forEach { recent ->
                            val accent = Color(recent.transport.colorHex)
                            Row(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(EchoLinkColors.SurfaceContainer)
                                    .border(1.dp, EchoLinkColors.ArmorStroke, CircleShape)
                                    .padding(start = 6.dp, end = 8.dp, top = 5.dp, bottom = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Row(
                                    modifier = Modifier.clickable { onTapRecentPeer(recent) },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    TacticalAvatarBadge(
                                        name = recent.name,
                                        accentColor = accent,
                                        size = 24.dp
                                    )
                                    Text(
                                        text = recent.name,
                                        style = EchoLinkTypography.BodySm.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        ),
                                        color = EchoLinkColors.TextOnSurface
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(EchoLinkColors.SurfaceContainerHighest)
                                        .clickable { onRenamePeer(recent.ipAddress, recent.name) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Rename",
                                        tint = EchoLinkColors.PrimaryLight,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(EchoLinkColors.ErrorLight.copy(alpha = 0.16f))
                                        .clickable { onRemoveRecentPeer(recent.ipAddress) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete from history",
                                        tint = EchoLinkColors.ErrorLight,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
