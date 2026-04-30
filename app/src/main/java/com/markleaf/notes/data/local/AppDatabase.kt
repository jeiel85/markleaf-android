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

import com.markleaf.notes.data.local.dao.NoteLinkDao
import com.markleaf.notes.data.local.entity.AttachmentEntity
import com.markleaf.notes.data.local.entity.NoteFtsEntity
import com.markleaf.notes.data.local.entity.NoteLinkEntity

@Database(
    entities = [
        NoteEntity::class, 
        TagEntity::class, 
        NoteTagCrossRef::class, 
        NoteFtsEntity::class,
        AttachmentEntity::class,
        NoteLinkEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun tagDao(): TagDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun noteLinkDao(): NoteLinkDao

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
