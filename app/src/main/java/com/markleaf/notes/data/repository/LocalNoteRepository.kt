package com.markleaf.notes.data.repository

import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.local.entity.toEntity
import com.markleaf.notes.data.local.entity.toDomain
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalNoteRepository(
    private val database: AppDatabase
) : NoteRepository {

    override fun observeNotes(): Flow<List<Note>> {
        return database.noteDao().observeNotes().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getNote(noteId: String): Note? {
        return database.noteDao().getNoteById(noteId)?.toDomain()
    }

    override suspend fun createNote(note: Note) {
        database.noteDao().insertNote(note.toEntity())
    }

    override suspend fun updateNote(note: Note) {
        database.noteDao().updateNote(note.toEntity())
    }

    override suspend fun moveToTrash(noteId: String) {
        database.noteDao().moveToTrash(noteId, System.currentTimeMillis())
    }

    override suspend fun setPinned(noteId: String, pinned: Boolean) {
        database.noteDao().setPinned(noteId, pinned)
    }

    override suspend fun restoreFromTrash(noteId: String) {
        database.noteDao().restoreFromTrash(noteId)
    }

    override suspend fun deleteForever(noteId: String) {
        database.noteDao().deleteForever(noteId)
    }

    override fun observeTrashedNotes(): Flow<List<Note>> {
        return database.noteDao().observeTrashedNotes().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchNotes(query: String): Flow<List<Note>> {
        val ftsQuery = query.toFtsPrefixQuery()
        val searchFlow = if (ftsQuery.isBlank()) {
            database.noteDao().searchNotesLike(query)
        } else {
            database.noteDao().searchNotesFts(ftsQuery)
        }
        return searchFlow.map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun reorderNotes(notes: List<Note>) {
        notes.forEachIndexed { index, note ->
            database.noteDao().updateSortOrder(note.id, index)
        }
    }

    private fun String.toFtsPrefixQuery(): String {
        return split(Regex("\\s+"))
            .map { token ->
                token.filter { char ->
                    char.isLetterOrDigit() || char == '_' || char == '-'
                }
            }
            .filter { it.isNotBlank() }
            .joinToString(separator = " ") { "$it*" }
    }
}
