package com.antigravity.echolink.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.SpatialAudio
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.echolink.model.IntercomMode
import com.antigravity.echolink.model.PacketTelemetry
import com.antigravity.echolink.model.PeerNode
import com.antigravity.echolink.ui.components.LiveWaveformStrip
import com.antigravity.echolink.ui.components.TacticalAvatarBadge
import com.antigravity.echolink.ui.components.VisualSignalBars
import com.antigravity.echolink.ui.theme.EchoLinkColors
import com.antigravity.echolink.ui.theme.EchoLinkTypography

/**
 * Screen 4: Modular, JIT-Optimized Active Voice Intercom Screen.
 * Decomposed into lightweight sub-composables to avoid Android ART bytecode method limits.
 */
@Composable
fun ActiveVoiceIntercomScreen(
    activePeers: List<PeerNode>,
    talkTargetPeerIp: String?,
    telemetry: PacketTelemetry,
    intercomMode: IntercomMode,
    isMicMuted: Boolean,
    isPttTransmitting: Boolean,
    proximityNoiseGateActive: Boolean,
    spatialAudioActive: Boolean,
    peerRxGainPercent: Int,
    isRxBoost3dB: Boolean,
    spectrumBands: List<Float>,
    liveVoiceDbFs: Int,
    onSelectTalkTarget: (String?) -> Unit,
    onRenamePeer: (String, String) -> Unit,
    onRemovePeerFromRoom: (String) -> Unit,
    onSelectMode: (IntercomMode) -> Unit,
    onToggleMute: () -> Unit,
    onPttPressChanged: (Boolean) -> Unit,
    onToggleProximityGate: () -> Unit,
    onToggleSpatialAudio: () -> Unit,
    onUpdateRxGain: (Float) -> Unit,
    onToggleRxBoost: () -> Unit,
    onAddPeer: () -> Unit,
    onDisconnect: () -> Unit
) {
    val primaryPeer = activePeers.find { it.ipAddress == talkTargetPeerIp }
        ?: activePeers.firstOrNull()
        ?: PeerNode(
            id = 0,
            name = "Awaiting Peer",
            roleTag = "Direct P2P Channel",
            nodeCode = "Ready to talk",
            distanceMeters = 0,
            transport = com.antigravity.echolink.model.TransportTier.WIFI_DIRECT,
            ipAddress = "0.0.0.0"
        )
    val isAllCallMode = (talkTargetPeerIp == null)
    val isSpeakingNow = !isMicMuted && liveVoiceDbFs > -42

    val signalStrength = when {
        telemetry.rssiDbm == 0 -> 4
        telemetry.rssiDbm > -55 -> 4
        telemetry.rssiDbm > -68 -> 3
        telemetry.rssiDbm > -78 -> 2
        else -> 1
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Status Bar
        IntercomStatusBar(primaryPeer = primaryPeer, signalStrength = signalStrength)

        // 2. Central Peer Card (Avatar, Waveform, Routing Strip, Volume)
        PeerVoiceCard(
            primaryPeer = primaryPeer,
            activePeers = activePeers,
            talkTargetPeerIp = talkTargetPeerIp,
            isAllCallMode = isAllCallMode,
            isSpeakingNow = isSpeakingNow,
            spectrumBands = spectrumBands,
            peerRxGainPercent = peerRxGainPercent,
            isRxBoost3dB = isRxBoost3dB,
            onSelectTalkTarget = onSelectTalkTarget,
            onRenamePeer = onRenamePeer,
            onRemovePeerFromRoom = onRemovePeerFromRoom,
            onUpdateRxGain = onUpdateRxGain,
            onToggleRxBoost = onToggleRxBoost,
            onAddPeer = onAddPeer
        )

        // 3. Mode Selector (Open Mic vs PTT)
        IntercomModeSwitcher(intercomMode = intercomMode, onSelectMode = onSelectMode)

        // 4. Control Deck
        if (intercomMode == IntercomMode.OPEN_MIC) {
            OpenMicDeck(
                isMicMuted = isMicMuted,
                proximityNoiseGateActive = proximityNoiseGateActive,
                spatialAudioActive = spatialAudioActive,
                onToggleMute = onToggleMute,
                onToggleProximityGate = onToggleProximityGate,
                onToggleSpatialAudio = onToggleSpatialAudio,
                onAddPeer = onAddPeer
            )
        } else {
            PushToTalkDeck(
                isPttTransmitting = isPttTransmitting,
                onPttPressChanged = onPttPressChanged
            )
        }

        // 5. End Session Button
        EndSessionButton(onDisconnect = onDisconnect)
    }
}

