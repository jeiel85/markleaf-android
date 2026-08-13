package com.markleaf.notes.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.local.entity.NoteEntity
import com.markleaf.notes.data.local.entity.NoteViewStateEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The "where I left off" table behind "Open notes at" (#214) — in particular
 * the pruning that runs when the setting is switched off (#262): turning the
 * setting back on later must not restore a stale position for every note.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NoteViewStateDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: NoteViewStateDao
    private lateinit var noteDao: com.markleaf.notes.data.local.dao.NoteDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        dao = db.noteViewStateDao()
        noteDao = db.noteDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun insertNote(id: String) {
        val now = System.currentTimeMillis()
        noteDao.insertNote(
            NoteEntity(
                id = id,
                title = id,
                contentMarkdown = "content",
                excerpt = "content",
                createdAt = now,
                updatedAt = now
            )
        )
    }

    @Test
    fun `positions round-trip per note`() = runTest {
        insertNote("a")
        insertNote("b")
        dao.upsert(NoteViewStateEntity(noteId = "a", caretOffset = 10, previewIndex = 2))
        dao.upsert(NoteViewStateEntity(noteId = "b", caretOffset = 200, previewIndex = 7))

        assertEquals(10, dao.getForNote("a")?.caretOffset)
        assertEquals(7, dao.getForNote("b")?.previewIndex)
        assertNull(dao.getForNote("missing"))
    }

    @Test
    fun `clearAll drops every recorded position`() = runTest {
        insertNote("a")
        insertNote("b")
        dao.upsert(NoteViewStateEntity(noteId = "a", caretOffset = 10, previewIndex = 2))
        dao.upsert(NoteViewStateEntity(noteId = "b", caretOffset = 200, previewIndex = 7))

        dao.clearAll()

        assertNull(dao.getForNote("a"))
        assertNull(dao.getForNote("b"))
    }

    @Test
    fun `clearAll is a no-op on an empty table`() = runTest {
        dao.clearAll() // must not throw
        assertNull(dao.getForNote("a"))
    }
}
