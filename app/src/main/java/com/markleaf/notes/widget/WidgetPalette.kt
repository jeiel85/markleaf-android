package com.markleaf.notes.widget

import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.ColorInt
import androidx.annotation.IdRes
import androidx.annotation.RequiresApi
import com.markleaf.notes.data.settings.ColorPalette
import com.markleaf.notes.data.settings.ThemeMode

/**
 * Which colours a home-screen widget paints itself with (#375).
 *
 * Input: the app context, plus the Appearance settings as [WidgetPaletteStore]
 * last mirrored them.
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
     * The override for the current settings, or `null` when there is none.
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
     * The wallpaper-derived surfaces to hand the host, one per night mode.
     *
     * The pair is the whole point. A widget's `RemoteViews` are cached by the
     * launcher and re-applied when its configuration changes, and the
     * two-argument `setColorStateList` / `setColorInt` calls let the host pick
     * between the values *at that moment* — so a device switching to dark
     * repaints the widget without the app being opened, which a single resolved
     * colour could not do while nothing schedules a widget update
     * (`updatePeriodMillis` is 0 by design).
     *
     * The Theme setting therefore chooses which pair the host is given rather
     * than resolving night here. Reading night from this process's
     * `Configuration` would be wrong twice over: the app renders dark straight
     * from the setting while the matching `-night` qualifier arrives through
     * `UiModeManager.setApplicationNightMode` (#354) at a moment this code does
     * not control, and the answer would be frozen into the pushed views anyway.
     *
     * The surfaces themselves are the same roles the app's top bar uses, so a
     * widget and the app it opens are the same colour — `dynamicLightColorScheme`'s
     * `primary` is tone 40 (`system_accent1_600`) against white, and the dark
     * scheme's is tone 80 (`system_accent1_200`) against tone 20. Taking the
     * pair rather than a background alone is what keeps the text readable on it:
     * those two are the ones Material guarantees a contrast ratio for, and the
     * layouts' `?android:attr/textColorPrimaryInverse` would resolve against the
     * *launcher's* theme, which knows nothing about the wallpaper accent.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun dynamicColors(context: Context): WidgetColors {
        val light = WidgetSurface(
            background = context.getColor(android.R.color.system_accent1_600),
            onBackground = context.getColor(android.R.color.system_accent1_0)
        )
        val dark = WidgetSurface(
            background = context.getColor(android.R.color.system_accent1_200),
            onBackground = context.getColor(android.R.color.system_accent1_800)
        )
        return when (WidgetPaletteStore.themeMode(context)) {
            ThemeMode.SYSTEM -> WidgetColors(notNight = light, night = dark)
            ThemeMode.LIGHT -> WidgetColors(notNight = light, night = light)
            ThemeMode.DARK -> WidgetColors(notNight = dark, night = dark)
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
data class WidgetSurface(
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
 * What the host is handed: one [WidgetSurface] for each of its night modes.
 *
 * Both entries are the same surface unless the Theme setting is
 * [ThemeMode.SYSTEM] — the host only gets a choice when the user has asked to
 * follow the device.
 */
data class WidgetColors(
    val notNight: WidgetSurface,
    val night: WidgetSurface
)

/**
 * Recolours the rounded background of [viewId].
 *
 * A tint rather than `setBackgroundColor`: the background is a shape drawable
 * carrying the 16dp corner radius, and setting a flat colour would replace the
 * shape with a square block. The two-value overload is API 31, which every
 * caller already is — [WidgetPalette.colors] returns null below it.
 */
fun RemoteViews.setWidgetBackground(@IdRes viewId: Int, colors: WidgetColors) {
    // Unreachable below Android 12 — WidgetPalette.colors() answers null there,
    // so no caller holds a WidgetColors to pass. It is stated here rather than at
    // each of the five call sites because that is one place for lint and the
    // next reader to find the rule, instead of five copies of it.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    setColorStateList(
        viewId,
        "setBackgroundTintList",
        ColorStateList.valueOf(colors.notNight.background),
        ColorStateList.valueOf(colors.night.background)
    )
}

/** The primary text role of [viewId], per the host's night mode. */
fun RemoteViews.setWidgetTextColor(@IdRes viewId: Int, colors: WidgetColors) {
    // Unreachable below Android 12 — WidgetPalette.colors() answers null there,
    // so no caller holds a WidgetColors to pass. It is stated here rather than at
    // each of the five call sites because that is one place for lint and the
    // next reader to find the rule, instead of five copies of it.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    setColorInt(viewId, "setTextColor", colors.notNight.onBackground, colors.night.onBackground)
}

/** The secondary text role of [viewId] — the same hue, less opaque. */
fun RemoteViews.setWidgetSecondaryTextColor(@IdRes viewId: Int, colors: WidgetColors) {
    // Unreachable below Android 12 — WidgetPalette.colors() answers null there,
    // so no caller holds a WidgetColors to pass. It is stated here rather than at
    // each of the five call sites because that is one place for lint and the
    // next reader to find the rule, instead of five copies of it.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    setColorInt(
        viewId,
        "setTextColor",
        colors.notNight.onBackgroundSecondary,
        colors.night.onBackgroundSecondary
    )
}

/**
 * Tints a single-colour icon to the primary text role.
 *
 * `setColorFilter` rather than a tint: `ImageView` has no tint setter a
 * `RemoteViews` may call by name, and a drawable prefers an explicit colour
 * filter over its XML `android:tint`.
 */
fun RemoteViews.setWidgetIconColor(@IdRes viewId: Int, colors: WidgetColors) {
    // Unreachable below Android 12 — WidgetPalette.colors() answers null there,
    // so no caller holds a WidgetColors to pass. It is stated here rather than at
    // each of the five call sites because that is one place for lint and the
    // next reader to find the rule, instead of five copies of it.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    setColorInt(viewId, "setColorFilter", colors.notNight.onBackground, colors.night.onBackground)
}
