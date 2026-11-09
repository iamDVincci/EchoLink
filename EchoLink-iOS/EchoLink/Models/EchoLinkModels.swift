import Foundation
import SwiftUI

enum TransportTier: String, Codable, CaseIterable, Identifiable {
    case lanHotspot = "LAN_HOTSPOT"
    case wifiDirect = "WIFI_DIRECT"
    case nearbyStar = "NEARBY_STAR"
    case bleL2cap = "BLE_L2CAP"

    var id: String { rawValue }

    var badge: String {
        switch self {
        case .lanHotspot: return "LAN"
        case .wifiDirect: return "P2P"
        case .nearbyStar: return "MESH"
        case .bleL2cap: return "BLE"
        }
    }

    var fullName: String {
        switch self {
        case .lanHotspot: return "LAN Mesh"
        case .wifiDirect: return "Wi-Fi Direct"
        case .nearbyStar: return "Nearby P2P Star"
        case .bleL2cap: return "BLE Audio / L2CAP"
        }
    }

    var bandLabel: String {
        switch self {
        case .lanHotspot: return "Wi-Fi LAN"
        case .wifiDirect: return "Wi-Fi Direct P2P"
        case .nearbyStar: return "Hybrid BLE->Wi-Fi"
        case .bleL2cap: return "BLE L2CAP"
        }
    }

    var colorHex: UInt64 {
        switch self {
        case .lanHotspot: return 0xFF4EDEA3
        case .wifiDirect: return 0xFF00F0FF
        case .nearbyStar: return 0xFF00DBE9
        case .bleL2cap: return 0xFFFFB95F
        }
    }

    var accentColor: Color {
        switch self {
        case .lanHotspot: return EchoLinkColors.emeraldPulse
        case .wifiDirect: return EchoLinkColors.cyberCyan
        case .nearbyStar: return EchoLinkColors.primaryFixedDim
        case .bleL2cap: return EchoLinkColors.amberWarning
        }
    }
}

enum IntercomMode: String, Codable {
    case openMic = "OPEN_MIC"
    case pushToTalk = "PUSH_TO_TALK"
}

enum NavTab: String, CaseIterable, Identifiable {
    case mesh = "mesh"
    case audio = "audio"
    case rfStats = "rf_stats"
    case hud = "hud"

    var id: String { rawValue }

    var label: String {
        switch self {
        case .mesh: return "Radar"
        case .audio: return "Audio"
        case .rfStats: return "Signal"
        case .hud: return "Setup"
        }
    }

    var sfSymbol: String {
        switch self {
        case .mesh: return "dot.radiowaves.left.and.right"
        case .audio: return "slider.vertical.3"
        case .rfStats: return "waveform.path.ecg"
        case .hud: return "antenna.radiowaves.left.and.right"
        }
    }
}

struct PeerNode: Identifiable, Codable, Equatable {
    var id: Int
    var name: String
    var roleTag: String
    var nodeCode: String
    var distanceMeters: Int
    var transport: TransportTier
    var ipAddress: String
    var controlPort: Int = 51110
    var voicePort: Int = 51111
    var rssiDbm: Int = -55
    var pingMs: Int = 0
    var bitrateKbps: Int = 24
    var isSpeaking: Bool = false
    var voiceDbFs: Float = -60.0
    var earbudCodec: String = "OPUS P2P"
    var isRealNetworkPeer: Bool = true
}

struct RecentPeer: Identifiable, Codable, Equatable {
    var id: String
    var name: String
    var ipAddress: String
    var subtitle: String
    var transport: TransportTier
    var lastConnectedEpochMs: Int64 = Int64(Date().timeIntervalSince1970 * 1000)
}

struct EarbudStatus: Equatable {
    var isConnected: Bool = false
    var deviceName: String = "iPhone Earpiece / Speaker"
    var batteryPercent: Int? = nil
    var protocolLabel: String = "Internal Hardware DSP"
    var routeType: String = "SPEAKER"
    var availableRoutes: [String] = [
        "iPhone Speakerphone",
        "Handset Receiver"
    ]
}

struct CallLinkSession: Equatable {
    var sessionId: String
    var pinCode: String
    var hostIp: String
    var controlPort: Int = 51110
    var voicePort: Int = 51111
    var transportTag: String = "P2P-DIRECT"
    var publicKeyHex: String
    var qrUri: String
    var remainingKeySeconds: Int = 300
}

struct PacketTelemetry: Equatable {
    var magicHeaderHex: String = "0x4147 (\"AG\")"
    var txSequenceNumber: Int = 0
    var rxSequenceNumber: Int = 0
    var timestampTicks48k: Int64 = 0
    var flagsByteHex: String = "0x00 (IDLE)"
    var nonceHex: String = "— (Awaiting Active Stream)"
    var cipherSuite: String = "ChaCha20-Poly1305 (256-bit)"
    var pingMs: Int = 0
    var jitterMs: Int = 0
    var packetLossPercent: Float = 0.0
    var rssiDbm: Int = 0
    var wifiFrequencyMhz: Int = 5180
    var codecBitrateKbps: Int = 24
    var sampleRateHz: Int = 48000
    var frameSizeMs: Int = 20
    var bufferFrames: Int = 128
}

struct HardwareReadinessState: Equatable {
    var earbudsConnected: Bool = false
    var connectedAudioDeviceName: String = "Internal Handset Audio"
    var wifiDirectRadarEnabled: Bool = true
    var oboeLowLatencyEnabled: Bool = true
    var bluetoothPermissionGranted: Bool = true
    var nearbyWifiPermissionGranted: Bool = true
    var micPermissionGranted: Bool = false
    var cameraPermissionGranted: Bool = false
    var isSyncingNodes: Bool = false
}
