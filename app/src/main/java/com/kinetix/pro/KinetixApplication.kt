package com.kinetix.pro

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.kinetix.pro.data.local.KinetixDatabase
import com.kinetix.pro.data.preferences.UserPreferencesRepository
import com.kinetix.pro.downloader.YtDlpNativeEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KinetixApplication : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: KinetixDatabase by lazy {
        KinetixDatabase.getInstance(applicationContext)
    }

    lateinit var userPreferences: UserPreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        userPreferences = UserPreferencesRepository(applicationContext)
        runCatching { createNotificationChannels() }

        // Initialize embedded yt-dlp + FFmpeg native runtime and auto-update yt-dlp in background
        appScope.launch {
            YtDlpNativeEngine.ensureInitialized(applicationContext)
            YtDlpNativeEngine.updateYtDlpIfPossible(applicationContext)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val downloadChannel = NotificationChannel(
                CHANNEL_DOWNLOADS,
                "Media Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live download progress for video, audio, and media"
            }

            val exportChannel = NotificationChannel(
                CHANNEL_EXPORTS,
                "Video Exports & Rendering",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress while rendering and exporting edited videos"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(downloadChannel)
            manager?.createNotificationChannel(exportChannel)
        }
    }

    companion object {
        const val CHANNEL_DOWNLOADS = "kinetix_downloads_channel"
        const val CHANNEL_EXPORTS = "kinetix_exports_channel"

        lateinit var instance: KinetixApplication
            private set
    }
}
