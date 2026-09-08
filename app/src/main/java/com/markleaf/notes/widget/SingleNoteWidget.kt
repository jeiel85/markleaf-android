package com.markleaf.notes.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.TypedValue
import android.widget.RemoteViews
import com.markleaf.notes.MainActivity
import com.markleaf.notes.R
import com.markleaf.notes.data.local.entity.NoteEntity

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
 *
 * The body **scrolls** (#371), which is why it is a `ListView` fed by
 * [SingleNoteWidgetService] rather than the single `TextView` it was until
 * v2.37.3. The same constraint that keeps Markdown unrendered is what forced
 * that: a widget may only hold a fixed set of views, `ScrollView` is not one of
 * them, and a `TextView` there clips at the widget's height with no way to
 * reach the rest. The scrollable containers a widget can hold are the
 * collection views, and every one of them needs a service behind it.
 *
 * A consequence worth naming: this class no longer reads the database. The note
 * is loaded by the factory, on the background thread `onDataSetChanged` already
 * runs on, instead of by a `runBlocking` on the receiver's main thread.
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
         * The original reason for a cap is gone: the body no longer crosses in
         * one `setTextViewText`, so it can no longer blow the ~1 MB Binder
         * payload a process gets. A different reason replaces it — the launcher's
         * adapter holds the rows this factory hands it, and an imported note may
         * run to `ExternalFile.MAX_CHARS`, two million characters.
         *
         * The number is unchanged at 20,000 because raising it is a measurement,
         * not a guess, and none has been taken. What did change is that 20,000
         * characters are now all *reachable* by scrolling rather than clipped at
         * the first screenful, which is the whole of #371.
         */
        internal const val MAX_BODY_CHARS = 20_000

        /**
         * How many rows the list may hold.
         *
         * [MAX_BODY_CHARS] alone does not bound the row count: 20,000 newlines
         * are 20,000 rows, each an inflated view the launcher keeps. This is the
         * second half of the same bound, and it is chosen by the same standard —
         * far past any note a person scrolls through on a home screen, and
         * unmeasured.
         */
        internal const val MAX_BODY_LINES = 2_000

        /**
         * What a blank line becomes.
         *
         * A row whose `TextView` holds nothing can collapse, and paragraphs then
         * run together — a scrollable widget that reads worse than the clipped
         * one it replaced. A non-breaking space keeps the row exactly one line
         * tall, at whatever size the widget is configured for.
         */
        internal const val BLANK_LINE = "\u00A0"

        /** The last row when the body did not fit; punctuation, so it needs no translation. */
        internal const val TRUNCATION_MARKER = "…"

        /**
         * Request-code namespaces for the widget's two tap targets.
         *
         * A `PendingIntent` is identified by its request code and by an intent
         * comparison that ignores extras — and the row template and the
         * whole-widget intent differ only in extras. Sharing a request code
         * would therefore make them the same `PendingIntent`, with
         * `FLAG_UPDATE_CURRENT` overwriting one from the other and their
         * mutability flags in conflict. Two bases, offset by the widget id, keep
         * every pair distinct; the values sit clear of the plain 0 and 1
         * [QuickNoteWidget] uses for intents that name the same activity.
         */
        private const val TEMPLATE_REQUEST_BASE = 0x510000
        private const val ROOT_REQUEST_BASE = 0x520000

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
            val bodySizeSp = SingleNoteWidgetStore.bodySizeSp(
                SingleNoteWidgetStore.textSize(context, appWidgetId)
            )

            views.setRemoteAdapter(R.id.single_note_list, factoryIntent(context, appWidgetId))
            views.setEmptyView(R.id.single_note_list, R.id.single_note_empty)
            // The "nothing to show" message is the one piece of text this class
            // still draws itself, so it is sized here; the rows are sized by the
            // factory, which is the only place that knows how many there are.
            views.setTextViewTextSize(
                R.id.single_note_empty,
                TypedValue.COMPLEX_UNIT_SP,
                bodySizeSp
            )

            // Tap-to-open, in two halves, because a ListView consumes taps that
            // land inside it and nothing else does.
            //
            // The rows get a template each one completes with its note id.
            // Mutable for that reason, and only for it — asking for both
            // MUTABLE and IMMUTABLE throws on Android 12+.
            views.setPendingIntentTemplate(
                R.id.single_note_list,
                PendingIntent.getActivity(
                    context,
                    TEMPLATE_REQUEST_BASE + appWidgetId,
                    openNoteTemplate(context),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
            )
            // The rest of the surface keeps the whole-widget tap it had before
            // #371: the padding, and the "nothing to show" view, which is what a
            // blank note renders — a widget the template alone would leave with
            // no tap target at all. It fires for a note that cannot be drawn
            // (locked, trashed, deleted) too, which the previous code did not do
            // for the locked case: telling those apart needs a database read,
            // and doing one here is exactly what this change moved off the
            // receiver's main thread. Nothing is revealed by it — the tap opens
            // Markleaf, and the Locked space's passcode is what stands in front
            // of the note.
            if (noteId != null) {
                views.setOnClickPendingIntent(
                    R.id.single_note_root,
                    PendingIntent.getActivity(
                        context,
                        ROOT_REQUEST_BASE + appWidgetId,
                        openNoteIntent(context, noteId),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
            // Without this an edited note keeps showing its old text: the views
            // above are the container, and the rows inside it are the factory's.
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.single_note_list)
        }

        /**
         * The adapter intent for [appWidgetId], made distinct by its `data`.
         *
         * `RemoteViewsService` keys its factories by intent, comparing
         * everything *except* extras. [QuickNoteWidget] can therefore pass the
         * widget id as a plain extra — every one of its instances shows the same
         * ten notes, so sharing a factory is invisible. Single-note widgets each
         * show a different note, and without a distinguishing `data` the second
         * one placed would silently render the first one's note.
         */
        internal fun factoryIntent(context: Context, appWidgetId: Int): Intent =
            Intent(context, SingleNoteWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }

        /**
         * Opens [noteId] in the app — the same entry point the recent-notes rows
         * use, so a widget tap and a widget-list tap land in the same place.
         */
        internal fun openNoteIntent(context: Context, noteId: String): Intent =
            openNoteTemplate(context).putExtra(QuickNoteWidget.EXTRA_NOTE_ID, noteId)

        /** [openNoteIntent] without the note id, which each row's fill-in supplies. */
        internal fun openNoteTemplate(context: Context): Intent =
            Intent(context, MainActivity::class.java).apply {
                action = QuickNoteWidget.ACTION_OPEN_NOTE
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
         * [showableBody] as the rows the list draws — one per line of the note.
         *
         * Lines, not fixed-size chunks: a row wraps its own long line across as
         * many visual lines as it needs, so nothing is cut at a row boundary. A
         * note that yields no rows renders the empty view, which is how a blank,
         * locked, trashed or deleted note all come out as "nothing to show"
         * without a second rule for each.
         *
         * Trailing blank lines are dropped rather than drawn: a file ending in a
         * newline would otherwise scroll past its last word into empty rows.
         */
        internal fun bodyRows(note: NoteEntity?): List<String> {
            val body = showableBody(note) ?: return emptyList()
            val cutByChars = (note?.contentMarkdown?.length ?: 0) > body.length

            val lines = body.split('\n')
                .map { it.removeSuffix("\r") }
                .dropLastWhile { it.isBlank() }
            if (lines.isEmpty()) return emptyList()

            val rows = lines.take(MAX_BODY_LINES).map { line ->
                if (line.isBlank()) BLANK_LINE else line
            }
            return if (cutByChars || lines.size > MAX_BODY_LINES) {
                rows + TRUNCATION_MARKER
            } else {
                rows
            }
        }
    }
}
