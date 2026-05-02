package com.markleaf.notes.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.markleaf.notes.domain.model.NoteSnapshot
import java.time.Instant

@Entity(
    tableName = "note_snapshots",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("noteId"),
        Index(value = ["noteId", "createdAt"])
    ]
)
data class NoteSnapshotEntity(
    @PrimaryKey
    val id: String,
    val noteId: String,
    val title: String,
    val contentMarkdown: String,
    val excerpt: String,
    val createdAt: Long
)

fun NoteSnapshotEntity.toDomain(): NoteSnapshot {
    return NoteSnapshot(
        id = id,
        noteId = noteId,
        title = title,
        contentMarkdown = contentMarkdown,
        excerpt = excerpt,
        createdAt = Instant.ofEpochMilli(createdAt)
    )
}
