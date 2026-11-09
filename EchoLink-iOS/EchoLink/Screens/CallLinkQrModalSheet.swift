import AVFoundation
import CoreImage.CIFilterBuiltins
import SwiftUI
import UIKit

struct CallLinkQrModalSheet: View {
    let session: CallLinkSession
    let remainingSeconds: Int
    let onDismiss: () -> Void

    @State private var copiedPin = false

    private var freshnessFraction: CGFloat {
        CGFloat(min(max(Double(remainingSeconds) / 300.0, 0.05), 1.0))
    }

    var body: some View {
        VStack(spacing: 16) {
            Capsule()
                .fill(EchoLinkColors.outlineVariant)
                .frame(width: 48, height: 5)
                .padding(.top, 10)

            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Host Voice Room")
                        .font(EchoLinkTypography.headlineSm)
                        .foregroundColor(EchoLinkColors.textOnSurface)
                    Text("Scan from another phone (iOS or Android) to connect")
                        .font(EchoLinkTypography.bodySm)
                        .foregroundColor(EchoLinkColors.textOnSurfaceVariant)
                }
                Spacer()
                Button(action: onDismiss) {
                    Image(systemName: "xmark")
                        .foregroundColor(EchoLinkColors.textOnSurfaceVariant)
                        .frame(width: 34, height: 34)
                        .background(EchoLinkColors.surfaceContainerHigh)
                        .clipShape(Circle())
                }
            }

            // QR Code Card
            ZStack {
                RoundedRectangle(cornerRadius: 22)
                    .fill(Color.white)
                    .frame(width: 220, height: 220)
                    .shadow(color: EchoLinkColors.cyberCyan.opacity(0.35), radius: 20)

                if let qrImg = Self.generateQrImage(from: session.qrUri) {
                    Image(uiImage: qrImg)
                        .interpolation(.none)
                        .resizable()
                        .scaledToFit()
                        .frame(width: 188, height: 188)
                }
            }

            // Copyable Room Code Pill
            Button {
                UIPasteboard.general.string = session.qrUri
                copiedPin = true
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.8) {
                    copiedPin = false
                }
            } label: {
                HStack {
                    Image(systemName: "checkmark.shield.fill")
                        .foregroundColor(EchoLinkColors.emeraldPulse)
                    Text(copiedPin ? "Link Copied" : "Room Code: \(session.pinCode)")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(EchoLinkColors.textOnSurface)
                    Spacer()
                    Image(systemName: copiedPin ? "checkmark" : "doc.on.doc")
                        .foregroundColor(copiedPin ? EchoLinkColors.emeraldPulse : EchoLinkColors.textOnSurfaceVariant)
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 10)
                .frame(width: 240)
                .background(EchoLinkColors.surfaceContainerLowest)
                .clipShape(Capsule())
                .overlay(Capsule().stroke(EchoLinkColors.armorStroke, lineWidth: 1))
            }

            // Visual Key Freshness Bar
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    Capsule()
                        .fill(EchoLinkColors.surfaceContainerHighest)
                    Capsule()
                        .fill(EchoLinkColors.cyberCyan)
                        .frame(width: geo.size.width * freshnessFraction)
                }
            }
            .frame(width: 220, height: 5)

            ShareLink(item: "Join my EchoLink Voice Room:\n\(session.qrUri)") {
                HStack(spacing: 8) {
                    Image(systemName: "square.and.arrow.up")
                    Text("Share Room Invite")
                        .font(.system(size: 15, weight: .bold, design: .rounded))
                }
                .foregroundColor(EchoLinkColors.obsidianCanvas)
                .frame(maxWidth: .infinity)
                .frame(height: 50)
                .background(EchoLinkColors.cyberCyan)
                .clipShape(Capsule())
            }

            Spacer(minLength: 8)
        }
        .padding(.horizontal, 24)
        .background(EchoLinkColors.surfaceContainerLow.ignoresSafeArea())
    }

    private static func generateQrImage(from text: String) -> UIImage? {
        let context = CIContext()
        let filter = CIFilter.qrCodeGenerator()
        filter.message = Data(text.utf8)
        filter.correctionLevel = "M"
        guard let outputImage = filter.outputImage,
              let cgimg = context.createCGImage(outputImage, from: outputImage.extent) else {
            return nil
        }
        return UIImage(cgImage: cgimg)
    }
}

