package com.markleaf.notes.util

import com.markleaf.notes.domain.model.Note

object ExportUtil {
    fun generateMarkdownContent(note: Note): String {
        val title = if (note.title.isBlank()) "Untitled" else note.title
        return "# $title\n\n${note.contentMarkdown}"
    }

    fun generateFileName(note: Note): String {
        val baseName = if (note.title.isBlank()) "untitled" else note.title
        val slug = SlugGenerator.generateSlug(baseName)
        return "$slug.md"
    }
}