@Composable
private fun IntercomStatusBar(
    primaryPeer: PeerNode,
    signalStrength: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(EchoLinkColors.SurfaceContainerLow)
            .border(1.dp, EchoLinkColors.ArmorStroke, CircleShape)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VisualSignalBars(
                strength0To4 = signalStrength,
                activeColor = EchoLinkColors.EmeraldPulse
            )
            Text(
                text = primaryPeer.transport.fullName,
                style = EchoLinkTypography.BodySm.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                color = EchoLinkColors.TextOnSurface
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = EchoLinkColors.CyberCyan,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = "Encrypted Voice",
                style = EchoLinkTypography.BodySm.copy(fontSize = 12.sp),
                color = EchoLinkColors.CyberCyan
            )
        }
    }
}

@Composable
private fun PeerVoiceCard(
    primaryPeer: PeerNode,
    activePeers: List<PeerNode>,
    talkTargetPeerIp: String?,
    isAllCallMode: Boolean,
    isSpeakingNow: Boolean,
    spectrumBands: List<Float>,
    peerRxGainPercent: Int,
    isRxBoost3dB: Boolean,
    onSelectTalkTarget: (String?) -> Unit,
    onRenamePeer: (String, String) -> Unit,
    onRemovePeerFromRoom: (String) -> Unit,
    onUpdateRxGain: (Float) -> Unit,
    onToggleRxBoost: () -> Unit,
    onAddPeer: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vadRingTransition")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isSpeakingNow) 1.18f else 1.02f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "ringScale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(EchoLinkColors.SurfaceContainerLow)
            .border(1.dp, EchoLinkColors.ArmorStroke, RoundedCornerShape(24.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Dynamic Halo Avatar
        Box(
            modifier = Modifier.size(136.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .scale(ringScale)
                    .clip(CircleShape)
                    .background(EchoLinkColors.EmeraldPulse.copy(alpha = if (isSpeakingNow) 0.18f else 0.06f))
            )
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .scale(ringScale * 0.96f)
                    .clip(CircleShape)
                    .background(EchoLinkColors.EmeraldPulse.copy(alpha = if (isSpeakingNow) 0.28f else 0.10f))
            )
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(EchoLinkColors.SurfaceContainer)
                    .border(
                        2.dp,
                        if (isSpeakingNow) EchoLinkColors.EmeraldPulse else EchoLinkColors.CyberCyan.copy(alpha = 0.6f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                TacticalAvatarBadge(
                    name = primaryPeer.name,
                    accentColor = EchoLinkColors.EmeraldPulse,
                    size = 92.dp
                )
            }
        }

        // Peer Name & Status
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(enabled = primaryPeer.ipAddress != "0.0.0.0") {
                        onRenamePeer(primaryPeer.ipAddress, primaryPeer.name)
                    }
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isAllCallMode && activePeers.size > 1) {
                        "All Room (${activePeers.size} Peers)"
                    } else {
                        primaryPeer.name
                    },
                    style = EchoLinkTypography.HeadlineSm.copy(fontSize = 18.sp),
                    color = EchoLinkColors.TextOnSurface
                )
                if (primaryPeer.ipAddress != "0.0.0.0") {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(EchoLinkColors.SurfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Rename Connected Device",
                            tint = EchoLinkColors.PrimaryLight,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
            Text(
                text = when {
                    isSpeakingNow && isAllCallMode && activePeers.size > 1 -> "Broadcasting to All..."
                    isSpeakingNow && !isAllCallMode -> "Talking 1-on-1 to ${primaryPeer.name}..."
                    isSpeakingNow -> "Speaking..."
                    !isAllCallMode -> "1-on-1 Direct Mode • Tap All to broadcast"
                    activePeers.size > 1 -> "All-Call Mode • Tap a person to talk 1-on-1"
                    else -> primaryPeer.roleTag
                },
                style = EchoLinkTypography.BodySm,
                color = if (isSpeakingNow) EchoLinkColors.EmeraldPulse else EchoLinkColors.TextOnSurfaceVariant
            )
        }

        // Routing Strip
        RoomRoutingStrip(
            activePeers = activePeers,
            talkTargetPeerIp = talkTargetPeerIp,
            isAllCallMode = isAllCallMode,
            onSelectTalkTarget = onSelectTalkTarget,
            onRenamePeer = onRenamePeer,
            onRemovePeerFromRoom = onRemovePeerFromRoom,
            onAddPeer = onAddPeer
        )

        // Waveform Strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(EchoLinkColors.SurfaceContainerLowest)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            LiveWaveformStrip(
                bands = spectrumBands,
                height = 42.dp
            )
        }

        // Volume Slider
        VolumeControlCard(
            peerRxGainPercent = peerRxGainPercent,
            isRxBoost3dB = isRxBoost3dB,
            onUpdateRxGain = onUpdateRxGain,
            onToggleRxBoost = onToggleRxBoost
        )
    }
}

