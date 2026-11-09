import Foundation

/// Exact binary implementation of PRD Section 6: Voice Data Channel (UDP Port 51111).
/// Byte-for-byte wire compatible with Android `EchoLinkPacket.kt`:
/// - Bytes 0–1:   Magic Header `0x4147` ("AG")
/// - Bytes 2–3:   Sequence Number (`UInt16` big-endian)
/// - Bytes 4–7:   Timestamp (`UInt32` big-endian, 48 kHz clock ticks)
/// - Byte 8:      Flags & Peer ID (Bit 0: VAD, Bit 1: PTT, Bits 4–7: Peer ID)
/// - Bytes 9–20:  96-bit Nonce (12 bytes)
/// - Bytes 21–N:  Encrypted Payload + 16-byte Poly1305 Auth Tag
struct EchoLinkVoicePacket {
    static let magicHeader: UInt16 = 0x4147 // "AG"
    static let headerBytes: Int = 21

    let sequenceNumber: Int
    let timestampTicks48k: Int64
    let isVadActive: Bool
    let isPttActive: Bool
    let peerId: Int
    let nonce12Bytes: Data
    let audioPayload: Data

    var flagsByte: UInt8 {
        var b: UInt8 = 0
        if isVadActive { b |= 0x01 }
        if isPttActive { b |= 0x02 }
        b |= UInt8((peerId & 0x0F) << 4)
        return b
    }

    static func encodeDatagram(packet: EchoLinkVoicePacket, crypto: EchoLinkCrypto) -> Data {
        var aadHeader = Data(count: 9)
        let seq = UInt16(packet.sequenceNumber & 0xFFFF)
        let ts = UInt32(truncatingIfNeeded: packet.timestampTicks48k)

        aadHeader[0] = UInt8((magicHeader >> 8) & 0xFF)
        aadHeader[1] = UInt8(magicHeader & 0xFF)
        aadHeader[2] = UInt8((seq >> 8) & 0xFF)
        aadHeader[3] = UInt8(seq & 0xFF)
        aadHeader[4] = UInt8((ts >> 24) & 0xFF)
        aadHeader[5] = UInt8((ts >> 16) & 0xFF)
        aadHeader[6] = UInt8((ts >> 8) & 0xFF)
        aadHeader[7] = UInt8(ts & 0xFF)
        aadHeader[8] = packet.flagsByte

        let encryptedWithTag = crypto.encryptPayload(
            nonce12Bytes: packet.nonce12Bytes,
            aadHeader9Bytes: aadHeader,
            plaintext: packet.audioPayload
        )

        var fullDatagram = Data(capacity: headerBytes + encryptedWithTag.count)
        fullDatagram.append(aadHeader)
        fullDatagram.append(packet.nonce12Bytes.prefix(12))
        fullDatagram.append(encryptedWithTag)
        return fullDatagram
    }

    static func decodeDatagram(datagram: Data, crypto: EchoLinkCrypto) -> EchoLinkVoicePacket? {
        guard datagram.count > headerBytes + 16 else { return nil }
        let bytes = [UInt8](datagram)

        let magic = (UInt16(bytes[0]) << 8) | UInt16(bytes[1])
        guard magic == magicHeader else { return nil }

        let seq = Int((UInt16(bytes[2]) << 8) | UInt16(bytes[3]))
        let ts = Int64(
            (UInt32(bytes[4]) << 24) |
            (UInt32(bytes[5]) << 16) |
            (UInt32(bytes[6]) << 8) |
            UInt32(bytes[7])
        )
        let flags = bytes[8]
        let isVad = (flags & 0x01) != 0
        let isPtt = (flags & 0x02) != 0
        let peerId = Int((flags >> 4) & 0x0F)

        let aadHeader = datagram.subdata(in: 0..<9)
        let nonce = datagram.subdata(in: 9..<21)
        let encryptedPayload = datagram.subdata(in: 21..<datagram.count)

        guard let decryptedAudio = crypto.decryptPayload(
            nonce12Bytes: nonce,
            aadHeader9Bytes: aadHeader,
            ciphertextWithTag: encryptedPayload
        ) else {
            return nil
        }

        return EchoLinkVoicePacket(
            sequenceNumber: seq,
            timestampTicks48k: ts,
            isVadActive: isVad,
            isPttActive: isPtt,
            peerId: peerId,
            nonce12Bytes: nonce,
            audioPayload: decryptedAudio
        )
    }
}
