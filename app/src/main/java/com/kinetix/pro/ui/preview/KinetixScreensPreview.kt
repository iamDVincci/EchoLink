package com.kinetix.pro.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinetix.pro.core.ui.components.ObsidianCard
import com.kinetix.pro.core.ui.components.ObsidianInnerCard
import com.kinetix.pro.core.ui.components.ObsidianUrlInput
import com.kinetix.pro.core.ui.components.PlatformBadge
import com.kinetix.pro.core.ui.components.RecommendedPill
import com.kinetix.pro.core.ui.components.SunsetChip
import com.kinetix.pro.core.ui.components.SunsetIconButton
import com.kinetix.pro.core.ui.components.SunsetOutlinedButton
import com.kinetix.pro.core.ui.components.SunsetPrimaryButton
import com.kinetix.pro.core.ui.components.SunsetSlider
import com.kinetix.pro.core.ui.theme.ContentHigh
import com.kinetix.pro.core.ui.theme.ContentMedium
import com.kinetix.pro.core.ui.theme.ContentMuted
import com.kinetix.pro.core.ui.theme.KinetixTheme
import com.kinetix.pro.core.ui.theme.ObsidianCanvas
import com.kinetix.pro.core.ui.theme.ObsidianCardStroke
import com.kinetix.pro.core.ui.theme.ObsidianSheetStroke
import com.kinetix.pro.core.ui.theme.ObsidianSurface1
import com.kinetix.pro.core.ui.theme.ObsidianSurface2
import com.kinetix.pro.core.ui.theme.PrimarySunset
import com.kinetix.pro.core.ui.theme.StatusSuccess
import com.kinetix.pro.core.ui.theme.SunsetGradient
import com.kinetix.pro.core.ui.theme.SunsetTint15
import com.kinetix.pro.core.ui.theme.TertiaryAmber
import com.kinetix.pro.ui.settings.SettingsScreen
import com.kinetix.pro.ui.templates.TemplatesScreen

