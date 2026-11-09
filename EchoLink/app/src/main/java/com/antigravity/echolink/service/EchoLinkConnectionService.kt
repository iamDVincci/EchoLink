package com.antigravity.echolink.service

import android.net.Uri
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager

/**
 * Android Telecom ConnectionService integration (PRD Section 3.3).
 * Registers EchoLink P2P sessions as system-level voice calls so connected wireless earbuds
 * (AirPods, Pixel Buds Pro, Galaxy Buds) automatically route SCO/BLE audio and deliver
 * single-tap/double-pinch stem hardware events.
 */
class EchoLinkConnectionService : ConnectionService() {

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        return createEchoLinkConnection(request?.address)
    }

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        return createEchoLinkConnection(request?.address)
    }

    private fun createEchoLinkConnection(address: Uri?): Connection {
        return object : Connection() {
            init {
                setAddress(address ?: Uri.parse("sip:echolink@antigravity.local"), TelecomManager.PRESENTATION_ALLOWED)
                setCallerDisplayName("EchoLink P2P Intercom", TelecomManager.PRESENTATION_ALLOWED)
                connectionProperties = PROPERTY_SELF_MANAGED
                connectionCapabilities = CAPABILITY_MUTE or CAPABILITY_HOLD or CAPABILITY_SUPPORT_HOLD
                audioModeIsVoip = true
                setActive()
            }

            override fun onAnswer() {
                setActive()
            }

            override fun onReject() {
                setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
                destroy()
            }

            override fun onDisconnect() {
                setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
                destroy()
            }

            override fun onHold() {
                setOnHold()
            }

            override fun onUnhold() {
                setActive()
            }
        }
    }
}
