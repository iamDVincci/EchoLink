package com.kinetix.pro.downloader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class ExtractorVersionInfo(
    val currentVersion: String,
    val latestVersion: String,
    val isUpdateAvailable: Boolean,
    val releaseNotes: String? = null,
    val downloadUrl: String? = null
)

class ExtractorUpdateChecker(
    private val okHttpClient: OkHttpClient = OkHttpClient()
) {
    private var installedVersion: String = "2026.04.10"

    suspend fun checkForUpdates(): ExtractorVersionInfo = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/yt-dlp/yt-dlp/releases/latest")
                .header("User-Agent", "KinetixPro-Android-Client")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonStr = response.body?.string() ?: ""
                val json = JSONObject(jsonStr)
                val tagName = json.optString("tag_name", installedVersion)
                val body = json.optString("body", "")

                val isNewer = tagName != installedVersion

                ExtractorVersionInfo(
                    currentVersion = installedVersion,
                    latestVersion = tagName,
                    isUpdateAvailable = isNewer,
                    releaseNotes = body.take(300),
                    downloadUrl = "https://github.com/yt-dlp/yt-dlp/releases/tag/$tagName"
                )
            } else {
                ExtractorVersionInfo(
                    currentVersion = installedVersion,
                    latestVersion = installedVersion,
                    isUpdateAvailable = false
                )
            }
        } catch (e: Exception) {
            ExtractorVersionInfo(
                currentVersion = installedVersion,
                latestVersion = installedVersion,
                isUpdateAvailable = false,
                releaseNotes = "Offline or update service currently unavailable: ${e.message}"
            )
        }
    }

    fun applyUpdatedVersion(newVersionTag: String) {
        installedVersion = newVersionTag
    }
}
