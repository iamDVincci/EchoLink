# EchoLink v2.4 — Cross-Platform P2P Tactical Voice Intercom (Android & iOS)

> 🌐 **Live Interactive Website & Documentation:** [**https://iamdvincci.github.io/EchoLink/**](https://iamdvincci.github.io/EchoLink/)  
> 📲 **Direct iOS IPA:** [`EchoLink-iOS-Unsigned.ipa`](https://github.com/iamDVincci/EchoLink/releases/download/v2.4.0/EchoLink-iOS-Unsigned.ipa) | 🤖 **Direct Android APK:** [`EchoLink-Android-v2.4.0.apk`](https://github.com/iamDVincci/EchoLink/releases/download/v2.4.0/EchoLink-Android-v2.4.0.apk)

**EchoLink** is a zero-server, ultra-low-latency Peer-to-Peer (P2P) voice intercom built natively for **Android (Kotlin / Jetpack Compose)** and **iOS (Swift / SwiftUI)** with 100% wire-protocol, cryptographic, DSP, and UI parity.

---

## Key Features
- **Cross-Platform Wire Compatibility (`Android <-> iOS`)**:
  - **UDP Discovery Beacon (`51112`)**: Automatic zero-configuration peer discovery (`ECHOLINK_PEER|2.4|<DeviceName>`) across local Wi-Fi and mobile hotspots.
  - **TCP Control & Room Handshake (`51110`)**: Supports `PAIR_REQ`/`PAIR_ACK`, multi-peer room invitations (`ROOM_INVITE`/`ROOM_PEER_ADD`), and clean disconnects (`UNPAIR`).
  - **Encrypted Real-Time Voice (`51111`)**: `16 kHz` 16-bit mono PCM audio framed in `20 ms` packets (`640 bytes`) with `0x4147 ("AG")` headers and **ChaCha20-Poly1305 AEAD (`RFC 7539`)** encryption + **Discontinuous Transmission (DTX)** silence suppression.
- **5-Stage Real-Time Studio DSP Pipeline**:
  1. **1st-Order IIR High-Pass Filter (`120 Hz`)**: Strips mic handling thumps and DC offset.
  2. **1st-Order IIR Low-Pass Filter (`6.5 kHz`)**: Eliminates high-frequency Bluetooth/earbud DAC hiss and aliasing.
  3. **Adaptive RMS Noise Gate (`-46 dBFS` threshold with `15 ms` attack / `220 ms` release)**: Completely silences background floor noise between words (`DTX`) so earbuds have zero white noise.
  4. **RBJ Biquad Peaking EQ (`2.8 kHz`, `Q = 1.2`, up to `+6.5 dB`)**: Boosts human speech consonant intelligibility.
  5. **Soft-Knee Vocal Boost Compressor (`+4.5 dB` makeup gain + `tanh` limiter) & 0.75 ms Haas 3D Spatial Stereo Widening**.
- **Host Multi-Peer Room Controls & Device Management**:
  - **Talk to All (Broadcast) vs. 1-on-1 Selective Routing**: When hosting multiple peers, switch between broadcasting to all connected peers simultaneously or talking privately 1-on-1 with any individual peer.
  - **Add Peers to Active Room**: Invite additional nearby devices into an ongoing call at any time.
  - **Custom Device Renaming & History Management**: Rename any peer (`✏️`) with persistent local storage, delete individual saved devices (`×`), or clear your entire connection history.

---

## 📲 How to Download & Install on Any iPhone

Every push to `main` automatically compiles `EchoLink-iOS-Unsigned.ipa` on GitHub Actions (`macos-14`) and publishes it to [**GitHub Releases**](../../releases/latest).

### Option 1: Install from Windows or Mac using Sideloadly (Recommended — Free Apple ID)
1. Download **`EchoLink-iOS-Unsigned.ipa`** from the [**Latest Release**](../../releases/latest/download/EchoLink-iOS-Unsigned.ipa).
2. Download and install [**Sideloadly**](https://sideloadly.io/) on your Windows PC or Mac (make sure non-Microsoft Store **iTunes** and **iCloud** are installed if on Windows).
3. Plug your iPhone into your computer via USB and tap **"Trust This Computer"** on your iPhone.
4. Open **Sideloadly**, select your iPhone in the **iDevice** dropdown, and enter your **Apple ID** (any free Apple ID works).
5. Drag and drop **`EchoLink-iOS-Unsigned.ipa`** into Sideloadly and click **Start** (enter your Apple ID password / 2FA code if prompted).
6. Once installed on your iPhone, go to **Settings → General → VPN & Device Management**, tap your **Apple ID** under *Developer App*, and tap **Trust**.
7. *(iOS 16+ only)* Go to **Settings → Privacy & Security → Developer Mode**, toggle it **ON**, and restart your iPhone when prompted.

### Option 2: Install Directly on Your iPhone using AltStore / SideStore / TrollStore
1. Open Safari on your iPhone and download **`EchoLink-iOS-Unsigned.ipa`** from [**Releases**](../../releases/latest/download/EchoLink-iOS-Unsigned.ipa).
2. Share/Open the downloaded `.ipa` file in **SideStore**, **AltStore**, or **TrollStore** to sign and install directly on-device.

### Option 3: Build Directly in Xcode on a Mac
1. Clone this repository and open `EchoLink-iOS/EchoLink.xcodeproj` in **Xcode 15+**.
2. In **Signing & Capabilities**, select your Personal Team (Apple ID).
3. Connect your iPhone and press **`Cmd + R`** to build and run.

---

## 🤖 Building the Android App
```bash
cd EchoLink
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
