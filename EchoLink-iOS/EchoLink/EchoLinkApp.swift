import SwiftUI

@main
struct EchoLinkApp: App {
    @StateObject private var viewModel = EchoLinkViewModel()

    var body: some Scene {
        WindowGroup {
            EchoLinkRootView(viewModel: viewModel)
                .preferredColorScheme(.dark)
                .onOpenURL { url in
                    if url.scheme == "echolink" {
                        let pin = URLComponents(url: url, resolvingAgainstBaseURL: false)?
                            .queryItems?
                            .first(where: { $0.name == "pin" })?
                            .value ?? ""
                        viewModel.joinByScannedQrOrPin(qrOrIp: url.absoluteString, pin: pin)
                    }
                }
        }
    }
}

struct EchoLinkRootView: View {
    @ObservedObject var viewModel: EchoLinkViewModel
    @State private var renameText: String = ""

    private var topTitle: String {
        viewModel.isInActiveVoiceSession ? "Voice Link" : "EchoLink"
    }

    private var topSubtitle: String {
        if viewModel.isInActiveVoiceSession { return "Encrypted P2P" }
        switch viewModel.selectedTab {
        case .mesh: return "Sonar Radar"
        case .audio: return "Audio Studio"
        case .rfStats: return "Signal Health"
        case .hud: return "Device Setup"
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            EchoLinkTopAppBar(
                title: topTitle,
                subtitle: topSubtitle,
                earbudStatus: viewModel.earbudStatus,
                onSelectRoute: viewModel.selectAudioRoute,
                showBackButton: viewModel.isInActiveVoiceSession,
                showEmergencySquelch: viewModel.isInActiveVoiceSession,
                onBackClick: { viewModel.disconnectActiveSession() },
                onEmergencySquelchClick: { viewModel.toggleMicMute() }
            )

            ZStack {
                EchoLinkColors.surfaceDim.ignoresSafeArea()

                if viewModel.isInActiveVoiceSession {
                    ActiveVoiceIntercomScreen(
                        activePeers: viewModel.activePeers,
                        talkTargetPeerIp: viewModel.talkTargetPeerIp,
                        telemetry: viewModel.packetTelemetry,
                        intercomMode: viewModel.intercomMode,
                        isMicMuted: viewModel.isMicMuted,
                        isPttTransmitting: viewModel.isPttTransmitting,
                        proximityNoiseGateActive: viewModel.proximityNoiseGateActive,
                        spatialAudioActive: viewModel.spatialAudioActive,
                        peerRxGainPercent: viewModel.peerRxGainPercent,
                        isRxBoost3dB: viewModel.isRxBoost3dB,
                        spectrumBands: viewModel.liveSpectrumBands,
                        liveVoiceDbFs: viewModel.liveVoiceDbFs,
                        onSelectTalkTarget: viewModel.selectTalkTargetPeer,
                        onRenamePeer: viewModel.openRenamePeerDialog,
                        onRemovePeerFromRoom: viewModel.removePeerFromRoom,
                        onSelectMode: viewModel.setIntercomMode,
                        onToggleMute: viewModel.toggleMicMute,
                        onPttPressChanged: viewModel.setPttPressed,
                        onToggleProximityGate: viewModel.toggleProximityNoiseGate,
                        onToggleSpatialAudio: viewModel.toggleSpatialAudio,
                        onUpdateRxGain: viewModel.updatePeerRxGain,
                        onToggleRxBoost: viewModel.toggleRxBoost3dB,
                        onAddPeer: viewModel.addPeerToMeshSession,
                        onDisconnect: viewModel.disconnectActiveSession
                    )
                } else {
                    switch viewModel.selectedTab {
                    case .hud:
                        ReadinessCheckScreen(
                            readinessState: viewModel.readinessState,
                            sampleRateHz: viewModel.selectedSampleRateHz,
                            frameSizeMs: viewModel.selectedFrameSizeMs,
                            onToggleWifiDirect: viewModel.toggleWifiDirectRadar,
                            onToggleOboeAudio: viewModel.toggleOboeLowLatency,
                            onRequestPermissions: viewModel.requestHardwarePermissions,
                            onLaunchSonarRadar: viewModel.launchSonarRadar
                        )
                    case .mesh:
                        SonarRadarHomeScreen(
                            discoveredPeers: viewModel.discoveredPeers,
                            recentPeers: viewModel.recentPeers,
                            localIp: viewModel.callLinkSession.hostIp,
                            wifiFrequencyMhz: viewModel.packetTelemetry.wifiFrequencyMhz,
                            toastBanner: viewModel.toastBannerMessage,
                            onTapPeerNode: viewModel.connectToRadarNode,
                            onTapRecentPeer: viewModel.connectToRecentPeer,
                            onRemoveRecentPeer: viewModel.removeRecentPeer,
                            onClearRecentPeers: viewModel.clearRecentPeers,
                            onRenamePeer: viewModel.openRenamePeerDialog,
                            onCreateCallLink: viewModel.openCallLinkQrModal,
                            onScanCallLink: viewModel.openScanLinkModal,
                            onDismissToast: viewModel.dismissToastBanner
                        )
                    case .rfStats:
                        RfStatsScreen(
                            telemetry: viewModel.packetTelemetry,
                            localIpAddress: viewModel.callLinkSession.hostIp
                        )
                    case .audio:
                        AudioEngineSettingsScreen(
                            sampleRateHz: viewModel.selectedSampleRateHz,
                            frameSizeMs: viewModel.selectedFrameSizeMs,
                            opusBitrateKbps: viewModel.selectedOpusBitrateKbps,
                            loopbackActive: viewModel.loopbackTestActive,
                            proximityGateActive: viewModel.proximityNoiseGateActive,
                            spatialAudioActive: viewModel.spatialAudioActive,
                            spectrumBands: viewModel.liveSpectrumBands,
                            peerRxGainPercent: viewModel.peerRxGainPercent,
                            onUpdateConfig: viewModel.updateAudioEngineConfig,
                            onUpdateRxGain: viewModel.updatePeerRxGain,
                            onToggleLoopback: viewModel.toggleLoopbackTest,
                            onToggleProximityGate: viewModel.toggleProximityNoiseGate,
                            onToggleSpatialAudio: viewModel.toggleSpatialAudio
                        )
                    }
                }
            }

            if !viewModel.isInActiveVoiceSession {
                EchoLinkBottomNavBar(
                    selectedTab: viewModel.selectedTab,
                    onSelectTab: viewModel.selectTab
                )
            }
        }
        .background(EchoLinkColors.surfaceDim.ignoresSafeArea())
        .sheet(isPresented: $viewModel.showCallLinkQrSheet) {
            CallLinkQrModalSheet(
                session: viewModel.callLinkSession,
                remainingSeconds: viewModel.keyRotationRemainingSeconds,
                onDismiss: viewModel.closeCallLinkQrModal
            )
            .presentationDetents([.medium, .large])
        }
        .sheet(isPresented: $viewModel.showScanLinkSheet) {
            ScanCallLinkSheet(
                onDismiss: viewModel.closeScanLinkModal,
                onJoinLink: viewModel.joinByScannedQrOrPin
            )
            .presentationDetents([.medium, .large])
        }
        .sheet(isPresented: $viewModel.showAddPeerSheet) {
            AddPeersToRoomSheet(
                activePeers: viewModel.activePeers,
                discoveredPeers: viewModel.discoveredPeers,
                recentPeers: viewModel.recentPeers,
                onAddDiscoveredPeer: viewModel.connectToRadarNode,
                onAddRecentPeer: viewModel.connectToRecentPeer,
                onRenamePeer: viewModel.openRenamePeerDialog,
                onRemoveRecentPeer: viewModel.removeRecentPeer,
                onShowHostQr: viewModel.openCallLinkQrModal,
                onOpenScanOrIp: viewModel.openScanLinkModal,
                onDismiss: viewModel.closeAddPeerSheet
            )
            .presentationDetents([.medium, .large])
        }
        .alert(
            "Rename Device",
            isPresented: Binding(
                get: { viewModel.renamingPeerTarget != nil },
                set: { if !$0 { viewModel.closeRenamePeerDialog() } }
            )
        ) {
            TextField("Device Name", text: $renameText)
            Button("Save Name") {
                if let (ip, _) = viewModel.renamingPeerTarget {
                    viewModel.renamePeer(ipAddress: ip, newName: renameText)
                }
            }
            Button("Cancel", role: .cancel) {
                viewModel.closeRenamePeerDialog()
            }
        } message: {
            if let (ip, _) = viewModel.renamingPeerTarget {
                Text("Set a custom display name for this peer (\(ip)):")
            }
        }
        .onChange(of: viewModel.renamingPeerTarget?.0) { _ in
            renameText = viewModel.renamingPeerTarget?.1 ?? ""
        }
    }
}
