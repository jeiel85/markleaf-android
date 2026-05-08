package com.markleaf.notes.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tracks image (and other) attachments referenced from a note's markdown.
 * The actual file lives at `<appFiles>/attachments/<noteId>/<id>.<ext>` —
 * this row only carries metadata so we can:
 *   • clean up orphan files when a note is permanently deleted
 *   • surface attachment counts / export them with the note
 *
 * Markdown body references attachments via relative paths
 * (e.g. `![](attachments/abc-123/xyz.png)`) that the renderer resolves
 * against `context.filesDir`. No INTERNET permission, no media permission —
 * SAF picks the source file and we copy it into our private storage.
 */
@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["noteId"])
    ]
)
data class AttachmentEntity(
    @PrimaryKey
    val id: String,
    val noteId: String,
    val fileName: String,
    val mimeType: String,
    val addedAt: Long
)
