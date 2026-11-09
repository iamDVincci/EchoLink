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

class ExportForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val projectName = intent?.getStringExtra(EXTRA_PROJECT_NAME) ?: "Exporting Video"
        val progress = intent?.getIntExtra(EXTRA_PROGRESS, 0) ?: 0
        val clipIndex = intent?.getIntExtra(EXTRA_CLIP_INDEX, 1) ?: 1
        val totalClips = intent?.getIntExtra(EXTRA_TOTAL_CLIPS, 1) ?: 1

        val notification = createNotification(projectName, progress, clipIndex, totalClips)
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

    private fun createNotification(
        projectName: String,
        progress: Int,
        clipIndex: Int,
        totalClips: Int
    ): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val clipStatus = if (totalClips > 1) " (Part $clipIndex of $totalClips)" else ""

        return NotificationCompat.Builder(this, KinetixApplication.CHANNEL_EXPORTS)
            .setContentTitle("Rendering: $projectName$clipStatus")
            .setContentText("$progress% rendered with hardware acceleration")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1002
        const val EXTRA_PROJECT_NAME = "extra_project_name"
        const val EXTRA_PROGRESS = "extra_progress"
        const val EXTRA_CLIP_INDEX = "extra_clip_index"
        const val EXTRA_TOTAL_CLIPS = "extra_total_clips"

        fun start(context: Context, projectName: String, progress: Int = 0, clipIndex: Int = 1, totalClips: Int = 1) {
            val intent = Intent(context, ExportForegroundService::class.java).apply {
                putExtra(EXTRA_PROJECT_NAME, projectName)
                putExtra(EXTRA_PROGRESS, progress)
                putExtra(EXTRA_CLIP_INDEX, clipIndex)
                putExtra(EXTRA_TOTAL_CLIPS, totalClips)
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, ExportForegroundService::class.java)
            context.stopService(intent)
        }
    }
}
