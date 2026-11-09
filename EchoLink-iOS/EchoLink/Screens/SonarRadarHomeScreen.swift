import SwiftUI

struct SonarRadarHomeScreen: View {
    let discoveredPeers: [PeerNode]
    let recentPeers: [RecentPeer]
    let localIp: String
    let wifiFrequencyMhz: Int
    let toastBanner: (String, String)?
    let onTapPeerNode: (PeerNode) -> Void
    let onTapRecentPeer: (RecentPeer) -> Void
    let onRemoveRecentPeer: (String) -> Void
    let onClearRecentPeers: () -> Void
    let onRenamePeer: (String, String) -> Void
    let onCreateCallLink: () -> Void
    let onScanCallLink: () -> Void
    let onDismissToast: () -> Void

    @State private var sweepAngle: Double = 0
    @State private var pulseScale: CGFloat = 1.0

    private var signalBars: Int {
        if wifiFrequencyMhz >= 4900 { return 4 }
        if wifiFrequencyMhz >= 2400 || localIp != "0.0.0.0" { return 3 }
        return 2
    }

    var body: some View {
        VStack {
            // 1. Clean Top Status Strip
            HStack {
                HStack(spacing: 7) {
                    Circle()
                        .fill(EchoLinkColors.emeraldPulse)
                        .frame(width: 8, height: 8)
                    Text(discoveredPeers.isEmpty ? "Radar Scanning" : "Peers Nearby")
                        .font(.system(size: 12, weight: .medium))
                        .foregroundColor(EchoLinkColors.textOnSurface)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(EchoLinkColors.surfaceContainerLow)
                .clipShape(Capsule())
                .overlay(Capsule().stroke(EchoLinkColors.armorStroke, lineWidth: 1))

                Spacer()

                HStack(spacing: 8) {
                    VisualSignalBars(strength0To4: signalBars, activeColor: EchoLinkColors.cyberCyan)
                    Text(wifiFrequencyMhz >= 4900 ? "High-Speed P2P" : "Local Mesh")
                        .font(.system(size: 12))
                        .foregroundColor(EchoLinkColors.textOnSurfaceVariant)
                }
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(EchoLinkColors.surfaceContainerLow)
                .clipShape(Capsule())
                .overlay(Capsule().stroke(EchoLinkColors.armorStroke, lineWidth: 1))
            }

            Spacer()

            // 2. Centered Phone-Proportioned Sonar Radar
            VStack(spacing: 10) {
                ZStack {
                    Circle()
                        .fill(EchoLinkColors.surfaceContainerLowest)
                        .overlay(Circle().stroke(EchoLinkColors.armorStroke, lineWidth: 1))
                        .shadow(color: EchoLinkColors.cyberCyan.opacity(0.20), radius: 22)

                    // Radar Grid & Sweep
                    Canvas { context, size in
                        let c = CGPoint(x: size.width / 2, y: size.height / 2)
                        let maxR = min(size.width, size.height) / 2
                        let gridColor = EchoLinkColors.outlineVariant.opacity(0.4)

                        var vLine = Path()
                        vLine.move(to: CGPoint(x: c.x, y: 0))
                        vLine.addLine(to: CGPoint(x: c.x, y: size.height))
                        context.stroke(vLine, with: .color(gridColor), style: StrokeStyle(lineWidth: 1, dash: [6, 6]))

                        var hLine = Path()
                        hLine.move(to: CGPoint(x: 0, y: c.y))
                        hLine.addLine(to: CGPoint(x: size.width, y: c.y))
                        context.stroke(hLine, with: .color(gridColor), style: StrokeStyle(lineWidth: 1, dash: [6, 6]))

                        for frac in [0.34, 0.64, 0.90] {
                            let r = maxR * frac
                            let rect = CGRect(x: c.x - r, y: c.y - r, width: r * 2, height: r * 2)
                            context.stroke(
                                Path(ellipseIn: rect),
                                with: .color(gridColor),
                                style: StrokeStyle(lineWidth: 1, dash: frac == 0.64 ? [4, 8] : [])
                            )
                        }
                    }

                    // Rotating Sweep Wedge
                    AngularGradient(
                        gradient: Gradient(colors: [
                            EchoLinkColors.cyberCyan.opacity(0.28),
                            EchoLinkColors.cyberCyan.opacity(0.02),
                            Color.clear,
                            Color.clear
                        ]),
                        center: .center,
                        startAngle: .degrees(0),
                        endAngle: .degrees(360)
                    )
                    .clipShape(Circle())
                    .rotationEffect(.degrees(sweepAngle))

                    // Center Pulse & Local Node
                    Circle()
                        .fill(EchoLinkColors.cyberCyan.opacity(0.14))
                        .frame(width: 56 * pulseScale, height: 56 * pulseScale)

                    ZStack(alignment: .bottomTrailing) {
                        TacticalAvatarBadge(name: "ME", accentColor: EchoLinkColors.cyberCyan, size: 46)
                        ZStack {
                            Circle()
                                .fill(EchoLinkColors.secondaryContainer)
                                .frame(width: 16, height: 16)
                            Image(systemName: "mic.fill")
                                .font(.system(size: 9))
                                .foregroundColor(EchoLinkColors.onSecondaryContainer)
                        }
                    }

                    // Discovered Peer Nodes on Radar
                    let offsets: [(CGFloat, CGFloat)] = [(52, 26), (-36, -52), (-40, 76)]
                    ForEach(Array(discoveredPeers.prefix(3).enumerated()), id: \.element.id) { idx, peer in
                        let offset = idx < offsets.count ? offsets[idx] : (36, -36)
                        let accent = peer.transport.accentColor
                        HStack(spacing: 6) {
                            Button {
                                onTapPeerNode(peer)
                            } label: {
                                HStack(spacing: 6) {
                                    TacticalAvatarBadge(name: peer.name, accentColor: accent, size: 32)
                                    HStack(spacing: 6) {
                                        Text(peer.name)
                                            .font(.system(size: 12, weight: .semibold))
                                            .foregroundColor(EchoLinkColors.textOnSurface)
                                        VisualSignalBars(strength0To4: peer.rssiDbm > -55 ? 4 : 3, activeColor: accent)
                                    }
                                    .padding(.leading, 9)
                                    .padding(.trailing, 4)
                                    .padding(.vertical, 4)
                                }
                            }
                            Button {
                                onRenamePeer(peer.ipAddress, peer.name)
                            } label: {
                                Image(systemName: "pencil")
                                    .font(.system(size: 11, weight: .semibold))
                                    .foregroundColor(EchoLinkColors.textOnSurfaceVariant)
                                    .padding(.trailing, 8)
                            }
                        }
                        .background(EchoLinkColors.surfaceContainerHigh.opacity(0.94))
                        .clipShape(Capsule())
                        .overlay(Capsule().stroke(accent.opacity(0.5), lineWidth: 0.8))
                        .offset(x: offset.0, y: offset.1)
                    }
                }
                .frame(width: 268, height: 268)

                Text(
                    discoveredPeers.isEmpty
                        ? "Looking for nearby EchoLink devices..."
                        : "Tap a device on the radar to start talking"
                )
                .font(EchoLinkTypography.bodySm)
                .foregroundColor(EchoLinkColors.textOnSurfaceVariant)
            }

            Spacer()

            // Optional Toast Banner
            if let (title, subtitle) = toastBanner {
                HStack {
                    Image(systemName: "cable.connector")
                        .foregroundColor(EchoLinkColors.emeraldPulse)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(title)
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(EchoLinkColors.primaryLight)
                            .lineLimit(1)
                        Text(subtitle)
                            .font(.system(size: 11))
                            .foregroundColor(EchoLinkColors.textOnSurfaceVariant)
                            .lineLimit(1)
                    }
                    Spacer()
                    Button(action: onDismissToast) {
                        Image(systemName: "xmark")
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundColor(EchoLinkColors.textOnSurfaceVariant)
                    }
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .background(EchoLinkColors.surfaceContainerHigh)
                .clipShape(RoundedRectangle(cornerRadius: 14))
                .overlay(
                    RoundedRectangle(cornerRadius: 14)
                        .stroke(EchoLinkColors.cyberCyan.opacity(0.4), lineWidth: 1)
                )
            }

            // 3. Clean Lower Action Dock + Connected Devices History (with Delete & Rename)
            VStack(spacing: 12) {
                HStack(spacing: 10) {
                    Button(action: onCreateCallLink) {
                        HStack(spacing: 12) {
                            ZStack {
                                RoundedRectangle(cornerRadius: 12)
                                    .fill(EchoLinkColors.cyberCyan.opacity(0.14))
                                    .frame(width: 42, height: 42)
                                Image(systemName: "qrcode")
                                    .font(.system(size: 20, weight: .semibold))
                                    .foregroundColor(EchoLinkColors.cyberCyan)
                            }
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Host Room")
                                    .font(.system(size: 15, weight: .semibold))
                                    .foregroundColor(EchoLinkColors.textOnSurface)
                                Text("Show QR code")
                                    .font(.system(size: 12))
                                    .foregroundColor(EchoLinkColors.textOnSurfaceVariant)
                            }
                            Spacer()
                        }
                        .padding(14)
                        .background(EchoLinkColors.surfaceContainer)
                        .clipShape(RoundedRectangle(cornerRadius: 18))
                        .overlay(RoundedRectangle(cornerRadius: 18).stroke(EchoLinkColors.armorStroke, lineWidth: 1))
                    }

                    Button(action: onScanCallLink) {
                        HStack(spacing: 12) {
                            ZStack {
                                RoundedRectangle(cornerRadius: 12)
                                    .fill(EchoLinkColors.emeraldPulse.opacity(0.14))
                                    .frame(width: 42, height: 42)
                                Image(systemName: "qrcode.viewfinder")
                                    .font(.system(size: 20, weight: .semibold))
                                    .foregroundColor(EchoLinkColors.emeraldPulse)
                            }
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Join Room")
                                    .font(.system(size: 15, weight: .semibold))
                                    .foregroundColor(EchoLinkColors.textOnSurface)
                                Text("Scan peer QR")
                                    .font(.system(size: 12))
                                    .foregroundColor(EchoLinkColors.textOnSurfaceVariant)
                            }
                            Spacer()
                        }
                        .padding(14)
                        .background(EchoLinkColors.surfaceContainer)
                        .clipShape(RoundedRectangle(cornerRadius: 18))
                        .overlay(RoundedRectangle(cornerRadius: 18).stroke(EchoLinkColors.armorStroke, lineWidth: 1))
                    }
                }

                // Connected Devices History Strip (Delete Individual, Rename, or Clear All)
                if !recentPeers.isEmpty {
                    VStack(spacing: 8) {
                        HStack {
                            HStack(spacing: 6) {
                                Image(systemName: "clock.arrow.circlepath")
                                    .font(.system(size: 12))
                                    .foregroundColor(EchoLinkColors.cyberCyan)
                                Text("Connected Devices")
                                    .font(.system(size: 11, weight: .semibold))
                                    .foregroundColor(EchoLinkColors.textOnSurfaceVariant)
                            }
                            Spacer()
                            Button(action: onClearRecentPeers) {
                                HStack(spacing: 4) {
                                    Image(systemName: "trash")
                                        .font(.system(size: 11))
                                    Text("Clear History")
                                        .font(.system(size: 11, weight: .semibold))
                                }
                                .foregroundColor(EchoLinkColors.errorLight)
                                .padding(.horizontal, 9)
                                .padding(.vertical, 3)
                                .background(EchoLinkColors.errorLight.opacity(0.14))
                                .clipShape(Capsule())
                            }
                        }

                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 8) {
                                ForEach(recentPeers) { recent in
                                    let accent = recent.transport.accentColor
                                    HStack(spacing: 7) {
                                        Button {
                                            onTapRecentPeer(recent)
                                        } label: {
                                            HStack(spacing: 7) {
                                                TacticalAvatarBadge(name: recent.name, accentColor: accent, size: 24)
                                                Text(recent.name)
                                                    .font(.system(size: 12, weight: .semibold))
                                                    .foregroundColor(EchoLinkColors.textOnSurface)
                                            }
                                        }

                                        Button {
                                            onRenamePeer(recent.ipAddress, recent.name)
                                        } label: {
                                            Image(systemName: "pencil")
                                                .font(.system(size: 10, weight: .bold))
                                                .foregroundColor(EchoLinkColors.primaryLight)
                                                .frame(width: 20, height: 20)
                                                .background(EchoLinkColors.surfaceContainerHighest)
                                                .clipShape(Circle())
                                        }

                                        Button {
                                            onRemoveRecentPeer(recent.ipAddress)
                                        } label: {
                                            Image(systemName: "xmark")
                                                .font(.system(size: 10, weight: .bold))
                                                .foregroundColor(EchoLinkColors.errorLight)
                                                .frame(width: 20, height: 20)
                                                .background(EchoLinkColors.errorLight.opacity(0.16))
                                                .clipShape(Circle())
                                        }
                                    }
                                    .padding(.leading, 6)
                                    .padding(.trailing, 8)
                                    .padding(.vertical, 5)
                                    .background(EchoLinkColors.surfaceContainer)
                                    .clipShape(Capsule())
                                    .overlay(Capsule().stroke(EchoLinkColors.armorStroke, lineWidth: 1))
                                }
                            }
                        }
                    }
                }
            }
            .padding(14)
            .background(EchoLinkColors.surfaceContainerLow)
            .clipShape(RoundedRectangle(cornerRadius: 24))
            .overlay(RoundedRectangle(cornerRadius: 24).stroke(EchoLinkColors.armorStroke, lineWidth: 1))
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
        .onAppear {
            withAnimation(.linear(duration: 3.4).repeatForever(autoreverses: false)) {
                sweepAngle = 360
            }
            withAnimation(.easeInOut(duration: 1.3).repeatForever(autoreverses: true)) {
                pulseScale = 1.42
            }
        }
    }
}
