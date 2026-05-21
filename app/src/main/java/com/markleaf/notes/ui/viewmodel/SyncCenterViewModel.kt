package com.markleaf.notes.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.domain.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SyncCenterViewModel(
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _conflictNotes = MutableStateFlow<List<Note>>(emptyList())
    val conflictNotes: StateFlow<List<Note>> = _conflictNotes

    init {
        observeConflictNotes()
    }

    private fun observeConflictNotes() {
        viewModelScope.launch {
            noteRepository.observeConflictNotes().collect { notes ->
                _conflictNotes.value = notes
            }
        }
    }

    fun deleteConflictNote(noteId: String) {
        viewModelScope.launch {
            noteRepository.deleteForever(noteId)
        }
    }
}
