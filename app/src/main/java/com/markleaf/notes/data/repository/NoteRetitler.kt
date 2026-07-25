package com.markleaf.notes.data.repository

import com.markleaf.notes.core.text.NoteTitleSource
import com.markleaf.notes.core.text.TitleExtractor
import com.markleaf.notes.domain.repository.NoteRepository

/**
 * Recompute stored titles and excerpts under a new title rule (#280).
 *
 * Titles are derived once, when a note is saved or imported, and then stored.
 * Without this pass, changing the rule would appear to do nothing until every
 * note had been edited again — the setting would read as broken.
 *
 * What it deliberately does *not* touch:
 *  - `updatedAt`, so no note jumps to the top of the list and the sync layer
 *    does not read a renamed note as an edited one.
 *  - Notes with no content, whose stored title is the empty string that the
 *    list renders as "Untitled". Deriving would write the literal word instead.
 *  - Conflict copies, whose title carries the "conflict copy" suffix that makes
 *    them findable; re-deriving from the body would erase it.
 *
 * Mirror filenames follow the title, but only when a note is next written.
 * *Sync Center → Tidy filenames* is the existing one-pass way to catch the
 * folder up, so this stays a database-only operation.
 */
object NoteRetitler {

    /** Retitle every stored note under [source]; returns how many changed. */
    suspend fun retitleAll(repository: NoteRepository, source: NoteTitleSource): Int {
        var changed = 0
        for (note in repository.getAllNotes()) {
            if (note.contentMarkdown.isBlank() || note.isConflictCopy) continue
            val title = TitleExtractor.extractTitle(note.contentMarkdown, source)
            val excerpt = TitleExtractor.generateExcerpt(note.contentMarkdown, source)
            if (title == note.title && excerpt == note.excerpt) continue
            repository.updateNote(note.copy(title = title, excerpt = excerpt))
            changed++
        }
        return changed
    }
}
