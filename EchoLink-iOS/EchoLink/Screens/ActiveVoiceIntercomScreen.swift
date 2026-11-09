import SwiftUI

struct ActiveVoiceIntercomScreen: View {
    let activePeers: [PeerNode]
    let talkTargetPeerIp: String?
    let telemetry: PacketTelemetry
    let intercomMode: IntercomMode
    let isMicMuted: Bool
    let isPttTransmitting: Bool
    let proximityNoiseGateActive: Bool
    let spatialAudioActive: Bool
    let peerRxGainPercent: Int
    let isRxBoost3dB: Bool
    let spectrumBands: [Float]
    let liveVoiceDbFs: Int
    let onSelectTalkTarget: (String?) -> Void
    let onRenamePeer: (String, String) -> Void
    let onRemovePeerFromRoom: (String) -> Void
    let onSelectMode: (IntercomMode) -> Void
    let onToggleMute: () -> Void
    let onPttPressChanged: (Bool) -> Void
    let onToggleProximityGate: () -> Void
    let onToggleSpatialAudio: () -> Void
    let onUpdateRxGain: (Float) -> Void
    let onToggleRxBoost: () -> Void
    let onAddPeer: () -> Void
    let onDisconnect: () -> Void

    private var primaryPeer: PeerNode {
        if let targetIp = talkTargetPeerIp,
           let matched = activePeers.first(where: { $0.ipAddress == targetIp }) {
            return matched
        }
        return activePeers.first ?? PeerNode(
            id: 0,
            name: "Awaiting Peer",
            roleTag: "Direct P2P Channel",
            nodeCode: "Ready to talk",
            distanceMeters: 0,
            transport: .wifiDirect,
            ipAddress: "0.0.0.0"
        )
    }

    private var isAllCallMode: Bool {
        talkTargetPeerIp == nil
    }

    private var isSpeakingNow: Bool {
        !isMicMuted && liveVoiceDbFs > -42
    }

    private var signalStrength: Int {
        if telemetry.rssiDbm == 0 || telemetry.rssiDbm > -55 { return 4 }
        if telemetry.rssiDbm > -68 { return 3 }
        if telemetry.rssiDbm > -78 { return 2 }
        return 1
    }