@Composable
private fun RoomRoutingStrip(
    activePeers: List<PeerNode>,
    talkTargetPeerIp: String?,
    isAllCallMode: Boolean,
    onSelectTalkTarget: (String?) -> Unit,
    onRenamePeer: (String, String) -> Unit,
    onRemovePeerFromRoom: (String) -> Unit,
    onAddPeer: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // "Talk to All"
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(
                    if (isAllCallMode) EchoLinkColors.EmeraldPulse else EchoLinkColors.SurfaceContainer
                )
                .border(
                    1.dp,
                    if (isAllCallMode) EchoLinkColors.EmeraldPulse else EchoLinkColors.ArmorStroke,
                    CircleShape
                )
                .clickable { onSelectTalkTarget(null) }
                .padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Groups,
                contentDescription = "Talk to All",
                tint = if (isAllCallMode) EchoLinkColors.ObsidianCanvas else EchoLinkColors.TextOnSurfaceVariant,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = "Talk to All (${activePeers.size.coerceAtLeast(1)})",
                style = EchoLinkTypography.BodySm.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                ),
                color = if (isAllCallMode) EchoLinkColors.ObsidianCanvas else EchoLinkColors.TextOnSurface
            )
        }

        // Individual Peer Pills
        activePeers.forEach { meshPeer ->
            val isSelectedIndividual = (talkTargetPeerIp == meshPeer.ipAddress)
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (isSelectedIndividual) EchoLinkColors.CyberCyan else EchoLinkColors.SurfaceContainer
                    )
                    .border(
                        1.dp,
                        if (isSelectedIndividual) EchoLinkColors.CyberCyan else EchoLinkColors.ArmorStroke,
                        CircleShape
                    )
                    .clickable { onSelectTalkTarget(meshPeer.ipAddress) }
                    .padding(start = 10.dp, end = 8.dp, top = 5.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = if (isSelectedIndividual) EchoLinkColors.ObsidianCanvas else EchoLinkColors.EmeraldPulse,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = meshPeer.name,
                    style = EchoLinkTypography.BodySm.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    ),
                    color = if (isSelectedIndividual) EchoLinkColors.ObsidianCanvas else EchoLinkColors.TextPrimary
                )
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Rename ${meshPeer.name}",
                    tint = if (isSelectedIndividual) {
                        EchoLinkColors.ObsidianCanvas.copy(alpha = 0.8f)
                    } else {
                        EchoLinkColors.TextOnSurfaceVariant
                    },
                    modifier = Modifier
                        .size(12.dp)
                        .clickable { onRenamePeer(meshPeer.ipAddress, meshPeer.name) }
                )
                if (activePeers.size > 1) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove ${meshPeer.name}",
                        tint = if (isSelectedIndividual) {
                            EchoLinkColors.ObsidianCanvas
                        } else {
                            EchoLinkColors.ErrorLight
                        },
                        modifier = Modifier
                            .size(13.dp)
                            .clickable { onRemovePeerFromRoom(meshPeer.ipAddress) }
                    )
                }
            }
        }

        // "+ Add" Pill
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(EchoLinkColors.SurfaceContainerHigh)
                .border(1.dp, EchoLinkColors.CyberCyan.copy(alpha = 0.5f), CircleShape)
                .clickable { onAddPeer() }
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.GroupAdd,
                contentDescription = "Add Person to Room",
                tint = EchoLinkColors.CyberCyan,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "+ Add",
                style = EchoLinkTypography.BodySm.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                ),
                color = EchoLinkColors.CyberCyan
            )
        }
    }
}

