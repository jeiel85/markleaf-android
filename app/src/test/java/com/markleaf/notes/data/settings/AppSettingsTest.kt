package com.markleaf.notes.data.settings

import com.markleaf.notes.core.text.NoteTitleSource
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
     * The grid (#279) and the title rule (#280) are both choices laid over
     * behaviour people already have. A default of anything else would rearrange
     * one user's list and rename another user's notes on update.
     */
    @Test
    fun notesStillRenderAsAListTitledByTheFirstHeading() {
        val settings = AppSettings()

        assertEquals(NotesLayout.LIST, settings.notesLayout)
        assertEquals(NoteTitleSource.FIRST_HEADING, settings.noteTitleSource)
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

    /**
     * Both appearance additions (#345, #346) default to exactly what the app
     * already did — follow the system dark mode, render at scale 1.0. Anything
     * else would change the app under every existing user on update.
     */
    @Test
    fun appearanceDefaultsMatchThePreExistingBehaviour() {
        val settings = AppSettings()

        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertEquals(EditorFontSize.MEDIUM, settings.editorFontSize)
        assertEquals(1.0f, settings.editorFontSize.scale)
    }
}
