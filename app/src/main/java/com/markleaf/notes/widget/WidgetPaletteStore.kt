package com.markleaf.notes.widget

import android.content.Context
import com.markleaf.notes.data.settings.ColorPalette
import com.markleaf.notes.data.settings.ThemeMode

/**
 * The two Appearance settings a widget paints itself from, mirrored where it can
 * read them (#375).
 *
 * SharedPreferences rather than the app's DataStore, for the same reason
 * [SingleNoteWidgetStore] is: `onUpdate` runs on the receiver's main thread and
 * has to answer immediately, and a DataStore read there would mean blocking it
 * on I/O. The authoritative copies stay in `markleaf_settings`; this is a cache
 * of two enums, written by `MainActivity` whenever either changes.
 *
 * Theme is here as well as Colors because the widget needs to know which *end*
 * of the dynamic palette to take, and its own `Configuration` cannot be trusted
 * to say: the app persists its night mode through `UiModeManager` (#354), which
 * lands on the process at a moment this code does not control. The setting the
 * app renders from is the one thing that is true immediately.
 *
 * A stale mirror is therefore possible in exactly one window — between a change
 * and the write that follows it — and it costs a widget painted in the previous
 * appearance until the next update. The same write triggers that update, so the
 * window closes immediately in practice.
 */
object WidgetPaletteStore {

    private const val PREFS_NAME = "widget_appearance"
    private const val KEY_PALETTE = "color_palette"
    private const val KEY_THEME_MODE = "theme_mode"

    /** Defaults to [ColorPalette.MARKLEAF_GREEN], as the setting itself does. */
    fun palette(context: Context): ColorPalette =
        prefs(context).getString(KEY_PALETTE, null)
            ?.let { stored -> ColorPalette.entries.firstOrNull { it.name == stored } }
            ?: ColorPalette.MARKLEAF_GREEN

    /** Defaults to [ThemeMode.SYSTEM], as the setting itself does. */
    fun themeMode(context: Context): ThemeMode =
        prefs(context).getString(KEY_THEME_MODE, null)
            ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
            ?: ThemeMode.SYSTEM

    /**
     * Stores both values, returning whether either differed from what was there.
     *
     * The return value is what lets the caller repaint only on a real change:
     * the settings arrive once per process whether or not the user touched them,
     * and repainting every widget on every launch would be work for nothing.
     *
     * `commit`, not `apply`: the caller's next act is to tell the launcher to
     * redraw, and those widgets read this file back on another thread. Call it
     * off the main thread — `MainActivity` does.
     */
    fun save(context: Context, palette: ColorPalette, themeMode: ThemeMode): Boolean {
        val prefs = prefs(context)
        val unchanged = prefs.getString(KEY_PALETTE, null) == palette.name &&
            prefs.getString(KEY_THEME_MODE, null) == themeMode.name
        if (unchanged) return false
        prefs.edit()
            .putString(KEY_PALETTE, palette.name)
            .putString(KEY_THEME_MODE, themeMode.name)
            .commit()
        return true
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
