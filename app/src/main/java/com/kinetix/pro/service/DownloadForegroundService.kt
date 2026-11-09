package com.kinetix.pro.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.kinetix.pro.KinetixApplication
import com.kinetix.pro.MainActivity
import com.kinetix.pro.R

class DownloadForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Downloading media"
        val progress = intent?.getIntExtra(EXTRA_PROGRESS, 0) ?: 0

        val notification = createNotification(title, progress)
        runCatching {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
        }

        return START_NOT_STICKY
    }

    private fun createNotification(title: String, progress: Int): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, KinetixApplication.CHANNEL_DOWNLOADS)
            .setContentTitle(title)
            .setContentText(if (progress > 0) "$progress% completed" else "Connecting...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_PROGRESS = "extra_progress"

        fun start(context: Context, title: String, progress: Int = 0) {
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_PROGRESS, progress)
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, DownloadForegroundService::class.java)
            context.stopService(intent)
        }
    }
}
