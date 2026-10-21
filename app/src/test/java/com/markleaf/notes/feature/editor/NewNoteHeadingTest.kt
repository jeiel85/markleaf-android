package com.markleaf.notes.feature.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * "Start new notes with a heading" (#544): which notes get the `# ` seed, and
 * that a note holding only the seed still counts as empty for the discard on
 * leaving (#405).
 */
class NewNoteHeadingTest {

    @Test
    fun `an empty note opens on the heading when the setting is on`() {
        assertEquals("# ", newNoteSeed(content = "", startWithHeading = true))
    }

    @Test
    fun `setting off leaves an empty note empty`() {
        assertNull(newNoteSeed(content = "", startWithHeading = false))
    }

    /** "Existing notes are not changed" — anything with text opens as stored. */
    @Test
    fun `a note with text is never seeded`() {
        assertNull(newNoteSeed(content = "Groceries", startWithHeading = true))
        assertNull(newNoteSeed(content = " ", startWithHeading = true))
        assertNull(newNoteSeed(content = "\n", startWithHeading = true))
    }

    @Test
    fun `the seed alone is an empty note`() {
        assertTrue(isEmptyNoteText("# "))
        assertTrue(isEmptyNoteText("#"))
        assertTrue(isEmptyNoteText("#\n\n"))
    }

    @Test
    fun `blank text is still an empty note`() {
        assertTrue(isEmptyNoteText(""))
        assertTrue(isEmptyNoteText("  \n "))
    }

    @Test
    fun `a heading with words is kept`() {
        assertFalse(isEmptyNoteText("# Groceries"))
        assertFalse(isEmptyNoteText("# \nmilk"))
        assertFalse(isEmptyNoteText("## "))
        assertFalse(isEmptyNoteText("#tag"))
    }
}
