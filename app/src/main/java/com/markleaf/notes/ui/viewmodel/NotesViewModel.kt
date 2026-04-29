package com.markleaf.notes.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.domain.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

class NotesViewModel(
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes

    init {
        observeNotes()
    }

    private fun observeNotes() {
        viewModelScope.launch {
            noteRepository.observeNotes().collect { noteList ->
                _notes.value = noteList
            }
        }
    }

    suspend fun createNote(): Note {
        val newNote = Note(
            id = UUID.randomUUID().toString(),
            title = "",
            contentMarkdown = "",
            excerpt = "",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        noteRepository.createNote(newNote)
        return newNote
    }

    fun moveToTrash(noteId: String) {
        viewModelScope.launch {
            noteRepository.moveToTrash(noteId)
        }
    }
}
