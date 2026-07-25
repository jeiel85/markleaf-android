package com.markleaf.notes.ui.viewmodel

import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.domain.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MarkleafViewModelFactoryTest {
    private val testDispatcher = StandardTestDispatcher()
    private val factory = MarkleafViewModelFactory(FakeNoteRepository())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun createsNotesViewModel() {
        val viewModel = factory.create(NotesViewModel::class.java) as Any

        assertTrue(viewModel is NotesViewModel)
    }

    @Test
    fun createsSearchViewModel() {
        val viewModel = factory.create(SearchViewModel::class.java) as Any

        assertTrue(viewModel is SearchViewModel)
    }

    @Test
    fun createsTrashViewModel() {
        val viewModel = factory.create(TrashViewModel::class.java) as Any

        assertTrue(viewModel is TrashViewModel)
    }

    @Test
    fun createsArchiveViewModel() {
        val viewModel = factory.create(ArchiveViewModel::class.java) as Any

        assertTrue(viewModel is ArchiveViewModel)
    }

    @Test
    fun createsLockedNotesViewModel() {
        val viewModel = factory.create(LockedNotesViewModel::class.java) as Any

        assertTrue(viewModel is LockedNotesViewModel)
    }

    private class FakeNoteRepository : NoteRepository {
        override fun observeNotes(): Flow<List<Note>> = flowOf(emptyList())
        override suspend fun getNote(noteId: String): Note? = null
        override suspend fun getAllNotes(): List<Note> = emptyList()
        override suspend fun createNote(note: Note) = Unit
        override suspend fun updateNote(note: Note) = Unit
        override suspend fun updateDerivedTitle(
            noteId: String,
            title: String,
            excerpt: String
        ) = Unit
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
