package com.markleaf.notes.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.domain.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TrashViewModel(
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _trashedNotes = MutableStateFlow<List<Note>>(emptyList())
    val trashedNotes: StateFlow<List<Note>> = _trashedNotes

    init {
        observeTrashedNotes()
    }

    private fun observeTrashedNotes() {
        viewModelScope.launch {
            noteRepository.observeTrashedNotes().collect { notes ->
                _trashedNotes.value = notes
            }
        }
    }

    fun restoreFromTrash(noteId: String) {
        viewModelScope.launch {
            noteRepository.restoreFromTrash(noteId)
        }
    }

    fun deleteForever(noteId: String) {
        viewModelScope.launch {
            noteRepository.deleteForever(noteId)
        }
    }
}
