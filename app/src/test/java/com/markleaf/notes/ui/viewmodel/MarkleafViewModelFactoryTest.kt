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

    private class FakeNoteRepository : NoteRepository {
        override fun observeNotes(): Flow<List<Note>> = flowOf(emptyList())
        override suspend fun getNote(noteId: String): Note? = null
        override suspend fun createNote(note: Note) = Unit
        override suspend fun updateNote(note: Note) = Unit
        override suspend fun moveToTrash(noteId: String) = Unit
        override suspend fun restoreFromTrash(noteId: String) = Unit
        override suspend fun deleteForever(noteId: String) = Unit
        override fun observeTrashedNotes(): Flow<List<Note>> = flowOf(emptyList())
        override fun searchNotes(query: String): Flow<List<Note>> = flowOf(emptyList())
        override fun getBacklinks(noteId: String): Flow<List<Note>> = flowOf(emptyList())
    }
}
