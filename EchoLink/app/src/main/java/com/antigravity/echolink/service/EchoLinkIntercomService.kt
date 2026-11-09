package com.antigravity.echolink.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.antigravity.echolink.MainActivity
import com.antigravity.echolink.R

/**
 * Foreground Service & Screen 5: Lock Screen / Pocket Mode Notification Widget (PRD Section 5 & Part 2 Screen 5).
 *
 * - Holds `PowerManager.PARTIAL_WAKE_LOCK` so screen-off pocket operation uses < 6% battery/hr.
 * - Displays compact Android 15 ongoing call notification banner:
 *   "EchoLink Active • 1 Peer Connected (18ms)" with two action buttons: `Mute Mic` and `Disconnect`.
 */
class EchoLinkIntercomService : Service() {

    companion object {
        const val CHANNEL_ID = "echolink_tactical_intercom_channel"
        const val NOTIFICATION_ID = 51111

        const val ACTION_START = "com.antigravity.echolink.action.START"
        const val ACTION_TOGGLE_MUTE = "com.antigravity.echolink.action.TOGGLE_MUTE"
        const val ACTION_DISCONNECT = "com.antigravity.echolink.action.DISCONNECT"

        const val EXTRA_PEER_COUNT = "extra_peer_count"
        const val EXTRA_PING_MS = "extra_ping_ms"
        const val EXTRA_IS_MUTED = "extra_is_muted"

        var onMuteToggleCallback: (() -> Unit)? = null
        var onDisconnectCallback: (() -> Unit)? = null
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var currentPeerCount = 1
    private var currentPingMs = 18
    private var isMuted = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE_MUTE -> {
                isMuted = !isMuted
                onMuteToggleCallback?.invoke()
                updateNotification()
            }
            ACTION_DISCONNECT -> {
                onDisconnectCallback?.invoke()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                currentPeerCount = intent?.getIntExtra(EXTRA_PEER_COUNT, 1) ?: 1
                currentPingMs = intent?.getIntExtra(EXTRA_PING_MS, 18) ?: 18
                isMuted = intent?.getBooleanExtra(EXTRA_IS_MUTED, false) ?: false
                val notification = buildPocketNotification()
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val fgType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                        } else {
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                        }
                        startForeground(NOTIFICATION_ID, notification, fgType)
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                } catch (_: Exception) {
                    // Graceful fallback if foreground mic permission hasn't been granted yet
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        releaseWakeLock()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun updateNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildPocketNotification())
    }

    private fun buildPocketNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPending = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val muteIntent = Intent(this, EchoLinkIntercomService::class.java).apply {
            action = ACTION_TOGGLE_MUTE
        }
        val mutePending = PendingIntent.getService(
            this,
            1,
            muteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val disconnectIntent = Intent(this, EchoLinkIntercomService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val disconnectPending = PendingIntent.getService(
            this,
            2,
            disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val peerWord = if (currentPeerCount == 1) "1 Peer" else "$currentPeerCount Peers"
        val micStateText = if (isMuted) "MIC MUTED • E2EE ChaCha20" else "LIVE MIC • Opus 24kbps • E2EE ChaCha20"
        val muteButtonLabel = if (isMuted) "Unmute Mic" else "Mute Mic"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_speakerphone)
            .setContentTitle("EchoLink Active • $peerWord Connected (${currentPingMs}ms)")
            .setContentText(micStateText)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(0xFF00F0FF.toInt())
            .setContentIntent(openPending)
            .addAction(android.R.drawable.ic_btn_speak_now, muteButtonLabel, mutePending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Disconnect", disconnectPending)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EchoLink::PocketIntercomWakeLock").apply {
                setReferenceCounted(false)
                acquire(4 * 60 * 60 * 1000L)
            }
        } catch (_: Exception) {
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (_: Exception) {
        }
    }
}
