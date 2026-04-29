package com.markleaf.notes.data.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.local.entity.NoteEntity
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.domain.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class TrashRepositoryTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var repository: NoteRepository
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        repository = LocalNoteRepository(database)
    }

    @After
    fun teardown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `moveToTrash sets trashed flag and deletedAt`() = runTest {
        val note = NoteEntity(
            id = "test-id",
            title = "Test",
            contentMarkdown = "Content",
            excerpt = "Excerpt",
            createdAt = Instant.now().toEpochMilli(),
            updatedAt = Instant.now().toEpochMilli(),
            trashed = false,
            deletedAt = null
        )
        database.noteDao().insertNote(note)

        repository.moveToTrash("test-id")
        testDispatcher.scheduler.advanceUntilIdle()

        val trashedNote = database.noteDao().getNoteById("test-id")
        assertNotNull(trashedNote)
        assertTrue(trashedNote!!.trashed)
        assertNotNull(trashedNote.deletedAt)
    }

    @Test
    fun `restoreFromTrash clears trashed flag and deletedAt`() = runTest {
        val note = NoteEntity(
            id = "test-id",
            title = "Test",
            contentMarkdown = "Content",
            excerpt = "Excerpt",
            createdAt = Instant.now().toEpochMilli(),
            updatedAt = Instant.now().toEpochMilli(),
            trashed = true,
            deletedAt = Instant.now().toEpochMilli()
        )
        database.noteDao().insertNote(note)

        repository.restoreFromTrash("test-id")
        testDispatcher.scheduler.advanceUntilIdle()

        val restoredNote = database.noteDao().getNoteById("test-id")
        assertNotNull(restoredNote)
        assertFalse(restoredNote!!.trashed)
        assertNull(restoredNote.deletedAt)
    }

    @Test
    fun `deleteForever removes note permanently`() = runTest {
        val note = NoteEntity(
            id = "test-id",
            title = "Test",
            contentMarkdown = "Content",
            excerpt = "Excerpt",
            createdAt = Instant.now().toEpochMilli(),
            updatedAt = Instant.now().toEpochMilli(),
            trashed = true,
            deletedAt = Instant.now().toEpochMilli()
        )
        database.noteDao().insertNote(note)

        repository.deleteForever("test-id")
        testDispatcher.scheduler.advanceUntilIdle()

        val deletedNote = database.noteDao().getNoteById("test-id")
        assertNull(deletedNote)
    }

    @Test
    fun `observeTrashedNotes only returns trashed notes`() = runTest {
        val note1 = NoteEntity(
            id = "note-1",
            title = "Trashed",
            contentMarkdown = "Content",
            excerpt = "Excerpt",
            createdAt = Instant.now().toEpochMilli(),
            updatedAt = Instant.now().toEpochMilli(),
            trashed = true,
            deletedAt = Instant.now().toEpochMilli()
        )
        val note2 = NoteEntity(
            id = "note-2",
            title = "Not Trashed",
            contentMarkdown = "Content",
            excerpt = "Excerpt",
            createdAt = Instant.now().toEpochMilli(),
            updatedAt = Instant.now().toEpochMilli(),
            trashed = false,
            deletedAt = null
        )
        database.noteDao().insertNote(note1)
        database.noteDao().insertNote(note2)

        var trashedNotes: List<Note> = emptyList()
        val job = launch {
            repository.observeTrashedNotes().collect { notes ->
                trashedNotes = notes
            }
        }

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, trashedNotes.size)
        assertEquals("note-1", trashedNotes.first().id)

        job.cancel()
    }
}
