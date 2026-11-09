import SwiftUI

struct ReadinessCheckScreen: View {
    let readinessState: HardwareReadinessState
    let sampleRateHz: Int
    let frameSizeMs: Int
    let onToggleWifiDirect: () -> Void
    let onToggleOboeAudio: () -> Void
    let onRequestPermissions: () -> Void
    let onLaunchSonarRadar: () -> Void

    @State private var phase: Double = 0

    var body: some View {
        VStack(spacing: 14) {
            // 1. Clean Animated Acoustic Wave Bridge Card
            HStack {
                VStack(spacing: 6) {
                    ZStack {
                        Circle()
                            .fill(EchoLinkColors.surfaceContainerHigh)
                            .frame(width: 48, height: 48)
                            .overlay(Circle().stroke(EchoLinkColors.cyberCyan.opacity(0.5), lineWidth: 1))
                        Image(systemName: "airpodspro")
                            .font(.system(size: 20))
                            .foregroundColor(EchoLinkColors.cyberCyan)
                    }
                    Text("You")
                        .font(.system(size: 11))
                        .foregroundColor(EchoLinkColors.textOnSurfaceVariant)
                }

                Canvas { context, size in
                    let w = size.width
                    let h = size.height
                    let midY = h / 2

                    var echoPath = Path()
                    var mainPath = Path()
                    let steps = 60
                    for i in 0...steps {
                        let x = (CGFloat(i) / CGFloat(steps)) * w
                        let norm = (Double(i) / Double(steps)) * 2.0 * Double.pi
                        let yMain = midY + CGFloat(sin(norm * 1.5 + phase)) * (h * 0.28)
                        let yEcho = midY + CGFloat(sin(norm * 1.5 - phase * 0.7)) * (h * 0.18)
                        if i == 0 {
                            mainPath.move(to: CGPoint(x: x, y: yMain))
                            echoPath.move(to: CGPoint(x: x, y: yEcho))
                        } else {
                            mainPath.addLine(to: CGPoint(x: x, y: yMain))
                            echoPath.addLine(to: CGPoint(x: x, y: yEcho))
                        }
                    }

                    context.stroke(
                        echoPath,
                        with: .color(EchoLinkColors.cyberCyan.opacity(0.28)),
                        style: StrokeStyle(lineWidth: 1.5, dash: [5, 7])
                    )
                    context.stroke(
                        mainPath,
                        with: .linearGradient(
                            Gradient(colors: [
                                EchoLinkColors.cyberCyan.opacity(0.3),
                                EchoLinkColors.cyberCyan,
                                EchoLinkColors.emeraldPulse
                            ]),
                            startPoint: .zero,
                            endPoint: CGPoint(x: w, y: 0)
                        ),
                        style: StrokeStyle(lineWidth: 3, lineCap: .round)
                    )
                }
                .frame(height: 92)
                .padding(.horizontal, 10)

                VStack(spacing: 6) {
                    ZStack {
                        Circle()
                            .fill(EchoLinkColors.surfaceContainerHigh)
                            .frame(width: 48, height: 48)
                            .overlay(Circle().stroke(EchoLinkColors.emeraldPulse.opacity(0.5), lineWidth: 1))
                        Image(systemName: "ear.and.waveform")
                            .font(.system(size: 20))
                            .foregroundColor(EchoLinkColors.emeraldPulse)
                    }
                    Text("Peer")
                        .font(.system(size: 11))
                        .foregroundColor(EchoLinkColors.textOnSurfaceVariant)
                }
            }
            .padding(.horizontal, 18)
            .padding(.vertical, 14)
            .frame(height: 148)
            .background(EchoLinkColors.surfaceContainerLowest)
            .clipShape(RoundedRectangle(cornerRadius: 22))
            .overlay(RoundedRectangle(cornerRadius: 22).stroke(EchoLinkColors.armorStroke, lineWidth: 1))

            // 2. Hardware & Permissions Checklist Card
            VStack(spacing: 12) {
                let allPermsReady = readinessState.micPermissionGranted &&
                    readinessState.bluetoothPermissionGranted &&
                    readinessState.nearbyWifiPermissionGranted

                HStack {
                    HStack(spacing: 8) {
                        Image(systemName: "checkmark.shield.fill")
                            .foregroundColor(EchoLinkColors.primaryFixedDim)
                        Text("Device Readiness")
                            .font(EchoLinkTypography.headlineSm)
                            .foregroundColor(EchoLinkColors.textOnSurface)
                    }
                    Spacer()
                    Button(action: onRequestPermissions) {
                        Text(allPermsReady ? "Ready" : "Grant Access")
                            .font(.system(size: 11, weight: .semibold))
                            .foregroundColor(allPermsReady ? EchoLinkColors.emeraldPulse : EchoLinkColors.amberWarning)
                            .padding(.horizontal, 10)
                            .padding(.vertical, 4)
                            .background(EchoLinkColors.surfaceContainerLowest)
                            .clipShape(Capsule())
                    }
                }

                // Row 1: Audio Endpoint
                HStack {
                    ZStack {
                        RoundedRectangle(cornerRadius: 10)
                            .fill(EchoLinkColors.surfaceContainerHighest)
                            .frame(width: 40, height: 40)
                        Image(systemName: "airpodspro")
                            .foregroundColor(
                                readinessState.earbudsConnected
                                    ? EchoLinkColors.emeraldPulse
                                    : EchoLinkColors.cyberCyan
                            )
                    }
                    VStack(alignment: .leading, spacing: 2) {
                        Text(readinessState.earbudsConnected ? "Headset Connected" : "Audio Output")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(EchoLinkColors.textOnSurface)
                        Text(readinessState.connectedAudioDeviceName)
                            .font(.system(size: 12))
                            .foregroundColor(EchoLinkColors.textOnSurfaceVariant)
                            .lineLimit(1)
                    }
                    Spacer()
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundColor(
                            readinessState.earbudsConnected ? EchoLinkColors.emeraldPulse : EchoLinkColors.cyberCyan
                        )
                }
                .padding(12)
                .background(EchoLinkColors.surfaceContainer)
                .clipShape(RoundedRectangle(cornerRadius: 16))

                // Row 2: Direct Peer Discovery
                Button {
                    onRequestPermissions()
                    onToggleWifiDirect()
                } label: {
                    HStack {
                        ZStack {
                            RoundedRectangle(cornerRadius: 10)
                                .fill(EchoLinkColors.surfaceContainerHighest)
                                .frame(width: 40, height: 40)
                            Image(systemName: "antenna.radiowaves.left.and.right")
                                .foregroundColor(EchoLinkColors.cyberCyan)
                        }
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Direct Peer Discovery")
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundColor(EchoLinkColors.textOnSurface)
                            Text("Offline Wi-Fi & Hotspot Radar")
                                .font(.system(size: 12))
                                .foregroundColor(EchoLinkColors.textOnSurfaceVariant)
                        }
                        Spacer()
                        Toggle("", isOn: .constant(readinessState.wifiDirectRadarEnabled))
                            .labelsHidden()
                            .tint(EchoLinkColors.cyberCyan)
                    }
                    .padding(12)
                    .background(EchoLinkColors.surfaceContainer)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                }

                // Row 3: Low-Latency Voice
                Button {
                    onRequestPermissions()
                    onToggleOboeAudio()
                } label: {
                    HStack {
                        ZStack {
                            RoundedRectangle(cornerRadius: 10)
                                .fill(EchoLinkColors.surfaceContainerHighest)
                                .frame(width: 40, height: 40)
                            Image(systemName: "mic.fill")
                                .foregroundColor(EchoLinkColors.emeraldPulse)
                        }
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Low-Latency Voice")
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundColor(EchoLinkColors.textOnSurface)
                            Text("Echo cancellation & noise suppression")
                                .font(.system(size: 12))
                                .foregroundColor(EchoLinkColors.textOnSurfaceVariant)
                        }
                        Spacer()
                        Toggle("", isOn: .constant(readinessState.oboeLowLatencyEnabled))
                            .labelsHidden()
                            .tint(EchoLinkColors.emeraldPulse)
                    }
                    .padding(12)
                    .background(EchoLinkColors.surfaceContainer)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                }
            }
            .padding(16)
            .background(EchoLinkColors.surfaceContainerLow)
            .clipShape(RoundedRectangle(cornerRadius: 24))
            .overlay(RoundedRectangle(cornerRadius: 24).stroke(EchoLinkColors.armorStroke, lineWidth: 1))

            Spacer()

            // 3. Primary Action Button
            Button {
                onRequestPermissions()
                onLaunchSonarRadar()
            } label: {
                HStack(spacing: 8) {
                    Text(readinessState.isSyncingNodes ? "Starting Radar..." : "Open Sonar Radar")
                        .font(.system(size: 16, weight: .bold, design: .rounded))
                    Image(systemName: "arrow.right")
                        .font(.system(size: 15, weight: .bold))
                }
                .foregroundColor(EchoLinkColors.obsidianCanvas)
                .frame(maxWidth: .infinity)
                .frame(height: 54)
                .background(EchoLinkColors.cyberCyan)
                .clipShape(Capsule())
                .shadow(color: EchoLinkColors.cyberCyan.opacity(0.4), radius: 16)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .onAppear {
            withAnimation(.linear(duration: 2.4).repeatForever(autoreverses: false)) {
                phase = 2 * Double.pi
            }
        }
    }
}
