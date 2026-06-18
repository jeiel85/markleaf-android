package com.markleaf.notes.util

import com.markleaf.notes.domain.model.Note

object ExportUtil {
    fun generateFileName(note: Note): String {
        val baseName = if (note.title.isBlank()) "untitled" else note.title
        val slug = SlugGenerator.generateSlug(baseName)
        return "$slug.md"
    }
}
