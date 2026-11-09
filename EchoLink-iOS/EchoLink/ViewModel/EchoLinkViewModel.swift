import AVFoundation
import Combine
import Foundation
import SwiftUI

/// Main ViewModel for EchoLink iOS, mirroring Android's `EchoLinkViewModel.kt` 1:1.
final class EchoLinkViewModel: ObservableObject {
    let audioEngine = NativeAudioEngine()
    let earbudRoutingManager = EarbudRoutingManager()
    let transportEngine: MultiTransportEngine

    @Published var selectedTab: NavTab = .mesh
    @Published var isInActiveVoiceSession: Bool = false
    @Published var showCallLinkQrSheet: Bool = false
    @Published var showScanLinkSheet: Bool = false
    @Published var showAddPeerSheet: Bool = false
    @Published var renamingPeerTarget: (String, String)? = nil

    @Published var readinessState = HardwareReadinessState()
    @Published var recentPeers: [RecentPeer] = []
    @Published var customPeerNames: [String: String] = [:]

    @Published var intercomMode: IntercomMode = .openMic
    @Published var isMicMuted: Bool = false
    @Published var isPttTransmitting: Bool = false
    @Published var proximityNoiseGateActive: Bool = true
    @Published var spatialAudioActive: Bool = true
    @Published var loopbackTestActive: Bool = false
    @Published var peerRxGainPercent: Int = 100
    @Published var isRxBoost3dB: Bool = false

    @Published var liveSpectrumBands: [Float] = Array(repeating: 0.12, count: 14)
    @Published var liveVoiceDbFs: Int = -60
    @Published var toastBannerMessage: (String, String)? = nil
    @Published var keyRotationRemainingSeconds: Int = 300

    @Published var selectedSampleRateHz: Int = 48000
    @Published var selectedFrameSizeMs: Int = 20
    @Published var selectedOpusBitrateKbps: Int = 24

    // Forwarded transport states for SwiftUI
    @Published var discoveredPeers: [PeerNode] = []
    @Published var activePeers: [PeerNode] = []
    @Published var packetTelemetry = PacketTelemetry()
    @Published var callLinkSession: CallLinkSession
    @Published var talkTargetPeerIp: String? = nil
    @Published var earbudStatus = EarbudStatus()

    private var cancellables = Set<AnyCancellable>()
    private var spectrumTimer: Timer?
    private var keyRotationTimer: Timer?

    init() {
        let engine = MultiTransportEngine(audioEngine: audioEngine)
        self.transportEngine = engine
        self.callLinkSession = engine.callLinkSession

        self.customPeerNames = Self.loadSavedCustomPeerNames()
        engine.setInitialCustomNames(self.customPeerNames)
        self.recentPeers = Self.loadSavedRecentPeers(customNames: self.customPeerNames)

        bindStreams()
        refreshPermissionAndHardwareReadiness()
        transportEngine.startRadarDiscovery()
        startLiveTelemetryAndSpectrumLoop()
        startKeyRotationCountdownLoop()

        earbudRoutingManager.onStemButtonTriggered = { [weak self] in
            self?.handleHardwareStemButtonPress()
        }

        transportEngine.onRemotePeerJoinedCallback = { [weak self] joinedPeer in
            guard let self = self else { return }
            self.saveRecentPeer(joinedPeer)
            if !self.isInActiveVoiceSession {
                self.earbudRoutingManager.engageVoiceCallAudioRoute()
                self.audioEngine.startEngine(sampleRate: self.selectedSampleRateHz, frameMs: self.selectedFrameSizeMs)
                self.isInActiveVoiceSession = true
            }
            self.toastBannerMessage = (
                "\(joinedPeer.name) Joined Room",
                "Connected on \(joinedPeer.ipAddress)"
            )
        }
    }

    private func bindStreams() {
        transportEngine.$discoveredPeers
            .receive(on: DispatchQueue.main)
            .assign(to: &$discoveredPeers)

        transportEngine.$activePeers
            .receive(on: DispatchQueue.main)
            .assign(to: &$activePeers)

        transportEngine.$packetTelemetry
            .receive(on: DispatchQueue.main)
            .assign(to: &$packetTelemetry)

        transportEngine.$callLinkSession
            .receive(on: DispatchQueue.main)
            .assign(to: &$callLinkSession)

        transportEngine.$talkTargetPeerIp
            .receive(on: DispatchQueue.main)
            .assign(to: &$talkTargetPeerIp)

        earbudRoutingManager.$earbudStatus
            .receive(on: DispatchQueue.main)
            .sink { [weak self] status in
                self?.earbudStatus = status
                self?.readinessState.earbudsConnected = status.isConnected
                self?.readinessState.connectedAudioDeviceName = "\(status.deviceName) (\(status.protocolLabel))"
            }
            .store(in: &cancellables)
    }

