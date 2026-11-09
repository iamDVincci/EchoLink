package com.antigravity.echolink.audio

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import com.antigravity.echolink.model.EarbudStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors real Bluetooth / Wired / Internal audio endpoints via AudioManager
 * and manages hardware voice communication routing.
 */
@Singleton
class EarbudRoutingManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    private val _earbudStatus = MutableStateFlow(EarbudStatus())
    val earbudStatus: StateFlow<EarbudStatus> = _earbudStatus.asStateFlow()

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            refreshConnectedAudioDevices()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            refreshConnectedAudioDevices()
        }
    }

    init {
        audioManager.registerAudioDeviceCallback(deviceCallback, null)
        refreshConnectedAudioDevices()
    }

    fun refreshConnectedAudioDevices() {
        val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val externalHeadset = outputs.firstOrNull { device ->
            val type = device.type
            type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (
                    type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                        type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                        type == AudioDeviceInfo.TYPE_BLE_BROADCAST
                    ))
        }

        val dynamicRoutes = mutableListOf<String>()
        outputs.forEach { dev ->
            val label = when (dev.type) {
                AudioDeviceInfo.TYPE_BLE_HEADSET,
                AudioDeviceInfo.TYPE_BLE_SPEAKER -> "${dev.productName ?: "BLE Audio"} (LE Audio LC3)"
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "${dev.productName ?: "Bluetooth Headset"} (BT HFP SCO)"
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "${dev.productName ?: "Bluetooth Audio"} (BT A2DP)"
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_USB_HEADSET -> "${dev.productName ?: "Wired Headset"} (Direct USB/3.5mm)"
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Phone Speakerphone"
                AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Handset Earpiece"
                else -> null
            }
            if (label != null && label !in dynamicRoutes) {
                dynamicRoutes.add(label)
            }
        }
        if (dynamicRoutes.isEmpty()) {
            dynamicRoutes.add("Phone Speakerphone")
            dynamicRoutes.add("Handset Earpiece")
        }

        if (externalHeadset != null) {
            val productName = externalHeadset.productName?.toString()?.takeIf { it.isNotBlank() }
                ?: "Bluetooth Headset"
            val isBle = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                (externalHeadset.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    externalHeadset.type == AudioDeviceInfo.TYPE_BLE_BROADCAST)
            val isWired = externalHeadset.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                externalHeadset.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                externalHeadset.type == AudioDeviceInfo.TYPE_USB_HEADSET

            val protocol = when {
                isBle -> "LE Audio LC3"
                isWired -> "Direct Wired PCM"
                externalHeadset.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "BT SCO Wideband"
                else -> "Bluetooth Audio"
            }

            _earbudStatus.value = EarbudStatus(
                isConnected = true,
                deviceName = productName,
                batteryPercent = queryBluetoothBatteryPercent(),
                protocolLabel = protocol,
                routeType = if (isBle) "BLE_HEADSET" else if (isWired) "WIRED" else "BLUETOOTH_SCO",
                availableRoutes = dynamicRoutes
            )
        } else {
            _earbudStatus.value = EarbudStatus(
                isConnected = false,
                deviceName = "${Build.MODEL} Handset",
                batteryPercent = null,
                protocolLabel = "Internal Hardware DSP",
                routeType = "SPEAKER",
                availableRoutes = dynamicRoutes
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun queryBluetoothBatteryPercent(): Int? {
        return try {
            val adapter = bluetoothManager?.adapter ?: return null
            val bonded = adapter.bondedDevices ?: return null
            for (device in bonded) {
                val isConnectedMethod = device.javaClass.getMethod("isConnected")
                val connected = isConnectedMethod.invoke(device) as? Boolean ?: false
                if (connected) {
                    val getBatteryMethod = device.javaClass.getMethod("getBatteryLevel")
                    val level = getBatteryMethod.invoke(device) as? Int ?: -1
                    if (level in 0..100) return level
                }
            }
            null
        } catch (_: Throwable) {
            null
        }
    }

    fun engageVoiceCallAudioRoute() {
        try {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val devices = audioManager.availableCommunicationDevices
                val preferred = devices.firstOrNull { dev ->
                    dev.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                        dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                        dev.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                        dev.type == AudioDeviceInfo.TYPE_USB_HEADSET
                } ?: devices.firstOrNull { dev ->
                    dev.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                }
                if (preferred != null) {
                    audioManager.setCommunicationDevice(preferred)
                }
            } else {
                @Suppress("DEPRECATION")
                if (_earbudStatus.value.isConnected) {
                    audioManager.startBluetoothSco()
                    audioManager.isBluetoothScoOn = true
                } else {
                    audioManager.isSpeakerphoneOn = true
                }
            }
        } catch (_: Exception) {
        }
    }

    fun selectAudioRoute(routeLabel: String) {
        val current = _earbudStatus.value
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val commDevices = audioManager.availableCommunicationDevices
                val targetDevice = when {
                    routeLabel.contains("Speaker", ignoreCase = true) ->
                        commDevices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                    routeLabel.contains("Earpiece", ignoreCase = true) ->
                        commDevices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE }
                    else ->
                        commDevices.firstOrNull {
                            it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                                it.type == AudioDeviceInfo.TYPE_USB_HEADSET
                        }
                }
                if (targetDevice != null) {
                    audioManager.setCommunicationDevice(targetDevice)
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager.isSpeakerphoneOn = routeLabel.contains("Speaker", ignoreCase = true)
            }
        } catch (_: Exception) {
        }

        when {
            routeLabel.contains("Speaker", ignoreCase = true) -> {
                _earbudStatus.value = current.copy(
                    deviceName = "${Build.MODEL} Speakerphone",
                    protocolLabel = "Internal Speaker DSP",
                    routeType = "SPEAKER"
                )
            }
            routeLabel.contains("Earpiece", ignoreCase = true) -> {
                _earbudStatus.value = current.copy(
                    deviceName = "${Build.MODEL} Earpiece",
                    protocolLabel = "Handset Earpiece",
                    routeType = "EARPIECE"
                )
            }
            else -> {
                refreshConnectedAudioDevices()
            }
        }
    }
}
