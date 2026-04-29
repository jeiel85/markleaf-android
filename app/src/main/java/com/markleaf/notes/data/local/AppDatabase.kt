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

@Database(
    entities = [NoteEntity::class, TagEntity::class, NoteTagCrossRef::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun tagDao(): TagDao

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