    func refreshPermissionAndHardwareReadiness() {
        earbudRoutingManager.refreshConnectedAudioDevices()
        let micStatus = AVAudioSession.sharedInstance().recordPermission
        let camStatus = AVCaptureDevice.authorizationStatus(for: .video)

        readinessState.micPermissionGranted = (micStatus == .granted)
        readinessState.cameraPermissionGranted = (camStatus == .authorized)
        readinessState.bluetoothPermissionGranted = true
        readinessState.nearbyWifiPermissionGranted = true
    }

    func requestHardwarePermissions() {
        AVAudioSession.sharedInstance().requestRecordPermission { [weak self] _ in
            DispatchQueue.main.async {
                self?.refreshPermissionAndHardwareReadiness()
            }
        }
        AVCaptureDevice.requestAccess(for: .video) { [weak self] _ in
            DispatchQueue.main.async {
                self?.refreshPermissionAndHardwareReadiness()
            }
        }
    }

    func selectTab(_ tab: NavTab) {
        selectedTab = tab
        if tab == .hud {
            refreshPermissionAndHardwareReadiness()
        }
    }

    func toggleWifiDirectRadar() {
        readinessState.wifiDirectRadarEnabled.toggle()
        if readinessState.wifiDirectRadarEnabled {
            transportEngine.startRadarDiscovery()
        } else {
            transportEngine.stopRadarDiscovery()
        }
    }

    func toggleOboeLowLatency() {
        readinessState.oboeLowLatencyEnabled.toggle()
    }

