package com.markleaf.notes.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.widget.RemoteViews
import com.markleaf.notes.MainActivity
import com.markleaf.notes.R
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.local.entity.NoteEntity
import kotlinx.coroutines.runBlocking

/**
 * Home-screen widget showing the body of one chosen note (#351).
 *
 * Distinct from [QuickNoteWidget], which lists the ten most recent notes as
 * title-plus-one-line rows; the two are placed separately and neither replaces
 * the other. The note and the text size are chosen per placed widget in
 * [SingleNoteWidgetConfigureActivity] and kept in [SingleNoteWidgetStore].
 *
 * The body is drawn as it was typed — Markdown source, not rendered. A
 * `RemoteViews` tree can only hold a fixed set of platform views, so `**bold**`
 * reads as `**bold**` here.
 */
class SingleNoteWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    /** The launcher reports removed widgets here; their stored note goes with them. */
    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        SingleNoteWidgetStore.forget(context, appWidgetIds)
    }

    companion object {

        /**
         * How much of a note's body reaches the widget.
         *
         * `RemoteViews` crosses a Binder transaction, whose payload is capped
         * around 1 MB for the whole process. An imported note may hold up to
         * `ExternalFile.MAX_CHARS` — two million characters — and handing that
         * to `setTextViewText` makes the update throw `TransactionTooLargeException`
         * instead of drawing anything. Nothing is lost by cutting: even a
         * full-screen widget at the smallest size shows a few thousand
         * characters, so this is far past the last legible line, and tapping
         * opens the whole note.
         */
        internal const val MAX_BODY_CHARS = 20_000

        /**
         * Repaints every placed single-note widget. Called wherever the notes
         * behind them may have moved — the same points that refresh the
         * recent-notes list.
         */
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, SingleNoteWidget::class.java)
            )
            for (appWidgetId in ids) {
                updateAppWidget(context, manager, appWidgetId)
            }
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_single_note)
            val noteId = SingleNoteWidgetStore.noteId(context, appWidgetId)
            val note = noteId?.let { loadShowableNote(context, it) }

            views.setTextViewText(
                R.id.single_note_body,
                note?.takeIf { it.isNotBlank() }
                    ?: context.getString(R.string.single_note_widget_unavailable)
            )
            views.setTextViewTextSize(
                R.id.single_note_body,
                TypedValue.COMPLEX_UNIT_SP,
                SingleNoteWidgetStore.bodySizeSp(
                    SingleNoteWidgetStore.textSize(context, appWidgetId)
                )
            )

            // Tapping opens the note in the app, through the same entry point the
            // recent-notes rows use. Only offered while there is a note to open.
            if (noteId != null && note != null) {
                val pending = PendingIntent.getActivity(
                    context,
                    appWidgetId,
                    openNoteIntent(context, noteId),
                    // Immutable: nothing fills this in later, unlike the
                    // recent-notes list, whose template each row completes.
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.single_note_root, pending)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        /**
         * Opens [noteId] in the app — the same entry point the recent-notes rows
         * use, so a widget tap and a widget-list tap land in the same place.
         */
        internal fun openNoteIntent(context: Context, noteId: String): Intent =
            Intent(context, MainActivity::class.java).apply {
                action = QuickNoteWidget.ACTION_OPEN_NOTE
                putExtra(QuickNoteWidget.EXTRA_NOTE_ID, noteId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

        /**
         * The body to draw for [note], or null when nothing may be drawn.
         *
         * A note can leave the widget's reach after it was chosen — deleted,
         * moved to the trash, or moved into the passcode-gated Locked space. The
         * home screen is visible without unlocking anything, so a locked note's
         * body can never render here: the picker never offers one, and this is
         * the guard for a note locked after it was already chosen.
         *
         * Separate from the read so the rule is testable without a database.
         */
        internal fun showableBody(note: NoteEntity?): String? = when {
            note == null -> null
            note.locked -> null
            note.trashed -> null
            else -> note.contentMarkdown.take(MAX_BODY_CHARS)
        }

        /**
         * Runs on the receiver's main thread by way of `onUpdate`; the read is a
         * single lookup by primary key.
         */
        private fun loadShowableNote(context: Context, noteId: String): String? =
            runBlocking {
                showableBody(AppDatabase.getInstance(context).noteDao().getNoteById(noteId))
            }
    }
}
