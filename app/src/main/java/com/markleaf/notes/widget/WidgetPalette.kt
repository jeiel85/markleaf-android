package com.markleaf.notes.widget

import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.RequiresApi
import com.markleaf.notes.R
import com.markleaf.notes.data.settings.ColorPalette
import com.markleaf.notes.data.settings.ThemeMode
import com.markleaf.notes.data.settings.WidgetOpacity

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
        WidgetPaletteStore.customColor(context)?.let { return customColors(it) }
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
     * The surfaces themselves are a *filled accent card* in each night mode,
     * which is what the green layout already is — `#FF4CAF50` with white text,
     * the same card whether the phone is light or dark. Taking a pair rather
     * than a background alone is what keeps the text readable on it: each is a
     * role pair Material guarantees a contrast ratio for, and the layouts'
     * `?android:attr/textColorPrimaryInverse` would resolve against the
     * *launcher's* theme, which knows nothing about the wallpaper accent.
     *
     * Which pair, per mode, is the correction in #394. Light is
     * `dynamicLightColorScheme`'s `primary` / `onPrimary` — tone 40
     * (`system_accent1_600`) against white — and dark is
     * `dynamicDarkColorScheme`'s `primaryContainer` / `onPrimaryContainer` —
     * tone 30 (`system_accent1_700`) against tone 90. Dark used to take the
     * dark scheme's `primary` instead, tone 80 against tone 20, and that is
     * *paler* than the light mode's tone 40: a user who set Theme = Light got
     * the darker widget of the two and reported it as the setting being
     * inverted (#375). `primary` inverts its lightness between the two schemes
     * because it is meant to be drawn *on* a surface, not to be one. The
     * container roles are the filled-surface pair, and they keep the order the
     * setting names — light mode lighter than dark mode.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun dynamicColors(context: Context): WidgetColors {
        val light = WidgetSurface(
            background = context.getColor(android.R.color.system_accent1_600),
            onBackground = context.getColor(android.R.color.system_accent1_0)
        )
        val dark = WidgetSurface(
            background = context.getColor(android.R.color.system_accent1_700),
            onBackground = context.getColor(android.R.color.system_accent1_100)
        )
        return when (WidgetPaletteStore.themeMode(context)) {
            ThemeMode.SYSTEM -> WidgetColors(notNight = light, night = dark)
            ThemeMode.LIGHT -> WidgetColors(notNight = light, night = light)
            ThemeMode.DARK -> WidgetColors(notNight = dark, night = dark)
        }
    }
}

/**
 * The surface for a colour the user picked for the widgets (#469).
 *
 * Input: the picked colour, opaque ARGB.
 * Output: the same [WidgetSurface] for both night modes — a colour chosen by
 * hand is the one the user wants to see, so the host is given no choice.
 *
 * Unlike the palettes, a picked colour comes with no role pair that promises a
 * readable text colour, so the text is chosen here: [readableTextColorOn].
 */
internal fun customColors(@ColorInt background: Int): WidgetColors {
    val surface = WidgetSurface(background, readableTextColorOn(background))
    return WidgetColors(notNight = surface, night = surface)
}

/**
 * White or black, whichever contrasts more with [background] (WCAG relative
 * luminance). One of the two always clears 4.5:1 against any opaque colour,
 * so the widget's title stays readable whatever was picked; the picker shows
 * the same choice as a preview. Pure arithmetic rather than
 * `ColorUtils.calculateContrast` so it can be tested without a framework.
 */
@ColorInt
internal fun readableTextColorOn(@ColorInt background: Int): Int {
    val luminance = relativeLuminance(background)
    val againstWhite = 1.05 / (luminance + 0.05)
    val againstBlack = (luminance + 0.05) / 0.05
    return if (againstWhite >= againstBlack) WHITE else BLACK
}

internal fun relativeLuminance(@ColorInt color: Int): Double {
    fun channel(shift: Int): Double {
        val c = ((color shr shift) and 0xFF) / 255.0
        return if (c <= 0.04045) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
    }
    return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
}

private const val WHITE = 0xFFFFFFFF.toInt()
private const val BLACK = 0xFF000000.toInt()

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

/**
 * Makes the rounded background of [viewId] as opaque as [opacity] asks (#469).
 *
 * Input: the widget's root view and the mirrored Widget background setting.
 * Output: the layout's `widget_background`, or its translucent twin, set on
 * the view.
 *
 * Why a drawable swap rather than an alpha: `View.setAlpha` would fade the text
 * along with the card, and the one-colour tint [setWidgetBackground] uses is
 * Android 12+ only — this has to work down to the app's minimum. The swap is
 * compatible with that tint in either order: the tint is kept on the view and
 * re-applied to the new drawable, and in its default `SRC_IN` mode it takes the
 * drawable's alpha, so Material You and a translucent step compose.
 *
 * Why OPAQUE still writes, unlike Markleaf Green in [WidgetPalette.colors]:
 * `AppWidgetHostView` re-applies a new `RemoteViews` onto the view it already
 * shows when the layout id is unchanged, instead of inflating afresh, so an
 * action left out keeps the previous one's effect. Writing nothing here would
 * leave a widget translucent after the user moved the setting back to 100%.
 */
fun RemoteViews.setWidgetOpacity(@IdRes viewId: Int, opacity: WidgetOpacity) {
    setInt(viewId, "setBackgroundResource", opacity.backgroundRes())
}

/** The `widget_background` drawable for this step — the layout's own at 100%. */
@DrawableRes
internal fun WidgetOpacity.backgroundRes(): Int = when (this) {
    WidgetOpacity.OPAQUE -> R.drawable.widget_background
    WidgetOpacity.HIGH -> R.drawable.widget_background_75
    WidgetOpacity.HALF -> R.drawable.widget_background_50
    WidgetOpacity.LOW -> R.drawable.widget_background_25
    WidgetOpacity.NONE -> R.drawable.widget_background_0
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