    func launchSonarRadar() {
        readinessState.isSyncingNodes = true
        refreshPermissionAndHardwareReadiness()
        transportEngine.startRadarDiscovery()
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
            self.readinessState.isSyncingNodes = false
            self.selectedTab = .mesh
        }
    }

    func connectToRadarNode(_ peer: PeerNode) {
        let resolvedName = transportEngine.resolvePeerName(ipAddress: peer.ipAddress, fallbackName: peer.name)
        var resolvedPeer = peer
        resolvedPeer.name = resolvedName

        toastBannerMessage = (
            "Linking with \(resolvedPeer.name) (\(resolvedPeer.ipAddress))",
            "Protocol: \(resolvedPeer.transport.fullName) • Port \(resolvedPeer.voicePort)"
        )
        saveRecentPeer(resolvedPeer)
        earbudRoutingManager.engageVoiceCallAudioRoute()
        audioEngine.startEngine(sampleRate: selectedSampleRateHz, frameMs: selectedFrameSizeMs)
        transportEngine.setIsRoomHost(true)
        transportEngine.connectToPeer(resolvedPeer)
        showAddPeerSheet = false
        isInActiveVoiceSession = true
    }

    func connectToRecentPeer(_ recent: RecentPeer) {
        let resolvedName = transportEngine.resolvePeerName(ipAddress: recent.ipAddress, fallbackName: recent.name)
        let peer = PeerNode(
            id: abs(recent.ipAddress.hashValue),
            name: resolvedName,
            roleTag: "Recent Peer",
            nodeCode: "IP \(recent.ipAddress)",
            distanceMeters: 5,
            transport: recent.transport,
            ipAddress: recent.ipAddress,
            isRealNetworkPeer: true
        )
        connectToRadarNode(peer)
    }

    // MARK: - Connected Device History Deletion & Renaming

    func removeRecentPeer(_ ipAddress: String) {
        recentPeers.removeAll { $0.ipAddress == ipAddress || $0.id == ipAddress }
        Self.persistRecentPeers(recentPeers)
    }

    func clearRecentPeers() {
        recentPeers = []
        UserDefaults.standard.removeObject(forKey: "recent_peers_json")
        toastBannerMessage = (
            "Device History Cleared",
            "Removed all saved connected devices"
        )
    }

    func openRenamePeerDialog(ipAddress: String, currentName: String) {
        renamingPeerTarget = (ipAddress, currentName)
    }

    func closeRenamePeerDialog() {
        renamingPeerTarget = nil
    }

    func renamePeer(ipAddress: String, newName: String) {
        let clean = newName.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !clean.isEmpty else {
            renamingPeerTarget = nil
            return
        }
        customPeerNames[ipAddress] = clean
        Self.persistCustomPeerNames(customPeerNames)
        transportEngine.setCustomPeerName(ipAddress: ipAddress, customName: clean)

        recentPeers = recentPeers.map { rec in
            if rec.ipAddress == ipAddress {
                var copy = rec
                copy.name = clean
                return copy
            }
            return rec
        }
        Self.persistRecentPeers(recentPeers)
        renamingPeerTarget = nil
        toastBannerMessage = (
            "Device Renamed to \"\(clean)\"",
            "Saved for \(ipAddress)"
        )
    }

    private func saveRecentPeer(_ peer: PeerNode) {
        guard !peer.ipAddress.isEmpty && peer.ipAddress != "0.0.0.0" else { return }
        let resolvedName = transportEngine.resolvePeerName(ipAddress: peer.ipAddress, fallbackName: peer.name)
        let entry = RecentPeer(
            id: peer.ipAddress,
            name: resolvedName,
            ipAddress: peer.ipAddress,
            subtitle: "\(peer.ipAddress) • \(peer.transport.badge)",
            transport: peer.transport
        )
        let filtered = recentPeers.filter { $0.ipAddress != peer.ipAddress }.prefix(7)
        recentPeers = [entry] + Array(filtered)
        Self.persistRecentPeers(recentPeers)
    }

    private static func loadSavedRecentPeers(customNames: [String: String]) -> [RecentPeer] {
        guard let data = UserDefaults.standard.data(forKey: "recent_peers_json"),
              let decoded = try? JSONDecoder().decode([RecentPeer].self, from: data) else {
            return []
        }
        return decoded.map { rec in
            if let custom = customNames[rec.ipAddress], !custom.isEmpty {
                var copy = rec
                copy.name = custom
                return copy
            }
            return rec
        }
    }

    private static func persistRecentPeers(_ peers: [RecentPeer]) {
        if let data = try? JSONEncoder().encode(peers) {
            UserDefaults.standard.set(data, forKey: "recent_peers_json")
        }
    }

    private static func loadSavedCustomPeerNames() -> [String: String] {
        guard let data = UserDefaults.standard.data(forKey: "custom_peer_names_json"),
              let decoded = try? JSONDecoder().decode([String: String].self, from: data) else {
            return [:]
        }
        return decoded
    }

    private static func persistCustomPeerNames(_ names: [String: String]) {
        if let data = try? JSONEncoder().encode(names) {
            UserDefaults.standard.set(data, forKey: "custom_peer_names_json")
        }
    }

    // MARK: - Room & Modal Controls

    func openCallLinkQrModal() {
        transportEngine.setIsRoomHost(true)
        _ = transportEngine.refreshCallLinkSession()
        keyRotationRemainingSeconds = 300
        showCallLinkQrSheet = true
        toastBannerMessage = (
            "Offline Mesh Room Created",
            "Host IP: \(callLinkSession.hostIp) • PIN: \(callLinkSession.pinCode)"
        )
    }

    func closeCallLinkQrModal() { showCallLinkQrSheet = false }
    func openScanLinkModal() { showScanLinkSheet = true }
    func closeScanLinkModal() { showScanLinkSheet = false }
    func openAddPeerSheet() { showAddPeerSheet = true }
    func closeAddPeerSheet() { showAddPeerSheet = false }

    func selectTalkTargetPeer(_ peerIp: String?) {
        transportEngine.setTalkTargetPeerIp(peerIp)
    }

    func removePeerFromRoom(_ peerIp: String) {
        transportEngine.removePeerFromRoom(peerIp)
        if transportEngine.activePeers.isEmpty {
            disconnectActiveSession()
        }
    }

    func joinByScannedQrOrPin(qrOrIp: String, pin: String) {
        let trimmed = qrOrIp.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            toastBannerMessage = (
                "Missing Peer IP or QR URI",
                "Scan a peer's QR code or enter their P2P/LAN IP address"
            )
            return
        }
        closeScanLinkModal()
        showAddPeerSheet = false
        earbudRoutingManager.engageVoiceCallAudioRoute()
        audioEngine.startEngine(sampleRate: selectedSampleRateHz, frameMs: selectedFrameSizeMs)
        let joined = transportEngine.connectByQrUriOrIp(trimmed, enteredPin: pin, appendToRoom: true)
        saveRecentPeer(joined)
        isInActiveVoiceSession = true
    }

    func dismissToastBanner() { toastBannerMessage = nil }

    // MARK: - Audio & Intercom Controls

    func setIntercomMode(_ mode: IntercomMode) {
        intercomMode = mode
        let isPtt = (mode == .pushToTalk)
        transportEngine.isPttActive = isPtt && isPttTransmitting
        audioEngine.setPttState(pttMode: isPtt, pressed: isPttTransmitting)
    }

    func setPttPressed(_ pressed: Bool) {
        isPttTransmitting = pressed
        let isPtt = (intercomMode == .pushToTalk)
        transportEngine.isPttActive = isPtt && pressed
        audioEngine.setPttState(pttMode: isPtt, pressed: pressed)
    }

    func toggleMicMute() {
        isMicMuted.toggle()
        audioEngine.setMuted(isMicMuted)
    }

    func toggleProximityNoiseGate() {
        proximityNoiseGateActive.toggle()
        audioEngine.setProximityNoiseGate(proximityNoiseGateActive)
    }

    func toggleSpatialAudio() {
        spatialAudioActive.toggle()
        audioEngine.setSpatialAudio(spatialAudioActive)
    }

    func toggleLoopbackTest() {
        loopbackTestActive.toggle()
        if loopbackTestActive {
            earbudRoutingManager.engageVoiceCallAudioRoute()
            audioEngine.startEngine(sampleRate: selectedSampleRateHz, frameMs: selectedFrameSizeMs)
        } else if !isInActiveVoiceSession {
            audioEngine.stopEngine()
        }
        audioEngine.setLoopback(loopbackTestActive)
    }

    func updatePeerRxGain(_ fraction0To1: Float) {
        let pct = min(max(Int((fraction0To1 * 150.0).rounded()), 0), 200)
        peerRxGainPercent = pct
        audioEngine.setRxGain(multiplier: Float(pct) / 100.0, boost3dB: isRxBoost3dB)
    }

    func toggleRxBoost3dB() {
        isRxBoost3dB.toggle()
        peerRxGainPercent = isRxBoost3dB ? 150 : 100
        audioEngine.setRxGain(multiplier: Float(peerRxGainPercent) / 100.0, boost3dB: isRxBoost3dB)
    }

    func addPeerToMeshSession() {
        showAddPeerSheet = true
    }

    func handleHardwareStemButtonPress() {
        if intercomMode == .pushToTalk {
            setPttPressed(!isPttTransmitting)
        } else {
            toggleMicMute()
        }
    }

    func selectAudioRoute(_ route: String) {
        earbudRoutingManager.selectAudioRoute(route)
    }

    func updateAudioEngineConfig(sampleRate: Int, frameMs: Int, bitrateKbps: Int) {
        selectedSampleRateHz = sampleRate
        selectedFrameSizeMs = frameMs
        selectedOpusBitrateKbps = bitrateKbps
        if isInActiveVoiceSession || loopbackTestActive {
            audioEngine.stopEngine()
            audioEngine.startEngine(sampleRate: sampleRate, frameMs: frameMs)
        }
    }

    func disconnectActiveSession() {
        isInActiveVoiceSession = false
        isPttTransmitting = false
        if !loopbackTestActive {
            audioEngine.stopEngine()
        }
        transportEngine.disconnectAll()
    }

    private func startLiveTelemetryAndSpectrumLoop() {
        spectrumTimer = Timer.scheduledTimer(withTimeInterval: 0.09, repeats: true) { [weak self] _ in
            guard let self = self else { return }
            if self.isInActiveVoiceSession || self.loopbackTestActive {
                let bands = self.audioEngine.getSpectrumBands()
                self.liveSpectrumBands = (0..<14).map { idx in
                    min(max(idx < bands.count ? bands[idx] : 0.12, 0.12), 1.0)
                }
                self.liveVoiceDbFs = min(max(Int(self.audioEngine.getCurrentDbFs().rounded()), -60), 0)
            } else {
                self.liveSpectrumBands = Array(repeating: 0.12, count: 14)
                self.liveVoiceDbFs = -60
            }
        }
    }

    private func startKeyRotationCountdownLoop() {
        keyRotationTimer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            guard let self = self else { return }
            let next = self.keyRotationRemainingSeconds - 1
            if next <= 0 {
                _ = self.transportEngine.refreshCallLinkSession()
                self.keyRotationRemainingSeconds = 300
            } else {
                self.keyRotationRemainingSeconds = next
            }
        }
    }
}
