package com.markleaf.notes.domain.model

import java.time.Instant

data class Tag(
    val id: String,
    val name: String,
    val normalizedName: String,
    val createdAt: Instant
)
