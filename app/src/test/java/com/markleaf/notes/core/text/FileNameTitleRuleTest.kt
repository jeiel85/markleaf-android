package com.markleaf.notes.core.text

import com.markleaf.notes.data.settings.AppSettings
import com.markleaf.notes.data.sync.MirrorFileNames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The File name title rule (D088, #429, #611): what a name may be, what a save
 * stores as the title, and what the rule does to the excerpt and to Keep file
 * names.
 */
class FileNameTitleRuleTest {

    private val fileName = NoteTitleSource.FILE_NAME

    @Test
    fun `a name the app accepts comes back from the folder unchanged`() {
        // The round trip the rule depends on: the title becomes the file name
        // through sanitizeBase, and the next import turns the file name back
        // into the title. Any name the field lets through must survive it.
        listOf("groceries", "Frankenstein", "Plan 2026-11", "노트", "a.b", "_CON", "\$HOME notes").forEach { typed ->
            val name = NoteNames.clean(NoteNames.filterTyped(typed))
            assertEquals(typed, name, MirrorFileNames.sanitizeBase(name))
            assertEquals(name, NoteNames.clean(NoteNames.fromFileName("$name.md")))
        }
    }

    @Test
    fun `characters a file name cannot hold never reach the name`() {
        assertEquals("ab cd", NoteNames.filterTyped("a/b\\ c:d*?\"<>|"))
        assertEquals("abcd", NoteNames.filterTyped("a/b:c*d"))
        assertEquals(NoteNames.MAX_LENGTH, NoteNames.filterTyped("x".repeat(500)).length)
    }

    @Test
    fun `trailing dots and spaces are left while typing and dropped on commit`() {
        assertEquals("notes. ", NoteNames.filterTyped("notes. "))
        assertEquals("notes", NoteNames.clean("notes. "))
    }

    @Test
    fun `a name another note has is refused ignoring case`() {
        assertEquals(NoteNames.Problem.TAKEN, NoteNames.problemWith("Groceries", listOf("groceries")))
        assertNull(NoteNames.problemWith("Groceries", listOf("Groceries list")))
    }

    @Test
    fun `a reserved device name is refused`() {
        assertEquals(NoteNames.Problem.RESERVED, NoteNames.problemWith(NoteNames.clean("con"), emptyList()))
        assertNull(NoteNames.problemWith(NoteNames.clean("console"), emptyList()))
    }

    @Test
    fun `a save keeps the stored name and does not read the body`() {
        assertEquals(
            "groceries",
            NoteTitles.forSave("groceries", "# Shopping for Saturday\n\nmilk", fileName, followsBody = false)
        )
    }

    @Test
    fun `a new note not yet named takes a file-safe first name from its body`() {
        assertEquals(
            "Plan A B",
            NoteTitles.forSave("Plan", "# Plan: A/B\n\nbody", fileName, followsBody = true)
        )
        // No name at all yet, as for a note that was empty: same as following.
        assertEquals("Ideas", NoteTitles.forSave("", "Ideas\nmore", fileName, followsBody = false))
    }

    @Test
    fun `the derived rules are unchanged`() {
        NoteTitleSource.entries.filterNot { it.isNamedByHand }.forEach { source ->
            assertEquals(
                TitleExtractor.extractTitle("# Plan\nbody", source),
                NoteTitles.forSave("stale", "# Plan\nbody", source, followsBody = false)
            )
        }
        assertEquals("Plan: A/B", TitleExtractor.extractTitle("# Plan: A/B", NoteTitleSource.FIRST_HEADING))
    }

    @Test
    fun `under File name the excerpt starts at the first line`() {
        // The name is not a line of the body, so no line is skipped as "the title".
        assertEquals("Chapter one\nIt was a dark night", TitleExtractor.generateExcerpt("# Chapter one\nIt was a dark night", fileName))
        assertEquals("It was a dark night", TitleExtractor.generateExcerpt("# Chapter one\nIt was a dark night"))
    }

    @Test
    fun `File name always keeps file names on a save`() {
        assertTrue(AppSettings(noteTitleSource = fileName).keepsFileNames)
        assertFalse(AppSettings().keepsFileNames)
        assertTrue(AppSettings(syncKeepFileNames = true).keepsFileNames)
    }

    @Test
    fun `a file called the wanted name means the folder took it`() {
        assertNull(NoteNames.nameTheFolderKept("groceries", "groceries.md"))
        assertNull(NoteNames.nameTheFolderKept("v1.2 notes", "v1.2 notes.txt"))
        assertEquals("shopping", NoteNames.nameTheFolderKept("groceries", "shopping.md"))
        // As the import sees it: capitals alone are not another name, and a
        // name the folder holds is made safe before it becomes the note's.
        assertNull(NoteNames.nameTheFolderKept("groceries", "Groceries.md"))
        assertEquals("a b", NoteNames.nameTheFolderKept("groceries", "a:b.md"))
    }
}
