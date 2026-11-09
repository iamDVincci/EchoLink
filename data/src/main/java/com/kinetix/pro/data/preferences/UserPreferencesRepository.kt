package com.kinetix.pro.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kinetix_settings")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val CONCURRENT_DOWNLOADS = intPreferencesKey("concurrent_downloads")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val DEFAULT_VIDEO_RES = stringPreferencesKey("default_video_res")
        val DEFAULT_AUDIO_FORMAT = stringPreferencesKey("default_audio_format")
        val AUTO_CLIPBOARD_DETECT = booleanPreferencesKey("auto_clipboard_detect")
        val COMPLETED_ONBOARDING = booleanPreferencesKey("completed_onboarding")
        val LAST_EXTRACTOR_UPDATE = longPreferencesKey("last_extractor_update")
        val YT_DLP_CHANNEL = stringPreferencesKey("yt_dlp_channel")
    }

    val concurrentDownloads: Flow<Int> = context.dataStore.data.map {
        it[PreferencesKeys.CONCURRENT_DOWNLOADS] ?: 2
    }

    val wifiOnly: Flow<Boolean> = context.dataStore.data.map {
        it[PreferencesKeys.WIFI_ONLY] ?: false
    }

    val defaultVideoRes: Flow<String> = context.dataStore.data.map {
        it[PreferencesKeys.DEFAULT_VIDEO_RES] ?: "1080p"
    }

    val defaultAudioFormat: Flow<String> = context.dataStore.data.map {
        it[PreferencesKeys.DEFAULT_AUDIO_FORMAT] ?: "MP3"
    }

    val autoClipboardDetect: Flow<Boolean> = context.dataStore.data.map {
        it[PreferencesKeys.AUTO_CLIPBOARD_DETECT] ?: true
    }

    val hasCompletedOnboarding: Flow<Boolean> = context.dataStore.data.map {
        it[PreferencesKeys.COMPLETED_ONBOARDING] ?: false
    }

    val lastExtractorUpdate: Flow<Long> = context.dataStore.data.map {
        it[PreferencesKeys.LAST_EXTRACTOR_UPDATE] ?: 0L
    }

    suspend fun setConcurrentDownloads(limit: Int) {
        context.dataStore.edit { it[PreferencesKeys.CONCURRENT_DOWNLOADS] = limit.coerceIn(1, 6) }
    }

    suspend fun setWifiOnly(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.WIFI_ONLY] = enabled }
    }

    suspend fun setDefaultVideoRes(resolution: String) {
        context.dataStore.edit { it[PreferencesKeys.DEFAULT_VIDEO_RES] = resolution }
    }

    suspend fun setDefaultAudioFormat(format: String) {
        context.dataStore.edit { it[PreferencesKeys.DEFAULT_AUDIO_FORMAT] = format }
    }

    suspend fun setAutoClipboardDetect(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AUTO_CLIPBOARD_DETECT] = enabled }
    }

    suspend fun setCompletedOnboarding(completed: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.COMPLETED_ONBOARDING] = completed }
    }

    suspend fun recordExtractorUpdate(epochMs: Long = System.currentTimeMillis()) {
        context.dataStore.edit { it[PreferencesKeys.LAST_EXTRACTOR_UPDATE] = epochMs }
    }
}
