package com.markleaf.notes.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.ParcelFileDescriptor
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.RemoteViews
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.markleaf.notes.R
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
 * What the single-note widget actually puts on a home screen (#351, #371).
 *
 * The Robolectric tests pin the rules — which notes may be drawn, what a line
 * becomes as a row — but they stop at the row list. This binds a real widget id
 * through `AppWidgetService` and inflates what the launcher would inflate.
 *
 * Two halves, because the widget now has two:
 *
 * - The **container** is asserted through the host, as before. Since #371 the
 *   assertion is that the body is a `ListView`: that is the whole of the fix,
 *   because a collection view is the only shape a widget can scroll in.
 * - The **rows** come from [SingleNoteWidgetFactory] against the real database,
 *   each one inflated through `RemoteViews.apply` so the assertions are still
 *   about a `TextView` a person would look at. They cannot be read off the host
 *   view: an adapter-backed list is populated asynchronously by the launcher's
 *   `RemoteViewsAdapter`, and asserting on that here would be a race dressed up
 *   as a test.
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

    /**
     * The fix itself (#371). A `TextView` in a widget clips at the widget's
     * height with no way to reach the rest, so the assertion is about which
     * container got inflated rather than about any text inside it.
     */
    @Test
    fun theBodyIsInflatedAsAScrollableList() {
        seedNote(locked = false)
        SingleNoteWidgetStore.save(context, appWidgetId, noteId, EditorFontSize.MEDIUM)

        assertNotNull("The widget inflated without a ListView", inflate().firstListView())
    }

    @Test
    fun theChosenNotesLinesAreTheRowsAtTheChosenSize() {
        seedNote(locked = false)
        SingleNoteWidgetStore.save(context, appWidgetId, noteId, EditorFontSize.EXTRA_LARGE)
        val factory = readyFactory()

        assertEquals(2, factory.getCount())
        assertEquals(listOf("line one", "line two"), (0 until factory.getCount()).map { rowText(factory.getViewAt(it)) })
        assertEquals(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                SingleNoteWidgetStore.bodySizeSp(EditorFontSize.EXTRA_LARGE),
                context.resources.displayMetrics
            ),
            rowView(factory.getViewAt(0)).textSize,
            0.5f
        )
    }

    /**
     * The end-to-end half: that a host asking for the adapter actually reaches
     * [SingleNoteWidgetService] and gets rows back. Nothing else here would
     * notice the service missing from the manifest, or declared without
     * `BIND_REMOTEVIEWS` — the widget would simply be empty on a home screen
     * while every other assertion stayed green.
     *
     * Polled rather than asserted once: the adapter connects to the service
     * asynchronously, so the wait is part of the check, not a race around it.
     */
    @Test
    fun theHostBindsTheServiceAndGetsRows() {
        seedNote(locked = false)
        SingleNoteWidgetStore.save(context, appWidgetId, noteId, EditorFontSize.MEDIUM)

        val list = requireNotNull(inflate().firstListView()) { "The widget inflated without a ListView" }

        val deadline = System.currentTimeMillis() + ADAPTER_TIMEOUT_MS
        var count = 0
        while (System.currentTimeMillis() < deadline) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { count = list.adapter?.count ?: 0 }
            if (count >= 2) break
            Thread.sleep(POLL_MS)
        }

        assertEquals("The list never received the note's rows from the service", 2, count)
    }

    /**
     * The guard that matters: a home screen is visible without unlocking
     * anything, so a note moved into the Locked space has to stop rendering —
     * not keep showing the text it had when it was chosen. With the body as a
     * list, "stops rendering" is an empty row set, which is what makes the
     * launcher swap in the empty view.
     */
    @Test
    fun aNoteLockedAfterItWasChosenStopsShowingItsText() {
        seedNote(locked = false)
        SingleNoteWidgetStore.save(context, appWidgetId, noteId, EditorFontSize.MEDIUM)
        val factory = readyFactory()
        assertEquals(2, factory.getCount())

        runBlocking { AppDatabase.getInstance(context).noteDao().setLocked(noteId, true) }
        factory.onDataSetChanged()

        assertEquals(0, factory.getCount())
        // And the view the launcher falls back to says so, rather than going blank.
        assertNotNull(
            "The empty view carries no 'nothing to show' message",
            inflate().findTextViewWithText(context.getString(R.string.single_note_widget_unavailable))
        )
    }

    private fun readyFactory(): SingleNoteWidgetFactory =
        SingleNoteWidgetFactory(context, appWidgetId).apply { onDataSetChanged() }

    private fun rowText(row: RemoteViews): String = rowView(row).text.toString()

    /** Inflates one row the way the launcher would, so the assertion is about a real view. */
    private fun rowView(row: RemoteViews): TextView {
        var found: TextView? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            found = row.apply(context, null).findViewById(R.id.single_note_line)
        }
        return requireNotNull(found) { "A row inflated without its TextView" }
    }

    private fun inflate(): View {
        SingleNoteWidget.updateAppWidget(context, manager, appWidgetId)
        var found: View? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val hostView = host.createView(context, appWidgetId, manager.getAppWidgetInfo(appWidgetId))
            hostView.measure(
                View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY)
            )
            found = hostView
        }
        return requireNotNull(found) { "The widget did not inflate" }
    }

    private fun View.firstListView(): ListView? = when (this) {
        is ListView -> this
        is ViewGroup -> (0 until childCount).firstNotNullOfOrNull { getChildAt(it).firstListView() }
        else -> null
    }

    private fun View.findTextViewWithText(text: String): TextView? = when {
        this is TextView && this.text?.toString() == text -> this
        this is ViewGroup ->
            (0 until childCount).firstNotNullOfOrNull { getChildAt(it).findTextViewWithText(text) }
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

        /** How long the adapter is given to connect to the service before the check fails. */
        const val ADAPTER_TIMEOUT_MS = 10_000L
        const val POLL_MS = 100L

        const val BODY = "line one\nline two"
    }
}
