package com.antigravity.echolink.model

import kotlinx.serialization.Serializable

enum class TransportTier(
    val badge: String,
    val fullName: String,
    val bandLabel: String,
    val colorHex: Long
) {
    LAN_HOTSPOT("LAN", "LAN Mesh", "Wi-Fi LAN", 0xFF4EDEA3),
    WIFI_DIRECT("P2P", "Wi-Fi Direct", "Wi-Fi Direct P2P", 0xFF00F0FF),
    NEARBY_STAR("MESH", "Nearby P2P Star", "Hybrid BLE->Wi-Fi", 0xFF00DBE9),
    BLE_L2CAP("BLE", "BLE Audio / L2CAP", "BLE L2CAP", 0xFFFFB95F)
}

enum class IntercomMode {
    OPEN_MIC,
    PUSH_TO_TALK
}

enum class NavTab(val route: String, val label: String) {
    MESH("mesh", "Radar"),
    AUDIO("audio", "Audio"),
    RF_STATS("rf_stats", "Signal"),
    HUD("hud", "Setup")
}

@Serializable
data class PeerNode(
    val id: Int,
    val name: String,
    val roleTag: String,
    val nodeCode: String,
    val distanceMeters: Int,
    val transport: TransportTier,
    val ipAddress: String,
    val controlPort: Int = 51110,
    val voicePort: Int = 51111,
    val rssiDbm: Int = -55,
    val pingMs: Int = 0,
    val bitrateKbps: Int = 24,
    val isSpeaking: Boolean = false,
    val voiceDbFs: Float = -60f,
    val earbudCodec: String = "OPUS P2P",
    val isRealNetworkPeer: Boolean = true
)

@Serializable
data class RecentPeer(
    val id: String,
    val name: String,
    val ipAddress: String,
    val subtitle: String,
    val transport: TransportTier,
    val lastConnectedEpochMs: Long = System.currentTimeMillis()
)

data class EarbudStatus(
    val isConnected: Boolean = false,
    val deviceName: String = "Phone Earpiece / Speaker",
    val batteryPercent: Int? = null,
    val protocolLabel: String = "Internal Hardware DSP",
    val routeType: String = "SPEAKER",
    val availableRoutes: List<String> = listOf(
        "Phone Speakerphone",
        "Handset Earpiece"
    )
)

data class CallLinkSession(
    val sessionId: String,
    val pinCode: String,
    val hostIp: String,
    val controlPort: Int = 51110,
    val voicePort: Int = 51111,
    val transportTag: String = "P2P-DIRECT",
    val publicKeyHex: String,
    val qrUri: String,
    val remainingKeySeconds: Int = 300
)

data class PacketTelemetry(
    val magicHeaderHex: String = "0x4147 (\"AG\")",
    val txSequenceNumber: Int = 0,
    val rxSequenceNumber: Int = 0,
    val timestampTicks48k: Long = 0L,
    val flagsByteHex: String = "0x00 (IDLE)",
    val nonceHex: String = "— (Awaiting Active Stream)",
    val cipherSuite: String = "ChaCha20-Poly1305 (256-bit)",
    val pingMs: Int = 0,
    val jitterMs: Int = 0,
    val packetLossPercent: Float = 0.0f,
    val rssiDbm: Int = 0,
    val wifiFrequencyMhz: Int = 0,
    val codecBitrateKbps: Int = 24,
    val sampleRateHz: Int = 48000,
    val frameSizeMs: Int = 20,
    val bufferFrames: Int = 128
)

data class HardwareReadinessState(
    val earbudsConnected: Boolean = false,
    val connectedAudioDeviceName: String = "Internal Handset Audio",
    val wifiDirectRadarEnabled: Boolean = true,
    val oboeLowLatencyEnabled: Boolean = true,
    val bluetoothPermissionGranted: Boolean = false,
    val nearbyWifiPermissionGranted: Boolean = false,
    val micPermissionGranted: Boolean = false,
    val cameraPermissionGranted: Boolean = false,
    val isSyncingNodes: Boolean = false
)