@Composable
private fun VolumeControlCard(
    peerRxGainPercent: Int,
    isRxBoost3dB: Boolean,
    onUpdateRxGain: (Float) -> Unit,
    onToggleRxBoost: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EchoLinkColors.SurfaceContainer)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Voice Volume",
                style = EchoLinkTypography.BodySm.copy(fontWeight = FontWeight.Medium),
                color = EchoLinkColors.TextOnSurface
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (isRxBoost3dB) EchoLinkColors.CyberCyan else EchoLinkColors.SurfaceContainerHighest
                    )
                    .clickable { onToggleRxBoost() }
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (isRxBoost3dB) "Boost On" else "Boost",
                    style = EchoLinkTypography.BodySm.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                    color = if (isRxBoost3dB) EchoLinkColors.ObsidianCanvas else EchoLinkColors.TextOnSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeDown,
                contentDescription = null,
                tint = EchoLinkColors.TextOnSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Slider(
                value = (peerRxGainPercent / 150f).coerceIn(0f, 1f),
                onValueChange = onUpdateRxGain,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = EchoLinkColors.PrimaryLight,
                    activeTrackColor = EchoLinkColors.CyberCyan,
                    inactiveTrackColor = EchoLinkColors.SurfaceContainerHighest
                )
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null,
                tint = EchoLinkColors.PrimaryLight,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun IntercomModeSwitcher(
    intercomMode: IntercomMode,
    onSelectMode: (IntercomMode) -> Unit
) {
    val isOpenMic = (intercomMode == IntercomMode.OPEN_MIC)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(EchoLinkColors.SurfaceContainerLowest)
            .border(1.dp, EchoLinkColors.ArmorStroke, CircleShape)
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .clip(CircleShape)
                .background(if (isOpenMic) EchoLinkColors.SecondaryContainer else Color.Transparent)
                .clickable { onSelectMode(IntercomMode.OPEN_MIC) },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                tint = if (isOpenMic) EchoLinkColors.OnSecondaryContainer else EchoLinkColors.TextOnSurfaceVariant,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Open Mic",
                style = EchoLinkTypography.BodyMd.copy(fontWeight = FontWeight.SemiBold),
                color = if (isOpenMic) EchoLinkColors.OnSecondaryContainer else EchoLinkColors.TextOnSurfaceVariant
            )
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .clip(CircleShape)
                .background(if (!isOpenMic) EchoLinkColors.CyberCyan else Color.Transparent)
                .clickable { onSelectMode(IntercomMode.PUSH_TO_TALK) },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.TouchApp,
                contentDescription = null,
                tint = if (!isOpenMic) EchoLinkColors.ObsidianCanvas else EchoLinkColors.TextOnSurfaceVariant,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Push-to-Talk",
                style = EchoLinkTypography.BodyMd.copy(fontWeight = FontWeight.SemiBold),
                color = if (!isOpenMic) EchoLinkColors.ObsidianCanvas else EchoLinkColors.TextOnSurfaceVariant
            )
        }
    }
}

