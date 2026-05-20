package com.markleaf.notes.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.markleaf.notes.MainActivity
import com.markleaf.notes.R

/**
 * Home-screen widget with two surfaces:
 *
 * 1. A "+" button in the header → create a new note (legacy behavior, kept
 *    for users who installed the widget before v2.16).
 * 2. A list of the 10 most recent notes (excluding trashed/archived). Tapping
 *    a row opens that note directly.
 *
 * The list is populated by [QuickNoteWidgetService] via RemoteViewsFactory.
 */
class QuickNoteWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_NOTES_CHANGED) {
            // Pushes a refresh to every active widget. Called from anywhere a
            // note mutation needs the widget to repaint (e.g. note saved/deleted).
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(android.content.ComponentName(context, QuickNoteWidget::class.java))
            if (ids.isNotEmpty()) {
                mgr.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
            }
        }
    }

    companion object {
        const val ACTION_CREATE_NOTE = "com.markleaf.notes.ACTION_CREATE_NOTE"
        const val ACTION_OPEN_NOTE = "com.markleaf.notes.ACTION_OPEN_NOTE"
        const val ACTION_NOTES_CHANGED = "com.markleaf.notes.ACTION_WIDGET_NOTES_CHANGED"
        const val EXTRA_NOTE_ID = "note_id"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_quick_note)

            // Header "+" → create-note intent
            val newNoteIntent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_CREATE_NOTE
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val newNotePending = PendingIntent.getActivity(
                context,
                0,
                newNoteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_new_note, newNotePending)

            // List adapter — service supplies note rows
            val serviceIntent = Intent(context, QuickNoteWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            views.setRemoteAdapter(R.id.widget_list, serviceIntent)
            views.setEmptyView(R.id.widget_list, R.id.widget_empty)

            // Pending intent template — fillIn from each row supplies EXTRA_NOTE_ID
            val openNoteTemplate = Intent(context, MainActivity::class.java).apply {
                action = ACTION_OPEN_NOTE
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openNotePending = PendingIntent.getActivity(
                context,
                1,
                openNoteTemplate,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_list, openNotePending)

            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list)
        }
    }
}
