package com.markleaf.notes.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.domain.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Backs the passcode-gated "Locked notes" screen (#155). Structurally a sibling
 * of [ArchiveViewModel]: it observes the locked subset and offers the two moves
 * that make sense there — take a note back out of the Locked space, or send it
 * to Trash (which also clears the lock, in the DAO).
 */
class LockedNotesViewModel(
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _lockedNotes = MutableStateFlow<List<Note>>(emptyList())
    val lockedNotes: StateFlow<List<Note>> = _lockedNotes

    init {
        viewModelScope.launch {
            noteRepository.observeLockedNotes().collect { notes ->
                _lockedNotes.value = notes
            }
        }
    }

    /** Take the note back out of the Locked space, into the normal notes list. */
    fun removeLock(noteId: String) {
        viewModelScope.launch {
            noteRepository.setLocked(noteId, false)
        }
    }

    fun moveToTrash(noteId: String) {
        viewModelScope.launch {
            noteRepository.moveToTrash(noteId)
        }
    }

    /**
     * Persist a note the sync mirror has just written, so its `lastImportedAt`
     * stamp lands. Without the stamp the note reads as "edited locally since the
     * last import" for ever and the next newer file from another device becomes
     * a conflict copy instead of a clean overwrite (#217).
     *
     * Safe to interleave with [removeLock]: that one updates the `locked` column
     * on its own, so neither write can clobber the other's field.
     */
    fun stampMirrored(note: Note) {
        viewModelScope.launch {
            noteRepository.updateNote(note)
        }
    }
}
