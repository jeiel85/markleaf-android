package com.markleaf.notes.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-logic checks for [MirrorFileNames]. The regex only uses the ASCII POSIX
 * classes `\p{Cntrl}` / `\s`, which are identical on the JVM and on Android's
 * ICU-backed regex, so a JVM unit test faithfully reflects on-device behaviour
 * (no Unicode-property or locale-case divergence to worry about).
 */
class MirrorFileNamesTest {

    @Test
    fun `plain title is kept as-is`() {
        assertEquals("My Note", MirrorFileNames.sanitizeBase("My Note"))
    }

    @Test
    fun `spaces and hyphens are preserved`() {
        assertEquals("Weekly Review - Q3", MirrorFileNames.sanitizeBase("Weekly Review - Q3"))
    }

    @Test
    fun `illegal characters become spaces and collapse`() {
        // / \ : * ? " < > | are all illegal on Windows/exFAT
        assertEquals("a b c", MirrorFileNames.sanitizeBase("a/b\\c"))
        assertEquals("path to file", MirrorFileNames.sanitizeBase("path:to?file"))
        assertEquals("quote and pipe", MirrorFileNames.sanitizeBase("quote\"and|pipe"))
    }

    @Test
    fun `control characters are stripped`() {
        assertEquals("tab here", MirrorFileNames.sanitizeBase("tab\there"))
    }

    @Test
    fun `leading and trailing whitespace and dots are trimmed`() {
        assertEquals("Note", MirrorFileNames.sanitizeBase("  Note  "))
        assertEquals("Note", MirrorFileNames.sanitizeBase("Note..."))
        assertEquals("Note", MirrorFileNames.sanitizeBase("Note. "))
    }

    @Test
    fun `empty or all-illegal title falls back to untitled`() {
        assertEquals("untitled", MirrorFileNames.sanitizeBase(""))
        assertEquals("untitled", MirrorFileNames.sanitizeBase("   "))
        assertEquals("untitled", MirrorFileNames.sanitizeBase("///"))
    }

    @Test
    fun `very long titles are capped`() {
        val long = "x".repeat(500)
        val result = MirrorFileNames.sanitizeBase(long)
        assertTrue(result.length <= MirrorFileNames.MAX_BASE_LENGTH)
    }

    @Test
    fun `windows reserved device names are escaped`() {
        assertEquals("_CON", MirrorFileNames.sanitizeBase("CON"))
        assertEquals("_com1", MirrorFileNames.sanitizeBase("com1"))
        // Reserved check is on the whole stem — a longer title is fine.
        assertEquals("CONtext", MirrorFileNames.sanitizeBase("CONtext"))
    }

    @Test
    fun `uniqueName returns plain name when free`() {
        assertEquals("My Note.md", MirrorFileNames.uniqueName("My Note", "md") { false })
    }

    @Test
    fun `uniqueName disambiguates collisions in order`() {
        val taken = mutableSetOf("note.md")
        val first = MirrorFileNames.uniqueName("note", "md") { it in taken }
        assertEquals("note (2).md", first)
        taken += "note (2).md"
        val second = MirrorFileNames.uniqueName("note", "md") { it in taken }
        assertEquals("note (3).md", second)
    }

    @Test
    fun `uniqueName honours the chosen extension`() {
        assertEquals("My Note.txt", MirrorFileNames.uniqueName("My Note", "txt") { false })
    }
}
