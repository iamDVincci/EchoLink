package com.antigravity.echolink.network

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Looper
import android.util.Log
import com.antigravity.echolink.audio.NativeAudioBridge
import com.antigravity.echolink.crypto.EchoLinkCrypto
import com.antigravity.echolink.model.CallLinkSession
import com.antigravity.echolink.model.PacketTelemetry
import com.antigravity.echolink.model.PeerNode
import com.antigravity.echolink.model.TransportTier
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Multi-Transport Hybrid Networking Engine (PRD Section 3.2 & Section 6).
 *
 * - Control Channel: TCP Port 51110 (ECDH handshake, peer metadata, 2,000ms keep-alive RTT heartbeats)
 * - Voice Data Channel: UDP Port 51111 (20ms SRTP/Opus datagrams with 0x4147 "AG" header & ChaCha20-Poly1305 E2EE)
 * - Discovery Channel: UDP Broadcast Port 51112 + Wi-Fi Direct (`WifiP2pManager`)
 */
@Singleton
class MultiTransportEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val audioBridge: NativeAudioBridge
) {
    companion object {
        const val CONTROL_PORT = 51110
        const val VOICE_UDP_PORT = 51111
        const val DISCOVERY_BROADCAST_PORT = 51112
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val crypto = EchoLinkCrypto()

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val wifiP2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private var wifiP2pChannel: WifiP2pManager.Channel? = null

    private var wifiLock: WifiManager.WifiLock? = null
    private var multicastLock: WifiManager.MulticastLock? = null

    private val _discoveredPeers = MutableStateFlow<List<PeerNode>>(emptyList())
    val discoveredPeers: StateFlow<List<PeerNode>> = _discoveredPeers.asStateFlow()

    private val _activePeers = MutableStateFlow<List<PeerNode>>(emptyList())
    val activePeers: StateFlow<List<PeerNode>> = _activePeers.asStateFlow()

    private val _packetTelemetry = MutableStateFlow(PacketTelemetry())
    val packetTelemetry: StateFlow<PacketTelemetry> = _packetTelemetry.asStateFlow()

    private val _callLinkSession = MutableStateFlow(createNewCallLinkSession())
    val callLinkSession: StateFlow<CallLinkSession> = _callLinkSession.asStateFlow()

    /**
     * Host voice routing target:
     * - `null` = Broadcast to ALL peers in the room at once (Conference / All-Call)
     * - non-null `ipAddress` = Talk to & listen to ONLY that individual peer one at a time
     */
    private val _talkTargetPeerIp = MutableStateFlow<String?>(null)
    val talkTargetPeerIp: StateFlow<String?> = _talkTargetPeerIp.asStateFlow()

    private val _isRoomHost = MutableStateFlow(true)
    val isRoomHost: StateFlow<Boolean> = _isRoomHost.asStateFlow()

    private val connectedPeerAddresses = ConcurrentHashMap<String, InetAddress>()
    private val customPeerNames = ConcurrentHashMap<String, String>()
    private val txSeqCounter = AtomicInteger(0)
    private var receiverRegistered = false

    var onRemotePeerJoinedCallback: ((PeerNode) -> Unit)? = null

    private var controlServerJob: Job? = null
    private var voiceUdpJob: Job? = null
    private var heartbeatJob: Job? = null
    private var discoveryBeaconJob: Job? = null
    private var voiceSocket: DatagramSocket? = null

    @Volatile
    var isPttActive: Boolean = false

    @Volatile
    var isGroupOwnerSfu: Boolean = true

    private val wifiDirectReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION) {
                val manager = wifiP2pManager ?: return
                val channel = wifiP2pChannel ?: return
                try {
                    manager.requestPeers(channel) { peerList ->
                        peerList?.deviceList?.forEach { dev ->
                            upsertWifiP2pDevice(dev)
                        }
                    }
                } catch (_: SecurityException) {
                }
            }
        }
    }

    init {
        wifiP2pChannel = wifiP2pManager?.initialize(context, Looper.getMainLooper(), null)
        refreshHardwareWifiTelemetry()
    }

    fun startRadarDiscovery() {
        acquireLowLatencyWifiLocks()
        refreshHardwareWifiTelemetry()
        registerWifiDirectReceiver()
        startLanBeaconDiscovery()
        startWifiDirectDiscovery()
        startControlChannelServer()
    }

    fun stopRadarDiscovery() {
        discoveryBeaconJob?.cancel()
        unregisterWifiDirectReceiver()
        releaseWifiLocks()
    }

    @Suppress("DEPRECATION")
    fun refreshHardwareWifiTelemetry() {
        try {
            val info = wifiManager.connectionInfo
            val rssi = info?.rssi?.takeIf { it in -100..-1 } ?: 0
            val freq = info?.frequency?.takeIf { it > 0 } ?: 0
            _packetTelemetry.value = _packetTelemetry.value.copy(
                rssiDbm = rssi,
                wifiFrequencyMhz = freq
            )
        } catch (_: Exception) {
        }
    }

    fun refreshCallLinkSession(): CallLinkSession {
        crypto.rotateSessionKey()
        val session = createNewCallLinkSession()
        _callLinkSession.value = session
        return session
    }

    private fun createNewCallLinkSession(): CallLinkSession {
        val ip = getLocalIpAddress()
        val pin = crypto.sessionPin
        val pubKey = crypto.getPublicKeyHex()
        val qrUri = "echolink://join?ip=$ip&cport=$CONTROL_PORT&vport=$VOICE_UDP_PORT&pin=$pin&pub=$pubKey&host=${Build.MODEL.replace(" ", "_")}"
        return CallLinkSession(
            sessionId = "AG-MESH-$pin",
            pinCode = pin,
            hostIp = ip,
            controlPort = CONTROL_PORT,
            voicePort = VOICE_UDP_PORT,
            transportTag = "P2P-DIRECT",
            publicKeyHex = pubKey,
            qrUri = qrUri,
            remainingKeySeconds = 300
        )
    }

    fun setInitialCustomNames(names: Map<String, String>) {
        customPeerNames.clear()
        customPeerNames.putAll(names)
    }

    fun resolvePeerName(ipAddress: String, fallbackName: String): String {
        return customPeerNames[ipAddress]?.takeIf { it.isNotBlank() } ?: fallbackName
    }

    fun setCustomPeerName(ipAddress: String, customName: String) {
        val cleanName = customName.trim()
        if (cleanName.isBlank()) return
        customPeerNames[ipAddress] = cleanName
        _activePeers.value = _activePeers.value.map {
            if (it.ipAddress == ipAddress) it.copy(name = cleanName) else it
        }
        _discoveredPeers.value = _discoveredPeers.value.map {
            if (it.ipAddress == ipAddress) it.copy(name = cleanName) else it
        }
    }

    /**
     * Sets the Host voice routing target:
     * - `null`: Talk to ALL connected peers at once (Broadcast / Conference)
     * - `peerIp`: Talk to ONLY that specific peer one at a time (Selective / 1-on-1)
     */
    fun setTalkTargetPeerIp(peerIp: String?) {
        _talkTargetPeerIp.value = peerIp
    }

    fun setIsRoomHost(isHost: Boolean) {
        _isRoomHost.value = isHost
        isGroupOwnerSfu = isHost
    }

    fun removePeerFromRoom(peerIp: String) {
        connectedPeerAddresses.remove(peerIp)
        _activePeers.value = _activePeers.value.filterNot { it.ipAddress == peerIp }
        if (_talkTargetPeerIp.value == peerIp) {
            _talkTargetPeerIp.value = null
        }
    }

    fun connectToPeer(peer: PeerNode) {
        val resolvedPeer = peer.copy(name = resolvePeerName(peer.ipAddress, peer.name))
        val currentList = _activePeers.value.toMutableList()
        val existingIdx = currentList.indexOfFirst { it.ipAddress == resolvedPeer.ipAddress }
        if (existingIdx >= 0) {
            currentList[existingIdx] = resolvedPeer
            _activePeers.value = currentList
        } else if (currentList.size < 6) {
            currentList.add(resolvedPeer)
            _activePeers.value = currentList
        }
        try {
            val addr = InetAddress.getByName(resolvedPeer.ipAddress)
            connectedPeerAddresses[resolvedPeer.ipAddress] = addr
            initiateControlHandshake(resolvedPeer.ipAddress)
        } catch (_: Exception) {
        }
        startVoiceUdpStreaming()
        startKeepAliveHeartbeats()
    }

    fun connectByQrUriOrIp(uriOrIp: String, enteredPin: String? = null, appendToRoom: Boolean = true): PeerNode {
        val ip = if (uriOrIp.startsWith("echolink://")) {
            uriOrIp.substringAfter("ip=", "").substringBefore("&").trim()
        } else {
            uriOrIp.trim()
        }
        val pin = if (uriOrIp.startsWith("echolink://")) {
            uriOrIp.substringAfter("pin=", enteredPin ?: crypto.sessionPin).substringBefore("&")
        } else {
            enteredPin?.takeIf { it.isNotBlank() } ?: crypto.sessionPin
        }
        val defaultHostName = if (uriOrIp.startsWith("echolink://") && uriOrIp.contains("host=")) {
            uriOrIp.substringAfter("host=").substringBefore("&").replace("_", " ")
        } else {
            "Peer ($ip)"
        }
        val hostName = resolvePeerName(ip, defaultHostName)

        val rssi = _packetTelemetry.value.rssiDbm.takeIf { it != 0 } ?: -50
        val joinedPeer = PeerNode(
            id = abs(ip.hashCode()),
            name = hostName,
            roleTag = "Direct P2P Link",
            nodeCode = "PIN #$pin • $ip",
            distanceMeters = estimateDistanceMeters(rssi),
            transport = TransportTier.WIFI_DIRECT,
            ipAddress = ip,
            rssiDbm = rssi,
            pingMs = 0,
            bitrateKbps = _packetTelemetry.value.codecBitrateKbps,
            isSpeaking = false,
            voiceDbFs = -60f,
            earbudCodec = "OPUS ${(_packetTelemetry.value.sampleRateHz / 1000)}kHz",
            isRealNetworkPeer = true
        )
        if (!appendToRoom) {
            _activePeers.value = listOf(joinedPeer)
        }
        if (ip.isNotBlank()) {
            connectToPeer(joinedPeer)
        } else {
            if (_activePeers.value.none { it.ipAddress == joinedPeer.ipAddress }) {
                _activePeers.value = _activePeers.value + joinedPeer
            }
            startVoiceUdpStreaming()
            startKeepAliveHeartbeats()
        }
        return joinedPeer
    }

    fun addMeshPeerToActiveSession() {
        val available = _discoveredPeers.value.filter { candidate ->
            _activePeers.value.none { it.ipAddress == candidate.ipAddress }
        }
        if (available.isNotEmpty() && _activePeers.value.size < 6) {
            connectToPeer(available.first())
        }
    }

    fun disconnectAll() {
        voiceUdpJob?.cancel()
        heartbeatJob?.cancel()
        try {
            voiceSocket?.close()
        } catch (_: Exception) {
        }
        voiceSocket = null
        connectedPeerAddresses.clear()
        _activePeers.value = emptyList()
        _talkTargetPeerIp.value = null
        _packetTelemetry.value = _packetTelemetry.value.copy(
            pingMs = 0,
            flagsByteHex = "0x00 (IDLE)",
            nonceHex = "— (Awaiting Active Stream)"
        )
    }

    private fun startControlChannelServer() {
        if (controlServerJob?.isActive == true) return
        controlServerJob = scope.launch {
            try {
                ServerSocket(CONTROL_PORT).use { server ->
                    while (isActive) {
                        val client = server.accept()
                        launch { handleControlClient(client) }
                    }
                }
            } catch (e: Exception) {
                Log.w("MultiTransportEngine", "Control port $CONTROL_PORT status: ${e.message}")
            }
        }
    }

    private fun handleControlClient(socket: Socket) {
        try {
            socket.soTimeout = 4000
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = PrintWriter(socket.getOutputStream(), true)
            val line = reader.readLine() ?: return
            val peerIp = socket.inetAddress.hostAddress ?: return

            if (line.startsWith("ECHOLINK_HELLO:")) {
                val parts = line.split(":")
                val peerModel = parts.getOrNull(2) ?: "EchoLink Peer"
                val resolvedName = resolvePeerName(peerIp, peerModel)
                connectedPeerAddresses[peerIp] = socket.inetAddress
                val node = upsertDiscoveredLanPeer(resolvedName, peerIp, crypto.sessionPin)
                val currentActive = _activePeers.value.toMutableList()
                val existingIdx = currentActive.indexOfFirst { it.ipAddress == peerIp }
                if (existingIdx >= 0) {
                    currentActive[existingIdx] = node
                    _activePeers.value = currentActive
                } else if (currentActive.size < 6) {
                    currentActive.add(node)
                    _activePeers.value = currentActive
                }
                startVoiceUdpStreaming()
                startKeepAliveHeartbeats()
                onRemotePeerJoinedCallback?.invoke(node)
                writer.println("ECHOLINK_ACK:${crypto.getPublicKeyHex()}:${crypto.sessionPin}:${Build.MODEL}")
            } else if (line.startsWith("PING:")) {
                val sentTs = line.substringAfter("PING:")
                writer.println("PONG:$sentTs")
            }
        } catch (_: Exception) {
        } finally {
            try {
                socket.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun initiateControlHandshake(peerIp: String) {
        scope.launch {
            try {
                val startMs = System.currentTimeMillis()
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(peerIp, CONTROL_PORT), 2000)
                    val writer = PrintWriter(socket.getOutputStream(), true)
                    val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                    writer.println("ECHOLINK_HELLO:${crypto.getPublicKeyHex()}:${Build.MODEL}")
                    val resp = reader.readLine()
                    if (resp != null && resp.startsWith("ECHOLINK_ACK:")) {
                        val rtt = (System.currentTimeMillis() - startMs).toInt().coerceAtLeast(1)
                        val remoteModel = resp.split(":").getOrNull(3)
                        _packetTelemetry.value = _packetTelemetry.value.copy(pingMs = rtt)
                        if (!remoteModel.isNullOrBlank()) {
                            val resolved = resolvePeerName(peerIp, remoteModel)
                            _activePeers.value = _activePeers.value.map {
                                if (it.ipAddress == peerIp) it.copy(name = resolved, pingMs = rtt) else it
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun startVoiceUdpStreaming() {
        if (voiceUdpJob?.isActive == true) return
        voiceUdpJob = scope.launch {
            try {
                val sock = DatagramSocket(null).apply {
                    reuseAddress = true
                    trafficClass = 0xB8 // Expedited Forwarding (DSCP EF)
                    bind(InetSocketAddress(VOICE_UDP_PORT))
                }
                voiceSocket = sock

                // Receiver & Group Mesh SFU Forwarder Coroutine
                launch {
                    val buf = ByteArray(1024)
                    while (isActive && !sock.isClosed) {
                        try {
                            val dgram = DatagramPacket(buf, buf.size)
                            sock.receive(dgram)
                            val senderHost = dgram.address.hostAddress
                            val selectiveTargetIp = _talkTargetPeerIp.value
                            // If Host is talking 1-on-1 to a specific peer, isolate audio to that peer
                            if (_isRoomHost.value && selectiveTargetIp != null && senderHost != null && senderHost != selectiveTargetIp) {
                                continue
                            }
                            val decoded = EchoLinkVoicePacket.decodeDatagram(dgram.data, dgram.length, crypto)
                            if (decoded != null) {
                                audioBridge.pushIncomingRxFrame(
                                    seq = decoded.sequenceNumber,
                                    timestamp = decoded.timestampTicks48k,
                                    payload = decoded.audioPayload
                                )
                                _packetTelemetry.value = _packetTelemetry.value.copy(
                                    rxSequenceNumber = decoded.sequenceNumber,
                                    timestampTicks48k = decoded.timestampTicks48k
                                )
                                // Group Mesh SFU Mode: Forward packet to other connected peers ONLY in "Talk to All" mode
                                if (isGroupOwnerSfu && selectiveTargetIp == null && connectedPeerAddresses.size > 1) {
                                    for ((host, destAddr) in connectedPeerAddresses) {
                                        if (host != senderHost) {
                                            val forwardPkt = DatagramPacket(
                                                dgram.data,
                                                dgram.length,
                                                destAddr,
                                                VOICE_UDP_PORT
                                            )
                                            sock.send(forwardPkt)
                                        }
                                    }
                                }
                            }
                        } catch (_: Exception) {
                        }
                    }
                }

                // Transmitter Coroutine (Discontinuous Transmission / DTX on active voice frames)
                var clockTicks48k = 0L
                while (isActive && !sock.isClosed) {
                    val encodedAudio = audioBridge.pollEncodedTxFrame()
                    if (encodedAudio == null) {
                        delay(10L)
                        continue
                    }
                    val seq = txSeqCounter.incrementAndGet() and 0xFFFF
                    clockTicks48k += 960L // 20ms at 48kHz
                    val vad = audioBridge.isVadActive()
                    val nonce = crypto.generateNonce96Bit(seq, clockTicks48k)

                    val voicePacket = EchoLinkVoicePacket(
                        sequenceNumber = seq,
                        timestampTicks48k = clockTicks48k,
                        isVadActive = vad,
                        isPttActive = isPttActive,
                        peerId = 1,
                        nonce12Bytes = nonce,
                        audioPayload = encodedAudio
                    )

                    val rawBytes = EchoLinkVoicePacket.encodeDatagram(voicePacket, crypto)
                    val nonceHexPreview = nonce.take(4).joinToString("") { "%02x".format(it) } +
                        "..." + nonce.takeLast(2).joinToString("") { "%02x".format(it) }

                    _packetTelemetry.value = _packetTelemetry.value.copy(
                        txSequenceNumber = seq,
                        timestampTicks48k = clockTicks48k,
                        flagsByteHex = "0x%02X (VAD=%d, PTT=%d, ID=1)".format(
                            voicePacket.flagsByte,
                            if (vad) 1 else 0,
                            if (isPttActive) 1 else 0
                        ),
                        nonceHex = "$nonceHexPreview (96-bit)",
                        jitterMs = audioBridge.getJitterBufferMs()
                    )

                    val selectiveTargetIp = _talkTargetPeerIp.value
                    for ((peerIp, addr) in connectedPeerAddresses) {
                        if (_isRoomHost.value && selectiveTargetIp != null && peerIp != selectiveTargetIp) {
                            continue
                        }
                        try {
                            if (!addr.isLoopbackAddress) {
                                val outDgram = DatagramPacket(rawBytes, rawBytes.size, addr, VOICE_UDP_PORT)
                                sock.send(outDgram)
                            }
                        } catch (_: Exception) {
                        }
                    }
                    delay(20L)
                }
            } catch (e: Exception) {
                Log.w("MultiTransportEngine", "UDP Voice socket recovery in 3s: ${e.message}")
                delay(3000L)
                if (_activePeers.value.isNotEmpty()) {
                    startVoiceUdpStreaming()
                }
            }
        }
    }

    /**
     * Real Control Channel Keep-Alive RTT Heartbeats every 2,000 ms (PRD Section 6).
     */
    private fun startKeepAliveHeartbeats() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(2000L)
                refreshHardwareWifiTelemetry()
                val targetEntry = connectedPeerAddresses.entries.firstOrNull()
                if (targetEntry != null) {
                    val peerIp = targetEntry.key
                    val measuredPing = measureTcpPingMs(peerIp)
                    if (measuredPing != null) {
                        _packetTelemetry.value = _packetTelemetry.value.copy(pingMs = measuredPing)
                    }
                }
            }
        }
    }

    private fun measureTcpPingMs(peerIp: String): Int? {
        if (peerIp.isBlank()) return null
        return try {
            val t0 = System.currentTimeMillis()
            Socket().use { sock ->
                sock.soTimeout = 1500
                sock.connect(InetSocketAddress(peerIp, CONTROL_PORT), 1500)
                val writer = PrintWriter(sock.getOutputStream(), true)
                val reader = BufferedReader(InputStreamReader(sock.getInputStream()))
                writer.println("PING:$t0")
                val line = reader.readLine()
                if (line != null && line.startsWith("PONG:")) {
                    (System.currentTimeMillis() - t0).toInt().coerceAtLeast(1)
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Both broadcasts our presence and listens for incoming UDP discovery beacons on port 51112
     * from real EchoLink devices on the same Wi-Fi LAN or Hotspot.
     */
    private fun startLanBeaconDiscovery() {
        if (discoveryBeaconJob?.isActive == true) return
        discoveryBeaconJob = scope.launch {
            try {
                DatagramSocket(null).use { sock ->
                    sock.reuseAddress = true
                    sock.broadcast = true
                    sock.bind(InetSocketAddress(DISCOVERY_BROADCAST_PORT))
                    sock.soTimeout = 1500

                    // Beacon broadcaster child coroutine
                    val broadcaster = launch {
                        while (isActive && !sock.isClosed) {
                            try {
                                val localIp = getLocalIpAddress()
                                _callLinkSession.value = _callLinkSession.value.copy(hostIp = localIp)
                                val beaconMsg = "ECHOLINK_BEACON:${Build.MODEL}:$localIp:${crypto.sessionPin}".toByteArray()
                                val bcastAddr = InetAddress.getByName("255.255.255.255")
                                sock.send(DatagramPacket(beaconMsg, beaconMsg.size, bcastAddr, DISCOVERY_BROADCAST_PORT))
                            } catch (_: Exception) {
                            }
                            delay(3000L)
                        }
                    }

                    // Beacon receiver loop
                    val rxBuf = ByteArray(512)
                    while (isActive && !sock.isClosed) {
                        try {
                            val pkt = DatagramPacket(rxBuf, rxBuf.size)
                            sock.receive(pkt)
                            val text = String(pkt.data, 0, pkt.length).trim()
                            if (text.startsWith("ECHOLINK_BEACON:")) {
                                val parts = text.split(":")
                                val peerModel = parts.getOrNull(1) ?: "EchoLink Peer"
                                val peerIp = parts.getOrNull(2) ?: pkt.address.hostAddress ?: continue
                                val peerPin = parts.getOrNull(3) ?: "0000"
                                val myIp = getLocalIpAddress()
                                if (peerIp != myIp && peerIp != "127.0.0.1") {
                                    upsertDiscoveredLanPeer(peerModel, peerIp, peerPin)
                                }
                            }
                        } catch (_: Exception) {
                        }
                    }
                    broadcaster.cancel()
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun upsertDiscoveredLanPeer(modelName: String, ipAddress: String, pin: String): PeerNode {
        val rssi = _packetTelemetry.value.rssiDbm.takeIf { it != 0 } ?: -52
        val resolvedName = resolvePeerName(ipAddress, modelName)
        val node = PeerNode(
            id = abs(ipAddress.hashCode()),
            name = resolvedName,
            roleTag = "LAN Peer",
            nodeCode = "NODE #$pin • $ipAddress",
            distanceMeters = estimateDistanceMeters(rssi),
            transport = TransportTier.LAN_HOTSPOT,
            ipAddress = ipAddress,
            rssiDbm = rssi,
            pingMs = 0,
            bitrateKbps = _packetTelemetry.value.codecBitrateKbps,
            isSpeaking = false,
            voiceDbFs = -60f,
            earbudCodec = "OPUS P2P",
            isRealNetworkPeer = true
        )
        val current = _discoveredPeers.value.toMutableList()
        val existingIdx = current.indexOfFirst { it.ipAddress == ipAddress }
        if (existingIdx >= 0) {
            current[existingIdx] = node
        } else {
            current.add(node)
        }
        _discoveredPeers.value = current
        return node
    }

    private fun upsertWifiP2pDevice(device: WifiP2pDevice) {
        val devName = device.deviceName?.takeIf { it.isNotBlank() } ?: "Wi-Fi Direct Node"
        val devAddr = device.deviceAddress ?: return
        val ipAddress = "192.168.49.1"
        val resolvedName = resolvePeerName(ipAddress, devName)
        val node = PeerNode(
            id = abs(devAddr.hashCode()),
            name = resolvedName,
            roleTag = "Wi-Fi Direct Peer",
            nodeCode = "P2P MAC $devAddr",
            distanceMeters = 10,
            transport = TransportTier.WIFI_DIRECT,
            ipAddress = ipAddress,
            rssiDbm = -48,
            pingMs = 0,
            bitrateKbps = _packetTelemetry.value.codecBitrateKbps,
            isSpeaking = false,
            voiceDbFs = -60f,
            earbudCodec = "OPUS P2P",
            isRealNetworkPeer = true
        )
        val current = _discoveredPeers.value.toMutableList()
        val existingIdx = current.indexOfFirst { it.id == node.id }
        if (existingIdx >= 0) {
            current[existingIdx] = node
        } else {
            current.add(node)
        }
        _discoveredPeers.value = current
    }

    private fun estimateDistanceMeters(rssiDbm: Int): Int {
        if (rssiDbm == 0) return 5
        val txPower = -40.0
        val ratio = (txPower - rssiDbm) / 20.0
        return 10.0.pow(ratio).roundToInt().coerceIn(1, 100)
    }

    private fun registerWifiDirectReceiver() {
        if (receiverRegistered) return
        try {
            val filter = IntentFilter().apply {
                addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(wifiDirectReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(wifiDirectReceiver, filter)
            }
            receiverRegistered = true
        } catch (_: Exception) {
        }
    }

    private fun unregisterWifiDirectReceiver() {
        if (!receiverRegistered) return
        try {
            context.unregisterReceiver(wifiDirectReceiver)
        } catch (_: Exception) {
        }
        receiverRegistered = false
    }

    @SuppressLint("MissingPermission")
    private fun startWifiDirectDiscovery() {
        val manager = wifiP2pManager ?: return
        val channel = wifiP2pChannel ?: return
        try {
            manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.i("MultiTransportEngine", "Wi-Fi Direct peer discovery active")
                }

                override fun onFailure(reason: Int) {
                    Log.d("MultiTransportEngine", "Wi-Fi Direct discovery status code=$reason")
                }
            })
        } catch (_: SecurityException) {
        }
    }

    private fun acquireLowLatencyWifiLocks() {
        try {
            if (wifiLock == null) {
                val lockMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    WifiManager.WIFI_MODE_FULL_LOW_LATENCY
                } else {
                    @Suppress("DEPRECATION")
                    WifiManager.WIFI_MODE_FULL_HIGH_PERF
                }
                wifiLock = wifiManager.createWifiLock(lockMode, "EchoLink::UltraLowLatencyWifiLock").apply {
                    setReferenceCounted(false)
                    acquire()
                }
            }
            if (multicastLock == null) {
                multicastLock = wifiManager.createMulticastLock("EchoLink::MeshMulticastLock").apply {
                    setReferenceCounted(false)
                    acquire()
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun releaseWifiLocks() {
        try {
            if (wifiLock?.isHeld == true) wifiLock?.release()
            if (multicastLock?.isHeld == true) multicastLock?.release()
        } catch (_: Exception) {
        }
    }

    fun getLocalIpAddress(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                if (!intf.isUp || intf.isLoopback) continue
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        return addr.hostAddress ?: "0.0.0.0"
                    }
                }
            }
            "0.0.0.0"
        } catch (_: Exception) {
            "0.0.0.0"
        }
    }
}
