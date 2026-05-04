package com.markleaf.notes.data.repository

import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.local.entity.NoteEntity
import com.markleaf.notes.data.local.entity.NoteLinkEntity
import com.markleaf.notes.data.local.entity.NoteSnapshotEntity
import com.markleaf.notes.data.local.entity.toEntity
import com.markleaf.notes.data.local.entity.toDomain
import com.markleaf.notes.domain.model.BacklinkSnippet
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.domain.model.NoteSnapshot
import com.markleaf.notes.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class LocalNoteRepository(
    private val database: AppDatabase
) : NoteRepository {
    private val wikiLinkPattern = Regex("""\[\[([^\]]+)]]""")
    private val snapshotIntervalMillis = 5 * 60 * 1000L
    private val maxSnapshotsPerNote = 50

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
        maybeSnapshotCurrentNote(note)
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

    override fun getBacklinks(noteId: String): Flow<List<Note>> {
        return database.noteDao().getBacklinkingNotes(noteId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getBacklinkSnippets(noteId: String): Flow<List<BacklinkSnippet>> {
        return database.noteDao().getBacklinkingNotes(noteId).map { entities ->
            val linksBySource = database.noteLinkDao()
                .getBacklinksToNoteList(noteId)
                .groupBy { it.sourceNoteId }

            entities.map { entity ->
                val note = entity.toDomain()
                val rawLabel = linksBySource[note.id]?.firstOrNull()?.rawLabel.orEmpty()
                BacklinkSnippet(
                    note = note,
                    snippet = buildBacklinkSnippet(note, rawLabel)
                )
            }
        }
    }

    suspend fun getSnapshots(noteId: String): List<NoteSnapshot> {
        return database.noteSnapshotDao().getSnapshotsForNote(noteId).map { it.toDomain() }
    }

    suspend fun getSnapshot(snapshotId: String): NoteSnapshot? {
        return database.noteSnapshotDao().getSnapshotById(snapshotId)?.toDomain()
    }

    suspend fun restoreSnapshot(snapshotId: String): Note? {
        val snapshot = database.noteSnapshotDao().getSnapshotById(snapshotId) ?: return null
        val current = database.noteDao().getNoteById(snapshot.noteId) ?: return null
        val now = System.currentTimeMillis()

        if (current.contentMarkdown != snapshot.contentMarkdown || current.title != snapshot.title) {
            database.noteSnapshotDao().insertSnapshot(
                NoteSnapshotEntity(
                    id = UUID.randomUUID().toString(),
                    noteId = current.id,
                    title = current.title,
                    contentMarkdown = current.contentMarkdown,
                    excerpt = current.excerpt,
                    createdAt = now
                )
            )
        }

        val restoredNote = current.copy(
            title = snapshot.title,
            contentMarkdown = snapshot.contentMarkdown,
            excerpt = snapshot.excerpt,
            updatedAt = now
        ).toDomain()
        database.noteDao().updateNote(restoredNote.toEntity())
        reindexNoteLinks(restoredNote)
        database.noteSnapshotDao().pruneSnapshotsForNote(current.id, maxSnapshotsPerNote)
        return restoredNote
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

    private suspend fun maybeSnapshotCurrentNote(nextNote: Note) {
        val current = database.noteDao().getNoteById(nextNote.id) ?: return
        if (!current.hasUserContentChange(nextNote)) return

        val snapshots = database.noteSnapshotDao().getSnapshotsForNote(nextNote.id)
        val latestSnapshot = snapshots.firstOrNull()
        val latestAlreadyMatchesCurrent = latestSnapshot?.contentMarkdown == current.contentMarkdown &&
            latestSnapshot.title == current.title
        val latestIsRecent = latestSnapshot != null &&
            current.updatedAt - latestSnapshot.createdAt < snapshotIntervalMillis

        if (latestAlreadyMatchesCurrent || latestIsRecent) return

        database.noteSnapshotDao().insertSnapshot(
            NoteSnapshotEntity(
                id = UUID.randomUUID().toString(),
                noteId = current.id,
                title = current.title,
                contentMarkdown = current.contentMarkdown,
                excerpt = current.excerpt,
                createdAt = current.updatedAt
            )
        )
        database.noteSnapshotDao().pruneSnapshotsForNote(current.id, maxSnapshotsPerNote)
    }

    private fun NoteEntity.hasUserContentChange(nextNote: Note): Boolean {
        return title != nextNote.title ||
            contentMarkdown != nextNote.contentMarkdown ||
            excerpt != nextNote.excerpt
    }

    private fun buildBacklinkSnippet(note: Note, rawLabel: String): String {
        val content = note.contentMarkdown.replace(Regex("\\s+"), " ").trim()
        val fallback = note.excerpt.ifBlank { content }.trim()
        if (content.isEmpty()) return fallback

        val candidates = listOf("[[$rawLabel]]", rawLabel).filter { it.isNotBlank() }
        val matchIndex = candidates
            .asSequence()
            .map { candidate -> content.indexOf(candidate, ignoreCase = true) }
            .firstOrNull { it >= 0 }
            ?: -1

        if (matchIndex < 0) return fallback.takeSnippet()

        val start = (matchIndex - snippetContextLength).coerceAtLeast(0)
        val end = (matchIndex + rawLabel.length + snippetContextLength).coerceAtMost(content.length)
        val prefix = if (start > 0) "..." else ""
        val suffix = if (end < content.length) "..." else ""
        return "$prefix${content.substring(start, end).trim()}$suffix"
    }

    private fun String.takeSnippet(): String {
        return if (length <= maxSnippetLength) this else take(maxSnippetLength).trimEnd() + "..."
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

    override suspend fun reorderNotes(notes: List<Note>) {
        notes.forEachIndexed { index, note ->
            database.noteDao().updateSortOrder(note.id, index)
        }
    }

    private companion object {
        const val snippetContextLength = 56
        const val maxSnippetLength = 140
    }
}