    var body: some View {
        VStack(spacing: 12) {
            // 1. Clean Visual Link Status Bar
            HStack {
                HStack(spacing: 8) {
                    VisualSignalBars(strength0To4: signalStrength, activeColor: EchoLinkColors.emeraldPulse)
                    Text(primaryPeer.transport.fullName)
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(EchoLinkColors.textOnSurface)
                }
                Spacer()
                HStack(spacing: 5) {
                    Image(systemName: "lock.fill")
                        .font(.system(size: 11))
                        .foregroundColor(EchoLinkColors.cyberCyan)
                    Text("Encrypted Voice")
                        .font(.system(size: 12))
                        .foregroundColor(EchoLinkColors.cyberCyan)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
            .background(EchoLinkColors.surfaceContainerLow)
            .clipShape(Capsule())
            .overlay(Capsule().stroke(EchoLinkColors.armorStroke, lineWidth: 1))

            // 2. Peer Audio Card with Reactive Voice Ring, Host Room Routing Bar, Live Waveform & Volume Slider
            VStack(spacing: 12) {
                ZStack {
                    Circle()
                        .fill(EchoLinkColors.emeraldPulse.opacity(isSpeakingNow ? 0.18 : 0.06))
                        .frame(width: isSpeakingNow ? 134 : 122, height: isSpeakingNow ? 134 : 122)
                        .animation(.easeInOut(duration: 0.35), value: isSpeakingNow)

                    Circle()
                        .fill(EchoLinkColors.emeraldPulse.opacity(isSpeakingNow ? 0.28 : 0.10))
                        .frame(width: isSpeakingNow ? 116 : 108, height: isSpeakingNow ? 116 : 108)
                        .animation(.easeInOut(duration: 0.35), value: isSpeakingNow)

                    ZStack {
                        Circle()
                            .fill(EchoLinkColors.surfaceContainer)
                            .frame(width: 96, height: 96)
                            .overlay(
                                Circle().stroke(
                                    isSpeakingNow ? EchoLinkColors.emeraldPulse : EchoLinkColors.cyberCyan.opacity(0.6),
                                    lineWidth: 2
                                )
                            )
                        TacticalAvatarBadge(
                            name: primaryPeer.name,
                            accentColor: EchoLinkColors.emeraldPulse,
                            size: 88
                        )
                    }
                }

                // Peer Name (with Rename Trigger) & Routing Mode Status
                VStack(spacing: 3) {
                    Button {
                        if primaryPeer.ipAddress != "0.0.0.0" {
                            onRenamePeer(primaryPeer.ipAddress, primaryPeer.name)
                        }
                    } label: {
                        HStack(spacing: 8) {
                            Text(
                                (isAllCallMode && activePeers.count > 1)
                                    ? "All Room (\(activePeers.count) Peers)"
                                    : primaryPeer.name
                            )
                            .font(.system(size: 18, weight: .semibold, design: .rounded))
                            .foregroundColor(EchoLinkColors.textOnSurface)

                            if primaryPeer.ipAddress != "0.0.0.0" {
                                Image(systemName: "pencil")
                                    .font(.system(size: 11, weight: .bold))
                                    .foregroundColor(EchoLinkColors.primaryLight)
                                    .frame(width: 22, height: 22)
                                    .background(EchoLinkColors.surfaceContainerHighest)
                                    .clipShape(Circle())
                            }
                        }
                    }

                    Text(statusSubtitleText)
                        .font(EchoLinkTypography.bodySm)
                        .foregroundColor(isSpeakingNow ? EchoLinkColors.emeraldPulse : EchoLinkColors.textOnSurfaceVariant)
                }

                // Host Room Routing Strip: Talk to ALL at once vs Individual Peer 1-on-1 + Add Person
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        Button {
                            onSelectTalkTarget(nil)
                        } label: {
                            HStack(spacing: 5) {
                                Image(systemName: "person.3.fill")
                                    .font(.system(size: 12))
                                Text("Talk to All (\(max(activePeers.count, 1)))")
                                    .font(.system(size: 11, weight: .semibold))
                            }
                            .foregroundColor(isAllCallMode ? EchoLinkColors.obsidianCanvas : EchoLinkColors.textOnSurface)
                            .padding(.horizontal, 11)
                            .padding(.vertical, 6)
                            .background(isAllCallMode ? EchoLinkColors.emeraldPulse : EchoLinkColors.surfaceContainer)
                            .clipShape(Capsule())
                            .overlay(
                                Capsule().stroke(
                                    isAllCallMode ? EchoLinkColors.emeraldPulse : EchoLinkColors.armorStroke,
                                    lineWidth: 1
                                )
                            )
                        }

                        ForEach(activePeers) { meshPeer in
                            let isSelected = (talkTargetPeerIp == meshPeer.ipAddress)
                            HStack(spacing: 6) {
                                Button {
                                    onSelectTalkTarget(meshPeer.ipAddress)
                                } label: {
                                    HStack(spacing: 5) {
                                        Image(systemName: "person.fill")
                                            .font(.system(size: 11))
                                            .foregroundColor(
                                                isSelected ? EchoLinkColors.obsidianCanvas : EchoLinkColors.emeraldPulse
                                            )
                                        Text(meshPeer.name)
                                            .font(.system(size: 11, weight: .semibold))
                                            .foregroundColor(
                                                isSelected ? EchoLinkColors.obsidianCanvas : EchoLinkColors.textPrimary
                                            )
                                    }
                                }

                                Button {
                                    onRenamePeer(meshPeer.ipAddress, meshPeer.name)
                                } label: {
                                    Image(systemName: "pencil")
                                        .font(.system(size: 10, weight: .bold))
                                        .foregroundColor(
                                            isSelected
                                                ? EchoLinkColors.obsidianCanvas.opacity(0.8)
                                                : EchoLinkColors.textOnSurfaceVariant
                                        )
                                }

                                if activePeers.count > 1 {
                                    Button {
                                        onRemovePeerFromRoom(meshPeer.ipAddress)
                                    } label: {
                                        Image(systemName: "xmark")
                                            .font(.system(size: 10, weight: .bold))
                                            .foregroundColor(
                                                isSelected ? EchoLinkColors.obsidianCanvas : EchoLinkColors.errorLight
                                            )
                                    }
                                }
                            }
                            .padding(.leading, 10)
                            .padding(.trailing, 8)
                            .padding(.vertical, 5)
                            .background(isSelected ? EchoLinkColors.cyberCyan : EchoLinkColors.surfaceContainer)
                            .clipShape(Capsule())
                            .overlay(
                                Capsule().stroke(
                                    isSelected ? EchoLinkColors.cyberCyan : EchoLinkColors.armorStroke,
                                    lineWidth: 1
                                )
                            )
                        }

                        Button(action: onAddPeer) {
                            HStack(spacing: 4) {
                                Image(systemName: "person.badge.plus")
                                    .font(.system(size: 12))
                                Text("+ Add")
                                    .font(.system(size: 11, weight: .semibold))
                            }
                            .foregroundColor(EchoLinkColors.cyberCyan)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 5)
                            .background(EchoLinkColors.surfaceContainerHigh)
                            .clipShape(Capsule())
                            .overlay(Capsule().stroke(EchoLinkColors.cyberCyan.opacity(0.5), lineWidth: 1))
                        }
                    }
                }

                // Live Bilateral Waveform Strip
                LiveWaveformStrip(bands: spectrumBands, height: 42)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .background(EchoLinkColors.surfaceContainerLowest)
                    .clipShape(RoundedRectangle(cornerRadius: 16))

                // Voice Volume Slider with Boost Pill
                VStack(spacing: 4) {
                    HStack {
                        Text("Voice Volume")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundColor(EchoLinkColors.textOnSurface)
                        Spacer()
                        Button(action: onToggleRxBoost) {
                            Text(isRxBoost3dB ? "Boost On" : "Boost")
                                .font(.system(size: 11, weight: .semibold))
                                .foregroundColor(
                                    isRxBoost3dB ? EchoLinkColors.obsidianCanvas : EchoLinkColors.textOnSurfaceVariant
                                )
                                .padding(.horizontal, 10)
                                .padding(.vertical, 3)
                                .background(
                                    isRxBoost3dB ? EchoLinkColors.cyberCyan : EchoLinkColors.surfaceContainerHighest
                                )
                                .clipShape(Capsule())
                        }
                    }

                    HStack(spacing: 8) {
                        Image(systemName: "speaker.fill")
                            .font(.system(size: 13))
                            .foregroundColor(EchoLinkColors.textOnSurfaceVariant)
                        Slider(
                            value: Binding(
                                get: { Double(peerRxGainPercent) / 150.0 },
                                set: { onUpdateRxGain(Float($0)) }
                            ),
                            in: 0...1
                        )
                        .tint(EchoLinkColors.cyberCyan)
                        Image(systemName: "speaker.wave.3.fill")
                            .font(.system(size: 13))
                            .foregroundColor(EchoLinkColors.primaryLight)
                    }
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(EchoLinkColors.surfaceContainer)
                .clipShape(RoundedRectangle(cornerRadius: 16))
            }
            .padding(16)
            .background(EchoLinkColors.surfaceContainerLow)
            .clipShape(RoundedRectangle(cornerRadius: 24))
            .overlay(RoundedRectangle(cornerRadius: 24).stroke(EchoLinkColors.armorStroke, lineWidth: 1))

            // 3. Mode Switcher (Open Mic vs Push-to-Talk)
            HStack(spacing: 0) {
                let isOpenMic = (intercomMode == .openMic)
                Button {
                    onSelectMode(.openMic)
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: "mic.fill")
                            .font(.system(size: 14))
                        Text("Open Mic")
                            .font(.system(size: 14, weight: .semibold))
                    }
                    .foregroundColor(
                        isOpenMic ? EchoLinkColors.onSecondaryContainer : EchoLinkColors.textOnSurfaceVariant
                    )
                    .frame(maxWidth: .infinity)
                    .frame(height: 40)
                    .background(isOpenMic ? EchoLinkColors.secondaryContainer : Color.clear)
                    .clipShape(Capsule())
                }

                Button {
                    onSelectMode(.pushToTalk)
                } label: {
                    HStack(spacing: 6) {
                        Image(systemName: "hand.tap.fill")
                            .font(.system(size: 14))
                        Text("Push-to-Talk")
                            .font(.system(size: 14, weight: .semibold))
                    }
                    .foregroundColor(!isOpenMic ? EchoLinkColors.obsidianCanvas : EchoLinkColors.textOnSurfaceVariant)
                    .frame(maxWidth: .infinity)
                    .frame(height: 40)
                    .background(!isOpenMic ? EchoLinkColors.cyberCyan : Color.clear)
                    .clipShape(Capsule())
                }
            }
            .padding(4)
            .background(EchoLinkColors.surfaceContainerLowest)
            .clipShape(Capsule())
            .overlay(Capsule().stroke(EchoLinkColors.armorStroke, lineWidth: 1))

