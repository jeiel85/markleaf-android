package com.markleaf.notes.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.markleaf.notes.domain.model.Note
import java.time.Instant

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["trashed", "pinned", "sortOrder"]),
        Index(value = ["trashed", "deletedAt"]),
        Index(value = ["title", "trashed"]),
        Index(value = ["sortOrder"])
    ]
)
data class NoteEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val contentMarkdown: String,
    val excerpt: String,
    val createdAt: Long,
    val updatedAt: Long,
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val trashed: Boolean = false,
    val deletedAt: Long? = null,
    val sortOrder: Int = 0,
    val lastImportedAt: Long? = null,
    /** Newest mirror-file timestamp the reconcile has resolved for this note.
     *  Keeps conflict-settling bookkeeping out of `updatedAt`, which is the
     *  user's own edit time and orders the notes list. */
    val remoteSeenAt: Long? = null,
    /** When true the note lives in the passcode-gated "Locked notes" space and is
     *  hidden from every normal list, search, tag and export path. This is a
     *  UI-visibility gate — the body still sits in the same Room DB in plain
     *  text, not encrypted at rest. See [com.markleaf.notes.feature.lock]. */
    val locked: Boolean = false,
    /** When true this note is the remote side of a sync conflict, kept next to
     *  the local note for manual merge. Replaces the old detection-by-title-
     *  substring, which pinned the label to one hardcoded language. */
    val isConflictCopy: Boolean = false
)

fun NoteEntity.toDomain(): Note {
    return Note(
        id = id,
        title = title,
        contentMarkdown = contentMarkdown,
        excerpt = excerpt,
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
        pinned = pinned,
        archived = archived,
        trashed = trashed,
        deletedAt = deletedAt?.let { Instant.ofEpochMilli(it) },
        tags = emptyList(),
        sortOrder = sortOrder,
        lastImportedAt = lastImportedAt?.let { Instant.ofEpochMilli(it) },
        remoteSeenAt = remoteSeenAt?.let { Instant.ofEpochMilli(it) },
        locked = locked,
        isConflictCopy = isConflictCopy
    )
}

fun Note.toEntity(): NoteEntity {
    return NoteEntity(
        id = id,
        title = title,
        contentMarkdown = contentMarkdown,
        excerpt = excerpt,
        createdAt = createdAt.toEpochMilli(),
        updatedAt = updatedAt.toEpochMilli(),
        pinned = pinned,
        archived = archived,
        trashed = trashed,
        deletedAt = deletedAt?.toEpochMilli(),
        sortOrder = sortOrder,
        lastImportedAt = lastImportedAt?.toEpochMilli(),
        remoteSeenAt = remoteSeenAt?.toEpochMilli(),
        locked = locked,
        isConflictCopy = isConflictCopy
    )
}
