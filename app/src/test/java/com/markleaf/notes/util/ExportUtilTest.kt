package com.markleaf.notes.util

import com.markleaf.notes.domain.model.Note
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ExportUtilTest {

    @Test
    fun `generateMarkdownContent includes title as heading`() {
        val note = Note(
            id = "1",
            title = "Test Note",
            contentMarkdown = "Content here",
            excerpt = "",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val markdown = ExportUtil.generateMarkdownContent(note)
        assert(markdown.startsWith("# Test Note"))
    }

    @Test
    fun `generateMarkdownContent handles blank title`() {
        val note = Note(
            id = "1",
            title = "",
            contentMarkdown = "Content here",
            excerpt = "",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val markdown = ExportUtil.generateMarkdownContent(note)
        assert(markdown.startsWith("# Untitled"))
    }

    @Test
    fun `generateFileName uses slugified title`() {
        val note = Note(
            id = "1",
            title = "Test Note",
            contentMarkdown = "Content",
            excerpt = "",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val fileName = ExportUtil.generateFileName(note)
        assertEquals("test-note.md", fileName)
    }

    @Test
    fun `generateFileName handles blank title`() {
        val note = Note(
            id = "1",
            title = "",
            contentMarkdown = "Content",
            excerpt = "",
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val fileName = ExportUtil.generateFileName(note)
        assertEquals("untitled.md", fileName)
    }
}
