package com.markleaf.notes.util

import com.markleaf.notes.domain.model.Note
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ExportAllNotesTest {
    private fun note(id: String, body: String, locked: Boolean = false, trashed: Boolean = false) =
        Note(id, id, body, "", Instant.EPOCH, Instant.EPOCH, locked = locked, trashed = trashed)

    @Test
    fun parentTagIncludesDescendantsButNotSimilarPrefixes() {
        val notes = listOf(
            note("parent", "#project"),
            note("child", "#project/alpha"),
            note("other", "#projects"),
            note("heading", "# Heading")
        )

        assertEquals(listOf("parent", "child"),
            ExportAllNotes.selectNotes(notes, "PROJECT").map { it.id })
    }

    @Test
    fun bulkExportExcludesLockedAndTrashedNotes() {
        val notes = listOf(
            note("visible", "#project"),
            note("locked", "#project", locked = true),
            note("trashed", "#project", trashed = true)
        )

        assertEquals(listOf("visible"), ExportAllNotes.selectNotes(notes).map { it.id })
        assertEquals(listOf("visible"), ExportAllNotes.selectNotes(notes, "project").map { it.id })
    }
}
