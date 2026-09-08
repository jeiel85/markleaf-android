package com.markleaf.notes.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.markleaf.notes.R
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.settings.EditorFontSize
import kotlinx.coroutines.runBlocking

/**
 * RemoteViewsService behind the scrolling body of [SingleNoteWidget] (#371).
 *
 * A widget scrolls only inside a collection view, and a collection view is fed
 * only by a service like this one. Each row is one line of the chosen note, so
 * a long line still wraps within its row instead of being cut at a boundary.
 *
 * Unlike [QuickNoteWidgetService], one factory per placed widget: every
 * single-note widget shows a different note, and its id arrives on the intent.
 * [SingleNoteWidget.factoryIntent] is what keeps those intents distinct.
 */
class SingleNoteWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        SingleNoteWidgetFactory(
            applicationContext,
            intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        )
}

/**
 * Why `runBlocking` is fine here: `onDataSetChanged` is called on a background
 * thread by the AppWidgetManager precisely so the factory can do synchronous
 * I/O, and the read is a single lookup by primary key. It is also strictly
 * better than what it replaces — the provider used to block the receiver's main
 * thread for the same row.
 */
internal class SingleNoteWidgetFactory(
    private val context: Context,
    private val appWidgetId: Int
) : RemoteViewsService.RemoteViewsFactory {

    private var rows: List<String> = emptyList()
    private var noteId: String? = null
    private var textSizeSp: Float = SingleNoteWidgetStore.bodySizeSp(EditorFontSize.MEDIUM)

    override fun onCreate() {}

    override fun onDataSetChanged() {
        // Re-read the placement every time rather than caching it: on Android 12+
        // a widget can be reconfigured onto a different note, or to a different
        // size, without ever being removed and re-added.
        val id = SingleNoteWidgetStore.noteId(context, appWidgetId)
        noteId = id
        textSizeSp = SingleNoteWidgetStore.bodySizeSp(
            SingleNoteWidgetStore.textSize(context, appWidgetId)
        )
        rows = if (id == null) {
            emptyList()
        } else {
            runBlocking {
                SingleNoteWidget.bodyRows(
                    AppDatabase.getInstance(context).noteDao().getNoteById(id)
                )
            }
        }
    }

    override fun onDestroy() {
        rows = emptyList()
        noteId = null
    }

    override fun getCount(): Int = rows.size

    override fun getViewAt(position: Int): RemoteViews {
        val view = RemoteViews(context.packageName, R.layout.widget_single_note_line)
        // getOrNull, not [position]: the launcher may ask for a row from the
        // count it last read, and a note edited in between can be shorter.
        view.setTextViewText(
            R.id.single_note_line,
            rows.getOrNull(position) ?: SingleNoteWidget.BLANK_LINE
        )
        view.setTextViewTextSize(
            R.id.single_note_line,
            TypedValue.COMPLEX_UNIT_SP,
            textSizeSp
        )
        // Every row opens the same note, so the fill-in is the same for all of
        // them; the template in the provider carries the action and component.
        noteId?.let {
            view.setOnClickFillInIntent(
                R.id.single_note_line_root,
                Intent().putExtra(QuickNoteWidget.EXTRA_NOTE_ID, it)
            )
        }
        return view
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    /**
     * False deliberately. The ids above are positions, and a line's position
     * changes whenever the note is edited above it — claiming they are stable
     * would let the launcher reuse a row for different text.
     */
    override fun hasStableIds(): Boolean = false
}
