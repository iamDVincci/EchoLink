package com.antigravity.echolink.network

import com.antigravity.echolink.crypto.EchoLinkCrypto
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Exact binary implementation of PRD Section 6: Voice Data Channel (UDP Port 51111).
 *
 * Byte Offset | Field Name         | Description / Data Type
 * Bytes 0–1   | Magic Header       | 0x4147 ("AG" signature)
 * Bytes 2–3   | Sequence Number    | uint16 packet index
 * Bytes 4–7   | Timestamp          | uint32 (48 kHz clock ticks)
 * Byte 8      | Flags & Peer ID    | Bit 0: VAD, Bit 1: PTT, Bits 4–7: Peer ID
 * Bytes 9–20  | Nonce              | 96-bit Initialization Vector (12 bytes)
 * Bytes 21–N  | Encrypted Payload  | Opus frame + 16-byte Poly1305 Auth Tag
 */
data class EchoLinkVoicePacket(
    val sequenceNumber: Int,
    val timestampTicks48k: Long,
    val isVadActive: Boolean,
    val isPttActive: Boolean,
    val peerId: Int,
    val nonce12Bytes: ByteArray,
    val audioPayload: ByteArray
) {
    val flagsByte: Int
        get() {
            var b = 0
            if (isVadActive) b = b or 0x01
            if (isPttActive) b = b or 0x02
            b = b or ((peerId and 0x0F) shl 4)
            return b and 0xFF
        }

    companion object {
        const val MAGIC_HEADER: Short = 0x4147 // "AG"
        const val HEADER_BYTES = 21

        fun encodeDatagram(
            packet: EchoLinkVoicePacket,
            crypto: EchoLinkCrypto
        ): ByteArray {
            val aadHeader = ByteArray(9)
            val headerBuf = ByteBuffer.wrap(aadHeader).order(ByteOrder.BIG_ENDIAN)
            headerBuf.putShort(MAGIC_HEADER)
            headerBuf.putShort((packet.sequenceNumber and 0xFFFF).toShort())
            headerBuf.putInt((packet.timestampTicks48k and 0xFFFFFFFFL).toInt())
            headerBuf.put(packet.flagsByte.toByte())

            val encryptedWithTag = crypto.encryptPayload(
                nonce12Bytes = packet.nonce12Bytes,
                aadHeader9Bytes = aadHeader,
                plaintext = packet.audioPayload
            )

            val fullDatagram = ByteArray(HEADER_BYTES + encryptedWithTag.size)
            System.arraycopy(aadHeader, 0, fullDatagram, 0, 9)
            System.arraycopy(packet.nonce12Bytes, 0, fullDatagram, 9, 12)
            System.arraycopy(encryptedWithTag, 0, fullDatagram, 21, encryptedWithTag.size)
            return fullDatagram
        }

        fun decodeDatagram(
            datagram: ByteArray,
            length: Int,
            crypto: EchoLinkCrypto
        ): EchoLinkVoicePacket? {
            if (length <= HEADER_BYTES + 16) return null
            val buf = ByteBuffer.wrap(datagram, 0, length).order(ByteOrder.BIG_ENDIAN)
            val magic = buf.short
            if (magic != MAGIC_HEADER) return null

            val seq = buf.short.toInt() and 0xFFFF
            val ts = buf.int.toLong() and 0xFFFFFFFFL
            val flags = buf.get().toInt() and 0xFF

            val isVad = (flags and 0x01) != 0
            val isPtt = (flags and 0x02) != 0
            val peerId = (flags ushr 4) and 0x0F

            val nonce = ByteArray(12)
            buf.get(nonce)

            val encryptedLen = length - HEADER_BYTES
            val encryptedPayload = ByteArray(encryptedLen)
            buf.get(encryptedPayload)

            val aadHeader = datagram.copyOfRange(0, 9)
            val decryptedAudio = crypto.decryptPayload(
                nonce12Bytes = nonce,
                aadHeader9Bytes = aadHeader,
                ciphertextWithTag = encryptedPayload
            ) ?: return null

            return EchoLinkVoicePacket(
                sequenceNumber = seq,
                timestampTicks48k = ts,
                isVadActive = isVad,
                isPttActive = isPtt,
                peerId = peerId,
                nonce12Bytes = nonce,
                audioPayload = decryptedAudio
            )
        }
    }
}
