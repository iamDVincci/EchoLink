package com.kinetix.pro.downloader

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.kinetix.pro.core.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

class MediaStoreExporter(private val context: Context) {

    suspend fun exportToMediaStore(
        sourceFile: File,
        targetFileName: String,
        mediaType: MediaType,
        mimeType: String = when (mediaType) {
            MediaType.VIDEO -> "video/mp4"
            MediaType.AUDIO -> "audio/mpeg"
            MediaType.IMAGE -> "image/jpeg"
        }
    ): Uri? = withContext(Dispatchers.IO) {
        if (!sourceFile.exists() || sourceFile.length() <= 0L) {
            return@withContext null
        }

        val resolver = context.contentResolver
        val nowSeconds = System.currentTimeMillis() / 1000L
        val nowMillis = System.currentTimeMillis()

        val collectionUri = when (mediaType) {
            MediaType.VIDEO -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }
            MediaType.AUDIO -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }
            MediaType.IMAGE -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
        }

        val relativeSubDir = when (mediaType) {
            MediaType.VIDEO -> "${Environment.DIRECTORY_MOVIES}/Kinetix Pro"
            MediaType.AUDIO -> "${Environment.DIRECTORY_MUSIC}/Kinetix Pro"
            MediaType.IMAGE -> "${Environment.DIRECTORY_PICTURES}/Kinetix Pro"
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, targetFileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.DATE_ADDED, nowSeconds)
            put(MediaStore.MediaColumns.DATE_MODIFIED, nowSeconds)
            put(MediaStore.MediaColumns.SIZE, sourceFile.length())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.DATE_TAKEN, nowMillis)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativeSubDir)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val itemUri = resolver.insert(collectionUri, contentValues) ?: return@withContext null

        try {
            resolver.openOutputStream(itemUri)?.use { out ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(out, bufferSize = 64 * 1024)
                    out.flush()
                }
            } ?: return@withContext null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val completeValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                    put(MediaStore.MediaColumns.SIZE, sourceFile.length())
                }
                resolver.update(itemUri, completeValues, null, null)
            }

            // Trigger MediaScanner so Samsung Gallery & Google Photos index the new file immediately
            runCatching {
                val basePublicDir = when (mediaType) {
                    MediaType.VIDEO -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                    MediaType.AUDIO -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                    MediaType.IMAGE -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                }
                val publicMediaFile = File(basePublicDir, "Kinetix Pro/$targetFileName")
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(publicMediaFile.absolutePath),
                    arrayOf(mimeType),
                    null
                )
            }

            itemUri
        } catch (e: Exception) {
            runCatching { resolver.delete(itemUri, null, null) }
            null
        }
    }

    suspend fun deleteExportedMedia(
        mediaStoreUriString: String?,
        localFilePath: String?
    ) = withContext(Dispatchers.IO) {
        if (!mediaStoreUriString.isNullOrBlank()) {
            runCatching {
                context.contentResolver.delete(Uri.parse(mediaStoreUriString), null, null)
            }
        }
        if (!localFilePath.isNullOrBlank()) {
            runCatching {
                val parsedPath = if (localFilePath.startsWith("file://")) {
                    Uri.parse(localFilePath).path
                } else {
                    localFilePath
                }
                if (!parsedPath.isNullOrBlank()) {
                    val file = File(parsedPath)
                    if (file.exists()) {
                        file.delete()
                    }
                }
            }
        }
    }
}
