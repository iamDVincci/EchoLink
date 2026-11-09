import SwiftUI

struct RfStatsScreen: View {
    let telemetry: PacketTelemetry
    let localIpAddress: String

    @State private var wavePhase: Double = 0

    private var signalStrengthFraction: CGFloat {
        if telemetry.rssiDbm == 0 { return 0.88 }
        if telemetry.rssiDbm > -52 { return 0.95 }
        if telemetry.rssiDbm > -65 { return 0.78 }
        if telemetry.rssiDbm > -75 { return 0.58 }
        return 0.40
    }

    private var stabilityFraction: CGFloat {
        if telemetry.pingMs == 0 { return 0.92 }
        if telemetry.pingMs < 15 { return 0.96 }
        if telemetry.pingMs < 35 { return 0.82 }
        return 0.65
    }

    var body: some View {
        VStack(spacing: 14) {
            // 1. Live RF Signal Oscilloscope Card
            VStack(spacing: 12) {
                HStack {
                    HStack(spacing: 8) {
                        Image(systemName: "antenna.radiowaves.left.and.right")
                            .foregroundColor(EchoLinkColors.cyberCyan)
                        Text("Live Signal Wave")
                            .font(EchoLinkTypography.headlineSm)
                            .foregroundColor(EchoLinkColors.textOnSurface)
                    }
                    Spacer()
                    VisualSignalBars(
                        strength0To4: signalStrengthFraction > 0.75 ? 4 : 3,
                        activeColor: EchoLinkColors.emeraldPulse
                    )
                }

                Canvas { context, size in
                    let w = size.width
                    let h = size.height
                    let midY = h / 2

                    var primaryPath = Path()
                    var harmonicPath = Path()
                    let steps = 72
                    for i in 0...steps {
                        let x = (CGFloat(i) / CGFloat(steps)) * w
                        let angle = (Double(i) / Double(steps)) * 2.5 * Double.pi
                        let y1 = midY + CGFloat(sin(angle + wavePhase)) * (h * 0.34 * signalStrengthFraction)
                        let y2 = midY + CGFloat(sin(angle * 1.6 - wavePhase)) * (h * 0.22 * stabilityFraction)
                        if i == 0 {
                            primaryPath.move(to: CGPoint(x: x, y: y1))
                            harmonicPath.move(to: CGPoint(x: x, y: y2))
                        } else {
                            primaryPath.addLine(to: CGPoint(x: x, y: y1))
                            harmonicPath.addLine(to: CGPoint(x: x, y: y2))
                        }
                    }

                    context.stroke(
                        harmonicPath,
                        with: .color(EchoLinkColors.emeraldPulse.opacity(0.35)),
                        style: StrokeStyle(lineWidth: 2, lineCap: .round)
                    )
                    context.stroke(
                        primaryPath,
                        with: .linearGradient(
                            Gradient(colors: [
                                EchoLinkColors.cyberCyan.opacity(0.4),
                                EchoLinkColors.cyberCyan,
                                EchoLinkColors.emeraldPulse
                            ]),
                            startPoint: .zero,
                            endPoint: CGPoint(x: w, y: 0)
                        ),
                        style: StrokeStyle(lineWidth: 3, lineCap: .round)
                    )
                }
                .frame(height: 110)
                .padding(12)
                .background(EchoLinkColors.surfaceContainerLowest)
                .clipShape(RoundedRectangle(cornerRadius: 16))
            }
            .padding(16)
            .background(EchoLinkColors.surfaceContainerLow)
            .clipShape(RoundedRectangle(cornerRadius: 24))
            .overlay(RoundedRectangle(cornerRadius: 24).stroke(EchoLinkColors.armorStroke, lineWidth: 1))

            // 2. Visual Radial Gauges
            HStack(spacing: 10) {
                VisualArcGaugeCard(
                    title: "Signal",
                    subtitle: "Strong",
                    fraction: signalStrengthFraction,
                    accentColor: EchoLinkColors.cyberCyan
                )
                VisualArcGaugeCard(
                    title: "Stability",
                    subtitle: "Smooth",
                    fraction: stabilityFraction,
                    accentColor: EchoLinkColors.emeraldPulse
                )
                VisualArcGaugeCard(
                    title: "Security",
                    subtitle: "Encrypted",
                    fraction: 1.0,
                    accentColor: EchoLinkColors.primaryLight
                )
            }

            // 3. Visual Wireless Range Matrix
            VStack(alignment: .leading, spacing: 12) {
                Text("Active Radio Channels")
                    .font(EchoLinkTypography.headlineSm)
                    .foregroundColor(EchoLinkColors.textOnSurface)

                let fractions: [CGFloat] = [0.95, 0.85, 0.72, 0.50]
                ForEach(Array(TransportTier.allCases.enumerated()), id: \.element.id) { idx, tier in
                    let fillFrac = idx < fractions.count ? fractions[idx] : 0.70
                    let barColor = tier.accentColor
                    VStack(spacing: 5) {
                        HStack {
                            Text(tier.fullName)
                                .font(.system(size: 12, weight: .medium))
                                .foregroundColor(EchoLinkColors.textOnSurface)
                            Spacer()
                            VisualSignalBars(strength0To4: 4 - (idx / 2), activeColor: barColor)
                        }
                        GeometryReader { geo in
                            ZStack(alignment: .leading) {
                                Capsule()
                                    .fill(EchoLinkColors.surfaceContainerHighest)
                                Capsule()
                                    .fill(barColor)
                                    .frame(width: geo.size.width * fillFrac)
                            }
                        }
                        .frame(height: 7)
                    }
                }
            }
            .padding(16)
            .background(EchoLinkColors.surfaceContainerLow)
            .clipShape(RoundedRectangle(cornerRadius: 24))
            .overlay(RoundedRectangle(cornerRadius: 24).stroke(EchoLinkColors.armorStroke, lineWidth: 1))

            Spacer(minLength: 0)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .onAppear {
            withAnimation(.linear(duration: 2.0).repeatForever(autoreverses: false)) {
                wavePhase = 2 * Double.pi
            }
        }
    }
}

private struct VisualArcGaugeCard: View {
    let title: String
    let subtitle: String
    let fraction: CGFloat
    let accentColor: Color

