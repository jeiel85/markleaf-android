package com.markleaf.notes.data.sync

import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.repository.LocalNoteLinkRepository
import com.markleaf.notes.data.repository.LocalNoteRepository
import com.markleaf.notes.data.repository.LocalTagRepository
import com.markleaf.notes.domain.model.Note

/**
 * Persists a note that arrived from the sync folder **and** reindexes everything
 * derived from its body — `#tags` and `[[wikilinks]]`.
 *
 * The editor's auto-save does this reindex on every save, but folder import used
 * to call only [LocalNoteRepository.createNote] / [LocalNoteRepository.updateNote],
 * so notes pulled in from a synced folder showed up with **none of their tags
 * recognised** until the user opened and re-saved each one (#138 follow-up). The
 * same gap left backlinks empty for imported notes.
 *
 * Centralised here so all three import call sites — foreground auto-reconcile,
 * the Sync Center, and Settings — stay in lockstep instead of each re-deriving
 * (and forgetting) the reindex step.
 */
class NoteImporter(
    private val notes: LocalNoteRepository,
    private val tags: LocalTagRepository,
    private val links: LocalNoteLinkRepository
) {
    constructor(database: AppDatabase) : this(
        LocalNoteRepository(database),
        LocalTagRepository(database),
        LocalNoteLinkRepository(database)
    )

    /** Insert an imported note and index its tags + links. */
    suspend fun create(note: Note) {
        notes.createNote(note)
        reindex(note)
    }

    /** Update an imported note and re-index its tags + links from the new body. */
    suspend fun update(note: Note) {
        notes.updateNote(note)
        reindex(note)
    }

    private suspend fun reindex(note: Note) {
        tags.reindexTagsForNote(note.id, note.contentMarkdown)
        links.reindexLinksForNote(note.id, note.contentMarkdown)
    }
}
