package com.markleaf.notes.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.markleaf.notes.MainActivity
import com.markleaf.notes.R
import com.markleaf.notes.data.local.entity.NoteEntity
import com.markleaf.notes.data.settings.EditorFontSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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

    /**
     * `RemoteViews` crosses a Binder transaction with a payload cap around 1 MB
     * for the whole process, and an imported note may hold `ExternalFile.MAX_CHARS`
     * — two million characters. Handing that over makes the update throw
     * `TransactionTooLargeException` instead of drawing, so the body is cut long
     * after the last line any widget could show.
     */
    @Test
    fun `a very long note is cut before it reaches the parcel`() {
        val huge = note().copy(contentMarkdown = "x".repeat(2_000_000))

        val body = SingleNoteWidget.showableBody(huge)

        assertEquals(SingleNoteWidget.MAX_BODY_CHARS, body?.length)
    }

    @Test
    fun `a note shorter than the cap is untouched`() {
        assertEquals("# Groceries\n\n- milk", SingleNoteWidget.showableBody(note()))
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

    // --- rows (#371) -------------------------------------------------------
    //
    // The body scrolls as a list, so what the widget draws is no longer one
    // string but a sequence of rows. These pin the row rules; that a ListView
    // is what holds them is checked on a device, where a launcher can bind it.

    @Test
    fun `each line of the note becomes its own row`() {
        val rows = SingleNoteWidget.bodyRows(note().copy(contentMarkdown = "one\ntwo\nthree"))

        assertEquals(listOf("one", "two", "three"), rows)
    }

    /**
     * A row holding nothing can collapse, which runs paragraphs together — a
     * scrolling widget that reads worse than the clipped one it replaced.
     */
    @Test
    fun `a blank line keeps its height`() {
        val rows = SingleNoteWidget.bodyRows(note().copy(contentMarkdown = "one\n\ntwo"))

        assertEquals(listOf("one", SingleNoteWidget.BLANK_LINE, "two"), rows)
    }

    /** A file ending in a newline would otherwise scroll past its last word. */
    @Test
    fun `trailing blank lines are not drawn`() {
        val rows = SingleNoteWidget.bodyRows(note().copy(contentMarkdown = "one\ntwo\n\n\n"))

        assertEquals(listOf("one", "two"), rows)
    }

    /**
     * Rows are lines, not fixed-size chunks: a long line wraps inside its own
     * row. Splitting it by length would cut a sentence at a row boundary.
     */
    @Test
    fun `a long line stays one row`() {
        val line = "x".repeat(5_000)

        assertEquals(listOf(line), SingleNoteWidget.bodyRows(note().copy(contentMarkdown = line)))
    }

    @Test
    fun `carriage returns do not survive into a row`() {
        val rows = SingleNoteWidget.bodyRows(note().copy(contentMarkdown = "one\r\ntwo"))

        assertEquals(listOf("one", "two"), rows)
    }

    /**
     * Both caps end the same way — a row saying the text goes on — so a reader
     * who reaches the bottom can tell a cut note from a finished one.
     */
    @Test
    fun `a note cut by the character cap ends in the truncation marker`() {
        val rows = SingleNoteWidget.bodyRows(note().copy(contentMarkdown = "x".repeat(2_000_000)))

        assertEquals(
            listOf("x".repeat(SingleNoteWidget.MAX_BODY_CHARS), SingleNoteWidget.TRUNCATION_MARKER),
            rows
        )
    }

    @Test
    fun `a note cut by the line cap ends in the truncation marker`() {
        val many = (1..SingleNoteWidget.MAX_BODY_LINES + 50).joinToString("\n") { "line $it" }

        val rows = SingleNoteWidget.bodyRows(note().copy(contentMarkdown = many))

        assertEquals(SingleNoteWidget.MAX_BODY_LINES + 1, rows.size)
        assertEquals(SingleNoteWidget.TRUNCATION_MARKER, rows.last())
    }

    @Test
    fun `a note that fits carries no truncation marker`() {
        assertEquals(
            listOf("# Groceries", SingleNoteWidget.BLANK_LINE, "- milk"),
            SingleNoteWidget.bodyRows(note())
        )
    }

    /**
     * The four "nothing to show" states collapse into one: no rows, so the
     * empty view renders and there is no second rule per state.
     */
    @Test
    fun `a locked, trashed, deleted or blank note yields no rows`() {
        assertEquals(emptyList<String>(), SingleNoteWidget.bodyRows(note(locked = true)))
        assertEquals(emptyList<String>(), SingleNoteWidget.bodyRows(note(trashed = true)))
        assertEquals(emptyList<String>(), SingleNoteWidget.bodyRows(null))
        assertEquals(
            emptyList<String>(),
            SingleNoteWidget.bodyRows(note().copy(contentMarkdown = "  \n\n"))
        )
    }

    /**
     * `RemoteViewsService` keys its factories by intent and ignores extras, so
     * two single-note widgets sharing one intent would share a factory — and the
     * second placed would quietly render the first one's note.
     */
    @Test
    fun `each widget gets its own adapter intent`() {
        val first = SingleNoteWidget.factoryIntent(context, 11)
        val second = SingleNoteWidget.factoryIntent(context, 12)

        assertNotEquals(first.data, second.data)
        assertFalse(first.filterEquals(second))
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
