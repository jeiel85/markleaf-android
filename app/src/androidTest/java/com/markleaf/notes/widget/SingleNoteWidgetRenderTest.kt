package com.markleaf.notes.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.ParcelFileDescriptor
import android.util.TypedValue
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
 * Binding needs `BIND_APPWIDGET`, which no app can hold by declaring it, so the
 * test grants it to itself through the instrumentation's shell. Doing that from
 * the CI workflow is not an option here — the managed device is created and torn
 * down inside the Gradle task, with no `adb` step in between — and without the
 * grant these tests skip, which is worse than not having them: a skipped test
 * looks green while guarding nothing.
 *
 * The `assumeTrue` below is the honest fallback for a device where even that is
 * refused; if it ever fires in CI, the tests are inert and the message says so.
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
        grantBindPermission()
        InstrumentationRegistry.getInstrumentation().runOnMainSync { host.startListening() }
        appWidgetId = host.allocateAppWidgetId()
        val bound = manager.bindAppWidgetIdIfAllowed(
            appWidgetId,
            ComponentName(context, SingleNoteWidget::class.java)
        )
        assumeTrue(
            "BIND_APPWIDGET was refused even after the shell grant — see the class comment",
            bound
        )
    }

    /**
     * Stands the test in for a launcher. `executeShellCommand` runs as the shell
     * user, which is what `appwidget grantbind` requires; the stream has to be
     * drained or the command may not have finished by the time we bind.
     */
    private fun grantBindPermission() {
        val fd = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
            "appwidget grantbind --package ${context.packageName} --user 0"
        )
        ParcelFileDescriptor.AutoCloseInputStream(fd).use { it.readBytes() }
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
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                SingleNoteWidgetStore.bodySizeSp(EditorFontSize.EXTRA_LARGE),
                body.resources.displayMetrics
            ),
            body.textSize,
            0.5f
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