@Composable
private fun OpenMicDeck(
    isMicMuted: Boolean,
    proximityNoiseGateActive: Boolean,
    spatialAudioActive: Boolean,
    onToggleMute: () -> Unit,
    onToggleProximityGate: () -> Unit,
    onToggleSpatialAudio: () -> Unit,
    onAddPeer: () -> Unit
) {
    val micAccent = if (isMicMuted) EchoLinkColors.CombatCrimson else EchoLinkColors.EmeraldPulse

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Noise Gate Quick Toggle
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.clickable { onToggleProximityGate() }
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(EchoLinkColors.SurfaceContainerHigh)
                    .border(
                        1.dp,
                        if (proximityNoiseGateActive) EchoLinkColors.EmeraldPulse else EchoLinkColors.ArmorStroke,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Hearing,
                    contentDescription = "Noise Gate",
                    tint = if (proximityNoiseGateActive) EchoLinkColors.EmeraldPulse else EchoLinkColors.TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = "Noise Gate",
                style = EchoLinkTypography.BodySm.copy(fontSize = 11.sp),
                color = if (proximityNoiseGateActive) EchoLinkColors.EmeraldPulse else EchoLinkColors.TextOnSurfaceVariant
            )
        }

        // Primary Mic Mute / Live Button
        Column(
            modifier = Modifier
                .size(82.dp)
                .shadow(18.dp, CircleShape, spotColor = micAccent)
                .clip(CircleShape)
                .background(EchoLinkColors.SurfaceContainerHigh)
                .border(2.dp, micAccent, CircleShape)
                .clickable { onToggleMute() },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = "Toggle Mic",
                tint = micAccent,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = if (isMicMuted) "Muted" else "Live",
                style = EchoLinkTypography.BodySm.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                color = micAccent
            )
        }

        // 3D Spatial Audio Toggle
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.clickable { onToggleSpatialAudio() }
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(EchoLinkColors.SurfaceContainerHigh)
                    .border(
                        1.dp,
                        if (spatialAudioActive) EchoLinkColors.CyberCyan else EchoLinkColors.ArmorStroke,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SpatialAudio,
                    contentDescription = "3D Audio",
                    tint = if (spatialAudioActive) EchoLinkColors.PrimaryLight else EchoLinkColors.TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = "3D Audio",
                style = EchoLinkTypography.BodySm.copy(fontSize = 11.sp),
                color = if (spatialAudioActive) EchoLinkColors.PrimaryLight else EchoLinkColors.TextSecondary
            )
        }

        // Add Peer Button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.clickable { onAddPeer() }
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(EchoLinkColors.SurfaceContainerHigh)
                    .border(1.dp, EchoLinkColors.ArmorStroke, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GroupAdd,
                    contentDescription = "Add Peer",
                    tint = EchoLinkColors.TextOnSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = "Add Peer",
                style = EchoLinkTypography.BodySm.copy(fontSize = 11.sp),
                color = EchoLinkColors.TextOnSurfaceVariant
            )
        }
    }
}

@Composable
private fun PushToTalkDeck(
    isPttTransmitting: Boolean,
    onPttPressChanged: (Boolean) -> Unit
) {
    val pttBg = if (isPttTransmitting) EchoLinkColors.EmeraldPulse else EchoLinkColors.CyberCyan
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
            .shadow(18.dp, RoundedCornerShape(24.dp), spotColor = pttBg)
            .clip(RoundedCornerShape(24.dp))
            .background(pttBg)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onPttPressChanged(true)
                        tryAwaitRelease()
                        onPttPressChanged(false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.RadioButtonChecked,
                contentDescription = null,
                tint = EchoLinkColors.ObsidianCanvas,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = if (isPttTransmitting) "Transmitting Voice..." else "Hold or Press Volume Key to Talk",
                style = EchoLinkTypography.HeadlineSm.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                color = EchoLinkColors.ObsidianCanvas
            )
        }
    }
}

@Composable
private fun EndSessionButton(onDisconnect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(CircleShape)
            .background(EchoLinkColors.ErrorLight.copy(alpha = 0.16f))
            .border(1.dp, EchoLinkColors.CombatCrimson.copy(alpha = 0.5f), CircleShape)
            .clickable { onDisconnect() },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CallEnd,
            contentDescription = null,
            tint = EchoLinkColors.ErrorLight,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "End Voice Link",
            style = EchoLinkTypography.HeadlineSm.copy(fontSize = 15.sp),
            color = EchoLinkColors.ErrorLight
        )
    }
}
