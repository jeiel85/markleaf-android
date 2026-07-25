package com.markleaf.notes.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.markleaf.notes.core.text.NoteTitleSource
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.domain.model.Note
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/**
 * The retitle pass behind the "Note title" setting (#280) — the part that makes
 * flipping the setting visible on notes that already exist.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NoteRetitlerTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: LocalNoteRepository

    private val created = Instant.ofEpochMilli(1_000L)
    private val edited = Instant.ofEpochMilli(2_000L)

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

    private fun note(
        id: String,
        title: String,
        content: String,
        excerpt: String = "",
        isConflictCopy: Boolean = false
    ) = Note(
        id = id,
        title = title,
        contentMarkdown = content,
        excerpt = excerpt,
        createdAt = created,
        updatedAt = edited,
        isConflictCopy = isConflictCopy
    )

    @Test
    fun `switching to first line retitles a note whose heading sits below a paragraph`() = runTest {
        repository.createNote(note("1", "Details", "Some plain text\n## Details\nmore"))

        val changed = NoteRetitler.retitleAll(repository, NoteTitleSource.FIRST_LINE)

        assertEquals(1, changed)
        val stored = repository.getNote("1")!!
        assertEquals("Some plain text", stored.title)
        assertEquals("Details\nmore", stored.excerpt)
    }

    /** A retitle is not an edit: the note must keep its place in the list. */
    @Test
    fun `retitling leaves updatedAt untouched`() = runTest {
        repository.createNote(note("1", "Details", "Some plain text\n## Details"))

        NoteRetitler.retitleAll(repository, NoteTitleSource.FIRST_LINE)

        assertEquals(edited, repository.getNote("1")!!.updatedAt)
    }

    @Test
    fun `an empty note keeps its empty title rather than becoming Untitled`() = runTest {
        repository.createNote(note("1", "", ""))

        val changed = NoteRetitler.retitleAll(repository, NoteTitleSource.FIRST_LINE)

        assertEquals(0, changed)
        assertEquals("", repository.getNote("1")!!.title)
    }

    /** The suffix is what makes a conflict copy findable; re-deriving loses it. */
    @Test
    fun `a conflict copy keeps its suffixed title`() = runTest {
        repository.createNote(
            note(
                id = "1",
                title = "My Note (copy from another device 12:00)",
                content = "Some plain text\n## My Note",
                isConflictCopy = true
            )
        )

        val changed = NoteRetitler.retitleAll(repository, NoteTitleSource.FIRST_LINE)

        assertEquals(0, changed)
        assertEquals(
            "My Note (copy from another device 12:00)",
            repository.getNote("1")!!.title
        )
    }

    /**
     * The pass reads every note, computes, and writes. Writing the whole row
     * back would restore the body it read at the start — undoing an edit or an
     * import that landed while it ran — so only the derived columns move.
     */
    @Test
    fun `retitling rewrites the derived columns and nothing else`() = runTest {
        repository.createNote(
            note("1", "Details", "Some plain text\n## Details", excerpt = "old excerpt")
        )
        val before = repository.getNote("1")!!

        // What the pass does to one note, with the row edited underneath it in
        // between — the concurrent-edit window.
        repository.updateNote(before.copy(contentMarkdown = "edited elsewhere", pinned = true))
        repository.updateDerivedTitle("1", "Some plain text", "Details")

        val after = repository.getNote("1")!!
        assertEquals("Some plain text", after.title)
        assertEquals("Details", after.excerpt)
        assertEquals("edited elsewhere", after.contentMarkdown)
        assertEquals(true, after.pinned)
        assertEquals(before.updatedAt, after.updatedAt)
        assertEquals(before.createdAt, after.createdAt)
    }

    @Test
    fun `a second pass under the same rule changes nothing`() = runTest {
        repository.createNote(note("1", "Details", "Some plain text\n## Details"))

        assertEquals(1, NoteRetitler.retitleAll(repository, NoteTitleSource.FIRST_LINE))
        assertEquals(0, NoteRetitler.retitleAll(repository, NoteTitleSource.FIRST_LINE))
    }

    @Test
    fun `switching back restores the heading title`() = runTest {
        repository.createNote(note("1", "Details", "Some plain text\n## Details"))

        NoteRetitler.retitleAll(repository, NoteTitleSource.FIRST_LINE)
        NoteRetitler.retitleAll(repository, NoteTitleSource.FIRST_HEADING)

        assertEquals("Details", repository.getNote("1")!!.title)
    }
}