struct ScanCallLinkSheet: View {
    let onDismiss: () -> Void
    let onJoinLink: (String, String) -> Void

    @State private var peerIp = ""
    @State private var pinCode = ""
    @State private var scannedOnce = false

    private var hasCameraPermission: Bool {
        AVCaptureDevice.authorizationStatus(for: .video) == .authorized
    }

    var body: some View {
        VStack(spacing: 14) {
            Capsule()
                .fill(EchoLinkColors.outlineVariant)
                .frame(width: 48, height: 5)
                .padding(.top, 10)

            HStack {
                HStack(spacing: 8) {
                    Image(systemName: "qrcode.viewfinder")
                        .foregroundColor(EchoLinkColors.emeraldPulse)
                    Text("Scan or Enter Call Link")
                        .font(EchoLinkTypography.headlineSm)
                        .foregroundColor(EchoLinkColors.textOnSurface)
                }
                Spacer()
                Button(action: onDismiss) {
                    Image(systemName: "xmark")
                        .foregroundColor(EchoLinkColors.textOnSurfaceVariant)
                }
            }

            // Live Camera QR Viewfinder
            ZStack {
                RoundedRectangle(cornerRadius: 20)
                    .fill(EchoLinkColors.surfaceContainerLowest)
                    .overlay(
                        RoundedRectangle(cornerRadius: 20)
                            .stroke(EchoLinkColors.emeraldPulse.opacity(0.6), lineWidth: 1)
                    )

                if hasCameraPermission {
                    QrScannerCameraRepresentable { decodedText in
                        if !scannedOnce && !decodedText.isEmpty {
                            scannedOnce = true
                            onJoinLink(decodedText, pinCode)
                        }
                    }
                    .clipShape(RoundedRectangle(cornerRadius: 20))
                } else {
                    VStack(spacing: 6) {
                        Image(systemName: "qrcode.viewfinder")
                            .font(.system(size: 36))
                            .foregroundColor(EchoLinkColors.amberWarning)
                        Text("Camera Access Optional")
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundColor(EchoLinkColors.amberWarning)
                        Text("Grant Camera on Setup tab or enter peer IP below")
                            .font(.system(size: 11))
                            .foregroundColor(EchoLinkColors.textOnSurfaceVariant)
                    }
                }
            }
            .frame(height: 176)

            TextField("Peer P2P / LAN IP or echolink:// URI", text: $peerIp)
                .textFieldStyle(.roundedBorder)
                .autocapitalization(.none)
                .disableAutocorrection(true)

            TextField("4-Digit Room PIN (Optional for LAN)", text: $pinCode)
                .textFieldStyle(.roundedBorder)
                .keyboardType(.numberPad)

            Button {
                onJoinLink(peerIp, pinCode)
            } label: {
                Text("Negotiate P2P Link & Join Voice Channel")
                    .font(.system(size: 15, weight: .bold, design: .rounded))
                    .foregroundColor(EchoLinkColors.obsidianCanvas)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(EchoLinkColors.emeraldPulse)
                    .clipShape(Capsule())
            }
            .disabled(peerIp.trimmingCharacters(in: .whitespaces).isEmpty)

            Spacer(minLength: 8)
        }
        .padding(.horizontal, 24)
        .background(EchoLinkColors.surfaceContainerLow.ignoresSafeArea())
    }
}

struct AddPeersToRoomSheet: View {
    let activePeers: [PeerNode]
    let discoveredPeers: [PeerNode]
    let recentPeers: [RecentPeer]
    let onAddDiscoveredPeer: (PeerNode) -> Void
    let onAddRecentPeer: (RecentPeer) -> Void
    let onRenamePeer: (String, String) -> Void
    let onRemoveRecentPeer: (String) -> Void
    let onShowHostQr: () -> Void
    let onOpenScanOrIp: () -> Void
    let onDismiss: () -> Void

