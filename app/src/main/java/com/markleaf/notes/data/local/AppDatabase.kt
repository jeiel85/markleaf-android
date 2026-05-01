package com.markleaf.notes.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.markleaf.notes.data.local.dao.AttachmentDao
import com.markleaf.notes.data.local.dao.NoteDao
import com.markleaf.notes.data.local.dao.NoteLinkDao
import com.markleaf.notes.data.local.dao.TagDao
import com.markleaf.notes.data.local.entity.AttachmentEntity
import com.markleaf.notes.data.local.entity.NoteEntity
import com.markleaf.notes.data.local.entity.NoteFtsEntity
import com.markleaf.notes.data.local.entity.NoteLinkEntity
import com.markleaf.notes.data.local.entity.NoteTagCrossRef
import com.markleaf.notes.data.local.entity.TagEntity

@Database(
    entities = [
        NoteEntity::class, 
        TagEntity::class, 
        NoteTagCrossRef::class, 
        NoteFtsEntity::class,
        AttachmentEntity::class,
        NoteLinkEntity::class
    ],
    version = 5,
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
                )
                .addMigrations(MIGRATION_4_5)
                .build().also { INSTANCE = it }
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `note_tag_cross_ref_new` (
                        `noteId` TEXT NOT NULL,
                        `tagId` INTEGER NOT NULL,
                        PRIMARY KEY(`noteId`, `tagId`),
                        FOREIGN KEY(`noteId`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `note_tag_cross_ref_new` (`noteId`, `tagId`)
                    SELECT CAST(`noteId` AS TEXT), `tagId`
                    FROM `note_tag_cross_ref`
                    WHERE CAST(`noteId` AS TEXT) IN (SELECT `id` FROM `notes`)
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `note_tag_cross_ref`")
                db.execSQL("ALTER TABLE `note_tag_cross_ref_new` RENAME TO `note_tag_cross_ref`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_tag_cross_ref_noteId` ON `note_tag_cross_ref` (`noteId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_tag_cross_ref_tagId` ON `note_tag_cross_ref` (`tagId`)")
            }
        }
    }
}
