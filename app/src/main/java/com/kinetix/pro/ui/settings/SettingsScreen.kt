package com.kinetix.pro.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinetix.pro.core.ui.components.ObsidianCard
import com.kinetix.pro.core.ui.components.ObsidianInnerCard
import com.kinetix.pro.core.ui.components.SunsetChip
import com.kinetix.pro.core.ui.components.SunsetOutlinedButton
import com.kinetix.pro.core.ui.components.SunsetSlider
import com.kinetix.pro.core.ui.components.formatFileSize
import com.kinetix.pro.core.ui.theme.ContentHigh
import com.kinetix.pro.core.ui.theme.ContentMedium
import com.kinetix.pro.core.ui.theme.ObsidianCanvas
import com.kinetix.pro.core.ui.theme.ObsidianSurface2
import com.kinetix.pro.core.ui.theme.PrimarySunset
import com.kinetix.pro.core.ui.theme.StatusSuccess
import com.kinetix.pro.core.ui.theme.TertiaryAmber
import com.kinetix.pro.data.preferences.UserPreferencesRepository
import com.kinetix.pro.downloader.ExtractorUpdateChecker
import com.kinetix.pro.downloader.ExtractorVersionInfo
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun SettingsScreen(
    onReplayOnboarding: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isInspection = LocalInspectionMode.current
    val coroutineScope = rememberCoroutineScope()
    val updateChecker = remember { ExtractorUpdateChecker() }
    val prefsRepo = remember { if (isInspection) null else UserPreferencesRepository(context) }

    val concurrentDownloads by (prefsRepo?.concurrentDownloads ?: flowOf(2)).collectAsState(initial = 2)
    val wifiOnly by (prefsRepo?.wifiOnly ?: flowOf(false)).collectAsState(initial = false)
    val autoClipboard by (prefsRepo?.autoClipboardDetect ?: flowOf(true)).collectAsState(initial = true)
    val defaultVideoRes by (prefsRepo?.defaultVideoRes ?: flowOf("1080p")).collectAsState(initial = "1080p")
    val defaultAudioFormat by (prefsRepo?.defaultAudioFormat ?: flowOf("MP3")).collectAsState(initial = "MP3")

    var isCheckingUpdates by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<ExtractorVersionInfo?>(null) }

    fun computeCacheBytes(): Long {
        if (isInspection) return 18_450_000L
        fun dirSize(dir: File?): Long {
            if (dir == null || !dir.exists()) return 0L
            return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        }
        return dirSize(context.cacheDir) + dirSize(context.externalCacheDir)
    }

    var cacheSizeBytes by remember { mutableLongStateOf(computeCacheBytes()) }
    var cacheCleanedMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianCanvas)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Settings",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ContentHigh,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Preferences, extractor engine updates & storage management",
                fontSize = 13.sp,
                color = ContentMedium,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        // Extractor Engine Self-Update Card
        item {
            ObsidianCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "yt-dlp & oEmbed Extractor Engine",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = ContentHigh
                            )
                            Text(
                                text = "Installed version: ${updateInfo?.currentVersion ?: "2026.04.10"}",
                                fontSize = 12.sp,
                                color = ContentMedium
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x3322C55E))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "ONLINE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusSuccess
                            )
                        }
                    }

                    Text(
                        text = "Extractor definitions update dynamically to stay compatible with YouTube, TikTok, Instagram, X, and Reddit without reinstalling the app.",
                        fontSize = 12.sp,
                        color = ContentMedium
                    )

                    SunsetOutlinedButton(
                        text = if (isCheckingUpdates) "Checking for updates..." else "Check for Extractor Update",
                        onClick = {
                            coroutineScope.launch {
                                isCheckingUpdates = true
                                val info = updateChecker.checkForUpdates()
                                prefsRepo?.recordExtractorUpdate()
                                isCheckingUpdates = false
                                updateInfo = info
                            }
                        },
                        icon = Icons.Default.Refresh,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (updateInfo != null && !isCheckingUpdates) {
                        Text(
                            text = if (updateInfo!!.isUpdateAvailable)
                                "New extractor engine available: ${updateInfo!!.latestVersion}"
                            else
                                "Extractor engine is up to date (${updateInfo!!.currentVersion})",
                            fontSize = 12.sp,
                            color = if (updateInfo!!.isUpdateAvailable) TertiaryAmber else StatusSuccess,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Default Quality & Codec Preferences
        item {
            ObsidianCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Default Stream Quality & Codec",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ContentHigh
                    )

                    Text("Preferred Video Resolution", fontSize = 12.sp, color = ContentMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("4K 60fps", "1080p", "720p").forEach { res ->
                            SunsetChip(
                                text = res,
                                selected = defaultVideoRes == res,
                                onClick = {
                                    coroutineScope.launch { prefsRepo?.setDefaultVideoRes(res) }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Text("Preferred Audio Extraction Format", fontSize = 12.sp, color = ContentMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("MP3", "M4A", "WAV").forEach { fmt ->
                            SunsetChip(
                                text = fmt,
                                selected = defaultAudioFormat == fmt,
                                onClick = {
                                    coroutineScope.launch { prefsRepo?.setDefaultAudioFormat(fmt) }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Downloader Preferences (DataStore Backed)
        item {
            ObsidianCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Download Configuration",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ContentHigh
                    )

                    SunsetSlider(
                        value = concurrentDownloads.toFloat(),
                        onValueChange = { limit ->
                            coroutineScope.launch {
                                prefsRepo?.setConcurrentDownloads(limit.toInt())
                            }
                        },
                        valueRange = 1f..6f,
                        label = "Concurrent Downloads Limit",
                        valueFormatter = { "${it.toInt()} parallel tasks" }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Download over Wi-Fi only", fontSize = 14.sp, color = ContentHigh)
                            Text("Prevents cellular mobile data consumption", fontSize = 11.sp, color = ContentMedium)
                        }
                        Switch(
                            checked = wifiOnly,
                            onCheckedChange = { enabled ->
                                coroutineScope.launch { prefsRepo?.setWifiOnly(enabled) }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = PrimarySunset, checkedTrackColor = ObsidianSurface2)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-detect Clipboard Links", fontSize = 14.sp, color = ContentHigh)
                            Text("Suggests fetching media when you copy a video link", fontSize = 11.sp, color = ContentMedium)
                        }
                        Switch(
                            checked = autoClipboard,
                            onCheckedChange = { enabled ->
                                coroutineScope.launch { prefsRepo?.setAutoClipboardDetect(enabled) }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = PrimarySunset, checkedTrackColor = ObsidianSurface2)
                        )
                    }
                }
            }
        }

        // Storage & Cache Management Card
        item {
            ObsidianCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Studio Render Cache & Tour",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = ContentHigh
                        )
                        Text(
                            text = formatFileSize(cacheSizeBytes),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimarySunset
                        )
                    }
                    Text(
                        text = "Temporary Media3 Transformer segments and thumbnail caches can be safely purged without affecting your saved videos in Movies/KinetixPro.",
                        fontSize = 12.sp,
                        color = ContentMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SunsetOutlinedButton(
                            text = "Clear Render Cache",
                            onClick = {
                                val freed = cacheSizeBytes
                                runCatching {
                                    context.cacheDir?.deleteRecursively()
                                    context.externalCacheDir?.deleteRecursively()
                                }
                                cacheSizeBytes = computeCacheBytes()
                                cacheCleanedMessage = "Purged ${formatFileSize(freed)} of temporary cache"
                            },
                            icon = Icons.Default.CleaningServices,
                            modifier = Modifier.weight(1f)
                        )

                        SunsetOutlinedButton(
                            text = "Onboarding Tour",
                            onClick = onReplayOnboarding,
                            icon = Icons.Default.Explore,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (cacheCleanedMessage != null) {
                        Text(
                            text = cacheCleanedMessage!!,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StatusSuccess
                        )
                    }
                }
            }
        }

        // Legal & Responsibility Notice
        item {
            ObsidianInnerCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = TertiaryAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Legal & Responsibility Notice",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ContentHigh
                        )
                    }
                    Text(
                        text = "Kinetix Pro is designed for downloading and editing content that you own, have explicit permission to use, or that is openly licensed under Creative Commons and public domain. DRM-protected and proprietary encrypted content is neither supported nor attempted.",
                        fontSize = 11.sp,
                        color = ContentMedium
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
