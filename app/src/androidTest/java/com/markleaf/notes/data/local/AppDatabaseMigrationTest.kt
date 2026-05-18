package com.markleaf.notes.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseName = "migration-test.db"

    @After
    fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migratesFromV4ToCurrentWithoutLosingNotesTagsOrSearch() {
        createVersion4Database()

        val db = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(*AppDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()

        db.openHelper.writableDatabase.query("SELECT id, title, sortOrder, lastImportedAt FROM notes").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("42", cursor.getString(0))
            assertEquals("Legacy Note", cursor.getString(1))
            assertEquals(0, cursor.getInt(2))
            assertTrue(cursor.isNull(3))
        }

        db.openHelper.writableDatabase.query("SELECT noteId, tagId FROM note_tag_cross_ref").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("42", cursor.getString(0))
            assertEquals(1L, cursor.getLong(1))
        }

        db.openHelper.writableDatabase.query(
            "SELECT rowid, title FROM notes_fts WHERE notes_fts MATCH ?",
            arrayOf("legacy")
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Legacy Note", cursor.getString(1))
        }

        db.openHelper.writableDatabase.query("SELECT COUNT(*) FROM note_links").use { cursor ->
            assertNotNull(cursor)
        }
        db.openHelper.writableDatabase.query("SELECT COUNT(*) FROM attachments").use { cursor ->
            assertNotNull(cursor)
        }

        db.close()
    }

    private fun createVersion4Database() {
        context.deleteDatabase(databaseName)
        val path = context.getDatabasePath(databaseName)
        path.parentFile?.mkdirs()

        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `notes` (
                    `id` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `contentMarkdown` TEXT NOT NULL,
                    `excerpt` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `pinned` INTEGER NOT NULL DEFAULT 0,
                    `archived` INTEGER NOT NULL DEFAULT 0,
                    `trashed` INTEGER NOT NULL DEFAULT 0,
                    `deletedAt` INTEGER,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_trashed_pinned_updatedAt` ON `notes` (`trashed`, `pinned`, `updatedAt`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_trashed_deletedAt` ON `notes` (`trashed`, `deletedAt`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_title_trashed` ON `notes` (`title`, `trashed`)")
            db.execSQL(
                """
                CREATE VIRTUAL TABLE IF NOT EXISTS `notes_fts` USING FTS4(
                    `title` TEXT NOT NULL,
                    `contentMarkdown` TEXT NOT NULL,
                    `excerpt` TEXT NOT NULL,
                    content=`notes`
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `tags` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `note_tag_cross_ref` (
                    `noteId` INTEGER NOT NULL,
                    `tagId` INTEGER NOT NULL,
                    PRIMARY KEY(`noteId`, `tagId`),
                    FOREIGN KEY(`noteId`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_tag_cross_ref_noteId` ON `note_tag_cross_ref` (`noteId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_note_tag_cross_ref_tagId` ON `note_tag_cross_ref` (`tagId`)")
            db.execSQL(
                """
                INSERT INTO `notes`
                    (`id`, `title`, `contentMarkdown`, `excerpt`, `createdAt`, `updatedAt`, `pinned`, `archived`, `trashed`, `deletedAt`)
                VALUES
                    ('42', 'Legacy Note', 'Legacy body #tag', 'Legacy body', 1000, 2000, 1, 0, 0, NULL)
                """.trimIndent()
            )
            db.execSQL("INSERT INTO `tags` (`id`, `name`, `createdAt`) VALUES (1, 'tag', 1000)")
            db.execSQL("INSERT INTO `note_tag_cross_ref` (`noteId`, `tagId`) VALUES (42, 1)")
            db.execSQL("INSERT INTO `notes_fts`(`notes_fts`) VALUES ('rebuild')")
            db.version = 4
        }
    }
}
