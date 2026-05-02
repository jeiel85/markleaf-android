package com.markleaf.notes.domain.model

import java.time.Instant

data class NoteSnapshot(
    val id: String,
    val noteId: String,
    val title: String,
    val contentMarkdown: String,
    val excerpt: String,
    val createdAt: Instant
)
