package com.markleaf.notes.util

import com.markleaf.notes.domain.model.Note
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ExportUtilTest {

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
