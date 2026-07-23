package com.markleaf.notes.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Where a note was last left, for the "Open notes at: where I left off" setting
 * (#214).
 *
 * Deliberately its own table rather than columns on `notes`. Reopening a note is
 * not editing it: a position written into the note row would ride along with
 * every `updateNote` call, and the one field the folder reconcile reads to
 * decide "has this note changed" is on that same row. Keeping the position out
 * here means recording it cannot touch `updatedAt`, cannot reorder the
 * recently-updated list, and cannot reach the mirror file — merely reading a
 * note must never look like an edit to whatever syncs the user's folder.
 *
 * The row is owned by the note and cascades away with it, so nothing needs to
 * remember to clean it up on delete.
 */
@Entity(
    tableName = "note_view_state",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class NoteViewStateEntity(
    /** Also the primary key — one position per note. Indexed as the PK, so the
     *  foreign key needs no index of its own. */
    @PrimaryKey
    val noteId: String,
    /**
     * Caret offset in the note's markdown. Stored as written and clamped on
     * read: the text can be shorter by the time the note is opened again — the
     * note may have been edited in another app, or a sync may have brought in a
     * smaller version — and an offset past the end must not be trusted.
     */
    val caretOffset: Int,
    /** Index of the rendered preview block that was in view. Clamped on read
     *  for the same reason. */
    val previewIndex: Int
)
