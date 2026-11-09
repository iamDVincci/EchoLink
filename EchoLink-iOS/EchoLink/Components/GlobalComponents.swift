import SwiftUI

struct AntigravityEmblem: View {
    var size: CGFloat = 32

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 10)
                .fill(EchoLinkColors.surfaceContainerLowest)
                .overlay(
                    RoundedRectangle(cornerRadius: 10)
                        .stroke(EchoLinkColors.cyberCyan.opacity(0.45), lineWidth: 1)
                )

            Canvas { context, canvasSize in
                let w = canvasSize.width
                let h = canvasSize.height
                var path = Path()
                path.move(to: CGPoint(x: w * 0.18, y: h * 0.80))
                path.addLine(to: CGPoint(x: w * 0.50, y: h * 0.16))
                path.addLine(to: CGPoint(x: w * 0.82, y: h * 0.80))

                context.stroke(
                    path,
                    with: .color(EchoLinkColors.cyberCyan),
                    style: StrokeStyle(lineWidth: w * 0.13, lineCap: .round, lineJoin: .round)
                )

                let dotRadius = w * 0.09
                let dotRect = CGRect(
                    x: w * 0.50 - dotRadius,
                    y: h * 0.56 - dotRadius,
                    width: dotRadius * 2,
                    height: dotRadius * 2
                )
                context.fill(Path(ellipseIn: dotRect), with: .color(EchoLinkColors.emeraldPulse))
            }
            .frame(width: size * 0.68, height: size * 0.68)
        }
        .frame(width: size, height: size)
    }
}

struct TacticalAvatarBadge: View {
    let name: String
    let accentColor: Color
    var size: CGFloat = 36

    private var initials: String {
        String(name.trimmingCharacters(in: .whitespacesAndNewlines).prefix(2)).uppercased()
    }

    var body: some View {
        ZStack {
            Circle()
                .fill(
                    RadialGradient(
                        colors: [
                            accentColor.opacity(0.32),
                            EchoLinkColors.surfaceContainerHigh,
                            EchoLinkColors.surfaceContainerLowest
                        ],
                        center: .center,
                        startRadius: 2,
                        endRadius: size * 0.55
                    )
                )
                .overlay(
                    Circle().stroke(accentColor.opacity(0.7), lineWidth: 1.2)
                )

            Text(initials)
                .font(.system(size: size * 0.34, weight: .bold, design: .rounded))
                .foregroundColor(EchoLinkColors.textPrimary)
        }
        .frame(width: size, height: size)
    }
}

struct VisualSignalBars: View {
    let strength0To4: Int
    var activeColor: Color = EchoLinkColors.emeraldPulse

    var body: some View {
        let clamped = min(max(strength0To4, 1), 4)
        HStack(alignment: .bottom, spacing: 2.5) {
            ForEach(1...4, id: \.self) { idx in
                let isLit = idx <= clamped
                RoundedRectangle(cornerRadius: 1.5)
                    .fill(isLit ? activeColor : EchoLinkColors.outlineVariant.opacity(0.45))
                    .frame(width: 3.2, height: 14.0 * (CGFloat(idx) / 4.0))
            }
        }
        .frame(height: 14)
    }
}

struct LiveWaveformStrip: View {
    let bands: [Float]
    var height: CGFloat = 44
    var primaryColor: Color = EchoLinkColors.cyberCyan
    var secondaryColor: Color = EchoLinkColors.emeraldPulse

    var body: some View {
        HStack(alignment: .center, spacing: 4) {
            ForEach(Array(bands.enumerated()), id: \.offset) { idx, fraction in
                let clamped = CGFloat(min(max(fraction, 0.14), 1.0))
                Capsule()
                    .fill(idx % 2 == 0 ? primaryColor : secondaryColor)
                    .frame(maxWidth: .infinity)
                    .frame(height: height * clamped)
                    .animation(.easeOut(duration: 0.08), value: clamped)
            }
        }
        .frame(height: height)
    }
}

struct EchoLinkTopAppBar: View {
    let title: String
    let subtitle: String
    let earbudStatus: EarbudStatus
    let onSelectRoute: (String) -> Void
    var showBackButton: Bool = false
    var showEmergencySquelch: Bool = false
    var onBackClick: () -> Void = {}
    var onEmergencySquelchClick: () -> Void = {}

