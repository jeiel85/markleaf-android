package com.markleaf.notes.feature.editor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Which mode a note opens in when "open notes in preview" is on (#200).
 *
 * The rule lived inline in the editor's load effect, where the only thing
 * exercising it was someone opening a note on a device (#204). It is one
 * boolean and one emptiness check, and getting it wrong is not subtle — a new
 * note that opens in preview has no editor and no keyboard, so the FAB appears
 * to do nothing.
 */
class OpenModeTest {

    @Test
    fun `setting off always opens in edit`() {
        assertFalse(opensInPreview(openNotesInPreview = false, content = "# A note"))
        assertFalse(opensInPreview(openNotesInPreview = false, content = ""))
    }

    @Test
    fun `an existing note opens in preview when the setting is on`() {
        assertTrue(opensInPreview(openNotesInPreview = true, content = "# A note"))
    }

    /**
     * The case the guard exists for. The FAB creates the note first and opens
     * it with a real id, so the id cannot distinguish a new note from an
     * existing one — only its content can.
     */
    @Test
    fun `a just-created note opens in edit even with the setting on`() {
        assertFalse(opensInPreview(openNotesInPreview = true, content = ""))
    }

    /**
     * Whitespace is content: the user typed it, and a note holding a newline
     * has something the preview can render. Emptiness is the "brand new"
     * signal, not blankness.
     */
    @Test
    fun `whitespace counts as content`() {
        assertTrue(opensInPreview(openNotesInPreview = true, content = "\n"))
        assertTrue(opensInPreview(openNotesInPreview = true, content = " "))
    }
}
