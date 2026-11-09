package com.kinetix.pro.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kinetix.pro.data.local.entity.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAtEpochMs DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: String): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE status = 'DOWNLOADING' OR status = 'QUEUED' ORDER BY createdAtEpochMs ASC")
    fun getActiveDownloads(): Flow<List<DownloadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(download: DownloadEntity)

    @Query("UPDATE downloads SET progressPercent = :progress, bytesDownloaded = :bytes, speedBytesPerSec = :speed, etaSeconds = :eta WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Int, bytes: Long, speed: Long, eta: Long)

    @Query("UPDATE downloads SET status = :status, localFilePath = :filePath, mediaStoreUri = :mediaUri, completedAtEpochMs = :completedAt WHERE id = :id")
    suspend fun updateCompleted(id: String, status: String, filePath: String?, mediaUri: String?, completedAt: Long)

    @Query("UPDATE downloads SET status = :status, errorMessage = :error WHERE id = :id")
    suspend fun updateError(id: String, status: String, error: String?)

    @Delete
    suspend fun delete(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: String)
}
