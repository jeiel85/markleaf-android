package com.markleaf.notes.widget

import android.content.Context

/**
 * Repaints every placed widget after the notes behind them may have moved.
 *
 * Both widgets used to be refreshed only from `MainActivity.onPause` and the
 * editor's autosave, which covers an edit made in the app and nothing else. A
 * note rewritten by the folder-sync import — or deleted on another device and
 * pulled in by it — landed in the database with no one telling the launcher,
 * and the import runs asynchronously, so it can and does finish after the
 * `onPause` that would have covered it. The widget then showed the old body
 * until the user next opened Markleaf and left it again (#262).
 *
 * A broadcast was the other candidate — `QuickNoteWidget` even carried an
 * exported `ACTION_WIDGET_NOTES_CHANGED` for it, declared and handled and
 * never once sent. Every write path that needs this is inside this process, so
 * the broadcast bought nothing but an entry point anyone on the device could
 * reach; it is gone, and the direct call it existed to imitate is here. This
 * also does the fuller job: the action only reloaded the *rows*, while
 * [QuickNoteWidget.refreshAll] repaints the container the palette lives on.
 *
 * `updatePeriodMillis` stays 0 on both widgets. A poll would wake the app on a
 * timer to discover that nothing changed; this fires exactly when something
 * did.
 */
internal object WidgetRefresh {

    /**
     * Both widget kinds, each guarded on its own.
     *
     * `runCatching` because this is a courtesy repaint at the edge of the app:
     * the launcher may have been updated or disabled underneath us, and a
     * failure to redraw a home-screen widget must not take down the save,
     * import or `onPause` that asked for it.
     */
    fun notesChanged(context: Context) {
        val app = context.applicationContext
        runCatching { QuickNoteWidget.refreshAll(app) }
        runCatching { SingleNoteWidget.refreshAll(app) }
    }
}
