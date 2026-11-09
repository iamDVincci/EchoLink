package com.kinetix.pro.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kinetix.pro.data.local.entity.EditProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EditProjectDao {
    @Query("SELECT * FROM edit_projects ORDER BY lastModifiedEpochMs DESC")
    fun getAllProjects(): Flow<List<EditProjectEntity>>

    @Query("SELECT * FROM edit_projects WHERE id = :id")
    suspend fun getProjectById(id: String): EditProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(project: EditProjectEntity)

    @Delete
    suspend fun delete(project: EditProjectEntity)

    @Query("DELETE FROM edit_projects WHERE id = :id")
    suspend fun deleteById(id: String)
}
