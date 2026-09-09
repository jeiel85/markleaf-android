package com.markleaf.notes.widget

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.annotation.RequiresApi
import com.markleaf.notes.data.settings.ColorPalette

/**
 * Which colours a home-screen widget paints itself with (#375).
 *
 * Input: the app context, plus the Colors setting as [WidgetPaletteStore] last
 * mirrored it out of DataStore.
 * Output: a [WidgetColors] to apply over the layout, or `null` for "leave the
 * layout alone".
 *
 * Why this is in code at all: the layouts paint `@color/widget_background`, a
 * fixed `#FF4CAF50`, and no resource qualifier can express "the palette the
 * user picked in Settings" — Material You's colours only exist at runtime and
 * the setting is not a device configuration. A `RemoteViews` tree cannot ask a
 * question either, so the answer has to be pushed into it at update time. That
 * is also why Markleaf Green returns `null` rather than its own colours: the
 * layout already *is* the green look, and re-stating it in code would be a
 * second copy free to drift from the first.
 */
object WidgetPalette {

    /**
     * The override for the current setting, or `null` when there is none.
     *
     * Null on API < 31 whatever the setting says: the dynamic colours below are
     * `android.R.color.system_accent1_*`, which the platform only defines from
     * Android 12. That matches the app, where `MarkleafTheme` gates
     * `dynamicColorScheme` on the same check and falls back to the green scheme.
     */
    fun colors(context: Context): WidgetColors? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return when (WidgetPaletteStore.palette(context)) {
            ColorPalette.MARKLEAF_GREEN -> null
            ColorPalette.MATERIAL_YOU -> dynamicColors(context)
        }
    }

    /**
     * The wallpaper-derived primary / on-primary pair, chosen for the night mode
     * the given context reports.
     *
     * The same roles the app's top bar uses, so a widget and the app it opens
     * are the same colour — `dynamicLightColorScheme`'s `primary` is tone 40
     * (`system_accent1_600`) against white, and the dark scheme's is tone 80
     * (`system_accent1_200`) against tone 20. Taking the pair rather than
     * picking a background alone is what keeps the text readable on it: those
     * two are the ones Material guarantees a contrast ratio for, and the
     * layouts' `?android:attr/textColorPrimaryInverse` would resolve against the
     * *launcher's* theme, which knows nothing about the wallpaper accent.
     *
     * Night mode is read from the context's own configuration rather than the
     * system's, because #354 gives the application a night-mode override — a
     * widget of an app forced to Dark should not repaint light when the phone
     * flips.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun dynamicColors(context: Context): WidgetColors {
        val night = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        return if (night) {
            WidgetColors(
                background = context.getColor(android.R.color.system_accent1_200),
                onBackground = context.getColor(android.R.color.system_accent1_800)
            )
        } else {
            WidgetColors(
                background = context.getColor(android.R.color.system_accent1_600),
                onBackground = context.getColor(android.R.color.system_accent1_0)
            )
        }
    }
}

/**
 * A widget surface's background and the text drawn on it.
 *
 * [onBackgroundSecondary] is [onBackground] at 70% alpha rather than a third
 * system colour: the layouts' secondary role is
 * `?android:attr/textColorSecondaryInverse`, which is the platform's own
 * primary-inverse-with-alpha, so this keeps the same relationship instead of
 * inventing a new one.
 */
data class WidgetColors(
    @ColorInt val background: Int,
    @ColorInt val onBackground: Int
) {
    @get:ColorInt
    val onBackgroundSecondary: Int
        get() = (onBackground and 0x00FFFFFF) or (SECONDARY_ALPHA shl 24)

    private companion object {
        const val SECONDARY_ALPHA = 0xB3
    }
}

/**
 * Recolours the rounded background of [viewId].
 *
 * A tint rather than `setBackgroundColor`: the background is a shape drawable
 * carrying the 16dp corner radius, and setting a flat colour would replace the
 * shape with a square block. `setColorStateList` is API 31, which every caller
 * already is — [WidgetPalette.colors] returns null below it.
 */
@RequiresApi(Build.VERSION_CODES.S)
fun RemoteViews.setWidgetBackground(@IdRes viewId: Int, @ColorInt color: Int) {
    setColorStateList(viewId, "setBackgroundTintList", ColorStateList.valueOf(color))
}
