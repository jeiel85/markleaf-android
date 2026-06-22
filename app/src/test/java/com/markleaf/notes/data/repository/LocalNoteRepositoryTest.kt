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
    private lateinit var tagRepository: LocalTagRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        repository = LocalNoteRepository(db)
        tagRepository = LocalTagRepository(db)
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

    @Test
    fun `setArchived hides note from active list and surfaces it in archive`() = runTest {
        val now = Instant.ofEpochMilli(1L)
        val note = Note(
            id = "n1",
            title = "Old",
            contentMarkdown = "Old",
            excerpt = "Old",
            createdAt = now,
            updatedAt = now
        )

        repository.createNote(note)
        repository.setArchived("n1", true)

        assertTrue(repository.observeNotes().first().isEmpty())
        assertEquals(listOf("n1"), repository.observeArchivedNotes().first().map { it.id })
        assertTrue(repository.observeTrashedNotes().first().isEmpty())
    }

    @Test
    fun `setArchived false brings note back to active list`() = runTest {
        val now = Instant.ofEpochMilli(1L)
        val note = Note(
            id = "n1",
            title = "Old",
            contentMarkdown = "Old",
            excerpt = "Old",
            createdAt = now,
            updatedAt = now
        )

        repository.createNote(note)
        repository.setArchived("n1", true)
        repository.setArchived("n1", false)

        assertEquals(listOf("n1"), repository.observeNotes().first().map { it.id })
        assertTrue(repository.observeArchivedNotes().first().isEmpty())
    }

    @Test
    fun `searchNotes excludes archived notes`() = runTest {
        val now = Instant.ofEpochMilli(1L)
        val active = Note(
            id = "active", title = "Hello", contentMarkdown = "Hello there",
            excerpt = "Hello there", createdAt = now, updatedAt = now
        )
        val archived = Note(
            id = "archived", title = "Hello two", contentMarkdown = "Hello again",
            excerpt = "Hello again", createdAt = now, updatedAt = now
        )

        repository.createNote(active)
        repository.createNote(archived)
        repository.setArchived("archived", true)

        val results = repository.searchNotes("Hello").first()
        assertEquals(listOf("active"), results.map { it.id })
    }

    @Test
    fun `searchNotes returns each note once even with duplicate fts postings`() = runTest {
        val now = Instant.ofEpochMilli(1L)
        repository.createNote(
            Note(
                id = "n1", title = "Apple", contentMarkdown = "Apple pie recipe",
                excerpt = "Apple pie", createdAt = now, updatedAt = now
            )
        )

        // Reproduce the #140 failure mode: a second FTS posting for the same
        // note rowid, as an out-of-sync index can accumulate. The old JOIN query
        // listed the note once per posting; the fixed `rowid IN (...)` query must
        // still return it exactly once.
        db.openHelper.writableDatabase.execSQL(
            "INSERT INTO notes_fts(docid, title, contentMarkdown, excerpt) " +
                "SELECT rowid, title, contentMarkdown, excerpt FROM notes WHERE id = 'n1'"
        )

        val results = repository.searchNotes("Apple").first()
        assertEquals(listOf("n1"), results.map { it.id })
    }

    @Test
    fun `searchNotes with tag query finds dash tag through tag index`() = runTest {
        val now = Instant.ofEpochMilli(1L)
        val tagged = Note(
            id = "tagged",
            title = "Tagged",
            contentMarkdown = "Moved from #old-notes",
            excerpt = "Moved from old notes",
            createdAt = now,
            updatedAt = now
        )
        val similar = Note(
            id = "similar",
            title = "Similar",
            contentMarkdown = "Moved from #oldnotes",
            excerpt = "Moved from oldnotes",
            createdAt = now,
            updatedAt = now.plusMillis(1L)
        )
        val archived = Note(
            id = "archived",
            title = "Archived",
            contentMarkdown = "Archived #old-notes",
            excerpt = "Archived old notes",
            createdAt = now,
            updatedAt = now.plusMillis(2L),
            archived = true
        )

        repository.createNote(tagged)
        repository.createNote(similar)
        repository.createNote(archived)
        tagRepository.reindexTagsForNote(tagged.id, tagged.contentMarkdown)
        tagRepository.reindexTagsForNote(similar.id, similar.contentMarkdown)
        tagRepository.reindexTagsForNote(archived.id, archived.contentMarkdown)

        val results = repository.searchNotes("#old-notes").first()

        assertEquals(listOf("tagged"), results.map { it.id })
    }
}
