import AVFoundation
import Combine
import Foundation
import MediaPlayer

/// Manages AirPods / Bluetooth HFP / BLE Audio routing and hardware stem button events on iOS.
final class EarbudRoutingManager: ObservableObject {
    @Published private(set) var earbudStatus = EarbudStatus()

    var onStemButtonTriggered: (() -> Void)?

    init() {
        refreshConnectedAudioDevices()
        registerRouteChangeNotifications()
        configureRemoteCommandCenter()
    }

    func refreshConnectedAudioDevices() {
        let session = AVAudioSession.sharedInstance()
        let outputs = session.currentRoute.outputs

        var routes: [String] = []
        var activeName = "iPhone Speakerphone"
        var activeProto = "Internal Hardware DSP"
        var isHeadset = false
        var routeType = "SPEAKER"

        for desc in outputs {
            routes.append(desc.portName)
            switch desc.portType {
            case .bluetoothHFP, .bluetoothA2DP, .bluetoothLE:
                isHeadset = true
                activeName = desc.portName
                activeProto = desc.portType == .bluetoothLE ? "BLE Audio LC3" : "Bluetooth HFP/AAC"
                routeType = "BLUETOOTH"
            case .headphones, .headsetMic:
                isHeadset = true
                activeName = desc.portName
                activeProto = "Wired Low-Latency"
                routeType = "WIRED"
            case .builtInReceiver:
                activeName = "Handset Receiver"
                routeType = "EARPIECE"
            default:
                break
            }
        }

        if !routes.contains("iPhone Speakerphone") { routes.append("iPhone Speakerphone") }
        if !routes.contains("Handset Receiver") { routes.append("Handset Receiver") }

        earbudStatus = EarbudStatus(
            isConnected: isHeadset,
            deviceName: activeName,
            batteryPercent: nil,
            protocolLabel: activeProto,
            routeType: routeType,
            availableRoutes: routes
        )
    }

    func engageVoiceCallAudioRoute() {
        let session = AVAudioSession.sharedInstance()
        do {
            try session.setCategory(
                .playAndRecord,
                mode: .voiceChat,
                options: [.allowBluetooth, .allowBluetoothA2DP, .defaultToSpeaker]
            )
            try session.setActive(true)
            refreshConnectedAudioDevices()
        } catch {
        }
    }

    func selectAudioRoute(_ routeName: String) {
        let session = AVAudioSession.sharedInstance()
        do {
            if routeName.localizedCaseInsensitiveContains("Speaker") {
                try session.overrideOutputAudioPort(.speaker)
            } else {
                try session.overrideOutputAudioPort(.none)
            }
            refreshConnectedAudioDevices()
        } catch {
        }
    }

    private func registerRouteChangeNotifications() {
        NotificationCenter.default.addObserver(
            forName: AVAudioSession.routeChangeNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.refreshConnectedAudioDevices()
        }
    }

    private func configureRemoteCommandCenter() {
        let center = MPRemoteCommandCenter.shared()
        center.togglePlayPauseCommand.isEnabled = true
        center.togglePlayPauseCommand.addTarget { [weak self] _ in
            self?.onStemButtonTriggered?()
            return .success
        }
    }
}
