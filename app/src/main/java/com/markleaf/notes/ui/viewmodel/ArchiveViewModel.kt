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

class ArchiveViewModel(
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _archivedNotes = MutableStateFlow<List<Note>>(emptyList())
    val archivedNotes: StateFlow<List<Note>> = _archivedNotes

    init {
        viewModelScope.launch {
            noteRepository.observeArchivedNotes().collect { notes ->
                _archivedNotes.value = notes
            }
        }
    }

    fun unarchive(noteId: String) {
        viewModelScope.launch {
            noteRepository.setArchived(noteId, false)
        }
    }

    fun moveToTrash(noteId: String) {
        viewModelScope.launch {
            noteRepository.moveToTrash(noteId)
        }
    }

    // A selection's actions (#596).

    fun unarchive(noteIds: Collection<String>) {
        viewModelScope.launch { noteRepository.setArchived(noteIds, false) }
    }

    fun moveToTrash(noteIds: Collection<String>) {
        viewModelScope.launch { noteRepository.moveToTrash(noteIds) }
    }

    /** The Undo for the last move to the trash, kept across navigation (#262). */
    val trashUndo = PendingTrashUndo()

    /** Moves [noteIds] to the trash and offers to undo it. */
    fun moveToTrashWithUndo(noteIds: List<String>) {
        moveToTrash(noteIds)
        trashUndo.offer(noteIds)
    }

    /** The snackbar for [move] finished; [undo] when its action was tapped. */
    fun settleTrashUndo(move: PendingTrashUndo.Move, undo: Boolean) {
        if (trashUndo.settle(move) && undo) restoreFromTrash(move.noteIds)
    }

    /** Undo for a move to the trash, under the retention sweep's lock (#566). */
    fun restoreFromTrash(noteIds: Collection<String>) {
        viewModelScope.launch {
            TrashPurge.lock.withLock { noteRepository.restoreFromTrash(noteIds) }
        }
    }
}