@Preview(
    name = "1. Downloader Hub (Obsidian Sunset)",
    showBackground = true,
    backgroundColor = 0xFF0D0E11,
    widthDp = 412,
    heightDp = 892
)
@Composable
fun DownloaderHubPreview() {
    KinetixTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianCanvas)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Brand Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(PrimarySunset)
                        )
                        Text(
                            text = "KINETIX PRO",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ContentHigh,
                            letterSpacing = (-0.5).sp
                        )
                    }
                    Text(
                        text = "Universal Media Grabber & CapCut Studio",
                        fontSize = 12.sp,
                        color = ContentMedium
                    )
                }
                SunsetOutlinedButton(
                    text = "Import Video",
                    onClick = {},
                    icon = Icons.Default.VideoLibrary
                )
            }

            // URL Hero Input Card
            ObsidianCard(modifier = Modifier.fillMaxWidth(), isHighlighted = true) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Paste Video or Audio Link",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = ContentHigh
                        )
                        PlatformBadge(platformName = "YOUTUBE")
                    }

                    ObsidianUrlInput(
                        value = "https://www.youtube.com/watch?v=kinetix_4k_hdr",
                        onValueChange = {},
                        onPasteClick = {},
                        onClearClick = {}
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("YouTube", "TikTok", "Instagram", "X / Twitter", "Reddit").forEachIndexed { idx, name ->
                            SunsetChip(
                                text = name,
                                selected = idx == 0,
                                onClick = {}
                            )
                        }
                    }

                    SunsetPrimaryButton(
                        text = "Fetch Formats & Stream",
                        onClick = {},
                        icon = Icons.Default.Download
                    )
                }
            }

            // Active Download Progress Card
            Text(
                text = "Active & Recent Transfers",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ContentHigh
            )

            ObsidianCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlatformBadge(platformName = "YOUTUBE")
                            RecommendedPill()
                        }
                        Text(
                            text = "68% • 14.8 MB/s",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimarySunset
                        )
                    }
                    Text(
                        text = "Cyberpunk Neon Tokyo Night Walk — 4K 60fps Dolby Vision Master",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ContentHigh
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(ObsidianSurface2)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.68f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(SunsetGradient)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("89.2 MB / 131.4 MB (8 HTTP Threads)", fontSize = 11.sp, color = ContentMedium)
                        Text("ETA 00:03", fontSize = 11.sp, color = TertiaryAmber, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Completed Item Ready for Studio
            ObsidianCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlatformBadge(platformName = "TIKTOK")
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = StatusSuccess,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "SAVED TO GALLERY",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusSuccess
                                )
                            }
                        }
                        Text("1080p MP4 • 24.6 MB", fontSize = 11.sp, color = ContentMedium)
                    }
                    Text(
                        text = "Cinematic Transitions & Speed Ramp Breakdown (No Watermark)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ContentHigh
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SunsetPrimaryButton(
                            text = "Edit in Studio",
                            onClick = {},
                            icon = Icons.Default.Edit,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Preview(
    name = "2. Studio Video Editor (Obsidian Sunset)",
    showBackground = true,
    backgroundColor = 0xFF0D0E11,
    widthDp = 412,
    heightDp = 892
)
@Composable
fun StudioEditorPreview() {
    KinetixTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianCanvas)
        ) {
            // Studio Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianSurface1)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SunsetIconButton(icon = Icons.AutoMirrored.Filled.ArrowBack, onClick = {}, size = 36)
                    SunsetIconButton(icon = Icons.Default.FolderOpen, onClick = {}, tint = TertiaryAmber, size = 36)
                    Column {
                        Text(
                            text = "Tokyo_Night_Master.mp4",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ContentHigh
                        )
                        Text(
                            text = "9:16 • 3 clips • Teal & Orange",
                            fontSize = 11.sp,
                            color = PrimarySunset
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SunsetIconButton(icon = Icons.AutoMirrored.Filled.Undo, onClick = {}, size = 34)
                    SunsetIconButton(icon = Icons.AutoMirrored.Filled.Redo, onClick = {}, size = 34)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(SunsetGradient)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Export",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ContentHigh
                        )
                    }
                }
            }

            // Video Preview Surface
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(310.dp)
                    .background(Color(0xFF07080A))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(175.dp)
                        .height(286.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF1F2430), Color(0xFF2E1B16), Color(0xFF0D0E11))
                            )
                        )
                        .border(1.5.dp, ObsidianSheetStroke, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xAA0D0E11))
                                .border(1.5.dp, PrimarySunset, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = PrimarySunset,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xCC0D0E11))
                                .border(1.dp, PrimarySunset, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "KINETIX STUDIO 4K",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ContentHigh
                            )
                        }
                    }

                    // HUD Badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xAA0D0E11))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text("GPU LUT • 1.25x", fontSize = 9.sp, color = TertiaryAmber, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Transport & Timecode Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianSurface1)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("00:14.2", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimarySunset)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SunsetChip(text = "Split into 3 Parts", selected = true, onClick = {})
                    SunsetChip(text = "9:16 Blur BG", selected = false, onClick = {})
                }
                Text("00:45.0", fontSize = 12.sp, color = ContentMedium)
            }

            // Multi-Track Timeline Strip
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianCanvas)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Part 01 (15.0s)" to true, "Part 02 (15.0s)" to false, "Part 03 (15.0s)" to false).forEach { (label, active) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) SunsetTint15 else ObsidianSurface2)
                                .border(
                                    width = if (active) 2.dp else 1.dp,
                                    color = if (active) PrimarySunset else ObsidianCardStroke,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Column {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) PrimarySunset else ContentHigh
                                )
                                Text(
                                    text = "1080p • 60fps",
                                    fontSize = 9.sp,
                                    color = ContentMedium
                                )
                            }
                        }
                    }
                }

                // Audio + FX Track Pill
                ObsidianInnerCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = TertiaryAmber, modifier = Modifier.size(16.dp))
                            Text("Voice: Deep Studio (-2.5 st) • BGM Ducking ON", fontSize = 11.sp, color = ContentHigh)
                        }
                        Text("100%", fontSize = 11.sp, color = PrimarySunset, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Active Tool Panel (Color & LUT Grading)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(ObsidianSurface1)
                    .border(1.dp, ObsidianSheetStroke, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SunsetSlider(
                    value = 0.85f,
                    onValueChange = {},
                    label = "LUT Filter Intensity (Teal & Orange)",
                    valueFormatter = { "85%" }
                )

                // Bottom Tool Dock
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(
                        Icons.Default.ContentCut to "Split",
                        Icons.Default.FilterVintage to "Filters",
                        Icons.Default.Tune to "Adjust",
                        Icons.Default.AspectRatio to "Ratio",
                        Icons.Default.TextFields to "Text",
                        Icons.Default.GraphicEq to "Voice",
                        Icons.Default.Audiotrack to "Audio",
                        Icons.Default.Speed to "Speed"
                    ).forEachIndexed { index, (icon, label) ->
                        val selected = index == 1
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) SunsetTint15 else ObsidianSurface2)
                                    .border(
                                        1.dp,
                                        if (selected) PrimarySunset else ObsidianCardStroke,
                                        RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (selected) PrimarySunset else ContentHigh,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) PrimarySunset else ContentMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(
    name = "3. Brand Templates Screen",
    showBackground = true,
    backgroundColor = 0xFF0D0E11,
    widthDp = 412,
    heightDp = 892
)
@Composable
fun TemplatesScreenPreview() {
    KinetixTheme {
        TemplatesScreen(onApplyTemplate = {})
    }
}

@Preview(
    name = "4. Settings & Extractor Engine Screen",
    showBackground = true,
    backgroundColor = 0xFF0D0E11,
    widthDp = 412,
    heightDp = 892
)
@Composable
fun SettingsScreenPreview() {
    KinetixTheme {
        SettingsScreen()
    }
}
