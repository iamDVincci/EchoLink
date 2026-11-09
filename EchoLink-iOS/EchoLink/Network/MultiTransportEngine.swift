import Combine
import Darwin
import Foundation
import UIKit

/// Multi-Transport Hybrid Networking Engine for iOS (PRD Section 3.2 & Section 6).
/// 100% wire-protocol compatible with Android `MultiTransportEngine.kt`:
/// - Control Channel: TCP Port 51110 (`ECHOLINK_HELLO`, `ECHOLINK_ACK`, `PING`/`PONG` RTT heartbeats)
/// - Voice Data Channel: UDP Port 51111 (20ms SRTP/PCM16LE datagrams with `0x4147` "AG" header & ChaCha20-Poly1305)
/// - Discovery Channel: UDP Broadcast Port 51112 (`ECHOLINK_BEACON:<model>:<ip>:<pin>`)
final class MultiTransportEngine: ObservableObject {
    static let controlPort: UInt16 = 51110
    static let voiceUdpPort: UInt16 = 51111
    static let discoveryBroadcastPort: UInt16 = 51112

    let crypto = EchoLinkCrypto()
    private let audioEngine: NativeAudioEngine

    @Published private(set) var discoveredPeers: [PeerNode] = []
    @Published private(set) var activePeers: [PeerNode] = []
    @Published private(set) var packetTelemetry = PacketTelemetry()
    @Published private(set) var callLinkSession: CallLinkSession
    @Published private(set) var talkTargetPeerIp: String? = nil // nil = Talk to ALL in Room at once
    @Published private(set) var isRoomHost: Bool = true

    var isPttActive: Bool = false
    var isGroupOwnerSfu: Bool = true
    var onRemotePeerJoinedCallback: ((PeerNode) -> Void)?

    private var customPeerNames: [String: String] = [:]
    private var connectedPeerIps: Set<String> = []
    private let stateLock = NSLock()

    private var txSeqCounter: Int = 0
    private var isRadarRunning = false
    private var isVoiceStreaming = false
    private var voiceSocketFd: Int32 = -1
    private var discoverySocketFd: Int32 = -1
    private var controlServerFd: Int32 = -1
    private var heartbeatTimer: Timer?

    private var deviceModelName: String {
        UIDevice.current.name.replacingOccurrences(of: ":", with: " ")
    }

    init(audioEngine: NativeAudioEngine) {
        self.audioEngine = audioEngine
        let ip = Self.getLocalIpAddress()
        let pin = crypto.sessionPin
        let pub = crypto.getPublicKeyHex()
        let safeHost = UIDevice.current.name.replacingOccurrences(of: " ", with: "_")
        self.callLinkSession = CallLinkSession(
            sessionId: "AG-MESH-\(pin)",
            pinCode: pin,
            hostIp: ip,
            controlPort: Int(Self.controlPort),
            voicePort: Int(Self.voiceUdpPort),
            transportTag: "P2P-DIRECT",
            publicKeyHex: pub,
            qrUri: "echolink://join?ip=\(ip)&cport=\(Self.controlPort)&vport=\(Self.voiceUdpPort)&pin=\(pin)&pub=\(pub)&host=\(safeHost)",
            remainingKeySeconds: 300
        )
    }

    func setInitialCustomNames(_ names: [String: String]) {
        stateLock.lock()
        customPeerNames = names
        stateLock.unlock()
    }

    func resolvePeerName(ipAddress: String, fallbackName: String) -> String {
        stateLock.lock()
        defer { stateLock.unlock() }
        if let custom = customPeerNames[ipAddress], !custom.trimmingCharacters(in: .whitespaces).isEmpty {
            return custom
        }
        return fallbackName
    }