    private var availableNearby: [PeerNode] {
        discoveredPeers.filter { candidate in
            !activePeers.contains(where: { $0.ipAddress == candidate.ipAddress })
        }
    }

    private var availableRecent: [RecentPeer] {
        recentPeers.filter { rec in
            !activePeers.contains(where: { $0.ipAddress == rec.ipAddress }) &&
            !availableNearby.contains(where: { $0.ipAddress == rec.ipAddress })
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Spacer()
                Capsule()
                    .fill(EchoLinkColors.outlineVariant)
                    .frame(width: 48, height: 5)
                Spacer()
            }
            .padding(.top, 10)

            HStack {
                HStack(spacing: 10) {
                    Image(systemName: "person.badge.plus")
                        .foregroundColor(EchoLinkColors.cyberCyan)
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Add People to Room")
                            .font(EchoLinkTypography.headlineSm)
                            .foregroundColor(EchoLinkColors.textOnSurface)
                        Text("\(activePeers.count) connected • Up to 6 peers in room")
                            .font(EchoLinkTypography.bodySm)
                            .foregroundColor(EchoLinkColors.textOnSurfaceVariant)
                    }
                }
                Spacer()
                Button(action: onDismiss) {
                    Image(systemName: "xmark")
                        .foregroundColor(EchoLinkColors.textOnSurfaceVariant)
                }
            }

            HStack(spacing: 10) {
                Button {
                    onDismiss()
                    onShowHostQr()
                } label: {
                    HStack(spacing: 10) {
                        Image(systemName: "qrcode")
                            .foregroundColor(EchoLinkColors.cyberCyan)
                        VStack(alignment: .leading, spacing: 1) {
                            Text("Room QR")
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundColor(EchoLinkColors.textOnSurface)
                            Text("Let peer scan")
                                .font(.system(size: 11))
                                .foregroundColor(EchoLinkColors.textOnSurfaceVariant)
                        }
                        Spacer()
                    }
                    .padding(12)
                    .background(EchoLinkColors.surfaceContainer)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(EchoLinkColors.armorStroke, lineWidth: 1))
                }

