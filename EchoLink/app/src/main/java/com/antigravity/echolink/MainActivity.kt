package com.antigravity.echolink

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.antigravity.echolink.model.IntercomMode
import com.antigravity.echolink.model.NavTab
import com.antigravity.echolink.ui.EchoLinkViewModel
import com.antigravity.echolink.ui.components.EchoLinkBottomNavBar
import com.antigravity.echolink.ui.components.EchoLinkTopAppBar
import com.antigravity.echolink.ui.screens.ActiveVoiceIntercomScreen
import com.antigravity.echolink.ui.screens.AddPeersToRoomSheet
import com.antigravity.echolink.ui.screens.AudioEngineSettingsScreen
import com.antigravity.echolink.ui.screens.CallLinkQrModalSheet
import com.antigravity.echolink.ui.screens.ReadinessCheckScreen
import com.antigravity.echolink.ui.screens.RenamePeerModalDialog
import com.antigravity.echolink.ui.screens.RfStatsScreen
import com.antigravity.echolink.ui.screens.ScanCallLinkSheet
import com.antigravity.echolink.ui.screens.SonarRadarHomeScreen
import com.antigravity.echolink.ui.theme.EchoLinkColors
import com.antigravity.echolink.ui.theme.EchoLinkTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: EchoLinkViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.parseColor("#080A0F")),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.parseColor("#080A0F"))
        )

        handleIncomingDeepLink(intent)

        setContent {
            EchoLinkTheme {
                EchoLinkRootApp(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissionAndHardwareReadiness()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingDeepLink(intent)
    }

    private fun handleIncomingDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "echolink") {
            val pin = data.getQueryParameter("pin") ?: ""
            viewModel.joinByScannedQrOrPin(data.toString(), pin)
        }
    }

    /**
     * Supports hardware earbud stem button (KEYCODE_HEADSETHOOK / KEYCODE_MEDIA_PLAY_PAUSE)
     * and Volume Up/Down Push-to-Talk triggers when in PTT mode (PRD Section 3.3 & Section 3.4).
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_HEADSETHOOK,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                viewModel.handleHardwareStemButtonPress()
                return true
            }
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (viewModel.isInActiveVoiceSession.value &&
                    viewModel.intercomMode.value == IntercomMode.PUSH_TO_TALK
                ) {
                    viewModel.setPttPressed(true)
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (viewModel.isInActiveVoiceSession.value &&
                    viewModel.intercomMode.value == IntercomMode.PUSH_TO_TALK
                ) {
                    viewModel.setPttPressed(false)
                    return true
                }
            }
        }
        return super.onKeyUp(keyCode, event)
    }
}

@Composable
fun EchoLinkRootApp(viewModel: EchoLinkViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val isInActiveSession by viewModel.isInActiveVoiceSession.collectAsState()
    val showCallLinkQrSheet by viewModel.showCallLinkQrSheet.collectAsState()
    val showScanLinkSheet by viewModel.showScanLinkSheet.collectAsState()
    val showAddPeerSheet by viewModel.showAddPeerSheet.collectAsState()
    val renamingPeerTarget by viewModel.renamingPeerTarget.collectAsState()
    val talkTargetPeerIp by viewModel.talkTargetPeerIp.collectAsState()

    val earbudStatus by viewModel.earbudStatus.collectAsState()
    val readinessState by viewModel.readinessState.collectAsState()
    val discoveredPeers by viewModel.discoveredPeers.collectAsState()
    val recentPeers by viewModel.recentPeers.collectAsState()
    val activePeers by viewModel.activePeers.collectAsState()
    val packetTelemetry by viewModel.packetTelemetry.collectAsState()
    val callLinkSession by viewModel.callLinkSession.collectAsState()

    val intercomMode by viewModel.intercomMode.collectAsState()
    val isMicMuted by viewModel.isMicMuted.collectAsState()
    val isPttTransmitting by viewModel.isPttTransmitting.collectAsState()
    val proximityGate by viewModel.proximityNoiseGateActive.collectAsState()
    val spatialAudio by viewModel.spatialAudioActive.collectAsState()
    val loopbackActive by viewModel.loopbackTestActive.collectAsState()
    val peerRxGain by viewModel.peerRxGainPercent.collectAsState()
    val isRxBoost by viewModel.isRxBoost3dB.collectAsState()
    val spectrumBands by viewModel.liveSpectrumBands.collectAsState()
    val liveVoiceDbFs by viewModel.liveVoiceDbFs.collectAsState()
    val toastBanner by viewModel.toastBannerMessage.collectAsState()
    val keyRotationSeconds by viewModel.keyRotationRemainingSeconds.collectAsState()

    val sampleRateHz by viewModel.selectedSampleRateHz.collectAsState()
    val frameSizeMs by viewModel.selectedFrameSizeMs.collectAsState()
    val opusBitrateKbps by viewModel.selectedOpusBitrateKbps.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        viewModel.onPermissionsResult(result)
    }

    val requestHardwarePermissions = {
        val perms = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms.add(Manifest.permission.BLUETOOTH_CONNECT)
            perms.add(Manifest.permission.BLUETOOTH_SCAN)
            perms.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(perms.toTypedArray())
    }

    val topTitle = if (isInActiveSession) {
        "Voice Link"
    } else {
        "EchoLink"
    }
    val topSubtitle = when {
        isInActiveSession -> "Encrypted P2P"
        selectedTab == NavTab.MESH -> "Sonar Radar"
        selectedTab == NavTab.AUDIO -> "Audio Studio"
        selectedTab == NavTab.RF_STATS -> "Signal Health"
        else -> "Device Setup"
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(EchoLinkColors.SurfaceDim),
        containerColor = EchoLinkColors.SurfaceDim,
        topBar = {
            EchoLinkTopAppBar(
                title = topTitle,
                subtitle = topSubtitle,
                earbudStatus = earbudStatus,
                onSelectRoute = viewModel::selectAudioRoute,
                showBackButton = isInActiveSession,
                showEmergencySquelch = isInActiveSession,
                onBackClick = { viewModel.disconnectActiveSession() },
                onEmergencySquelchClick = { viewModel.toggleMicMute() }
            )
        },
        bottomBar = {
            if (!isInActiveSession) {
                EchoLinkBottomNavBar(
                    selectedTab = selectedTab,
                    onSelectTab = viewModel::selectTab
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(EchoLinkColors.SurfaceDim)
        ) {
            if (isInActiveSession) {
                ActiveVoiceIntercomScreen(
                    activePeers = activePeers,
                    talkTargetPeerIp = talkTargetPeerIp,
                    telemetry = packetTelemetry,
                    intercomMode = intercomMode,
                    isMicMuted = isMicMuted,
                    isPttTransmitting = isPttTransmitting,
                    proximityNoiseGateActive = proximityGate,
                    spatialAudioActive = spatialAudio,
                    peerRxGainPercent = peerRxGain,
                    isRxBoost3dB = isRxBoost,
                    spectrumBands = spectrumBands,
                    liveVoiceDbFs = liveVoiceDbFs,
                    onSelectTalkTarget = viewModel::selectTalkTargetPeer,
                    onRenamePeer = viewModel::openRenamePeerDialog,
                    onRemovePeerFromRoom = viewModel::removePeerFromRoom,
                    onSelectMode = viewModel::setIntercomMode,
                    onToggleMute = viewModel::toggleMicMute,
                    onPttPressChanged = viewModel::setPttPressed,
                    onToggleProximityGate = viewModel::toggleProximityNoiseGate,
                    onToggleSpatialAudio = viewModel::toggleSpatialAudio,
                    onUpdateRxGain = viewModel::updatePeerRxGain,
                    onToggleRxBoost = viewModel::toggleRxBoost3dB,
                    onAddPeer = viewModel::addPeerToMeshSession,
                    onDisconnect = viewModel::disconnectActiveSession
                )
            } else {
                when (selectedTab) {
                    NavTab.HUD -> ReadinessCheckScreen(
                        readinessState = readinessState,
                        sampleRateHz = sampleRateHz,
                        frameSizeMs = frameSizeMs,
                        onToggleWifiDirect = viewModel::toggleWifiDirectRadar,
                        onToggleOboeAudio = viewModel::toggleOboeLowLatency,
                        onRequestPermissions = requestHardwarePermissions,
                        onLaunchSonarRadar = viewModel::launchSonarRadar
                    )
                    NavTab.MESH -> SonarRadarHomeScreen(
                        discoveredPeers = discoveredPeers,
                        recentPeers = recentPeers,
                        localIp = callLinkSession.hostIp,
                        wifiFrequencyMhz = packetTelemetry.wifiFrequencyMhz,
                        toastBanner = toastBanner,
                        onTapPeerNode = viewModel::connectToRadarNode,
                        onTapRecentPeer = viewModel::connectToRecentPeer,
                        onRemoveRecentPeer = viewModel::removeRecentPeer,
                        onClearRecentPeers = viewModel::clearRecentPeers,
                        onRenamePeer = viewModel::openRenamePeerDialog,
                        onCreateCallLink = viewModel::openCallLinkQrModal,
                        onScanCallLink = viewModel::openScanLinkModal,
                        onDismissToast = viewModel::dismissToastBanner
                    )
                    NavTab.RF_STATS -> RfStatsScreen(
                        telemetry = packetTelemetry,
                        localIpAddress = callLinkSession.hostIp
                    )
                    NavTab.AUDIO -> AudioEngineSettingsScreen(
                        sampleRateHz = sampleRateHz,
                        frameSizeMs = frameSizeMs,
                        opusBitrateKbps = opusBitrateKbps,
                        loopbackActive = loopbackActive,
                        proximityGateActive = proximityGate,
                        spatialAudioActive = spatialAudio,
                        spectrumBands = spectrumBands,
                        peerRxGainPercent = peerRxGain,
                        onUpdateConfig = viewModel::updateAudioEngineConfig,
                        onUpdateRxGain = viewModel::updatePeerRxGain,
                        onToggleLoopback = viewModel::toggleLoopbackTest,
                        onToggleProximityGate = viewModel::toggleProximityNoiseGate,
                        onToggleSpatialAudio = viewModel::toggleSpatialAudio
                    )
                }
            }

            if (showCallLinkQrSheet) {
                CallLinkQrModalSheet(
                    session = callLinkSession,
                    remainingSeconds = keyRotationSeconds,
                    onDismiss = viewModel::closeCallLinkQrModal,
                    onShareViaHotspot = viewModel::closeCallLinkQrModal
                )
            }

            if (showScanLinkSheet) {
                ScanCallLinkSheet(
                    onDismiss = viewModel::closeScanLinkModal,
                    onJoinLink = viewModel::joinByScannedQrOrPin
                )
            }

            if (showAddPeerSheet) {
                AddPeersToRoomSheet(
                    activePeers = activePeers,
                    discoveredPeers = discoveredPeers,
                    recentPeers = recentPeers,
                    onAddDiscoveredPeer = viewModel::connectToRadarNode,
                    onAddRecentPeer = viewModel::connectToRecentPeer,
                    onRenamePeer = viewModel::openRenamePeerDialog,
                    onRemoveRecentPeer = viewModel::removeRecentPeer,
                    onShowHostQr = viewModel::openCallLinkQrModal,
                    onOpenScanOrIp = viewModel::openScanLinkModal,
                    onDismiss = viewModel::closeAddPeerSheet
                )
            }

            renamingPeerTarget?.let { (ipAddr, currentName) ->
                RenamePeerModalDialog(
                    ipAddress = ipAddr,
                    currentName = currentName,
                    onDismiss = viewModel::closeRenamePeerDialog,
                    onConfirmRename = viewModel::renamePeer
                )
            }
        }
    }
}