    private var compactRouteName: String {
        if earbudStatus.isConnected {
            return String(earbudStatus.deviceName.prefix(14))
        } else if earbudStatus.routeType == "EARPIECE" {
            return "Receiver"
        } else {
            return "Speaker"
        }
    }

    var body: some View {
        HStack {
            HStack(spacing: 10) {
                if showBackButton {
                    Button(action: onBackClick) {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(EchoLinkColors.textOnSurface)
                            .frame(width: 36, height: 36)
                            .background(EchoLinkColors.surfaceContainerHigh)
                            .clipShape(Circle())
                    }
                } else {
                    AntigravityEmblem(size: 34)
                }

                VStack(alignment: .leading, spacing: 1) {
                    Text(title)
                        .font(EchoLinkTypography.headlineSm)
                        .foregroundColor(EchoLinkColors.textOnSurface)
                        .lineLimit(1)
                    Text(subtitle)
                        .font(.system(size: 11))
                        .foregroundColor(EchoLinkColors.textOnSurfaceVariant)
                        .lineLimit(1)
                }
            }

            Spacer()

            HStack(spacing: 8) {
                let dotColor = earbudStatus.isConnected ? EchoLinkColors.emeraldPulse : EchoLinkColors.cyberCyan
                Menu {
                    ForEach(earbudStatus.availableRoutes, id: \.self) { route in
                        Button(route) {
                            onSelectRoute(route)
                        }
                    }
                } label: {
                    HStack(spacing: 6) {
                        Circle()
                            .fill(dotColor)
                            .frame(width: 7, height: 7)
                        Image(systemName: earbudStatus.isConnected ? "airpodspro" : "speaker.wave.2.fill")
                            .font(.system(size: 13))
                            .foregroundColor(dotColor)
                        Text(compactRouteName)
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundColor(EchoLinkColors.textOnSurface)
                            .lineLimit(1)
                        Image(systemName: "chevron.down")
                            .font(.system(size: 10, weight: .semibold))
                            .foregroundColor(EchoLinkColors.textOnSurfaceVariant)
                    }
                    .padding(.horizontal, 11)
                    .frame(height: 34)
                    .background(EchoLinkColors.surfaceContainerHigh)
                    .clipShape(Capsule())
                    .overlay(Capsule().stroke(EchoLinkColors.armorStroke, lineWidth: 1))
                }

                if showEmergencySquelch {
                    Button(action: onEmergencySquelchClick) {
                        Image(systemName: "mic.slash.fill")
                            .font(.system(size: 15))
                            .foregroundColor(EchoLinkColors.errorLight)
                            .frame(width: 34, height: 34)
                            .background(EchoLinkColors.errorLight.opacity(0.16))
                            .clipShape(Circle())
                    }
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(EchoLinkColors.surfaceDim.opacity(0.95))
    }
}

struct EchoLinkBottomNavBar: View {
    let selectedTab: NavTab
    let onSelectTab: (NavTab) -> Void

    var body: some View {
        HStack {
            ForEach(NavTab.allCases) { tab in
                let isSelected = (tab == selectedTab)
                let tint = isSelected ? EchoLinkColors.cyberCyan : EchoLinkColors.textOnSurfaceVariant
                Button {
                    onSelectTab(tab)
                } label: {
                    VStack(spacing: 3) {
                        Image(systemName: tab.sfSymbol)
                            .font(.system(size: 18, weight: isSelected ? .semibold : .regular))
                            .foregroundColor(tint)
                        Text(tab.label)
                            .font(.system(size: 11, weight: isSelected ? .semibold : .regular))
                            .foregroundColor(tint)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 6)
                    .background(
                        RoundedRectangle(cornerRadius: 14)
                            .fill(isSelected ? EchoLinkColors.cyberCyan.opacity(0.12) : Color.clear)
                    )
                }
                .frame(maxWidth: .infinity)
            }
        }
        .padding(.horizontal, 12)
        .frame(height: 60)
        .background(EchoLinkColors.surfaceDim.opacity(0.96))
        .overlay(
            Rectangle()
                .frame(height: 0.5)
                .foregroundColor(EchoLinkColors.armorStroke),
            alignment: .top
        )
    }
}