    func setCustomPeerName(ipAddress: String, customName: String) {
        let clean = customName.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !clean.isEmpty else { return }
        stateLock.lock()
        customPeerNames[ipAddress] = clean
        stateLock.unlock()

        DispatchQueue.main.async {
            self.activePeers = self.activePeers.map {
                $0.ipAddress == ipAddress ? PeerNode(
                    id: $0.id, name: clean, roleTag: $0.roleTag, nodeCode: $0.nodeCode,
                    distanceMeters: $0.distanceMeters, transport: $0.transport, ipAddress: $0.ipAddress,
                    controlPort: $0.controlPort, voicePort: $0.voicePort, rssiDbm: $0.rssiDbm,
                    pingMs: $0.pingMs, bitrateKbps: $0.bitrateKbps, isSpeaking: $0.isSpeaking,
                    voiceDbFs: $0.voiceDbFs, earbudCodec: $0.earbudCodec, isRealNetworkPeer: $0.isRealNetworkPeer
                ) : $0
            }
            self.discoveredPeers = self.discoveredPeers.map {
                $0.ipAddress == ipAddress ? PeerNode(
                    id: $0.id, name: clean, roleTag: $0.roleTag, nodeCode: $0.nodeCode,
                    distanceMeters: $0.distanceMeters, transport: $0.transport, ipAddress: $0.ipAddress,
                    controlPort: $0.controlPort, voicePort: $0.voicePort, rssiDbm: $0.rssiDbm,
                    pingMs: $0.pingMs, bitrateKbps: $0.bitrateKbps, isSpeaking: $0.isSpeaking,
                    voiceDbFs: $0.voiceDbFs, earbudCodec: $0.earbudCodec, isRealNetworkPeer: $0.isRealNetworkPeer
                ) : $0
            }
        }
    }

    func setTalkTargetPeerIp(_ peerIp: String?) {
        DispatchQueue.main.async {
            self.talkTargetPeerIp = peerIp
        }
    }

    func setIsRoomHost(_ isHost: Bool) {
        DispatchQueue.main.async {
            self.isRoomHost = isHost
        }
        isGroupOwnerSfu = isHost
    }

    func removePeerFromRoom(_ peerIp: String) {
        stateLock.lock()
        connectedPeerIps.remove(peerIp)
        stateLock.unlock()
        DispatchQueue.main.async {
            self.activePeers.removeAll { $0.ipAddress == peerIp }
            if self.talkTargetPeerIp == peerIp {
                self.talkTargetPeerIp = nil
            }
        }
    }

    @discardableResult
    func refreshCallLinkSession() -> CallLinkSession {
        crypto.rotateSessionKey()
        let ip = Self.getLocalIpAddress()
        let pin = crypto.sessionPin
        let pub = crypto.getPublicKeyHex()
        let safeHost = deviceModelName.replacingOccurrences(of: " ", with: "_")
        let session = CallLinkSession(
            sessionId: "AG-MESH-\(pin)",
            pinCode: pin,
            hostIp: ip,
            controlPort: Int(Self.controlPort),
            voicePort: Int(Self.voiceUdpPort),
            transportTag: "P2P-DIRECT",
            publicKeyHex: pub,
            qrUri: "echolink://join?ip=\(ip)&cport=\(Self.controlPort)&vport=\(Self.voiceUdpPort)&pin=\(pin)&pub=\(pub)&host=\(safeHost)",
            remainingKeySeconds: 300
        )
        DispatchQueue.main.async {
            self.callLinkSession = session
        }
        return session
    }

    func startRadarDiscovery() {
        guard !isRadarRunning else { return }
        isRadarRunning = true
        refreshCallLinkSession()
        startControlChannelServer()
        startLanBeaconDiscovery()
    }

    func stopRadarDiscovery() {
        isRadarRunning = false
        if discoverySocketFd >= 0 {
            Darwin.close(discoverySocketFd)
            discoverySocketFd = -1
        }
    }

    func connectToPeer(_ peer: PeerNode) {
        let resolvedName = resolvePeerName(ipAddress: peer.ipAddress, fallbackName: peer.name)
        var resolvedPeer = peer
        resolvedPeer.name = resolvedName

        stateLock.lock()
        connectedPeerIps.insert(resolvedPeer.ipAddress)
        stateLock.unlock()

        DispatchQueue.main.async {
            if let idx = self.activePeers.firstIndex(where: { $0.ipAddress == resolvedPeer.ipAddress }) {
                self.activePeers[idx] = resolvedPeer
            } else if self.activePeers.count < 6 {
                self.activePeers.append(resolvedPeer)
            }
        }

        initiateControlHandshake(peerIp: resolvedPeer.ipAddress)
        startVoiceUdpStreaming()
        startKeepAliveHeartbeats()
    }

