package com.markleaf.notes.domain.model

import java.time.Instant

data class Note(
    val id: String,
    val title: String,
    val contentMarkdown: String,
    val excerpt: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val trashed: Boolean = false,
    val deletedAt: Instant? = null,
    val tags: List<Tag> = emptyList()
)