            // 4. Lower Ergonomic Thumb Control Deck
            if intercomMode == .openMic {
                HStack {
                    Spacer()
                    // Noise Gate Toggle
                    Button(action: onToggleProximityGate) {
                        VStack(spacing: 4) {
                            ZStack {
                                Circle()
                                    .fill(EchoLinkColors.surfaceContainerHigh)
                                    .frame(width: 52, height: 52)
                                    .overlay(
                                        Circle().stroke(
                                            proximityNoiseGateActive
                                                ? EchoLinkColors.emeraldPulse
                                                : EchoLinkColors.armorStroke,
                                            lineWidth: 1
                                        )
                                    )
                                Image(systemName: "ear.and.waveform")
                                    .font(.system(size: 19))
                                    .foregroundColor(
                                        proximityNoiseGateActive
                                            ? EchoLinkColors.emeraldPulse
                                            : EchoLinkColors.textSecondary
                                    )
                            }
                            Text("Noise Gate")
                                .font(.system(size: 11))
                                .foregroundColor(
                                    proximityNoiseGateActive
                                        ? EchoLinkColors.emeraldPulse
                                        : EchoLinkColors.textOnSurfaceVariant
                                )
                        }
                    }

                    Spacer()

                    // Primary Mic Mute / Live Button
                    let micAccent = isMicMuted ? EchoLinkColors.combatCrimson : EchoLinkColors.emeraldPulse
                    Button(action: onToggleMute) {
                        VStack(spacing: 3) {
                            Image(systemName: isMicMuted ? "mic.slash.fill" : "mic.fill")
                                .font(.system(size: 26, weight: .bold))
                                .foregroundColor(micAccent)
                            Text(isMicMuted ? "Muted" : "Live")
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(micAccent)
                        }
                        .frame(width: 80, height: 80)
                        .background(EchoLinkColors.surfaceContainerHigh)
                        .clipShape(Circle())
                        .overlay(Circle().stroke(micAccent, lineWidth: 2))
                        .shadow(color: micAccent.opacity(0.35), radius: 14)
                    }

                    Spacer()

                    // 3D Spatial Audio Toggle
                    Button(action: onToggleSpatialAudio) {
                        VStack(spacing: 4) {
                            ZStack {
                                Circle()
                                    .fill(EchoLinkColors.surfaceContainerHigh)
                                    .frame(width: 52, height: 52)
                                    .overlay(
                                        Circle().stroke(
                                            spatialAudioActive ? EchoLinkColors.cyberCyan : EchoLinkColors.armorStroke,
                                            lineWidth: 1
                                        )
                                    )
                                Image(systemName: "person.spatialaudio.stereo.fill")
                                    .font(.system(size: 19))
                                    .foregroundColor(
                                        spatialAudioActive ? EchoLinkColors.primaryLight : EchoLinkColors.textSecondary
                                    )
                            }
                            Text("3D Audio")
                                .font(.system(size: 11))
                                .foregroundColor(
                                    spatialAudioActive ? EchoLinkColors.primaryLight : EchoLinkColors.textSecondary
                                )
                        }
                    }

                    Spacer()

                    // Add Peer Button
                    Button(action: onAddPeer) {
                        VStack(spacing: 4) {
                            ZStack {
                                Circle()
                                    .fill(EchoLinkColors.surfaceContainerHigh)
                                    .frame(width: 52, height: 52)
                                    .overlay(Circle().stroke(EchoLinkColors.armorStroke, lineWidth: 1))
                                Image(systemName: "person.badge.plus")
                                    .font(.system(size: 19))
                                    .foregroundColor(EchoLinkColors.textOnSurface)
                            }
                            Text("Add Peer")
                                .font(.system(size: 11))
                                .foregroundColor(EchoLinkColors.textOnSurfaceVariant)
                        }
                    }
                    Spacer()
                }
            } else {
                // Hold-to-Talk Tactile Pad
                let pttBg = isPttTransmitting ? EchoLinkColors.emeraldPulse : EchoLinkColors.cyberCyan
                HStack(spacing: 8) {
                    Image(systemName: "record.circle.fill")
                        .font(.system(size: 20))
                        .foregroundColor(EchoLinkColors.obsidianCanvas)
                    Text(isPttTransmitting ? "Transmitting Voice..." : "Hold to Talk")
                        .font(.system(size: 15, weight: .bold, design: .rounded))
                        .foregroundColor(EchoLinkColors.obsidianCanvas)
                }
                .frame(maxWidth: .infinity)
                .frame(height: 76)
                .background(pttBg)
                .clipShape(RoundedRectangle(cornerRadius: 24))
                .shadow(color: pttBg.opacity(0.4), radius: 14)
                .gesture(
                    DragGesture(minimumDistance: 0)
                        .onChanged { _ in
                            if !isPttTransmitting { onPttPressChanged(true) }
                        }
                        .onEnded { _ in
                            onPttPressChanged(false)
                        }
                )
            }

