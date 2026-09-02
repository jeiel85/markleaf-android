package com.markleaf.notes.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.local.entity.NoteEntity
import com.markleaf.notes.data.settings.EditorFontSize
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What the single-note widget actually puts on a home screen (#351).
 *
 * The Robolectric tests pin the rules — which notes may be drawn, what size the
 * text becomes — but they build `RemoteViews` and stop there. This binds a real
 * widget id through `AppWidgetService` and inflates what the launcher would
 * inflate, so the assertions are about the `TextView` a person would look at.
 *
 * Binding needs `BIND_APPWIDGET`, which no app can hold by declaring it:
 *
 * ```
 * adb shell appwidget grantbind --package com.markleaf.notes.debug --user 0
 * ```
 *
 * Without that grant the bind is refused and these are skipped rather than
 * failed — the code under test is fine, the harness simply is not allowed to
 * stand in for a launcher.
 */
@RunWith(AndroidJUnit4::class)
class SingleNoteWidgetRenderTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manager = AppWidgetManager.getInstance(context)
    private val host = AppWidgetHost(context, HOST_ID)

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private val noteId = "single-note-widget-render-test"

    @Before
    fun setUp() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { host.startListening() }
        appWidgetId = host.allocateAppWidgetId()
        val bound = manager.bindAppWidgetIdIfAllowed(
            appWidgetId,
            ComponentName(context, SingleNoteWidget::class.java)
        )
        assumeTrue("BIND_APPWIDGET not granted — see the class comment", bound)
    }

    @After
    fun tearDown() {
        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            host.deleteAppWidgetId(appWidgetId)
            SingleNoteWidgetStore.forget(context, intArrayOf(appWidgetId))
        }
        InstrumentationRegistry.getInstrumentation().runOnMainSync { host.stopListening() }
        runBlocking { AppDatabase.getInstance(context).noteDao().deleteForever(noteId) }
    }

    @Test
    fun theChosenNotesBodyIsDrawnAtTheChosenSize() {
        seedNote(locked = false)
        SingleNoteWidgetStore.save(context, appWidgetId, noteId, EditorFontSize.EXTRA_LARGE)

        val body = renderBody()

        assertEquals(BODY, body.text.toString())
        assertEquals(
            SingleNoteWidgetStore.bodySizeSp(EditorFontSize.EXTRA_LARGE),
            body.textSize / body.resources.displayMetrics.scaledDensity,
            0.05f
        )
    }

    /**
     * The guard that matters: a home screen is visible without unlocking
     * anything, so a note moved into the Locked space has to stop rendering —
     * not keep showing the text it had when it was chosen.
     */
    @Test
    fun aNoteLockedAfterItWasChosenStopsShowingItsText() {
        seedNote(locked = false)
        SingleNoteWidgetStore.save(context, appWidgetId, noteId, EditorFontSize.MEDIUM)
        assertEquals(BODY, renderBody().text.toString())

        runBlocking {
            AppDatabase.getInstance(context).noteDao().setLocked(noteId, true)
        }
        val body = renderBody()

        assertEquals(
            context.getString(com.markleaf.notes.R.string.single_note_widget_unavailable),
            body.text.toString()
        )
    }

    private fun renderBody(): TextView {
        SingleNoteWidget.updateAppWidget(context, manager, appWidgetId)
        var found: TextView? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val hostView = host.createView(context, appWidgetId, manager.getAppWidgetInfo(appWidgetId))
            hostView.measure(
                View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY)
            )
            found = hostView.firstTextView()
        }
        return requireNotNull(found) { "The widget inflated without a TextView" }
    }

    private fun View.firstTextView(): TextView? = when (this) {
        is TextView -> this
        is ViewGroup -> (0 until childCount).firstNotNullOfOrNull { getChildAt(it).firstTextView() }
        else -> null
    }

    private fun seedNote(locked: Boolean) = runBlocking {
        AppDatabase.getInstance(context).noteDao().insertNote(
            NoteEntity(
                id = noteId,
                title = "Render test",
                contentMarkdown = BODY,
                excerpt = "line one",
                createdAt = 0L,
                updatedAt = 0L,
                locked = locked
            )
        )
        assertNotNull(AppDatabase.getInstance(context).noteDao().getNoteById(noteId))
    }

    private companion object {
        const val HOST_ID = 0x4D4C
        const val BODY = "line one\nline two"
    }
}
