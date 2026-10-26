package com.markleaf.notes.ui.viewmodel

import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.domain.repository.NoteRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/** "Open the list on" (#545): which tag the list opens on, and when it is applied. */
@OptIn(ExperimentalCoroutinesApi::class)
class StartupTagTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `unset opens all notes`() {
        assertNull(startupTag(null, listOf("journal")))
        assertNull(startupTag("  ", listOf("journal")))
    }

    @Test
    fun `an existing tag is opened, normalized`() {
        assertEquals("journal", startupTag("Journal", listOf("journal", "study")))
    }

    @Test
    fun `a parent with only child tags still opens`() {
        assertEquals("work", startupTag("work", listOf("work/site", "study")))
    }

    @Test
    fun `a renamed or deleted tag falls back to all notes`() {
        assertNull(startupTag("journal", listOf("diary", "study")))
        // A tag that merely starts with the same letters is a different tag.
        assertNull(startupTag("work", listOf("workout")))
    }

    @Test
    fun `the setting is applied once per list`() = runTest {
        val viewModel = NotesViewModel(EmptyRepository())
        viewModel.applyStartupTag { "journal" }
        assertEquals("journal", viewModel.selectedTag.value)

        // The reader switches to Study, leaves for a note and comes back.
        viewModel.selectTag("study")
        viewModel.applyStartupTag { "journal" }
        assertEquals("study", viewModel.selectedTag.value)
    }

    @Test
    fun `a read cancelled before it answers runs again`() = runTest {
        val viewModel = NotesViewModel(EmptyRepository())
        try {
            viewModel.applyStartupTag { throw CancellationException("rotated") }
            fail("expected the cancellation to propagate")
        } catch (_: CancellationException) {
        }
        viewModel.applyStartupTag { "journal" }
        assertEquals("journal", viewModel.selectedTag.value)
    }

    @Test
    fun `a tag picked while the setting was read wins`() = runTest {
        val viewModel = NotesViewModel(EmptyRepository())
        viewModel.applyStartupTag {
            viewModel.selectTag("study")
            "journal"
        }
        assertEquals("study", viewModel.selectedTag.value)
    }

    private class EmptyRepository : NoteRepository {
        override fun observeNotes(): Flow<List<Note>> = flowOf(emptyList())
        override suspend fun getNote(noteId: String): Note? = null
        override fun observeNote(noteId: String): Flow<Note?> = flowOf(null)
        override suspend fun getAllNotes(): List<Note> = emptyList()
        override suspend fun createNote(note: Note) = Unit
        override suspend fun updateNote(note: Note) = Unit
        override suspend fun updateDerivedTitle(noteId: String, title: String, excerpt: String) = Unit
        override suspend fun moveToTrash(noteId: String) = Unit
        override suspend fun setPinned(noteId: String, pinned: Boolean) = Unit
        override suspend fun setArchived(noteId: String, archived: Boolean) = Unit
        override suspend fun restoreFromTrash(noteId: String) = Unit
        override suspend fun deleteForever(noteId: String) = Unit
        override suspend fun reorderNotes(notes: List<Note>) = Unit
        override fun observeTrashedNotes(): Flow<List<Note>> = flowOf(emptyList())
        override fun observeArchivedNotes(): Flow<List<Note>> = flowOf(emptyList())
        override fun observeLockedNotes(): Flow<List<Note>> = flowOf(emptyList())
        override suspend fun setLocked(noteId: String, locked: Boolean) = Unit
        override suspend fun unlockAllLocked() = Unit
        override fun searchNotes(query: String): Flow<List<Note>> = flowOf(emptyList())
        override fun observeConflictNotes(): Flow<List<Note>> = flowOf(emptyList())
    }
}
