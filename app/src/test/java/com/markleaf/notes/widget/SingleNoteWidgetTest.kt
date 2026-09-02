package com.markleaf.notes.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.markleaf.notes.MainActivity
import com.markleaf.notes.R
import com.markleaf.notes.data.local.entity.NoteEntity
import com.markleaf.notes.data.settings.EditorFontSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The single-note widget's two rules (#351): which notes may be drawn on a home
 * screen, and how the chosen text size becomes a size in sp.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SingleNoteWidgetTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `an ordinary note contributes its body`() {
        assertEquals("# Groceries\n\n- milk", SingleNoteWidget.showableBody(note()))
    }

    /**
     * The home screen is visible without unlocking anything, so a note moved into
     * the passcode-gated space after it was chosen must stop rendering rather
     * than keep showing the text it had when it was picked.
     */
    @Test
    fun `a locked note contributes nothing`() {
        assertNull(SingleNoteWidget.showableBody(note(locked = true)))
    }

    @Test
    fun `a trashed note contributes nothing`() {
        assertNull(SingleNoteWidget.showableBody(note(trashed = true)))
    }

    @Test
    fun `a deleted note contributes nothing`() {
        assertNull(SingleNoteWidget.showableBody(null))
    }

    @Test
    fun `text size scales the widget body around the medium default`() {
        assertEquals(
            SingleNoteWidgetStore.BASE_TEXT_SIZE_SP,
            SingleNoteWidgetStore.bodySizeSp(EditorFontSize.MEDIUM),
            0.001f
        )
        assertEquals(12.25f, SingleNoteWidgetStore.bodySizeSp(EditorFontSize.SMALL), 0.001f)
        assertEquals(15.75f, SingleNoteWidgetStore.bodySizeSp(EditorFontSize.LARGE), 0.001f)
        assertEquals(17.5f, SingleNoteWidgetStore.bodySizeSp(EditorFontSize.EXTRA_LARGE), 0.001f)
    }

    /**
     * A widget tap has to land in the same place a recent-notes row does —
     * `MainActivity` already handles this action, including when it is already
     * running, so the widget adds no second way of opening a note.
     */
    @Test
    fun `tapping the widget opens its note through the shared entry point`() {
        val intent = SingleNoteWidget.openNoteIntent(context, "note-1")

        assertEquals(QuickNoteWidget.ACTION_OPEN_NOTE, intent.action)
        assertEquals("note-1", intent.getStringExtra(QuickNoteWidget.EXTRA_NOTE_ID))
        assertEquals(
            MainActivity::class.java.name,
            intent.component?.className
        )
    }

    /**
     * A smoke test of the same shape as [QuickNoteWidgetTest], and with the same
     * reach: it exercises the whole of `updateAppWidget` — including an
     * unconfigured widget, which is the state every widget is in for the moment
     * between being dropped and the picker returning.
     */
    @Test
    fun `building the widget views does not throw`() {
        val manager = AppWidgetManager.getInstance(context)
        val ids = shadowOf(manager)
            .createWidgets(SingleNoteWidget::class.java, R.layout.widget_single_note, 1)

        SingleNoteWidget.updateAppWidget(context, manager, ids.first())
    }

    private fun note(locked: Boolean = false, trashed: Boolean = false) = NoteEntity(
        id = "note-1",
        title = "Groceries",
        contentMarkdown = "# Groceries\n\n- milk",
        excerpt = "- milk",
        createdAt = 0L,
        updatedAt = 0L,
        locked = locked,
        trashed = trashed
    )
}
