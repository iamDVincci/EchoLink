package com.kinetix.pro.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kinetix.pro.data.local.dao.DownloadDao
import com.kinetix.pro.data.local.dao.EditProjectDao
import com.kinetix.pro.data.local.dao.MediaFileDao
import com.kinetix.pro.data.local.dao.TemplateDao
import com.kinetix.pro.data.local.entity.DownloadEntity
import com.kinetix.pro.data.local.entity.EditProjectEntity
import com.kinetix.pro.data.local.entity.ExportJobEntity
import com.kinetix.pro.data.local.entity.MediaFileEntity
import com.kinetix.pro.data.local.entity.TemplateEntity

@Database(
    entities = [
        DownloadEntity::class,
        MediaFileEntity::class,
        EditProjectEntity::class,
        TemplateEntity::class,
        ExportJobEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(KinetixTypeConverters::class)
abstract class KinetixDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
    abstract fun mediaFileDao(): MediaFileDao
    abstract fun editProjectDao(): EditProjectDao
    abstract fun templateDao(): TemplateDao

    companion object {
        @Volatile
        private var INSTANCE: KinetixDatabase? = null

        fun getInstance(context: Context): KinetixDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    KinetixDatabase::class.java,
                    "kinetix_pro.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
