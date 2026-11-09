package com.kinetix.pro.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kinetix.pro.data.local.entity.MediaFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaFileDao {
    @Query("SELECT * FROM media_files ORDER BY addedAtEpochMs DESC")
    fun getAllMediaFiles(): Flow<List<MediaFileEntity>>

    @Query("SELECT * FROM media_files WHERE mediaType = :type ORDER BY addedAtEpochMs DESC")
    fun getMediaFilesByType(type: String): Flow<List<MediaFileEntity>>

    @Query("SELECT * FROM media_files WHERE title LIKE '%' || :query || '%' ORDER BY addedAtEpochMs DESC")
    fun searchMediaFiles(query: String): Flow<List<MediaFileEntity>>

    @Query("SELECT * FROM media_files WHERE id = :id")
    suspend fun getMediaFileById(id: String): MediaFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mediaFile: MediaFileEntity)

    @Delete
    suspend fun delete(mediaFile: MediaFileEntity)

    @Query("DELETE FROM media_files WHERE id = :id")
    suspend fun deleteById(id: String)
}
