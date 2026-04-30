package com.markleaf.notes.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class NoteDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var noteDao: NoteDao

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        noteDao = db.noteDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGetNote() = runBlocking {
        val note = NoteEntity(
            id = "test-id",
            title = "Test Note",
            contentMarkdown = "# Test Content",
            excerpt = "Test Content",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        noteDao.insertNote(note)
        val loaded = noteDao.getNoteById("test-id")
        assertEquals(note.title, loaded?.title)
    }

    @Test
    @Throws(Exception::class)
    fun moveToTrashAndObserve() = runBlocking {
        val note = NoteEntity(
            id = "trash-test",
            title = "To be trashed",
            contentMarkdown = "content",
            excerpt = "excerpt",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        noteDao.insertNote(note)
        
        noteDao.moveToTrash("trash-test", System.currentTimeMillis())
        
        val activeNotes = noteDao.observeNotes().first()
        assertTrue(activeNotes.none { it.id == "trash-test" })
        
        val trashedNotes = noteDao.observeTrashedNotes().first()
        assertTrue(trashedNotes.any { it.id == "trash-test" })
    }
}
