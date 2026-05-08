package com.markleaf.notes.data.repository

import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.local.entity.NoteLinkEntity
import com.markleaf.notes.data.local.entity.toDomain
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.util.WikilinkExtractor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Discovers and tracks `[[Title]]` wikilinks for a note.
 *
 * Same shape as [LocalTagRepository] — the editor calls
 * [reindexLinksForNote] each time auto-save fires, and the editor's
 * backlinks panel observes [observeBacklinks].
 */
class LocalNoteLinkRepository(
    private val database: AppDatabase
) {

    /**
     * Replace the source note's wikilink rows with the current set scanned
     * from [content]. Idempotent — calling twice with the same content yields
     * the same row set.
     */
    suspend fun reindexLinksForNote(sourceNoteId: String, content: String) {
        val dao = database.noteLinkDao()
        dao.clearForNote(sourceNoteId)
        val links = WikilinkExtractor.extract(content).mapIndexed { index, target ->
            NoteLinkEntity(
                sourceNoteId = sourceNoteId,
                targetTitle = target,
                normalizedTitle = WikilinkExtractor.normalize(target),
                position = index
            )
        }
        if (links.isNotEmpty()) {
            dao.insertAll(links)
        }
    }

    /**
     * Notes that contain a `[[…]]` link whose normalized text matches
     * [noteTitle]. The current note is filtered out so we never list a note
     * as backlinking to itself.
     */
    fun observeBacklinks(noteTitle: String, excludeNoteId: String): Flow<List<Note>> {
        val normalized = WikilinkExtractor.normalize(noteTitle)
        if (normalized.isEmpty()) {
            // No title yet (new untitled note) → nothing can link to it.
            return database.noteLinkDao().observeBacklinks("", excludeNoteId).map { emptyList() }
        }
        return database.noteLinkDao()
            .observeBacklinks(normalized, excludeNoteId)
            .map { entities -> entities.map { it.toDomain() } }
    }
}
