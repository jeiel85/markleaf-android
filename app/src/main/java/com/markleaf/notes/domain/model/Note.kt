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
    val tags: List<Tag> = emptyList(),
    val sortOrder: Int = 0,
    /** Set by [com.markleaf.notes.data.sync.NoteFolderMirror] when this note's
     *  body was last copied in from the sync folder. Used as the conflict
     *  baseline: if [updatedAt] is later than this, the local copy has been
     *  edited since the last sync and an incoming newer file is treated as a
     *  conflict (kept as a duplicate) rather than silently overwriting. */
    val lastImportedAt: Instant? = null
)
