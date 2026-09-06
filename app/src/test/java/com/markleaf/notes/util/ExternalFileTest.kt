package com.markleaf.notes.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * The two rules that decide what an outside file becomes (#139, #326): the name
 * seeds a title only when the text does not already carry one, and no file — not
 * even one that turns out to be a gigabyte of binary — is read without a ceiling.
 */
class ExternalFileTest {

    @Test
    fun `file name becomes the title when the text has none`() {
        val body = ExternalFile.noteBody("Bought milk and eggs.", "shopping list.md")

        assertEquals("# shopping list\n\nBought milk and eggs.", body)
    }

    @Test
    fun `a file that already opens with a heading is left alone`() {
        val text = "# Weekly review\n\nWhat went well…"

        assertEquals(text, ExternalFile.noteBody(text, "notes-2026-08-20.md"))
    }

    @Test
    fun `frontmatter counts as titling itself`() {
        // Prepending a heading above `---` would break the block for every other
        // tool that reads it, Markleaf's own sync included.
        val text = "---\ntitle: Retro\n---\n\nBody"

        assertEquals(text, ExternalFile.noteBody(text, "retro.md"))
    }

    @Test
    fun `a name without an extension still seeds the title`() {
        assertEquals("# README\n\nHello", ExternalFile.noteBody("Hello", "README"))
    }

    @Test
    fun `only the last extension is dropped`() {
        assertEquals("# notes.backup\n\nHello", ExternalFile.noteBody("Hello", "notes.backup.md"))
    }

    @Test
    fun `an unnamed file keeps the text as it is`() {
        assertEquals("Hello", ExternalFile.noteBody("Hello", null))
        assertEquals("Hello", ExternalFile.noteBody("Hello", "   "))
    }

    @Test
    fun `reading stops at the cap`() {
        val huge = "x".repeat(ExternalFile.MAX_CHARS + 5_000)

        val read = ExternalFile.readCapped(huge.byteInputStream())

        assertEquals(ExternalFile.MAX_CHARS, read.length)
    }

    @Test
    fun `a file shorter than the cap is read whole`() {
        val text = "# Title\n\nA few kilobytes at most, like every real note file."

        assertEquals(text, ExternalFile.readCapped(text.byteInputStream()))
    }

    @Test
    fun `text is decoded as UTF-8`() {
        val text = "# 회의록\n\n다국어 파일도 열린다 — ünicode, 日本語, 中文."

        assertTrue(ExternalFile.readCapped(text.byteInputStream()) == text)
    }

    @Test
    fun `ordinary files use the source modification time for both note dates`() {
        val modified = Instant.parse("2026-08-20T10:15:30Z")

        val seed = ExternalFile.noteSeed(
            ExternalFile.Document("notes.md", "A note", lastModifiedAt = modified)
        )

        assertEquals(modified, seed.createdAt)
        assertEquals(modified, seed.updatedAt)
    }

    @Test
    fun `Markleaf frontmatter preserves both explicit note dates`() {
        val created = Instant.parse("2026-08-19T10:15:30Z")
        val updated = Instant.parse("2026-08-20T10:15:30Z")
        val text = """
            ---
            markleaf_id: note-1
            created_at: $created
            updated_at: $updated
            ---

            # Body
        """.trimIndent()

        val seed = ExternalFile.noteSeed(
            ExternalFile.Document("notes.md", text, lastModifiedAt = Instant.now())
        )

        assertEquals(created, seed.createdAt)
        assertEquals(updated, seed.updatedAt)
    }
}