            // 5. End Voice Link Button
            Button(action: onDisconnect) {
                HStack(spacing: 8) {
                    Image(systemName: "phone.down.fill")
                        .font(.system(size: 16))
                    Text("End Voice Link")
                        .font(.system(size: 15, weight: .semibold, design: .rounded))
                }
                .foregroundColor(EchoLinkColors.errorLight)
                .frame(maxWidth: .infinity)
                .frame(height: 48)
                .background(EchoLinkColors.errorLight.opacity(0.16))
                .clipShape(Capsule())
                .overlay(Capsule().stroke(EchoLinkColors.combatCrimson.opacity(0.5), lineWidth: 1))
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
    }

    private var statusSubtitleText: String {
        if isSpeakingNow && isAllCallMode && activePeers.count > 1 {
            return "Broadcasting to All..."
        } else if isSpeakingNow && !isAllCallMode {
            return "Talking 1-on-1 to \(primaryPeer.name)..."
        } else if isSpeakingNow {
            return "Speaking..."
        } else if !isAllCallMode {
            return "1-on-1 Direct Mode • Tap All to broadcast"
        } else if activePeers.count > 1 {
            return "All-Call Mode • Tap a person to talk 1-on-1"
        } else {
            return primaryPeer.roleTag
        }
    }
}
