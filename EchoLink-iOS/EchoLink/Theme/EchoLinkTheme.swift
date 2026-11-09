import SwiftUI

enum EchoLinkColors {
    static let obsidianCanvas = Color(hex: 0x080A0F)
    static let surfaceDim = Color(hex: 0x111319)
    static let surfaceContainerLowest = Color(hex: 0x0C0E13)
    static let surfaceContainerLow = Color(hex: 0x191B21)
    static let surfaceContainer = Color(hex: 0x1E1F25)
    static let surfaceContainerHigh = Color(hex: 0x282A30)
    static let surfaceContainerHighest = Color(hex: 0x33353A)
    static let hullCard = Color(hex: 0x121722)
    static let armorStroke = Color(hex: 0x1F293D)
    static let outlineVariant = Color(hex: 0x3B494B)

    static let cyberCyan = Color(hex: 0x00F0FF)
    static let primaryLight = Color(hex: 0xDBFCFF)
    static let primaryFixedDim = Color(hex: 0x00DBE9)
    static let onPrimaryContainer = Color(hex: 0x00363A)

    static let emeraldPulse = Color(hex: 0x4EDEA3)
    static let emeraldSecondary = Color(hex: 0x10B981)
    static let secondaryContainer = Color(hex: 0x00A572)
    static let onSecondaryContainer = Color(hex: 0x00311F)

    static let amberWarning = Color(hex: 0xFFB95F)
    static let combatCrimson = Color(hex: 0xEF4444)
    static let errorLight = Color(hex: 0xFFB4AB)

    static let textPrimary = Color(hex: 0xF8FAFC)
    static let textOnSurface = Color(hex: 0xE2E2E9)
    static let textSecondary = Color(hex: 0x94A3B8)
    static let textOnSurfaceVariant = Color(hex: 0xB9CACB)
}

extension Color {
    init(hex: UInt64, alpha: Double = 1.0) {
        let r = Double((hex >> 16) & 0xFF) / 255.0
        let g = Double((hex >> 8) & 0xFF) / 255.0
        let b = Double(hex & 0xFF) / 255.0
        self.init(.sRGB, red: r, green: g, blue: b, opacity: alpha)
    }
}

enum EchoLinkTypography {
    static let headlineXlMobile = Font.system(size: 28, weight: .bold, design: .rounded)
    static let headlineSm = Font.system(size: 16, weight: .semibold, design: .rounded)
    static let bodyMd = Font.system(size: 14, weight: .regular, design: .default)
    static let bodySm = Font.system(size: 12, weight: .regular, design: .default)
    static let telemetryMd = Font.system(size: 14, weight: .medium, design: .monospaced)
    static let telemetrySm = Font.system(size: 11, weight: .medium, design: .monospaced)
    static let labelCaps = Font.system(size: 11, weight: .bold, design: .rounded)
}
