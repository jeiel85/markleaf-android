package com.markleaf.notes.data.repository

import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.local.entity.NoteLinkEntity
import com.markleaf.notes.data.local.entity.toEntity
import com.markleaf.notes.data.local.entity.toDomain
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalNoteRepository(
    private val database: AppDatabase
) : NoteRepository {
    private val wikiLinkPattern = Regex("""\[\[([^\]]+)]]""")

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
        reindexNoteLinks(note)
    }

    override suspend fun moveToTrash(noteId: String) {
        database.noteDao().moveToTrash(noteId, System.currentTimeMillis())
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
        return database.noteDao().searchNotes(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getBacklinks(noteId: String): Flow<List<Note>> {
        return database.noteDao().getBacklinkingNotes(noteId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    private suspend fun reindexNoteLinks(note: Note) {
        database.noteLinkDao().deleteLinksFromNote(note.id)
        wikiLinkPattern.findAll(note.contentMarkdown)
            .map { it.groupValues[1].trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { rawLabel ->
                val targetNoteId = database.noteDao().getNoteByTitle(rawLabel)?.id
                database.noteLinkDao().insertLink(
                    NoteLinkEntity(
                        sourceNoteId = note.id,
                        targetNoteId = targetNoteId,
                        rawLabel = rawLabel
                    )
                )
            }
    }
}
