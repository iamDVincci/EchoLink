package com.kinetix.pro.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kinetix.pro.data.local.entity.TemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
    @Query("SELECT * FROM templates ORDER BY createdAtEpochMs DESC")
    fun getAllTemplates(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates WHERE id = :id")
    suspend fun getTemplateById(id: String): TemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(template: TemplateEntity)

    @Delete
    suspend fun delete(template: TemplateEntity)

    @Query("DELETE FROM templates WHERE id = :id")
    suspend fun deleteById(id: String)
}