    var body: some View {
        VStack(spacing: 8) {
            ZStack {
                Circle()
                    .trim(from: 0, to: 0.75)
                    .stroke(
                        EchoLinkColors.surfaceContainerHighest,
                        style: StrokeStyle(lineWidth: 6, lineCap: .round)
                    )
                    .rotationEffect(.degrees(135))

                Circle()
                    .trim(from: 0, to: 0.75 * min(max(fraction, 0.1), 1.0))
                    .stroke(
                        accentColor,
                        style: StrokeStyle(lineWidth: 6, lineCap: .round)
                    )
                    .rotationEffect(.degrees(135))

                Circle()
                    .fill(accentColor)
                    .frame(width: 10, height: 10)
            }
            .frame(width: 64, height: 64)

            VStack(spacing: 1) {
                Text(title)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(EchoLinkColors.textOnSurface)
                Text(subtitle)
                    .font(.system(size: 11))
                    .foregroundColor(accentColor)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 14)
        .padding(.horizontal, 10)
        .background(EchoLinkColors.surfaceContainerLow)
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .overlay(RoundedRectangle(cornerRadius: 20).stroke(EchoLinkColors.armorStroke, lineWidth: 1))
    }
}

struct AudioEngineSettingsScreen: View {
    let sampleRateHz: Int
    let frameSizeMs: Int
    let opusBitrateKbps: Int
    let loopbackActive: Bool
    let proximityGateActive: Bool
    let spatialAudioActive: Bool
    let spectrumBands: [Float]
    let peerRxGainPercent: Int
    let onUpdateConfig: (Int, Int, Int) -> Void
    let onUpdateRxGain: (Float) -> Void
    let onToggleLoopback: () -> Void
    let onToggleProximityGate: () -> Void
    let onToggleSpatialAudio: () -> Void

