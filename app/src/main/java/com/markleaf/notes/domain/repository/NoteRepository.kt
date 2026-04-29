package com.markleaf.notes.domain.repository

import com.markleaf.notes.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeNotes(): Flow<List<Note>>
    suspend fun getNote(noteId: String): Note?
    suspend fun createNote(note: Note)
    suspend fun updateNote(note: Note)
    suspend fun moveToTrash(noteId: String)
    suspend fun restoreFromTrash(noteId: String)
    suspend fun deleteForever(noteId: String)
    fun observeTrashedNotes(): Flow<List<Note>>
    fun searchNotes(query: String): Flow<List<Note>>
}
