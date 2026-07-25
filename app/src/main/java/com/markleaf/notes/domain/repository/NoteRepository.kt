package com.markleaf.notes.domain.repository

import com.markleaf.notes.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeNotes(): Flow<List<Note>>
    suspend fun getNote(noteId: String): Note?
    /** Every note regardless of state (active/archived/trashed) — used by the
     *  folder reconcile so hidden notes aren't re-imported as new (#148). */
    suspend fun getAllNotes(): List<Note>
    suspend fun createNote(note: Note)
    suspend fun updateNote(note: Note)

    /** Rewrite only the title and excerpt of one note — see
     *  [com.markleaf.notes.data.repository.NoteRetitler] for why the retitle
     *  pass must not write the whole row back (#280). */
    suspend fun updateDerivedTitle(noteId: String, title: String, excerpt: String)
    suspend fun moveToTrash(noteId: String)
    suspend fun setPinned(noteId: String, pinned: Boolean)
    suspend fun setArchived(noteId: String, archived: Boolean)
    suspend fun restoreFromTrash(noteId: String)
    suspend fun deleteForever(noteId: String)
    fun observeTrashedNotes(): Flow<List<Note>>
    fun observeArchivedNotes(): Flow<List<Note>>
    /** Notes in the passcode-gated "Locked notes" space (#155). */
    fun observeLockedNotes(): Flow<List<Note>>
    /** Move a note into ([locked] = true) or out of ([locked] = false) the
     *  Locked space. */
    suspend fun setLocked(noteId: String, locked: Boolean)
    /** Clear the lock on every locked note (used when the passcode is removed). */
    suspend fun unlockAllLocked()
    fun searchNotes(query: String): Flow<List<Note>>
    suspend fun reorderNotes(notes: List<Note>)
    fun observeConflictNotes(): Flow<List<Note>>
}