    private var claritySliderValue: Double {
        if sampleRateHz >= 48000 { return 1.0 }
        if sampleRateHz >= 24000 { return 0.5 }
        return 0.0
    }

    private var speedSliderValue: Double {
        frameSizeMs <= 10 ? 1.0 : 0.0
    }

    var body: some View {
        VStack(spacing: 12) {
            // 1. Live Microphone Waveform Monitor Card
            VStack(spacing: 12) {
                HStack {
                    HStack(spacing: 8) {
                        Image(systemName: "mic.fill")
                            .foregroundColor(loopbackActive ? EchoLinkColors.emeraldPulse : EchoLinkColors.cyberCyan)
                        Text(loopbackActive ? "Listening to Mic..." : "Voice Monitor")
                            .font(EchoLinkTypography.headlineSm)
                            .foregroundColor(EchoLinkColors.textOnSurface)
                    }
                    Spacer()
                    Text(loopbackActive ? "LIVE" : "STANDBY")
                        .font(.system(size: 11, weight: .bold))
                        .foregroundColor(
                            loopbackActive ? EchoLinkColors.emeraldPulse : EchoLinkColors.textOnSurfaceVariant
                        )
                        .padding(.horizontal, 10)
                        .padding(.vertical, 4)
                        .background(
                            loopbackActive
                                ? EchoLinkColors.emeraldPulse.opacity(0.18)
                                : EchoLinkColors.surfaceContainerHighest
                        )
                        .clipShape(Capsule())
                }

                LiveWaveformStrip(bands: spectrumBands, height: 46)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 12)
                    .background(EchoLinkColors.surfaceContainerLowest)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
            }
            .padding(16)
            .background(EchoLinkColors.surfaceContainerLow)
            .clipShape(RoundedRectangle(cornerRadius: 24))
            .overlay(RoundedRectangle(cornerRadius: 24).stroke(EchoLinkColors.armorStroke, lineWidth: 1))

            // 2. Interactive Visual Sliders Card
            VStack(spacing: 12) {
                // Voice Clarity Slider (Drives 2.8 kHz Vocal Presence EQ)
                VStack(spacing: 4) {
                    HStack {
                        HStack(spacing: 6) {
                            Image(systemName: "waveform")
                                .foregroundColor(EchoLinkColors.cyberCyan)
                            Text("Voice Clarity")
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundColor(EchoLinkColors.textOnSurface)
                        }
                        Spacer()
                        Text(
                            sampleRateHz >= 48000
                                ? "Studio HD"
                                : (sampleRateHz >= 24000 ? "Balanced" : "Battery Saver")
                        )
                        .font(.system(size: 12))
                        .foregroundColor(EchoLinkColors.cyberCyan)
                    }
                    Slider(
                        value: Binding(
                            get: { claritySliderValue },
                            set: { v in
                                let nextSr = v > 0.66 ? 48000 : (v > 0.33 ? 24000 : 16000)
                                onUpdateConfig(nextSr, frameSizeMs, opusBitrateKbps)
                            }
                        ),
                        in: 0...1,
                        step: 0.5
                    )
                    .tint(EchoLinkColors.cyberCyan)
                }

                // Response Speed Slider
                VStack(spacing: 4) {
                    HStack {
                        HStack(spacing: 6) {
                            Image(systemName: "gauge.with.dots.needle.67percent")
                                .foregroundColor(EchoLinkColors.emeraldPulse)
                            Text("Response Speed")
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundColor(EchoLinkColors.textOnSurface)
                        }
                        Spacer()
                        Text(frameSizeMs <= 10 ? "Ultra-Fast" : "Smooth")
                            .font(.system(size: 12))
                            .foregroundColor(EchoLinkColors.emeraldPulse)
                    }
                    Slider(
                        value: Binding(
                            get: { speedSliderValue },
                            set: { v in
                                let nextMs = v >= 0.5 ? 10 : 20
                                onUpdateConfig(sampleRateHz, nextMs, opusBitrateKbps)
                            }
                        ),
                        in: 0...1
                    )
                    .tint(EchoLinkColors.emeraldPulse)
                }

                // Output Boost Slider
                VStack(spacing: 4) {
                    HStack {
                        HStack(spacing: 6) {
                            Image(systemName: "speaker.wave.3.fill")
                                .foregroundColor(EchoLinkColors.primaryLight)
                            Text("Output Boost")
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundColor(EchoLinkColors.textOnSurface)
                        }
                        Spacer()
                        Text(peerRxGainPercent > 110 ? "Boosted" : "Standard")
                            .font(.system(size: 12))
                            .foregroundColor(EchoLinkColors.primaryLight)
                    }
                    Slider(
                        value: Binding(
                            get: { Double(peerRxGainPercent) / 150.0 },
                            set: { onUpdateRxGain(Float($0)) }
                        ),
                        in: 0...1
                    )
                    .tint(EchoLinkColors.primaryLight)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 14)
            .background(EchoLinkColors.surfaceContainerLow)
            .clipShape(RoundedRectangle(cornerRadius: 24))
            .overlay(RoundedRectangle(cornerRadius: 24).stroke(EchoLinkColors.armorStroke, lineWidth: 1))

            // 3. Acoustic Enhancement Toggles (Noise Gate & 3D Spatial)
            HStack(spacing: 10) {
                CompactAudioFeatureToggle(
                    title: "Noise Gate",
                    subtitle: proximityGateActive ? "On" : "Off",
                    sfSymbol: "ear.and.waveform",
                    active: proximityGateActive,
                    accentColor: EchoLinkColors.emeraldPulse,
                    onClick: onToggleProximityGate
                )
                CompactAudioFeatureToggle(
                    title: "3D Spatial",
                    subtitle: spatialAudioActive ? "On" : "Off",
                    sfSymbol: "person.spatialaudio.stereo.fill",
                    active: spatialAudioActive,
                    accentColor: EchoLinkColors.cyberCyan,
                    onClick: onToggleSpatialAudio
                )
            }

            // 4. Hardware Mic Test Button
            Button(action: onToggleLoopback) {
                Text(loopbackActive ? "Stop Microphone Test" : "Test Microphone & Waveform")
                    .font(.system(size: 15, weight: .bold, design: .rounded))
                    .foregroundColor(loopbackActive ? EchoLinkColors.obsidianCanvas : EchoLinkColors.primaryLight)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(loopbackActive ? EchoLinkColors.emeraldPulse : EchoLinkColors.surfaceContainerHigh)
                    .clipShape(Capsule())
            }

            Spacer(minLength: 0)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
    }
}

private struct CompactAudioFeatureToggle: View {
    let title: String
    let subtitle: String
    let sfSymbol: String
    let active: Bool
    let accentColor: Color
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            HStack {
                HStack(spacing: 10) {
                    Image(systemName: sfSymbol)
                        .font(.system(size: 18))
                        .foregroundColor(active ? accentColor : EchoLinkColors.textOnSurfaceVariant)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(title)
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundColor(EchoLinkColors.textOnSurface)
                        Text(subtitle)
                            .font(.system(size: 11))
                            .foregroundColor(active ? accentColor : EchoLinkColors.textOnSurfaceVariant)
                    }
                }
                Spacer()
                ZStack(alignment: active ? .trailing : .leading) {
                    Capsule()
                        .fill(active ? accentColor.opacity(0.25) : EchoLinkColors.surfaceContainerHighest)
                        .frame(width: 36, height: 20)
                    Circle()
                        .fill(active ? accentColor : EchoLinkColors.textSecondary)
                        .frame(width: 16, height: 16)
                        .padding(2)
                }
            }
            .padding(14)
            .frame(maxWidth: .infinity)
            .background(EchoLinkColors.surfaceContainerLow)
            .clipShape(RoundedRectangle(cornerRadius: 18))
            .overlay(
                RoundedRectangle(cornerRadius: 18).stroke(
                    active ? accentColor.opacity(0.5) : EchoLinkColors.armorStroke,
                    lineWidth: 1
                )
            )
        }
    }
}
