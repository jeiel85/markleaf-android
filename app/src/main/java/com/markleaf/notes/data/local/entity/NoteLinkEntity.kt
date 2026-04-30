package com.markleaf.notes.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "note_links",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceNoteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sourceNoteId"), Index("targetNoteId")]
)
data class NoteLinkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceNoteId: String,
    val targetNoteId: String?, // Null if the target note doesn't exist yet
    val rawLabel: String,      // The text inside [[...]]
    val createdAt: Long = System.currentTimeMillis()
)