    @discardableResult
    func connectByQrUriOrIp(_ uriOrIp: String, enteredPin: String? = nil, appendToRoom: Bool = true) -> PeerNode {
        let ip: String
        let pin: String
        let defaultHostName: String

        if uriOrIp.hasPrefix("echolink://") {
            ip = Self.extractQueryParam(from: uriOrIp, key: "ip") ?? uriOrIp
            pin = Self.extractQueryParam(from: uriOrIp, key: "pin") ?? enteredPin ?? crypto.sessionPin
            let rawHost = Self.extractQueryParam(from: uriOrIp, key: "host")?.replacingOccurrences(of: "_", with: " ")
            defaultHostName = (rawHost?.isEmpty == false) ? rawHost! : "Peer (\(ip))"
        } else {
            ip = uriOrIp.trimmingCharacters(in: .whitespacesAndNewlines)
            pin = (enteredPin?.isEmpty == false) ? enteredPin! : crypto.sessionPin
            defaultHostName = "Peer (\(ip))"
        }

        let resolvedName = resolvePeerName(ipAddress: ip, fallbackName: defaultHostName)
        let joinedPeer = PeerNode(
            id: abs(ip.hashValue),
            name: resolvedName,
            roleTag: "Direct P2P Link",
            nodeCode: "PIN #\(pin) • \(ip)",
            distanceMeters: 4,
            transport: .wifiDirect,
            ipAddress: ip,
            rssiDbm: -50,
            pingMs: 0,
            bitrateKbps: packetTelemetry.codecBitrateKbps,
            isSpeaking: false,
            voiceDbFs: -60.0,
            earbudCodec: "OPUS \(packetTelemetry.sampleRateHz / 1000)kHz",
            isRealNetworkPeer: true
        )

        if !appendToRoom {
            DispatchQueue.main.async {
                self.activePeers = [joinedPeer]
            }
        }
        connectToPeer(joinedPeer)
        return joinedPeer
    }

    func disconnectAll() {
        isVoiceStreaming = false
        heartbeatTimer?.invalidate()
        heartbeatTimer = nil
        if voiceSocketFd >= 0 {
            Darwin.close(voiceSocketFd)
            voiceSocketFd = -1
        }
        stateLock.lock()
        connectedPeerIps.removeAll()
        stateLock.unlock()

        DispatchQueue.main.async {
            self.activePeers = []
            self.talkTargetPeerIp = nil
            self.packetTelemetry.pingMs = 0
            self.packetTelemetry.flagsByteHex = "0x00 (IDLE)"
            self.packetTelemetry.nonceHex = "— (Awaiting Active Stream)"
        }
    }

    // MARK: - UDP Voice Streaming (Port 51111) with Host Broadcast vs 1-on-1 Routing

    private func startVoiceUdpStreaming() {
        guard !isVoiceStreaming else { return }
        isVoiceStreaming = true

        DispatchQueue.global(qos: .userInteractive).async { [weak self] in
            guard let self = self else { return }
            let fd = Darwin.socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP)
            guard fd >= 0 else {
                self.isVoiceStreaming = false
                return
            }
            var reuse: Int32 = 1
            setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, &reuse, socklen_t(MemoryLayout<Int32>.size))
            setsockopt(fd, SOL_SOCKET, SO_REUSEPORT, &reuse, socklen_t(MemoryLayout<Int32>.size))

            var bindAddr = sockaddr_in()
            bindAddr.sin_len = UInt8(MemoryLayout<sockaddr_in>.size)
            bindAddr.sin_family = sa_family_t(AF_INET)
            bindAddr.sin_port = Self.voiceUdpPort.bigEndian
            bindAddr.sin_addr.s_addr = INADDR_ANY.bigEndian

