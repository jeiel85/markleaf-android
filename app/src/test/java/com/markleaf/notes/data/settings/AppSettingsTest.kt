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
}
