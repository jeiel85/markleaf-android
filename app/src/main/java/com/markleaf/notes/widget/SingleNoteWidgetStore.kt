package com.markleaf.notes.widget

import android.content.Context
import com.markleaf.notes.data.settings.EditorFontSize

/**
 * Which note each placed [SingleNoteWidget] shows, and at what text size (#351).
 *
 * SharedPreferences rather than the app's DataStore: an `onUpdate` runs on the
 * receiver's main thread and has to answer immediately, and DataStore would mean
 * blocking a coroutine there for a value this small. Nothing outside the widget
 * reads it — it is placement state belonging to an id the launcher owns, not a
 * user setting, which is also why it is a separate file from `markleaf_settings`.
 *
 * Entries are keyed by `appWidgetId` and removed in [forget] when the launcher
 * reports the widget deleted, so removing a widget does not leave its note id
 * behind.
 */
object SingleNoteWidgetStore {

    private const val PREFS_NAME = "single_note_widget"
    private const val KEY_NOTE_PREFIX = "note_"
    private const val KEY_TEXT_SIZE_PREFIX = "text_size_"

    /** Text size of the widget's body, in sp, before [EditorFontSize] scaling. */
    const val BASE_TEXT_SIZE_SP = 14f

    fun noteId(context: Context, appWidgetId: Int): String? =
        prefs(context).getString(KEY_NOTE_PREFIX + appWidgetId, null)

    fun textSize(context: Context, appWidgetId: Int): EditorFontSize {
        val stored = prefs(context).getString(KEY_TEXT_SIZE_PREFIX + appWidgetId, null)
            ?: return EditorFontSize.MEDIUM
        return EditorFontSize.entries.firstOrNull { it.name == stored } ?: EditorFontSize.MEDIUM
    }

    fun save(context: Context, appWidgetId: Int, noteId: String, textSize: EditorFontSize) {
        prefs(context).edit()
            .putString(KEY_NOTE_PREFIX + appWidgetId, noteId)
            .putString(KEY_TEXT_SIZE_PREFIX + appWidgetId, textSize.name)
            .apply()
    }

    /** Drops every stored value for [appWidgetIds]. Called from `onDeleted`. */
    fun forget(context: Context, appWidgetIds: IntArray) {
        val editor = prefs(context).edit()
        for (appWidgetId in appWidgetIds) {
            editor.remove(KEY_NOTE_PREFIX + appWidgetId)
            editor.remove(KEY_TEXT_SIZE_PREFIX + appWidgetId)
        }
        editor.apply()
    }

    /** Body text size in sp for a widget configured at [textSize]. */
    fun bodySizeSp(textSize: EditorFontSize): Float = BASE_TEXT_SIZE_SP * textSize.scale

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
