package com.markleaf.notes.widget

import android.content.Context
import com.markleaf.notes.data.settings.ColorPalette

/**
 * The Colors setting, mirrored where a widget can read it (#375).
 *
 * SharedPreferences rather than the app's DataStore, for the same reason
 * [SingleNoteWidgetStore] is: `onUpdate` runs on the receiver's main thread and
 * has to answer immediately, and a DataStore read there would mean blocking it
 * on I/O. The authoritative copy stays in `markleaf_settings`; this is a cache
 * of one enum, written by `MainActivity` whenever the setting changes.
 *
 * A stale mirror is therefore possible in exactly one window — between a change
 * and the write that follows it — and it costs a widget painted in the previous
 * palette until the next update. The same write triggers that update, so the
 * window closes immediately in practice.
 */
object WidgetPaletteStore {

    private const val PREFS_NAME = "widget_appearance"
    private const val KEY_PALETTE = "color_palette"

    /** Defaults to [ColorPalette.MARKLEAF_GREEN], as the setting itself does. */
    fun palette(context: Context): ColorPalette {
        val stored = prefs(context).getString(KEY_PALETTE, null) ?: return ColorPalette.MARKLEAF_GREEN
        return ColorPalette.entries.firstOrNull { it.name == stored } ?: ColorPalette.MARKLEAF_GREEN
    }

    /**
     * Stores [palette], returning whether it differed from what was there.
     *
     * The return value is what lets the caller repaint only on a real change:
     * the setting arrives once per process whether or not the user touched it,
     * and repainting every widget on every launch would be work for nothing.
     *
     * `commit`, not `apply`: the caller's next act is to tell the launcher to
     * redraw, and those widgets read this file back on another thread.
     */
    fun save(context: Context, palette: ColorPalette): Boolean {
        val prefs = prefs(context)
        if (prefs.getString(KEY_PALETTE, null) == palette.name) return false
        prefs.edit().putString(KEY_PALETTE, palette.name).commit()
        return true
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
