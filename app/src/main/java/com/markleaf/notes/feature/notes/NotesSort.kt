package com.markleaf.notes.feature.notes

import com.markleaf.notes.data.settings.NotesSortMode
import com.markleaf.notes.domain.model.Note

/**
 * Orders the notes list for the top-bar sort menu (#191). Pinned notes always
 * come first — pinning is an explicit user intent that outranks any sort — and
 * the chosen order is applied within the pinned and unpinned groups alike.
 *
 * Title comparisons ignore case; untitled notes group together at the start of
 * A→Z (and the end of Z→A) rather than being special-cased.
 */
internal fun sortNotesForDisplay(notes: List<Note>, mode: NotesSortMode): List<Note> {
    val comparator = when (mode) {
        NotesSortMode.UPDATED_DESC -> compareByDescending<Note> { it.updatedAt }
        NotesSortMode.UPDATED_ASC -> compareBy<Note> { it.updatedAt }
        NotesSortMode.TITLE_ASC -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
        NotesSortMode.TITLE_DESC ->
            compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.title }
    }
    return notes.sortedWith(
        compareByDescending<Note> { it.pinned }.then(comparator)
    )
}
