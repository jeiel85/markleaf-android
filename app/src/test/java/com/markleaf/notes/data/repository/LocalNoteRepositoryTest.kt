package com.markleaf.notes.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.domain.model.Note
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocalNoteRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: LocalNoteRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        repository = LocalNoteRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `createNote then getNote returns inserted note`() = runTest {
        val now = Instant.ofEpochMilli(1L)
        val note = Note(
            id = "n1",
            title = "Hello",
            contentMarkdown = "# Hello",
            excerpt = "Hello",
            createdAt = now,
            updatedAt = now
        )

        repository.createNote(note)

        val loaded = repository.getNote("n1")
        assertEquals("Hello", loaded?.title)
        assertEquals("# Hello", loaded?.contentMarkdown)
    }

    @Test
    fun `moveToTrash hides note from active list and exposes it in trash`() = runTest {
        val now = Instant.ofEpochMilli(1L)
        val note = Note(
            id = "n1",
            title = "T",
            contentMarkdown = "T",
            excerpt = "T",
            createdAt = now,
            updatedAt = now
        )

        repository.createNote(note)
        repository.moveToTrash("n1")

        assertTrue(repository.observeNotes().first().isEmpty())
        assertEquals(listOf("n1"), repository.observeTrashedNotes().first().map { it.id })
    }

    @Test
    fun `restoreFromTrash brings note back to active list`() = runTest {
        val now = Instant.ofEpochMilli(1L)
        val note = Note(
            id = "n1",
            title = "T",
            contentMarkdown = "T",
            excerpt = "T",
            createdAt = now,
            updatedAt = now
        )

        repository.createNote(note)
        repository.moveToTrash("n1")
        repository.restoreFromTrash("n1")

        assertEquals(listOf("n1"), repository.observeNotes().first().map { it.id })
        assertTrue(repository.observeTrashedNotes().first().isEmpty())
    }

    @Test
    fun `deleteForever removes note completely`() = runTest {
        val now = Instant.ofEpochMilli(1L)
        val note = Note(
            id = "n1",
            title = "T",
            contentMarkdown = "T",
            excerpt = "T",
            createdAt = now,
            updatedAt = now
        )

        repository.createNote(note)
        repository.moveToTrash("n1")
        repository.deleteForever("n1")

        assertNull(repository.getNote("n1"))
    }
}
