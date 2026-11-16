package com.markleaf.notes.feature.notes

import com.markleaf.notes.data.settings.NotesSortMode
import com.markleaf.notes.domain.model.Note

/**
 * The order a sort mode stands for, on its own. Title comparisons ignore case;
 * untitled notes group together at the start of A→Z (and the end of Z→A) rather
 * than being special-cased.
 */
internal fun NotesSortMode.comparator(): Comparator<Note> = when (this) {
    NotesSortMode.UPDATED_DESC -> compareByDescending { it.updatedAt }
    NotesSortMode.UPDATED_ASC -> compareBy { it.updatedAt }
    NotesSortMode.CREATED_DESC -> compareByDescending { it.createdAt }
    NotesSortMode.CREATED_ASC -> compareBy { it.createdAt }
    NotesSortMode.TITLE_ASC -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
    NotesSortMode.TITLE_DESC -> compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.title }
}

/**
 * Orders the notes list for the top-bar sort menu (#191). Pinned notes always
 * come first — pinning is an explicit user intent that outranks any sort — and
 * the chosen order is applied within the pinned and unpinned groups alike.
 */
internal fun sortNotesForDisplay(notes: List<Note>, mode: NotesSortMode): List<Note> =
    notes.sortedWith(compareByDescending<Note> { it.pinned }.then(mode.comparator()))

/**
 * Orders the archive for its own sort menu (#592). Unlike the notes list, a
 * pinned note gets no head start: the archive shows no pin, so a note that was
 * pinned before it was archived would sit at the top for no visible reason.
 */
internal fun sortArchivedNotes(notes: List<Note>, mode: NotesSortMode): List<Note> =
    notes.sortedWith(mode.comparator())
