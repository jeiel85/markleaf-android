package com.markleaf.notes.data.repository

import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.local.entity.toEntity
import com.markleaf.notes.data.local.entity.toDomain
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.domain.repository.NoteRepository
import com.markleaf.notes.util.TagParser
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

    override suspend fun getAllNotes(): List<Note> {
        return database.noteDao().getAllNotes().map { it.toDomain() }
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

    override suspend fun setArchived(noteId: String, archived: Boolean) {
        database.noteDao().setArchived(noteId, archived)
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

    override fun observeArchivedNotes(): Flow<List<Note>> {
        return database.noteDao().observeArchivedNotes().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeLockedNotes(): Flow<List<Note>> {
        return database.noteDao().observeLockedNotes().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun setLocked(noteId: String, locked: Boolean) {
        database.noteDao().setLocked(noteId, locked)
    }

    override suspend fun unlockAllLocked() {
        database.noteDao().unlockAllLocked()
    }

    override fun searchNotes(query: String): Flow<List<Note>> {
        val trimmedQuery = query.trim()
        val tagName = trimmedQuery
            .takeIf { it.startsWith("#") }
            ?.removePrefix("#")
            ?.takeIf { it.isNotBlank() }
            ?.let(TagParser::normalizeTagName)

        val searchFlow = when {
            tagName != null -> database.noteDao().searchNotesByTag(tagName)
            else -> {
                val ftsQuery = query.toFtsPrefixQuery()
                if (ftsQuery.isBlank()) {
                    database.noteDao().searchNotesLike(query)
                } else {
                    database.noteDao().searchNotesFts(ftsQuery)
                }
            }
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

    override fun observeConflictNotes(): Flow<List<Note>> {
        return database.noteDao().observeConflictNotes().map { entities ->
            entities.map { it.toDomain() }
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
