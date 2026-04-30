package com.markleaf.notes.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.markleaf.notes.data.local.dao.NoteDao
import com.markleaf.notes.data.local.dao.TagDao
import com.markleaf.notes.data.local.entity.NoteEntity
import com.markleaf.notes.data.local.entity.NoteTagCrossRef
import com.markleaf.notes.data.local.entity.TagEntity

import com.markleaf.notes.data.local.dao.AttachmentDao
import com.markleaf.notes.data.local.entity.AttachmentEntity
import com.markleaf.notes.data.local.entity.NoteFtsEntity

@Database(
    entities = [
        NoteEntity::class, 
        TagEntity::class, 
        NoteTagCrossRef::class, 
        NoteFtsEntity::class,
        AttachmentEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun tagDao(): TagDao
    abstract fun attachmentDao(): AttachmentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "markleaf.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
