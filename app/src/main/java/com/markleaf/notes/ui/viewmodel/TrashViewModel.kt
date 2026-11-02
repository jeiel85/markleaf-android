package com.markleaf.notes.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.domain.repository.NoteRepository
import com.markleaf.notes.util.TrashPurge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock

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
            // Waits out the retention sweep's handling of this note, if it is
            // mid-delete, so the sweep never deletes a note restored under it (#566).
            TrashPurge.lock.withLock {
                noteRepository.restoreFromTrash(noteId)
            }
        }
    }

    fun deleteForever(noteId: String) {
        viewModelScope.launch {
            noteRepository.deleteForever(noteId)
        }
    }
}
