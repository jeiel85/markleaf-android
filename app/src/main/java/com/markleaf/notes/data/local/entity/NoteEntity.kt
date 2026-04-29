package com.markleaf.notes.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.markleaf.notes.domain.model.Note
import java.time.Instant

@Entity(tableName = "notes")
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
    val deletedAt: Long? = null
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
        tags = emptyList()
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
        deletedAt = deletedAt?.toEpochMilli()
    )
}