            let bindRes = withUnsafePointer(to: &bindAddr) {
                $0.withMemoryRebound(to: sockaddr.self, capacity: 1) {
                    Darwin.bind(fd, $0, socklen_t(MemoryLayout<sockaddr_in>.size))
                }
            }
            if bindRes < 0 {
                Darwin.close(fd)
                self.isVoiceStreaming = false
                return
            }
            self.voiceSocketFd = fd

            // Receiver & Group Mesh SFU Forwarder Thread
            DispatchQueue.global(qos: .userInteractive).async { [weak self] in
                guard let self = self else { return }
                var buf = [UInt8](repeating: 0, count: 1024)
                while self.isVoiceStreaming && self.voiceSocketFd >= 0 {
                    var srcAddr = sockaddr_in()
                    var srcLen = socklen_t(MemoryLayout<sockaddr_in>.size)
                    let n = withUnsafeMutablePointer(to: &srcAddr) {
                        $0.withMemoryRebound(to: sockaddr.self, capacity: 1) {
                            Darwin.recvfrom(fd, &buf, buf.count, 0, $0, &srcLen)
                        }
                    }
                    if n > 0 {
                        let senderIp = String(cString: inet_ntoa(srcAddr.sin_addr))
                        let selectiveTarget = self.talkTargetPeerIp
                        if self.isRoomHost, let target = selectiveTarget, senderIp != target {
                            continue
                        }
                        let rawData = Data(bytes: buf, count: n)
                        if let decoded = EchoLinkVoicePacket.decodeDatagram(datagram: rawData, crypto: self.crypto) {
                            self.audioEngine.pushIncomingRxFrame(
                                seq: decoded.sequenceNumber,
                                timestamp: decoded.timestampTicks48k,
                                payload: decoded.audioPayload
                            )
                            DispatchQueue.main.async {
                                self.packetTelemetry.rxSequenceNumber = decoded.sequenceNumber
                                self.packetTelemetry.timestampTicks48k = decoded.timestampTicks48k
                            }

                            // SFU Mesh Forwarder in "Talk to All" mode
                            self.stateLock.lock()
                            let peers = Array(self.connectedPeerIps)
                            self.stateLock.unlock()

                            if self.isGroupOwnerSfu && selectiveTarget == nil && peers.count > 1 {
                                for destIp in peers where destIp != senderIp {
                                    Self.sendUdpBytes(rawData, toIp: destIp, port: Self.voiceUdpPort, socketFd: fd)
                                }
                            }
                        }
                    }
                }
            }

