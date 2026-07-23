package com.markleaf.notes.data.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {
    @Test
    fun defaultSettingsUseVisibleMarkdownAndComfortableLineWidth() {
        val settings = AppSettings()

        assertEquals(MarkdownSyntaxVisibility.SHOW, settings.markdownSyntaxVisibility)
        assertEquals(EditorLineWidth.COMFORTABLE, settings.lineWidth)
        assertEquals(800, settings.lineWidth.maxWidthDp)
    }

    /** The list/search additions from #188/#191/#192/#193 must all default to
     *  the pre-existing behaviour so an update changes nothing until opted into. */
    @Test
    fun defaultSettingsKeepPreExistingListAndSearchBehaviour() {
        val settings = AppSettings()

        assertTrue(settings.notesShowPreview)
        assertFalse(settings.reopenLastNote)
        assertEquals(NotesSortMode.UPDATED_DESC, settings.notesSortMode)
        assertFalse(settings.searchTitlesOnly)
        assertNull(settings.lastOpenedNoteId)
    }

    /**
     * "Open notes at" was asked for by someone who wanted the bottom (#214) —
     * but it was added as an option precisely so nobody else's habits change.
     * A default of anything but [OpenNotesAt.TOP] would move every existing
     * user's notes under them on update.
     */
    @Test
    fun notesStillOpenAtTheTopByDefault() {
        assertEquals(OpenNotesAt.TOP, AppSettings().openNotesAt)
    }
}
