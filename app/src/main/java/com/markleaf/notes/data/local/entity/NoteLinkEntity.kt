package com.markleaf.notes.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Cross-reference of `[[Title]]` wikilinks discovered in note bodies.
 * One row per `[[Title]]` occurrence in [sourceNoteId]'s `contentMarkdown`.
 *
 * The link is stored by *normalized title*, not by resolved note id, because
 * the target note may not exist yet (Bear/Obsidian convention: clicking a
 * broken link offers to create it). When the target note is renamed the
 * link is automatically dangling — that's intentional, and matches the
 * filesystem-friendly mental model of v2.1's folder mirror.
 */
@Entity(
    tableName = "note_links",
    primaryKeys = ["sourceNoteId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceNoteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sourceNoteId"]),
        Index(value = ["normalizedTitle"])
    ]
)
data class NoteLinkEntity(
    val sourceNoteId: String,
    val targetTitle: String,
    val normalizedTitle: String,
    val position: Int
)
