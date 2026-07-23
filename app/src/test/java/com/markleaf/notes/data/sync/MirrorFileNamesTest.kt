package com.markleaf.notes.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-logic checks for [MirrorFileNames]. The regex only uses the ASCII POSIX
 * classes `\p{Cntrl}` / `\s`, which are identical on the JVM and on Android's
 * ICU-backed regex, so a JVM unit test faithfully reflects on-device behaviour
 * (no Unicode-property or locale-case divergence to worry about).
 */
class MirrorFileNamesTest {

    // --- #213: recognising a note's own file when its id has gone missing ----

    @Test
    fun `isPlainNameFor matches the undisambiguated name`() {
        assertTrue(MirrorFileNames.isPlainNameFor("Notes.md", "Notes"))
        assertTrue(MirrorFileNames.isPlainNameFor("Notes.txt", "Notes"))
    }

    @Test
    fun `isPlainNameFor rejects a disambiguated copy`() {
        // The whole point of the adoption fallback is to stop " (2)" files from
        // breeding — taking one over would defeat it.
        assertFalse(MirrorFileNames.isPlainNameFor("Notes (2).md", "Notes"))
        assertFalse(MirrorFileNames.isPlainNameFor("Notes (10).md", "Notes"))
    }

    @Test
    fun `isPlainNameFor ignores case`() {
        // A synced folder can land on exFAT or a Windows share, where notes.md
        // and Notes.md are the same file.
        assertTrue(MirrorFileNames.isPlainNameFor("NOTES.md", "Notes"))
    }

    @Test
    fun `isPlainNameFor keeps dots inside the title`() {
        assertTrue(MirrorFileNames.isPlainNameFor("v1.2 plan.md", "v1.2 plan"))
        assertFalse(MirrorFileNames.isPlainNameFor("v1.3 plan.md", "v1.2 plan"))
    }

    @Test
    fun `isPlainNameFor rejects an unrelated name`() {
        assertFalse(MirrorFileNames.isPlainNameFor("Other.md", "Notes"))
        assertFalse(MirrorFileNames.isPlainNameFor("", "Notes"))
    }

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