                Button {
                    onDismiss()
                    onOpenScanOrIp()
                } label: {
                    HStack(spacing: 10) {
                        Image(systemName: "qrcode.viewfinder")
                            .foregroundColor(EchoLinkColors.emeraldPulse)
                        VStack(alignment: .leading, spacing: 1) {
                            Text("Scan / IP")
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundColor(EchoLinkColors.textOnSurface)
                            Text("Add by QR/IP")
                                .font(.system(size: 11))
                                .foregroundColor(EchoLinkColors.textOnSurfaceVariant)
                        }
                        Spacer()
                    }
                    .padding(12)
                    .background(EchoLinkColors.surfaceContainer)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(EchoLinkColors.armorStroke, lineWidth: 1))
                }
            }

            if !availableNearby.isEmpty {
                Text("Nearby Devices")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(EchoLinkColors.cyberCyan)

                ForEach(availableNearby.prefix(4)) { peer in
                    HStack {
                        TacticalAvatarBadge(name: peer.name, accentColor: peer.transport.accentColor, size: 34)
                        VStack(alignment: .leading, spacing: 2) {
                            HStack(spacing: 6) {
                                Text(peer.name)
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundColor(EchoLinkColors.textOnSurface)
                                Button {
                                    onRenamePeer(peer.ipAddress, peer.name)
                                } label: {
                                    Image(systemName: "pencil")
                                        .font(.system(size: 12))
                                        .foregroundColor(EchoLinkColors.textOnSurfaceVariant)
                                }
                            }
                            Text(peer.transport.fullName)
                                .font(.system(size: 11))
                                .foregroundColor(EchoLinkColors.textOnSurfaceVariant)
                        }
                        Spacer()
                        Button {
                            onAddDiscoveredPeer(peer)
                        } label: {
                            HStack(spacing: 4) {
                                Image(systemName: "plus")
                                Text("Add")
                            }
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(EchoLinkColors.obsidianCanvas)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(EchoLinkColors.emeraldPulse)
                            .clipShape(Capsule())
                        }
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 10)
                    .background(EchoLinkColors.surfaceContainer)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(EchoLinkColors.armorStroke, lineWidth: 1))
                }
            }

            if !availableRecent.isEmpty {
                Text("Recent Connected Devices")
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundColor(EchoLinkColors.textOnSurfaceVariant)

                ForEach(availableRecent.prefix(4)) { recent in
                    HStack {
                        TacticalAvatarBadge(name: recent.name, accentColor: recent.transport.accentColor, size: 34)
                        VStack(alignment: .leading, spacing: 2) {
                            HStack(spacing: 6) {
                                Text(recent.name)
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundColor(EchoLinkColors.textOnSurface)
                                Button {
                                    onRenamePeer(recent.ipAddress, recent.name)
                                } label: {
                                    Image(systemName: "pencil")
                                        .font(.system(size: 12))
                                        .foregroundColor(EchoLinkColors.textOnSurfaceVariant)
                                }
                            }
                            Text(recent.transport.fullName)
                                .font(.system(size: 11))
                                .foregroundColor(EchoLinkColors.textOnSurfaceVariant)
                        }
                        Spacer()
                        Button {
                            onAddRecentPeer(recent)
                        } label: {
                            HStack(spacing: 4) {
                                Image(systemName: "plus")
                                Text("Add")
                            }
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(EchoLinkColors.obsidianCanvas)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(EchoLinkColors.cyberCyan)
                            .clipShape(Capsule())
                        }

                        Button {
                            onRemoveRecentPeer(recent.ipAddress)
                        } label: {
                            Image(systemName: "xmark")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(EchoLinkColors.errorLight)
                                .padding(6)
                        }
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 10)
                    .background(EchoLinkColors.surfaceContainer)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                    .overlay(RoundedRectangle(cornerRadius: 16).stroke(EchoLinkColors.armorStroke, lineWidth: 1))
                }
            }

            Spacer(minLength: 8)
        }
        .padding(.horizontal, 22)
        .background(EchoLinkColors.surfaceContainerLow.ignoresSafeArea())
    }
}

struct QrScannerCameraRepresentable: UIViewControllerRepresentable {
    let onQrDecoded: (String) -> Void

    func makeUIViewController(context: Context) -> QrScannerViewController {
        let vc = QrScannerViewController()
        vc.onCodeScanned = onQrDecoded
        return vc
    }

    func updateUIViewController(_ uiViewController: QrScannerViewController, context: Context) {}
}

final class QrScannerViewController: UIViewController, AVCaptureMetadataOutputObjectsDelegate {
    var onCodeScanned: ((String) -> Void)?
    private let captureSession = AVCaptureSession()

    override func viewDidLoad() {
        super.viewDidLoad()
        guard let videoCaptureDevice = AVCaptureDevice.default(for: .video),
              let videoInput = try? AVCaptureDeviceInput(device: videoCaptureDevice),
              captureSession.canAddInput(videoInput) else { return }

        captureSession.addInput(videoInput)
        let metadataOutput = AVCaptureMetadataOutput()
        if captureSession.canAddOutput(metadataOutput) {
            captureSession.addOutput(metadataOutput)
            metadataOutput.setMetadataObjectsDelegate(self, queue: DispatchQueue.main)
            metadataOutput.metadataObjectTypes = [.qr]
        }

        let previewLayer = AVCaptureVideoPreviewLayer(session: captureSession)
        previewLayer.frame = view.layer.bounds
        previewLayer.videoGravity = .resizeAspectFill
        view.layer.addSublayer(previewLayer)

        DispatchQueue.global(qos: .userInitiated).async {
            self.captureSession.startRunning()
        }
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        if captureSession.isRunning {
            captureSession.stopRunning()
        }
    }

    func metadataOutput(
        _ output: AVCaptureMetadataOutput,
        didOutput metadataObjects: [AVMetadataObject],
        from connection: AVCaptureConnection
    ) {
        if let readable = metadataObjects.first as? AVMetadataMachineReadableCodeObject,
           let value = readable.stringValue {
            onCodeScanned?(value)
        }
    }
}
