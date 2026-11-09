package com.antigravity.echolink

import com.antigravity.echolink.crypto.EchoLinkCrypto
import com.antigravity.echolink.network.EchoLinkVoicePacket
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EchoLinkPacketTest {

    @Test
    fun `encode and decode UDP 51111 voice packet preserves 0x4147 AG header and ChaCha20-Poly1305 payload`() {
        val crypto = EchoLinkCrypto()
        val samplePayload = ByteArray(48) { (it * 3).toByte() }
        val nonce = crypto.generateNonce96Bit(sequenceNumber = 42, timestampTicks = 96000L)

        val original = EchoLinkVoicePacket(
            sequenceNumber = 42,
            timestampTicks48k = 96000L,
            isVadActive = true,
            isPttActive = false,
            peerId = 2,
            nonce12Bytes = nonce,
            audioPayload = samplePayload
        )

        val datagram = EchoLinkVoicePacket.encodeDatagram(original, crypto)
        // Verify Bytes 0-1 Magic Header == 0x4147 ("AG")
        assertEquals(0x41.toByte(), datagram[0])
        assertEquals(0x47.toByte(), datagram[1])

        val decoded = EchoLinkVoicePacket.decodeDatagram(datagram, datagram.size, crypto)
        assertNotNull(decoded)
        assertEquals(42, decoded!!.sequenceNumber)
        assertEquals(96000L, decoded.timestampTicks48k)
        assertTrue(decoded.isVadActive)
        assertEquals(false, decoded.isPttActive)
        assertEquals(2, decoded.peerId)
        assertArrayEquals(samplePayload, decoded.audioPayload)
    }
}
