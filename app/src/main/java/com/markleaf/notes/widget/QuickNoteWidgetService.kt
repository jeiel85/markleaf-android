package com.markleaf.notes.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.markleaf.notes.R
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.local.entity.toDomain
import com.markleaf.notes.domain.model.Note
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * RemoteViewsService that backs the recent-notes list inside [QuickNoteWidget].
 *
 * Why runBlocking is OK here: RemoteViewsFactory.onDataSetChanged() is called on a
 * background thread by the AppWidgetManager precisely so we can do synchronous I/O.
 * Notes are bounded (top 10), so the read is fast.
 */
class QuickNoteWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        QuickNoteWidgetFactory(applicationContext)
}

private class QuickNoteWidgetFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var notes: List<Note> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        notes = runBlocking {
            val db = AppDatabase.getInstance(context)
            // observeNotes already filters trashed + archived and sorts by
            // pinned → sortOrder → updatedAt. Take the top 10 for the widget.
            db.noteDao().observeNotes().first().take(10).map { it.toDomain() }
        }
    }

    override fun onDestroy() {
        notes = emptyList()
    }

    override fun getCount(): Int = notes.size

    override fun getViewAt(position: Int): RemoteViews {
        val note = notes[position]
        val view = RemoteViews(context.packageName, R.layout.widget_quick_note_item)
        view.setTextViewText(
            R.id.widget_item_title,
            note.title.ifBlank { context.getString(R.string.untitled_parenthesized) }
        )
        view.setTextViewText(R.id.widget_item_excerpt, note.excerpt)

        val fillIn = Intent().apply {
            putExtra(QuickNoteWidget.EXTRA_NOTE_ID, note.id)
        }
        view.setOnClickFillInIntent(R.id.widget_item_root, fillIn)
        return view
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long =
        notes.getOrNull(position)?.id?.hashCode()?.toLong() ?: position.toLong()
    override fun hasStableIds(): Boolean = true
}
