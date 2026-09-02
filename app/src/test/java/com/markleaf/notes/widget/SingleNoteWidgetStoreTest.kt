package com.markleaf.notes.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.markleaf.notes.data.settings.EditorFontSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Per-widget placement state for [SingleNoteWidget] (#351). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SingleNoteWidgetStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `an unconfigured widget has no note and the default size`() {
        assertNull(SingleNoteWidgetStore.noteId(context, 1))
        assertEquals(EditorFontSize.MEDIUM, SingleNoteWidgetStore.textSize(context, 1))
    }

    @Test
    fun `each widget keeps its own note and size`() {
        SingleNoteWidgetStore.save(context, 1, "note-a", EditorFontSize.SMALL)
        SingleNoteWidgetStore.save(context, 2, "note-b", EditorFontSize.EXTRA_LARGE)

        assertEquals("note-a", SingleNoteWidgetStore.noteId(context, 1))
        assertEquals(EditorFontSize.SMALL, SingleNoteWidgetStore.textSize(context, 1))
        assertEquals("note-b", SingleNoteWidgetStore.noteId(context, 2))
        assertEquals(EditorFontSize.EXTRA_LARGE, SingleNoteWidgetStore.textSize(context, 2))
    }

    /**
     * Launcher ids are reused, so a removed widget that left its note behind
     * would hand that note to whatever is placed next.
     */
    @Test
    fun `removing a widget forgets its note`() {
        SingleNoteWidgetStore.save(context, 1, "note-a", EditorFontSize.LARGE)
        SingleNoteWidgetStore.save(context, 2, "note-b", EditorFontSize.LARGE)

        SingleNoteWidgetStore.forget(context, intArrayOf(1))

        assertNull(SingleNoteWidgetStore.noteId(context, 1))
        assertEquals(EditorFontSize.MEDIUM, SingleNoteWidgetStore.textSize(context, 1))
        assertEquals("note-b", SingleNoteWidgetStore.noteId(context, 2))
    }

    /**
     * A size written by a build that had different names — or a corrupted file —
     * must not stop the widget rendering.
     */
    @Test
    fun `an unrecognised stored size falls back to the default`() {
        context.getSharedPreferences("single_note_widget", Context.MODE_PRIVATE)
            .edit()
            .putString("text_size_7", "GIGANTIC")
            .apply()

        assertEquals(EditorFontSize.MEDIUM, SingleNoteWidgetStore.textSize(context, 7))
    }
}