            // Transmitter Loop (Discontinuous Transmission / DTX)
            var clockTicks48k: Int64 = 0
            while self.isVoiceStreaming && self.voiceSocketFd >= 0 {
                guard let encodedAudio = self.audioEngine.pollEncodedTxFrame() else {
                    Thread.sleep(forTimeInterval: 0.010)
                    continue
                }
                self.txSeqCounter = (self.txSeqCounter + 1) & 0xFFFF
                let seq = self.txSeqCounter
                clockTicks48k += 960
                let vad = self.audioEngine.isVadActive()
                let nonce = self.crypto.generateNonce96Bit(sequenceNumber: seq, timestampTicks: clockTicks48k)

                let pkt = EchoLinkVoicePacket(
                    sequenceNumber: seq,
                    timestampTicks48k: clockTicks48k,
                    isVadActive: vad,
                    isPttActive: self.isPttActive,
                    peerId: 1,
                    nonce12Bytes: nonce,
                    audioPayload: encodedAudio
                )
                let rawDatagram = EchoLinkVoicePacket.encodeDatagram(packet: pkt, crypto: self.crypto)

                self.stateLock.lock()
                let targets = Array(self.connectedPeerIps)
                self.stateLock.unlock()

                let selectiveTarget = self.talkTargetPeerIp
                for peerIp in targets {
                    if self.isRoomHost, let target = selectiveTarget, peerIp != target {
                        continue
                    }
                    Self.sendUdpBytes(rawDatagram, toIp: peerIp, port: Self.voiceUdpPort, socketFd: fd)
                }

                DispatchQueue.main.async {
                    self.packetTelemetry.txSequenceNumber = seq
                    self.packetTelemetry.timestampTicks48k = clockTicks48k
                }
                Thread.sleep(forTimeInterval: 0.020)
            }
        }
    }

    // MARK: - UDP Discovery Broadcast & Listener (Port 51112)

    private func startLanBeaconDiscovery() {
        DispatchQueue.global(qos: .utility).async { [weak self] in
            guard let self = self else { return }
            let fd = Darwin.socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP)
            guard fd >= 0 else { return }
            var yes: Int32 = 1
            setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, &yes, socklen_t(MemoryLayout<Int32>.size))
            setsockopt(fd, SOL_SOCKET, SO_REUSEPORT, &yes, socklen_t(MemoryLayout<Int32>.size))
            setsockopt(fd, SOL_SOCKET, SO_BROADCAST, &yes, socklen_t(MemoryLayout<Int32>.size))

            var tv = timeval(tv_sec: 1, tv_usec: 500_000)
            setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, &tv, socklen_t(MemoryLayout<timeval>.size))

            var addr = sockaddr_in()
            addr.sin_len = UInt8(MemoryLayout<sockaddr_in>.size)
            addr.sin_family = sa_family_t(AF_INET)
            addr.sin_port = Self.discoveryBroadcastPort.bigEndian
            addr.sin_addr.s_addr = INADDR_ANY.bigEndian

            let bindRes = withUnsafePointer(to: &addr) {
                $0.withMemoryRebound(to: sockaddr.self, capacity: 1) {
                    Darwin.bind(fd, $0, socklen_t(MemoryLayout<sockaddr_in>.size))
                }
            }
            if bindRes < 0 {
                Darwin.close(fd)
                return
            }
            self.discoverySocketFd = fd

            // Broadcaster Loop
            DispatchQueue.global(qos: .utility).async { [weak self] in
                guard let self = self else { return }
                while self.isRadarRunning && self.discoverySocketFd >= 0 {
                    let localIp = Self.getLocalIpAddress()
                    DispatchQueue.main.async {
                        self.callLinkSession.hostIp = localIp
                    }
                    let msg = "ECHOLINK_BEACON:\(self.deviceModelName):\(localIp):\(self.crypto.sessionPin)"
                    if let data = msg.data(using: .utf8) {
                        Self.sendUdpBytes(data, toIp: "255.255.255.255", port: Self.discoveryBroadcastPort, socketFd: fd)
                    }
                    Thread.sleep(forTimeInterval: 3.0)
                }
            }

            // Receiver Loop
            var rxBuf = [UInt8](repeating: 0, count: 512)
            while self.isRadarRunning && self.discoverySocketFd >= 0 {
                var srcAddr = sockaddr_in()
                var srcLen = socklen_t(MemoryLayout<sockaddr_in>.size)
                let n = withUnsafeMutablePointer(to: &srcAddr) {
                    $0.withMemoryRebound(to: sockaddr.self, capacity: 1) {
                        Darwin.recvfrom(fd, &rxBuf, rxBuf.count, 0, $0, &srcLen)
                    }
                }
                if n > 0, let text = String(bytes: rxBuf.prefix(n), encoding: .utf8)?.trimmingCharacters(in: .whitespacesAndNewlines) {
                    if text.hasPrefix("ECHOLINK_BEACON:") {
                        let parts = text.components(separatedBy: ":")
                        let peerModel = parts.count > 1 ? parts[1] : "EchoLink Peer"
                        let peerIp = parts.count > 2 ? parts[2] : String(cString: inet_ntoa(srcAddr.sin_addr))
                        let peerPin = parts.count > 3 ? parts[3] : "0000"
                        let myIp = Self.getLocalIpAddress()
                        if peerIp != myIp && peerIp != "127.0.0.1" && peerIp != "0.0.0.0" {
                            _ = self.upsertDiscoveredLanPeer(modelName: peerModel, ipAddress: peerIp, pin: peerPin)
                        }
                    }
                }
            }
        }
    }

    @discardableResult
    private func upsertDiscoveredLanPeer(modelName: String, ipAddress: String, pin: String) -> PeerNode {
        let resolvedName = resolvePeerName(ipAddress: ipAddress, fallbackName: modelName)
        let node = PeerNode(
            id: abs(ipAddress.hashValue),
            name: resolvedName,
            roleTag: "LAN Peer",
            nodeCode: "NODE #\(pin) • \(ipAddress)",
            distanceMeters: 4,
            transport: .lanHotspot,
            ipAddress: ipAddress,
            rssiDbm: -50,
            pingMs: 0,
            bitrateKbps: packetTelemetry.codecBitrateKbps,
            isSpeaking: false,
            voiceDbFs: -60.0,
            earbudCodec: "OPUS P2P",
            isRealNetworkPeer: true
        )
        DispatchQueue.main.async {
            if let idx = self.discoveredPeers.firstIndex(where: { $0.ipAddress == ipAddress }) {
                self.discoveredPeers[idx] = node
            } else {
                self.discoveredPeers.append(node)
            }
        }
        return node
    }

    // MARK: - TCP Control Channel (Port 51110)

    private func startControlChannelServer() {
        guard controlServerFd < 0 else { return }
        DispatchQueue.global(qos: .utility).async { [weak self] in
            guard let self = self else { return }
            let fd = Darwin.socket(AF_INET, SOCK_STREAM, IPPROTO_TCP)
            guard fd >= 0 else { return }
            var yes: Int32 = 1
            setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, &yes, socklen_t(MemoryLayout<Int32>.size))

            var addr = sockaddr_in()
            addr.sin_len = UInt8(MemoryLayout<sockaddr_in>.size)
            addr.sin_family = sa_family_t(AF_INET)
            addr.sin_port = Self.controlPort.bigEndian
            addr.sin_addr.s_addr = INADDR_ANY.bigEndian

            let bindRes = withUnsafePointer(to: &addr) {
                $0.withMemoryRebound(to: sockaddr.self, capacity: 1) {
                    Darwin.bind(fd, $0, socklen_t(MemoryLayout<sockaddr_in>.size))
                }
            }
            guard bindRes == 0, Darwin.listen(fd, 8) == 0 else {
                Darwin.close(fd)
                return
            }
            self.controlServerFd = fd

            while self.controlServerFd >= 0 {
                var clientAddr = sockaddr_in()
                var clientLen = socklen_t(MemoryLayout<sockaddr_in>.size)
                let clientFd = withUnsafeMutablePointer(to: &clientAddr) {
                    $0.withMemoryRebound(to: sockaddr.self, capacity: 1) {
                        Darwin.accept(fd, $0, &clientLen)
                    }
                }
                if clientFd >= 0 {
                    let peerIp = String(cString: inet_ntoa(clientAddr.sin_addr))
                    DispatchQueue.global(qos: .userInitiated).async {
                        self.handleControlClient(clientFd: clientFd, peerIp: peerIp)
                    }
                }
            }
        }
    }

    private func handleControlClient(clientFd: Int32, peerIp: String) {
        defer { Darwin.close(clientFd) }
        var buf = [UInt8](repeating: 0, count: 512)
        let n = Darwin.recv(clientFd, &buf, buf.count - 1, 0)
        guard n > 0, let line = String(bytes: buf.prefix(n), encoding: .utf8)?.trimmingCharacters(in: .whitespacesAndNewlines) else {
            return
        }

        if line.hasPrefix("ECHOLINK_HELLO:") {
            let parts = line.components(separatedBy: ":")
            let peerModel = parts.count > 2 ? parts[2] : "EchoLink Peer"
            let resolved = resolvePeerName(ipAddress: peerIp, fallbackName: peerModel)
            stateLock.lock()
            connectedPeerIps.insert(peerIp)
            stateLock.unlock()

            let node = upsertDiscoveredLanPeer(modelName: resolved, ipAddress: peerIp, pin: crypto.sessionPin)
            DispatchQueue.main.async {
                if let idx = self.activePeers.firstIndex(where: { $0.ipAddress == peerIp }) {
                    self.activePeers[idx] = node
                } else if self.activePeers.count < 6 {
                    self.activePeers.append(node)
                }
                self.onRemotePeerJoinedCallback?(node)
            }
            startVoiceUdpStreaming()
            startKeepAliveHeartbeats()

            let ack = "ECHOLINK_ACK:\(crypto.getPublicKeyHex()):\(crypto.sessionPin):\(deviceModelName)\n"
            if let data = ack.data(using: .utf8) {
                _ = data.withUnsafeBytes { Darwin.send(clientFd, $0.baseAddress, data.count, 0) }
            }
        } else if line.hasPrefix("PING:") {
            let ts = line.replacingOccurrences(of: "PING:", with: "")
            let pong = "PONG:\(ts)\n"
            if let data = pong.data(using: .utf8) {
                _ = data.withUnsafeBytes { Darwin.send(clientFd, $0.baseAddress, data.count, 0) }
            }
        }
    }

    private func initiateControlHandshake(peerIp: String) {
        DispatchQueue.global(qos: .userInitiated).async { [weak self] in
            guard let self = self else { return }
            let startMs = Int64(Date().timeIntervalSince1970 * 1000)
            let fd = Darwin.socket(AF_INET, SOCK_STREAM, IPPROTO_TCP)
            guard fd >= 0 else { return }
            defer { Darwin.close(fd) }

            var tv = timeval(tv_sec: 2, tv_usec: 0)
            setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, &tv, socklen_t(MemoryLayout<timeval>.size))
            setsockopt(fd, SOL_SOCKET, SO_SNDTIMEO, &tv, socklen_t(MemoryLayout<timeval>.size))

            var addr = sockaddr_in()
            addr.sin_len = UInt8(MemoryLayout<sockaddr_in>.size)
            addr.sin_family = sa_family_t(AF_INET)
            addr.sin_port = Self.controlPort.bigEndian
            addr.sin_addr.s_addr = inet_addr(peerIp)

            let res = withUnsafePointer(to: &addr) {
                $0.withMemoryRebound(to: sockaddr.self, capacity: 1) {
                    Darwin.connect(fd, $0, socklen_t(MemoryLayout<sockaddr_in>.size))
                }
            }
            guard res == 0 else { return }

            let hello = "ECHOLINK_HELLO:\(self.crypto.getPublicKeyHex()):\(self.deviceModelName)\n"
            if let data = hello.data(using: .utf8) {
                _ = data.withUnsafeBytes { Darwin.send(fd, $0.baseAddress, data.count, 0) }
            }

            var buf = [UInt8](repeating: 0, count: 512)
            let n = Darwin.recv(fd, &buf, buf.count - 1, 0)
            if n > 0, let resp = String(bytes: buf.prefix(n), encoding: .utf8)?.trimmingCharacters(in: .whitespacesAndNewlines),
               resp.hasPrefix("ECHOLINK_ACK:") {
                let rtt = max(1, Int(Int64(Date().timeIntervalSince1970 * 1000) - startMs))
                let parts = resp.components(separatedBy: ":")
                let remoteModel = parts.count > 3 ? parts[3] : nil
                DispatchQueue.main.async {
                    self.packetTelemetry.pingMs = rtt
                    if let remoteModel = remoteModel, !remoteModel.isEmpty {
                        let resolved = self.resolvePeerName(ipAddress: peerIp, fallbackName: remoteModel)
                        self.activePeers = self.activePeers.map {
                            if $0.ipAddress == peerIp {
                                var copy = $0
                                copy.name = resolved
                                copy.pingMs = rtt
                                return copy
                            }
                            return $0
                        }
                    }
                }
            }
        }
    }

    private func startKeepAliveHeartbeats() {
        DispatchQueue.main.async {
            self.heartbeatTimer?.invalidate()
            self.heartbeatTimer = Timer.scheduledTimer(withTimeInterval: 2.0, repeats: true) { [weak self] _ in
                guard let self = self else { return }
                self.stateLock.lock()
                let firstIp = self.connectedPeerIps.first
                self.stateLock.unlock()
                if let ip = firstIp {
                    self.measureTcpPing(peerIp: ip)
                }
            }
        }
    }

    private func measureTcpPing(peerIp: String) {
        DispatchQueue.global(qos: .utility).async { [weak self] in
            let t0 = Int64(Date().timeIntervalSince1970 * 1000)
            let fd = Darwin.socket(AF_INET, SOCK_STREAM, IPPROTO_TCP)
            guard fd >= 0 else { return }
            defer { Darwin.close(fd) }

            var tv = timeval(tv_sec: 1, tv_usec: 500_000)
            setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, &tv, socklen_t(MemoryLayout<timeval>.size))

            var addr = sockaddr_in()
            addr.sin_len = UInt8(MemoryLayout<sockaddr_in>.size)
            addr.sin_family = sa_family_t(AF_INET)
            addr.sin_port = Self.controlPort.bigEndian
            addr.sin_addr.s_addr = inet_addr(peerIp)

            let res = withUnsafePointer(to: &addr) {
                $0.withMemoryRebound(to: sockaddr.self, capacity: 1) {
                    Darwin.connect(fd, $0, socklen_t(MemoryLayout<sockaddr_in>.size))
                }
            }
            guard res == 0 else { return }
            let ping = "PING:\(t0)\n"
            if let data = ping.data(using: .utf8) {
                _ = data.withUnsafeBytes { Darwin.send(fd, $0.baseAddress, data.count, 0) }
            }
            var buf = [UInt8](repeating: 0, count: 128)
            let n = Darwin.recv(fd, &buf, buf.count - 1, 0)
            if n > 0, let resp = String(bytes: buf.prefix(n), encoding: .utf8), resp.hasPrefix("PONG:") {
                let rtt = max(1, Int(Int64(Date().timeIntervalSince1970 * 1000) - t0))
                DispatchQueue.main.async {
                    self?.packetTelemetry.pingMs = rtt
                }
            }
        }
    }

    // MARK: - Helpers

    private static func sendUdpBytes(_ data: Data, toIp: String, port: UInt16, socketFd: Int32) {
        var dest = sockaddr_in()
        dest.sin_len = UInt8(MemoryLayout<sockaddr_in>.size)
        dest.sin_family = sa_family_t(AF_INET)
        dest.sin_port = port.bigEndian
        dest.sin_addr.s_addr = inet_addr(toIp)

        _ = data.withUnsafeBytes { rawBuf in
            withUnsafePointer(to: &dest) {
                $0.withMemoryRebound(to: sockaddr.self, capacity: 1) { addrPtr in
                    Darwin.sendto(socketFd, rawBuf.baseAddress, data.count, 0, addrPtr, socklen_t(MemoryLayout<sockaddr_in>.size))
                }
            }
        }
    }

    private static func extractQueryParam(from uri: String, key: String) -> String? {
        guard let components = URLComponents(string: uri),
              let items = components.queryItems else { return nil }
        return items.first(where: { $0.name == key })?.value
    }

    static func getLocalIpAddress() -> String {
        var address = "0.0.0.0"
        var ifaddr: UnsafeMutablePointer<ifaddrs>?
        if getifaddrs(&ifaddr) == 0, let firstAddr = ifaddr {
            for ptr in sequence(first: firstAddr, next: { $0.pointee.ifa_next }) {
                let interface = ptr.pointee
                let addrFamily = interface.ifa_addr.pointee.sa_family
                if addrFamily == UInt8(AF_INET) {
                    let name = String(cString: interface.ifa_name)
                    if name == "en0" || name.hasPrefix("bridge") || name.hasPrefix("ap") {
                        var hostname = [CChar](repeating: 0, count: Int(NI_MAXHOST))
                        getnameinfo(
                            interface.ifa_addr,
                            socklen_t(interface.ifa_addr.pointee.sa_len),
                            &hostname,
                            socklen_t(hostname.count),
                            nil,
                            socklen_t(0),
                            NI_NUMERICHOST
                        )
                        address = String(cString: hostname)
                        if address != "127.0.0.1" { break }
                    }
                }
            }
            freeifaddrs(ifaddr)
        }
        return address
    }
}
