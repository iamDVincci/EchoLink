package com.kinetix.pro.ui.library

import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.kinetix.pro.core.ui.components.ObsidianCard
import com.kinetix.pro.core.ui.components.ObsidianSearchBar
import com.kinetix.pro.core.ui.components.PlatformBadge
import com.kinetix.pro.core.ui.components.SunsetChip
import com.kinetix.pro.core.ui.components.SunsetIconButton
import com.kinetix.pro.core.ui.components.SunsetOutlinedButton
import com.kinetix.pro.core.ui.components.SunsetPrimaryButton
import com.kinetix.pro.core.ui.components.formatDuration
import com.kinetix.pro.core.ui.components.formatFileSize
import com.kinetix.pro.core.ui.theme.ContentHigh
import com.kinetix.pro.core.ui.theme.ContentMedium
import com.kinetix.pro.core.ui.theme.ObsidianCanvas
import com.kinetix.pro.core.ui.theme.ObsidianSurface1
import com.kinetix.pro.core.ui.theme.ObsidianSurface2
import com.kinetix.pro.core.ui.theme.PrimarySunset
import com.kinetix.pro.data.repository.KinetixRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LibraryMediaItem(
    val id: String,
    val title: String,
    val type: String, // VIDEO, AUDIO, IMAGE
    val durationSeconds: Long,
    val sizeBytes: Long,
    val thumbnailBitmap: Bitmap? = null,
    val videoUri: String,
    val filePath: String,
    val platform: String,
    val dateAdded: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onNavigateToEditor: (projectId: String, videoUri: String?, title: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isInspection = LocalInspectionMode.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { if (isInspection) null else KinetixRepository(context) }
    val savedProjects by (repository?.observeAllProjects() ?: flowOf(emptyList())).collectAsState(initial = emptyList())

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterTab by remember { mutableIntStateOf(0) }
    var localDiskItems by remember { mutableStateOf<List<LibraryMediaItem>>(emptyList()) }
    var previewingItem by remember { mutableStateOf<LibraryMediaItem?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                val resolvedUri = try {
                    val dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
                    if (!dir.exists()) dir.mkdirs()
                    val importedFile = File(dir, "kinetix_lib_import_${System.currentTimeMillis()}.mp4")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        importedFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (importedFile.exists() && importedFile.length() > 0L) {
                        Uri.fromFile(importedFile).toString()
                    } else {
                        uri.toString()
                    }
                } catch (_: Exception) {
                    uri.toString()
                }
                withContext(Dispatchers.Main) {
                    onNavigateToEditor("gallery_import_${System.currentTimeMillis()}", resolvedUri, "Gallery Video")
                }
            }
        }
    }

    // Scan real downloaded/exported files on device storage & extract real thumbnails + durations
    LaunchedEffect(refreshTrigger) {
        val scanned = withContext(Dispatchers.IO) {
            val dirs = listOfNotNull(
                context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "Kinetix Pro"),
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Kinetix Pro")
            )
            val dateFmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
            val allFiles = dirs
                .flatMap { d -> d.listFiles()?.toList().orEmpty() }
                .filter { it.isFile && it.length() > 0L && !it.name.startsWith("tmp_") && it.name != "kinetix_studio_showcase.mp4" }
                .distinctBy { it.name }
                .sortedByDescending { it.lastModified() }

            allFiles.mapIndexed { idx, file ->
                val ext = file.extension.lowercase()
                val isAudio = ext in listOf("mp3", "m4a", "wav", "flac", "aac")
                val isImage = ext in listOf("jpg", "jpeg", "png", "webp")
                val mediaType = when {
                    isAudio -> "AUDIO"
                    isImage -> "IMAGE"
                    else -> "VIDEO"
                }

                var durationSec = 0L
                var thumbBitmap: Bitmap? = null
                if (!isImage) {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(file.absolutePath)
                        val durMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                        durationSec = (durMs / 1000L).coerceAtLeast(1L)
                        if (mediaType == "VIDEO") {
                            thumbBitmap = retriever.getScaledFrameAtTime(
                                500_000L,
                                MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                                320,
                                240
                            ) ?: retriever.getFrameAtTime(0L)
                        }
                    } catch (_: Exception) {
                    } finally {
                        runCatching { retriever.release() }
                    }
                }

                LibraryMediaItem(
                    id = "disk_${file.name}_$idx",
                    title = file.nameWithoutExtension.replace("_", " "),
                    type = mediaType,
                    durationSeconds = durationSec,
                    sizeBytes = file.length(),
                    thumbnailBitmap = thumbBitmap,
                    videoUri = Uri.fromFile(file).toString(),
                    filePath = file.absolutePath,
                    platform = if (file.name.contains("part_", ignoreCase = true)) "Studio Export" else "Saved Media",
                    dateAdded = dateFmt.format(Date(file.lastModified()))
                )
            }
        }
        localDiskItems = scanned
    }

    // Real device storage telemetry
    val (usedStorageText, freeStorageText, storageFraction) = remember(localDiskItems) {
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val freeBytes = stat.availableBytes
            val totalBytes = stat.totalBytes.coerceAtLeast(1L)
            val appBytes = localDiskItems.sumOf { it.sizeBytes }
            val ratio = ((totalBytes - freeBytes).toFloat() / totalBytes.toFloat()).coerceIn(0.05f, 0.95f)
            Triple(
                "Kinetix Library (${localDiskItems.size} files): ${formatFileSize(appBytes)}",
                "${formatFileSize(freeBytes)} free on device",
                ratio
            )
        } catch (_: Exception) {
            Triple("Kinetix Library: 0 B", "Available", 0.15f)
        }
    }

    val filteredItems = localDiskItems.filter { item ->
        val matchesSearch = item.title.contains(searchQuery, ignoreCase = true)
        val matchesType = when (selectedFilterTab) {
            1 -> item.type == "VIDEO"
            2 -> item.type == "AUDIO"
            3 -> item.type == "IMAGE"
            else -> true
        }
        matchesSearch && matchesType
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianCanvas)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Media Library",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ContentHigh,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Your downloaded videos, audio tracks & Studio exports",
                        fontSize = 13.sp,
                        color = ContentMedium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                SunsetOutlinedButton(
                    text = "Import Video",
                    onClick = {
                        videoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    },
                    icon = Icons.Default.VideoFile,
                    modifier = Modifier.height(40.dp)
                )
            }
        }

        // Search Bar
        item {
            ObsidianSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it }
            )
        }

        // Storage indicator
        item {
            ObsidianCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = usedStorageText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ContentHigh
                        )
                        Text(
                            text = freeStorageText,
                            fontSize = 12.sp,
                            color = ContentMedium
                        )
                    }
                    LinearProgressIndicator(
                        progress = { storageFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = PrimarySunset,
                        trackColor = ObsidianSurface2
                    )
                }
            }
        }

        // Filter Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val tabs = listOf("All Media", "Videos", "Audio", "Images")
                items(tabs.indices.toList()) { index ->
                    SunsetChip(
                        text = tabs[index],
                        selected = selectedFilterTab == index,
                        onClick = { selectedFilterTab = index }
                    )
                }
            }
        }

        val validDrafts = savedProjects.filter { draft ->
            draft.sourceUri.isNotBlank() &&
                (!draft.sourceUri.startsWith("file://") || File(Uri.parse(draft.sourceUri).path.orEmpty()).exists())
        }

        if (validDrafts.isNotEmpty() && selectedFilterTab == 0) {
            item {
                Text(
                    text = "Saved Studio Drafts (${validDrafts.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimarySunset
                )
            }

            items(validDrafts.take(5), key = { "draft_${it.id}" }) { draft ->
                ObsidianCard(
                    modifier = Modifier.fillMaxWidth(),
                    isHighlighted = true,
                    onClick = { onNavigateToEditor(draft.id, draft.sourceUri, draft.name) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PlatformBadge(platformName = "STUDIO DRAFT")
                                Text(
                                    text = "${draft.canvas.ratio.label.split(" ").first()} • ${draft.clips.size} clip(s) • ${draft.filter.filterName}",
                                    fontSize = 11.sp,
                                    color = ContentMedium
                                )
                            }
                            Text(
                                text = draft.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ContentHigh,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    coroutineScope.launch { repository?.deleteProject(draft.id) }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Draft",
                                    tint = ContentMedium,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            SunsetIconButton(
                                icon = Icons.Default.Edit,
                                onClick = { onNavigateToEditor(draft.id, draft.sourceUri, draft.name) },
                                tint = PrimarySunset,
                                size = 38
                            )
                        }
                    }
                }
            }
        }

        if (filteredItems.isEmpty()) {
            item {
                ObsidianCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = PrimarySunset,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "No Media Files Yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ContentHigh
                        )
                        Text(
                            text = "Download a video from the Download tab or import any video from your phone gallery to edit in Studio.",
                            fontSize = 12.sp,
                            color = ContentMedium
                        )
                        SunsetPrimaryButton(
                            text = "Import Video from Phone",
                            onClick = {
                                videoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            icon = Icons.Default.VideoFile
                        )
                    }
                }
            }
        } else {
            items(filteredItems, key = { it.id }) { item ->
                ObsidianCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { previewingItem = item }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 88.dp, height = 66.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ObsidianSurface2)
                                .clickable { previewingItem = item },
                            contentAlignment = Alignment.Center
                        ) {
                            if (item.thumbnailBitmap != null) {
                                Image(
                                    bitmap = item.thumbnailBitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xB30D0E11)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (item.type == "AUDIO") Icons.Default.Audiotrack else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = ContentHigh,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PlatformBadge(platformName = item.platform)
                                if (item.durationSeconds > 0L) {
                                    Text(
                                        text = formatDuration(item.durationSeconds),
                                        fontSize = 11.sp,
                                        color = ContentMedium
                                    )
                                }
                            }
                            Text(
                                text = item.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = ContentHigh,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${formatFileSize(item.sizeBytes)} • ${item.dateAdded}",
                                fontSize = 11.sp,
                                color = ContentMedium
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    runCatching {
                                        val f = File(item.filePath)
                                        if (f.exists()) f.delete()
                                        refreshTrigger++
                                    }
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete File",
                                    tint = ContentMedium,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            if (item.type == "VIDEO") {
                                SunsetIconButton(
                                    icon = Icons.Default.Edit,
                                    onClick = { onNavigateToEditor("project_${item.id}", item.videoUri, item.title) },
                                    tint = PrimarySunset,
                                    size = 38
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // In-App Media Player Preview Modal
    if (previewingItem != null) {
        val mediaItem = previewingItem!!
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val previewPlayer = remember(mediaItem.videoUri) {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(mediaItem.videoUri))
                prepare()
                playWhenReady = true
            }
        }
        DisposableEffect(previewPlayer) {
            onDispose { previewPlayer.release() }
        }

        ModalBottomSheet(
            onDismissRequest = { previewingItem = null },
            sheetState = sheetState,
            containerColor = ObsidianSurface1
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = mediaItem.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ContentHigh,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(9f / 16f)
                        .height(360.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = true
                                player = previewPlayer
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (mediaItem.type == "VIDEO") {
                        SunsetPrimaryButton(
                            text = "Edit in Studio",
                            onClick = {
                                val uri = mediaItem.videoUri
                                val title = mediaItem.title
                                val id = mediaItem.id
                                previewingItem = null
                                onNavigateToEditor("project_$id", uri, title)
                            },
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Edit
                        )
                    }
                    SunsetOutlinedButton(
                        text = "Share",
                        onClick = {
                            runCatching {
                                val f = File(mediaItem.filePath)
                                if (f.exists()) {
                                    val shareUri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        f
                                    )
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = if (mediaItem.type == "AUDIO") "audio/*" else "video/*"
                                        putExtra(Intent.EXTRA_STREAM, shareUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share Media"))
                                }
                            }
                        },
                        icon = Icons.Default.Share
                    )
                }
            }
        }
    }
}
