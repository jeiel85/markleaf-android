package com.markleaf.notes.domain.model

import java.time.Instant

data class Tag(
    val id: Long,
    val name: String,
    val normalizedName: String,
    val createdAt: Instant
)

data class TagSummary(
    val tag: Tag,
    val noteCount: Int
)
