package com.markleaf.notes.core.text

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.repository.LocalNoteRepository
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.domain.repository.NoteRepository
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Today's note for the new-note shortcuts (#481). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class DailyNoteTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: LocalNoteRepository

    private val today = LocalDate.of(2026, 10, 6)
    private val morning = Instant.parse("2026-10-06T08:00:00Z")

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = LocalNoteRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun todaysNote(titleSource: NoteTitleSource = NoteTitleSource.FIRST_HEADING) = runBlocking {
        DailyNote.findOrCreate(repository, today, morning, titleSource)
    }

    private fun existing(
        title: String,
        updatedAt: Instant = morning,
        trashed: Boolean = false,
        archived: Boolean = false
    ): Note = runBlocking {
        Note(
            id = UUID.randomUUID().toString(),
            title = title,
            contentMarkdown = "# $title",
            excerpt = "",
            createdAt = updatedAt,
            updatedAt = updatedAt,
            trashed = trashed,
            archived = archived
        ).also { repository.createNote(it) }
    }

    @Test
    fun theFirstUseOfTheDayCreatesANoteTitledWithTheDate() {
        val note = todaysNote()

        assertEquals("2026-10-06", note.title)
        assertEquals("# 2026-10-06\n\n", note.contentMarkdown)
        assertEquals(note, runBlocking { repository.getNote(note.id) })
    }

    @Test
    fun laterUsesTheSameDayAddToTheSameNote() {
        val first = todaysNote()
        val second = todaysNote()

        assertEquals(first.id, second.id)
        assertEquals(1, runBlocking { repository.getAllNotes() }.size)
    }

    @Test
    fun yesterdaysNoteIsNotReused() {
        val yesterday = existing("2026-10-05")

        val note = todaysNote()

        assertNotEquals(yesterday.id, note.id)
        assertEquals("2026-10-06", note.title)
    }

    @Test
    fun aTrashedOrArchivedNoteForTheDateIsNotAddedTo() {
        val trashed = existing("2026-10-06", trashed = true)
        val archived = existing("2026-10-06", archived = true)

        val note = todaysNote()

        assertNotEquals(trashed.id, note.id)
        assertNotEquals(archived.id, note.id)
    }

    @Test
    fun withTwoNotesForTheDateTheMostRecentlyEditedOneIsUsed() {
        existing("2026-10-06", updatedAt = Instant.parse("2026-10-06T07:00:00Z"))
        val recent = existing("2026-10-06", updatedAt = Instant.parse("2026-10-06T09:00:00Z"))

        assertEquals(recent.id, todaysNote().id)
    }

    /** Reads the notes, then waits, so two overlapping callers both see "none yet". */
    private class SlowLookup(private val inner: NoteRepository) : NoteRepository by inner {
        override suspend fun getAllNotes(): List<Note> = inner.getAllNotes().also { delay(200) }
    }

    @Test
    fun twoOverlappingUsesStillMakeOneNote() = runBlocking {
        // #516 review: two shortcut launches overlapping both found no note and
        // each created one, breaking one-note-per-day.
        val slow = SlowLookup(repository)
        val first = async(Dispatchers.Default) { DailyNote.findOrCreate(slow, today, morning, NoteTitleSource.FIRST_HEADING) }
        val second = async(Dispatchers.Default) { DailyNote.findOrCreate(slow, today, morning, NoteTitleSource.FIRST_HEADING) }

        assertEquals(first.await().id, second.await().id)
        assertEquals(1, repository.getAllNotes().size)
    }

    @Test
    fun theFirstLineTitleRuleTitlesItWithTheDateToo() {
        // #280's other rule strips the heading markers from the first line; the
        // note still has to be found by its date the next time.
        val note = todaysNote(NoteTitleSource.FIRST_LINE)

        assertEquals("2026-10-06", note.title)
        assertEquals(note.id, todaysNote(NoteTitleSource.FIRST_LINE).id)
    }
}
