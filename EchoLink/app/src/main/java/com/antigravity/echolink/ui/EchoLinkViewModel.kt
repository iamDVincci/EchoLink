package com.antigravity.echolink.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.echolink.audio.EarbudRoutingManager
import com.antigravity.echolink.audio.NativeAudioBridge
import com.antigravity.echolink.model.CallLinkSession
import com.antigravity.echolink.model.EarbudStatus
import com.antigravity.echolink.model.HardwareReadinessState
import com.antigravity.echolink.model.IntercomMode
import com.antigravity.echolink.model.NavTab
import com.antigravity.echolink.model.PacketTelemetry
import com.antigravity.echolink.model.PeerNode
import com.antigravity.echolink.model.RecentPeer
import com.antigravity.echolink.network.MultiTransportEngine
import com.antigravity.echolink.service.EchoLinkIntercomService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class EchoLinkViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val audioBridge: NativeAudioBridge,
    private val earbudRoutingManager: EarbudRoutingManager,
    private val transportEngine: MultiTransportEngine
) : ViewModel() {

    private val prefs = appContext.getSharedPreferences("echolink_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    val earbudStatus: StateFlow<EarbudStatus> = earbudRoutingManager.earbudStatus
    val discoveredPeers: StateFlow<List<PeerNode>> = transportEngine.discoveredPeers
    val activePeers: StateFlow<List<PeerNode>> = transportEngine.activePeers
    val packetTelemetry: StateFlow<PacketTelemetry> = transportEngine.packetTelemetry
    val callLinkSession: StateFlow<CallLinkSession> = transportEngine.callLinkSession
    val talkTargetPeerIp: StateFlow<String?> = transportEngine.talkTargetPeerIp
    val isRoomHost: StateFlow<Boolean> = transportEngine.isRoomHost

    private val _customPeerNames = MutableStateFlow<Map<String, String>>(loadSavedCustomPeerNames())
    val customPeerNames: StateFlow<Map<String, String>> = _customPeerNames.asStateFlow()

    private val _recentPeers = MutableStateFlow<List<RecentPeer>>(loadSavedRecentPeers())
    val recentPeers: StateFlow<List<RecentPeer>> = _recentPeers.asStateFlow()

    private val _selectedTab = MutableStateFlow(NavTab.MESH)
    val selectedTab: StateFlow<NavTab> = _selectedTab.asStateFlow()

    private val _isInActiveVoiceSession = MutableStateFlow(false)
    val isInActiveVoiceSession: StateFlow<Boolean> = _isInActiveVoiceSession.asStateFlow()

    private val _showCallLinkQrSheet = MutableStateFlow(false)
    val showCallLinkQrSheet: StateFlow<Boolean> = _showCallLinkQrSheet.asStateFlow()

    private val _showScanLinkSheet = MutableStateFlow(false)
    val showScanLinkSheet: StateFlow<Boolean> = _showScanLinkSheet.asStateFlow()

    private val _showAddPeerSheet = MutableStateFlow(false)
    val showAddPeerSheet: StateFlow<Boolean> = _showAddPeerSheet.asStateFlow()

    private val _renamingPeerTarget = MutableStateFlow<Pair<String, String>?>(null)
    val renamingPeerTarget: StateFlow<Pair<String, String>?> = _renamingPeerTarget.asStateFlow()

    private val _readinessState = MutableStateFlow(HardwareReadinessState())
    val readinessState: StateFlow<HardwareReadinessState> = _readinessState.asStateFlow()

    private val _intercomMode = MutableStateFlow(IntercomMode.OPEN_MIC)
    val intercomMode: StateFlow<IntercomMode> = _intercomMode.asStateFlow()

    private val _isMicMuted = MutableStateFlow(false)
    val isMicMuted: StateFlow<Boolean> = _isMicMuted.asStateFlow()

    private val _isPttTransmitting = MutableStateFlow(false)
    val isPttTransmitting: StateFlow<Boolean> = _isPttTransmitting.asStateFlow()

    private val _proximityNoiseGateActive = MutableStateFlow(true)
    val proximityNoiseGateActive: StateFlow<Boolean> = _proximityNoiseGateActive.asStateFlow()

    private val _spatialAudioActive = MutableStateFlow(true)
    val spatialAudioActive: StateFlow<Boolean> = _spatialAudioActive.asStateFlow()

    private val _loopbackTestActive = MutableStateFlow(false)
    val loopbackTestActive: StateFlow<Boolean> = _loopbackTestActive.asStateFlow()

    private val _peerRxGainPercent = MutableStateFlow(100)
    val peerRxGainPercent: StateFlow<Int> = _peerRxGainPercent.asStateFlow()

    private val _isRxBoost3dB = MutableStateFlow(false)
    val isRxBoost3dB: StateFlow<Boolean> = _isRxBoost3dB.asStateFlow()

    private val _liveSpectrumBands = MutableStateFlow(List(14) { 0.12f })
    val liveSpectrumBands: StateFlow<List<Float>> = _liveSpectrumBands.asStateFlow()

    private val _liveVoiceDbFs = MutableStateFlow(-60)
    val liveVoiceDbFs: StateFlow<Int> = _liveVoiceDbFs.asStateFlow()

    private val _toastBannerMessage = MutableStateFlow<Pair<String, String>?>(null)
    val toastBannerMessage: StateFlow<Pair<String, String>?> = _toastBannerMessage.asStateFlow()

    private val _keyRotationRemainingSeconds = MutableStateFlow(300)
    val keyRotationRemainingSeconds: StateFlow<Int> = _keyRotationRemainingSeconds.asStateFlow()

    private val _selectedSampleRateHz = MutableStateFlow(48000)
    val selectedSampleRateHz: StateFlow<Int> = _selectedSampleRateHz.asStateFlow()

    private val _selectedFrameSizeMs = MutableStateFlow(20)
    val selectedFrameSizeMs: StateFlow<Int> = _selectedFrameSizeMs.asStateFlow()

    private val _selectedOpusBitrateKbps = MutableStateFlow(24)
    val selectedOpusBitrateKbps: StateFlow<Int> = _selectedOpusBitrateKbps.asStateFlow()

    init {
        transportEngine.setInitialCustomNames(_customPeerNames.value)

        EchoLinkIntercomService.onMuteToggleCallback = {
            toggleMicMute()
        }
        EchoLinkIntercomService.onDisconnectCallback = {
            disconnectActiveSession()
        }
        transportEngine.onPeerLeftCallback = { peerIp ->
            viewModelScope.launch {
                val remaining = transportEngine.activePeers.value
                val peerName = transportEngine.resolvePeerName(peerIp, "Peer")
                if (remaining.isEmpty()) {
                    disconnectActiveSession()
                    _toastBannerMessage.value = Pair(
                        "$peerName Left Session",
                        "All peers disconnected. Returned to Radar."
                    )
                    delay(3500L)
                    if (_toastBannerMessage.value?.first?.contains("Left Session") == true) {
                        _toastBannerMessage.value = null
                    }
                } else {
                    _toastBannerMessage.value = Pair(
                        "$peerName Left Room",
                        "Remaining active peers: ${remaining.size}"
                    )
                    delay(3000L)
                    if (_toastBannerMessage.value?.first?.contains("Left Room") == true) {
                        _toastBannerMessage.value = null
                    }
                }
            }
        }
        transportEngine.onRemotePeerJoinedCallback = { joinedPeer ->
            viewModelScope.launch {
                saveRecentPeer(joinedPeer)
                if (!_isInActiveVoiceSession.value) {
                    earbudRoutingManager.engageVoiceCallAudioRoute()
                    audioBridge.startEngine(_selectedSampleRateHz.value, _selectedFrameSizeMs.value)
                    _isInActiveVoiceSession.value = true
                }
                startForegroundIntercomService(peerCount = transportEngine.activePeers.value.size.coerceAtLeast(1))
                _toastBannerMessage.value = Pair(
                    "${joinedPeer.name} Joined Room",
                    "Connected on ${joinedPeer.ipAddress}"
                )
                delay(4000L)
                if (_toastBannerMessage.value?.first?.contains("Joined Room") == true) {
                    _toastBannerMessage.value = null
                }
            }
        }

        transportEngine.onHandshakeStatusCallback = { success, peerIp, remoteModel, rtt ->
            viewModelScope.launch {
                val peerName = transportEngine.resolvePeerName(peerIp, remoteModel ?: "Peer")
                if (success) {
                    _toastBannerMessage.value = Pair(
                        "Connected with $peerName",
                        "Encrypted P2P Voice Active • Latency: ${rtt}ms"
                    )
                    delay(4000L)
                    if (_toastBannerMessage.value?.first?.startsWith("Connected with") == true) {
                        _toastBannerMessage.value = null
                    }
                } else {
                    disconnectActiveSession()
                    _toastBannerMessage.value = Pair(
                        "Linking Failed with $peerName",
                        "Could not reach $peerIp on port 51110. Ensure both devices are on same Wi-Fi/Hotspot."
                    )
                    delay(5000L)
                    if (_toastBannerMessage.value?.first?.startsWith("Linking Failed") == true) {
                        _toastBannerMessage.value = null
                    }
                }
            }
        }

        refreshPermissionAndHardwareReadiness()
        transportEngine.startRadarDiscovery()
        startLiveTelemetryAndSpectrumLoop()
        startKeyRotationCountdownLoop()

        viewModelScope.launch {
            earbudStatus.collect { status ->
                _readinessState.value = _readinessState.value.copy(
                    earbudsConnected = status.isConnected,
                    connectedAudioDeviceName = "${status.deviceName} (${status.protocolLabel})"
                )
            }
        }
    }

    fun refreshPermissionAndHardwareReadiness() {
        val micGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val btGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val wifiGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }

        val camGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        earbudRoutingManager.refreshConnectedAudioDevices()
        val eb = earbudRoutingManager.earbudStatus.value

        _readinessState.value = _readinessState.value.copy(
            earbudsConnected = eb.isConnected,
            connectedAudioDeviceName = "${eb.deviceName} (${eb.protocolLabel})",
            micPermissionGranted = micGranted,
            bluetoothPermissionGranted = btGranted,
            nearbyWifiPermissionGranted = wifiGranted,
            cameraPermissionGranted = camGranted
        )
    }

    fun selectTab(tab: NavTab) {
        _selectedTab.value = tab
        if (tab == NavTab.HUD) {
            refreshPermissionAndHardwareReadiness()
        } else if (tab == NavTab.MESH || tab == NavTab.RF_STATS) {
            transportEngine.refreshHardwareWifiTelemetry()
        }
    }

    fun toggleWifiDirectRadar() {
        val next = !_readinessState.value.wifiDirectRadarEnabled
        _readinessState.value = _readinessState.value.copy(wifiDirectRadarEnabled = next)
        if (next) transportEngine.startRadarDiscovery() else transportEngine.stopRadarDiscovery()
    }

    fun toggleOboeLowLatency() {
        val next = !_readinessState.value.oboeLowLatencyEnabled
        _readinessState.value = _readinessState.value.copy(oboeLowLatencyEnabled = next)
    }

    fun onPermissionsResult(grantedMap: Map<String, Boolean>) {
        refreshPermissionAndHardwareReadiness()
    }

    fun launchSonarRadar() {
        viewModelScope.launch {
            _readinessState.value = _readinessState.value.copy(isSyncingNodes = true)
            refreshPermissionAndHardwareReadiness()
            transportEngine.startRadarDiscovery()
            delay(350L)
            _readinessState.value = _readinessState.value.copy(isSyncingNodes = false)
            _selectedTab.value = NavTab.MESH
        }
    }

    fun connectToRadarNode(peer: PeerNode) {
        viewModelScope.launch {
            if (peer.ipAddress.isBlank() || peer.ipAddress == "0.0.0.0" || peer.ipAddress == "192.168.49.1") {
                _toastBannerMessage.value = Pair(
                    "Cannot Link with ${peer.name}",
                    "Device IP address is unresolved. Ensure device is on local Wi-Fi or Hotspot."
                )
                delay(4000L)
                if (_toastBannerMessage.value?.first?.startsWith("Cannot Link") == true) {
                    _toastBannerMessage.value = null
                }
                return@launch
            }

            val resolvedName = transportEngine.resolvePeerName(peer.ipAddress, peer.name)
            val resolvedPeer = peer.copy(name = resolvedName)
            _toastBannerMessage.value = Pair(
                "Linking with ${resolvedPeer.name} (${resolvedPeer.ipAddress})...",
                "Protocol: ${resolvedPeer.transport.fullName} • Port ${resolvedPeer.voicePort}"
            )
            saveRecentPeer(resolvedPeer)
            earbudRoutingManager.engageVoiceCallAudioRoute()
            audioBridge.startEngine(_selectedSampleRateHz.value, _selectedFrameSizeMs.value)
            transportEngine.setIsRoomHost(true)
            transportEngine.connectToPeer(resolvedPeer)
            startForegroundIntercomService(peerCount = transportEngine.activePeers.value.size.coerceAtLeast(1))
            delay(200L)
            _isInActiveVoiceSession.value = true
            _showAddPeerSheet.value = false
        }
    }

    fun connectToRecentPeer(recent: RecentPeer) {
        val resolvedName = transportEngine.resolvePeerName(recent.ipAddress, recent.name)
        val peer = PeerNode(
            id = recent.ipAddress.hashCode(),
            name = resolvedName,
            roleTag = "Recent Peer",
            nodeCode = "IP ${recent.ipAddress}",
            distanceMeters = 5,
            transport = recent.transport,
            ipAddress = recent.ipAddress,
            isRealNetworkPeer = true
        )
        connectToRadarNode(peer)
    }

    fun removeRecentPeer(ipAddress: String) {
        val updated = _recentPeers.value.filterNot { it.ipAddress == ipAddress || it.id == ipAddress }
        _recentPeers.value = updated
        try {
            if (updated.isEmpty()) {
                prefs.edit().remove("recent_peers_json").apply()
            } else {
                prefs.edit().putString("recent_peers_json", json.encodeToString(updated)).apply()
            }
        } catch (_: Exception) {
        }
    }

    fun clearRecentPeers() {
        _recentPeers.value = emptyList()
        prefs.edit().remove("recent_peers_json").apply()
        _toastBannerMessage.value = Pair(
            "Device History Cleared",
            "Removed all saved connected devices"
        )
        viewModelScope.launch {
            delay(3000L)
            if (_toastBannerMessage.value?.first == "Device History Cleared") {
                _toastBannerMessage.value = null
            }
        }
    }

    fun openRenamePeerDialog(ipAddress: String, currentName: String) {
        _renamingPeerTarget.value = Pair(ipAddress, currentName)
    }

    fun closeRenamePeerDialog() {
        _renamingPeerTarget.value = null
    }

    fun renamePeer(ipAddress: String, newName: String) {
        val clean = newName.trim()
        if (clean.isBlank()) {
            _renamingPeerTarget.value = null
            return
        }
        val updatedMap = _customPeerNames.value.toMutableMap().apply {
            put(ipAddress, clean)
        }
        _customPeerNames.value = updatedMap
        try {
            prefs.edit().putString("custom_peer_names_json", json.encodeToString(updatedMap)).apply()
        } catch (_: Exception) {
        }

        transportEngine.setCustomPeerName(ipAddress, clean)

        val updatedRecents = _recentPeers.value.map {
            if (it.ipAddress == ipAddress) it.copy(name = clean) else it
        }
        _recentPeers.value = updatedRecents
        try {
            prefs.edit().putString("recent_peers_json", json.encodeToString(updatedRecents)).apply()
        } catch (_: Exception) {
        }

        _renamingPeerTarget.value = null
        _toastBannerMessage.value = Pair(
            "Device Renamed to \"$clean\"",
            "Saved for $ipAddress"
        )
        viewModelScope.launch {
            delay(3000L)
            if (_toastBannerMessage.value?.first?.startsWith("Device Renamed") == true) {
                _toastBannerMessage.value = null
            }
        }
    }

    private fun loadSavedCustomPeerNames(): Map<String, String> {
        val raw = prefs.getString("custom_peer_names_json", null) ?: return emptyMap()
        return try {
            json.decodeFromString<Map<String, String>>(raw)
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun saveRecentPeer(peer: PeerNode) {
        if (peer.ipAddress.isBlank() || peer.ipAddress == "0.0.0.0") return
        val resolvedName = transportEngine.resolvePeerName(peer.ipAddress, peer.name)
        val entry = RecentPeer(
            id = peer.ipAddress,
            name = resolvedName,
            ipAddress = peer.ipAddress,
            subtitle = "${peer.ipAddress} • ${peer.transport.badge}",
            transport = peer.transport,
            lastConnectedEpochMs = System.currentTimeMillis()
        )
        val updated = listOf(entry) + _recentPeers.value.filterNot { it.ipAddress == peer.ipAddress }.take(7)
        _recentPeers.value = updated
        try {
            prefs.edit().putString("recent_peers_json", json.encodeToString(updated)).apply()
        } catch (_: Exception) {
        }
    }

    private fun loadSavedRecentPeers(): List<RecentPeer> {
        val raw = prefs.getString("recent_peers_json", null) ?: return emptyList()
        return try {
            val savedNames = loadSavedCustomPeerNames()
            json.decodeFromString<List<RecentPeer>>(raw).map { rec ->
                val custom = savedNames[rec.ipAddress]?.takeIf { it.isNotBlank() }
                if (custom != null) rec.copy(name = custom) else rec
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun openCallLinkQrModal() {
        transportEngine.setIsRoomHost(true)
        transportEngine.refreshCallLinkSession()
        _keyRotationRemainingSeconds.value = 300
        _showCallLinkQrSheet.value = true
        _toastBannerMessage.value = Pair(
            "Offline Mesh Room Created",
            "Host IP: ${callLinkSession.value.hostIp} • PIN: ${callLinkSession.value.pinCode}"
        )
        viewModelScope.launch {
            delay(4000L)
            if (_toastBannerMessage.value?.first == "Offline Mesh Room Created") {
                _toastBannerMessage.value = null
            }
        }
    }

    fun closeCallLinkQrModal() {
        _showCallLinkQrSheet.value = false
    }

    fun openScanLinkModal() {
        _showScanLinkSheet.value = true
    }

    fun closeScanLinkModal() {
        _showScanLinkSheet.value = false
    }

    fun openAddPeerSheet() {
        _showAddPeerSheet.value = true
    }

    fun closeAddPeerSheet() {
        _showAddPeerSheet.value = false
    }

    fun selectTalkTargetPeer(peerIp: String?) {
        transportEngine.setTalkTargetPeerIp(peerIp)
    }

    fun removePeerFromRoom(peerIp: String) {
        transportEngine.removePeerFromRoom(peerIp)
        val remaining = transportEngine.activePeers.value
        if (remaining.isEmpty()) {
            disconnectActiveSession()
        } else {
            startForegroundIntercomService(peerCount = remaining.size)
        }
    }

    fun joinByScannedQrOrPin(qrOrIp: String, pin: String) {
        val trimmed = qrOrIp.trim()
        if (trimmed.isBlank()) {
            _toastBannerMessage.value = Pair(
                "Missing Peer IP or QR URI",
                "Scan a peer's QR code or enter their P2P/LAN IP address"
            )
            viewModelScope.launch {
                delay(3500L)
                if (_toastBannerMessage.value?.first == "Missing Peer IP or QR URI") {
                    _toastBannerMessage.value = null
                }
            }
            return
        }
        closeScanLinkModal()
        _showAddPeerSheet.value = false
        earbudRoutingManager.engageVoiceCallAudioRoute()
        audioBridge.startEngine(_selectedSampleRateHz.value, _selectedFrameSizeMs.value)
        val joined = transportEngine.connectByQrUriOrIp(trimmed, pin, appendToRoom = true)
        saveRecentPeer(joined)
        startForegroundIntercomService(peerCount = transportEngine.activePeers.value.size.coerceAtLeast(1))
        _isInActiveVoiceSession.value = true
        _toastBannerMessage.value = Pair(
            "Linking with ${joined.name} (${joined.ipAddress})...",
            "Connecting to encrypted P2P channel"
        )
    }

    fun dismissToastBanner() {
        _toastBannerMessage.value = null
    }

    fun setIntercomMode(mode: IntercomMode) {
        _intercomMode.value = mode
        val isPtt = (mode == IntercomMode.PUSH_TO_TALK)
        transportEngine.isPttActive = isPtt && _isPttTransmitting.value
        audioBridge.setPttState(isPtt, _isPttTransmitting.value)
    }

    fun setPttPressed(pressed: Boolean) {
        _isPttTransmitting.value = pressed
        val isPtt = (_intercomMode.value == IntercomMode.PUSH_TO_TALK)
        transportEngine.isPttActive = isPtt && pressed
        audioBridge.setPttState(isPtt, pressed)
    }

    fun toggleMicMute() {
        val next = !_isMicMuted.value
        _isMicMuted.value = next
        audioBridge.setMuted(next)
        startForegroundIntercomService(peerCount = activePeers.value.size.coerceAtLeast(1))
    }

    fun toggleProximityNoiseGate() {
        val next = !_proximityNoiseGateActive.value
        _proximityNoiseGateActive.value = next
        audioBridge.setProximityNoiseGate(next)
    }

    fun toggleSpatialAudio() {
        val next = !_spatialAudioActive.value
        _spatialAudioActive.value = next
        audioBridge.setSpatialAudio(next)
    }

    fun toggleLoopbackTest() {
        val next = !_loopbackTestActive.value
        _loopbackTestActive.value = next
        if (next) {
            earbudRoutingManager.engageVoiceCallAudioRoute()
            audioBridge.startEngine(_selectedSampleRateHz.value, _selectedFrameSizeMs.value)
        } else if (!_isInActiveVoiceSession.value) {
            audioBridge.stopEngine()
        }
        audioBridge.setLoopback(next)
    }

    fun updatePeerRxGain(fraction0To1: Float) {
        val pct = (fraction0To1.coerceIn(0f, 1f) * 150f).roundToInt().coerceIn(0, 200)
        _peerRxGainPercent.value = pct
        audioBridge.setRxGain(pct / 100f, _isRxBoost3dB.value)
    }

    fun toggleRxBoost3dB() {
        val next = !_isRxBoost3dB.value
        _isRxBoost3dB.value = next
        _peerRxGainPercent.value = if (next) 150 else 100
        audioBridge.setRxGain(_peerRxGainPercent.value / 100f, next)
    }

    fun addPeerToMeshSession() {
        _showAddPeerSheet.value = true
    }

    fun handleHardwareStemButtonPress() {
        if (_intercomMode.value == IntercomMode.PUSH_TO_TALK) {
            setPttPressed(!_isPttTransmitting.value)
        } else {
            toggleMicMute()
        }
    }

    fun selectAudioRoute(route: String) {
        earbudRoutingManager.selectAudioRoute(route)
    }

    fun updateAudioEngineConfig(sampleRate: Int, frameMs: Int, bitrateKbps: Int) {
        _selectedSampleRateHz.value = sampleRate
        _selectedFrameSizeMs.value = frameMs
        _selectedOpusBitrateKbps.value = bitrateKbps
        if (_isInActiveVoiceSession.value || _loopbackTestActive.value) {
            audioBridge.stopEngine()
            audioBridge.startEngine(sampleRate, frameMs)
        }
    }

    private val isDisconnecting = java.util.concurrent.atomic.AtomicBoolean(false)

    fun disconnectActiveSession() {
        if (isDisconnecting.getAndSet(true)) return
        try {
            _isInActiveVoiceSession.value = false
            _isPttTransmitting.value = false
            if (!_loopbackTestActive.value) {
                audioBridge.stopEngine()
            }
            earbudRoutingManager.resetAudioRoute()
            transportEngine.disconnectAll()
            _toastBannerMessage.value = Pair(
                "Session Disconnected",
                "Returned to Radar Scanning"
            )
            viewModelScope.launch {
                delay(2500L)
                if (_toastBannerMessage.value?.first == "Session Disconnected") {
                    _toastBannerMessage.value = null
                }
            }
            try {
                val stopIntent = Intent(appContext, EchoLinkIntercomService::class.java).apply {
                    action = EchoLinkIntercomService.ACTION_DISCONNECT
                    putExtra("EXTRA_FROM_VM", true)
                }
                appContext.startService(stopIntent)
            } catch (_: Exception) {
            }
        } finally {
            isDisconnecting.set(false)
        }
    }

    private fun startForegroundIntercomService(peerCount: Int) {
        try {
            val intent = Intent(appContext, EchoLinkIntercomService::class.java).apply {
                action = EchoLinkIntercomService.ACTION_START
                putExtra(EchoLinkIntercomService.EXTRA_PEER_COUNT, peerCount)
                putExtra(EchoLinkIntercomService.EXTRA_PING_MS, packetTelemetry.value.pingMs)
                putExtra(EchoLinkIntercomService.EXTRA_IS_MUTED, _isMicMuted.value)
            }
            appContext.startForegroundService(intent)
        } catch (_: Exception) {
        }
    }

    private val idleSpectrumBands = List(14) { 0.12f }

    private fun startLiveTelemetryAndSpectrumLoop() {
        viewModelScope.launch {
            while (isActive) {
                if (_isInActiveVoiceSession.value || _loopbackTestActive.value) {
                    delay(90L)
                    val nativeBands = audioBridge.getSpectrumBands()
                    _liveSpectrumBands.value = List(14) { idx ->
                        nativeBands.getOrElse(idx) { 0.12f }.coerceIn(0.12f, 1.0f)
                    }
                    _liveVoiceDbFs.value = audioBridge.getCurrentDbFs().roundToInt().coerceIn(-60, 0)
                } else {
                    if (_liveVoiceDbFs.value != -60) {
                        _liveSpectrumBands.value = idleSpectrumBands
                        _liveVoiceDbFs.value = -60
                    }
                    delay(400L)
                }
            }
        }
    }

    private fun startKeyRotationCountdownLoop() {
        viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                val next = _keyRotationRemainingSeconds.value - 1
                if (next <= 0) {
                    transportEngine.refreshCallLinkSession()
                    _keyRotationRemainingSeconds.value = 300
                } else {
                    _keyRotationRemainingSeconds.value = next
                }
            }
        }
    }
}
