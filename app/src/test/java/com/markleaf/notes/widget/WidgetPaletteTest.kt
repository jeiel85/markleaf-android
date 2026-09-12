package com.markleaf.notes.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.markleaf.notes.R
import kotlinx.coroutines.runBlocking
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.data.settings.ColorPalette
import com.markleaf.notes.data.settings.InMemoryPreferencesDataStore
import com.markleaf.notes.data.settings.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The Colors setting reaching the widgets (#375).
 *
 * The defect this pins: the widget layouts paint a fixed `#FF4CAF50`, so a user
 * on Material You saw Markleaf Green on the home screen whatever Settings said.
 * These tests cover the two halves the widgets depend on — the mirror they read
 * the setting from, and the rule that turns it into colours.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetPaletteTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `an unmirrored setting reads as the defaults`() {
        assertEquals(ColorPalette.MARKLEAF_GREEN, WidgetPaletteStore.palette(context))
        assertEquals(ThemeMode.SYSTEM, WidgetPaletteStore.themeMode(context))
    }

    @Test
    fun `the mirrored settings are what come back`() {
        WidgetPaletteStore.save(context, ColorPalette.MATERIAL_YOU, ThemeMode.DARK)

        assertEquals(ColorPalette.MATERIAL_YOU, WidgetPaletteStore.palette(context))
        assertEquals(ThemeMode.DARK, WidgetPaletteStore.themeMode(context))
    }

    /**
     * The return value is what stops every launch repainting every widget: the
     * settings arrive once per process whether or not the user touched them.
     * Either one moving is a repaint, which is why the check is on the pair.
     */
    @Test
    fun `saving reports only a real change`() {
        assertTrue(WidgetPaletteStore.save(context, ColorPalette.MATERIAL_YOU, ThemeMode.SYSTEM))
        assertFalse(WidgetPaletteStore.save(context, ColorPalette.MATERIAL_YOU, ThemeMode.SYSTEM))
        assertTrue(WidgetPaletteStore.save(context, ColorPalette.MATERIAL_YOU, ThemeMode.DARK))
        assertTrue(WidgetPaletteStore.save(context, ColorPalette.MARKLEAF_GREEN, ThemeMode.DARK))
    }

    /** Null means "leave the layout alone", which already draws the green. */
    @Test
    fun `markleaf green asks for no override`() {
        WidgetPaletteStore.save(context, ColorPalette.MARKLEAF_GREEN, ThemeMode.SYSTEM)

        assertNull(WidgetPalette.colors(context))
    }

    @Test
    fun `material you asks for an override`() {
        WidgetPaletteStore.save(context, ColorPalette.MATERIAL_YOU, ThemeMode.SYSTEM)

        assertNotNull(WidgetPalette.colors(context))
    }

    /**
     * Theme = SYSTEM is the only case where the host gets a choice, and it must
     * get one: a widget's views are cached by the launcher and re-applied when
     * its configuration changes, so handing it both surfaces is what lets a
     * device switching to dark repaint the widget with the app closed. Nothing
     * schedules a widget update otherwise — `updatePeriodMillis` is 0.
     */
    @Test
    fun `following the system hands the host both surfaces`() {
        WidgetPaletteStore.save(context, ColorPalette.MATERIAL_YOU, ThemeMode.SYSTEM)

        val colors = requireNotNull(WidgetPalette.colors(context))

        assertNotEquals(colors.notNight.background, colors.night.background)
        assertEquals(
            context.getColor(android.R.color.system_accent1_600),
            colors.notNight.background
        )
        assertEquals(context.getColor(android.R.color.system_accent1_700), colors.night.background)
    }

    /**
     * A fixed Theme is fixed on the home screen too: both entries are the same
     * surface, so the host's night mode cannot move a widget away from what the
     * app is rendering. Reading night from this process's `Configuration`
     * instead would be wrong twice over — the `-night` qualifier arrives through
     * `UiModeManager` (#354) at a moment this code does not control, and the
     * answer would be frozen into the pushed views anyway.
     */
    @Test
    fun `a fixed theme gives the host no choice`() {
        WidgetPaletteStore.save(context, ColorPalette.MATERIAL_YOU, ThemeMode.DARK)
        val dark = requireNotNull(WidgetPalette.colors(context))

        assertEquals(dark.notNight, dark.night)
        assertEquals(context.getColor(android.R.color.system_accent1_700), dark.notNight.background)

        WidgetPaletteStore.save(context, ColorPalette.MATERIAL_YOU, ThemeMode.LIGHT)
        val light = requireNotNull(WidgetPalette.colors(context))

        assertEquals(light.notNight, light.night)
        assertEquals(context.getColor(android.R.color.system_accent1_600), light.notNight.background)
    }

    /**
     * The defect reported on top of #375, and the one assertion that would have
     * caught it: Theme = Light handed back the *darker* of the two surfaces.
     *
     * It came from taking the dark scheme's `primary` for night — tone 80
     * against tone 20 — while day took the light scheme's, tone 40 against
     * white. `primary` is meant to be drawn *on* a surface rather than to be
     * one, so it inverts its lightness between the schemes, and the widget
     * inverted with it. Stated as an ordering rather than as two resource ids
     * because the ordering is the thing a reader on their home screen can see;
     * the ids above pin which colours satisfy it today.
     */
    @Test
    fun `the night surface is the darker of the two`() {
        WidgetPaletteStore.save(context, ColorPalette.MATERIAL_YOU, ThemeMode.SYSTEM)

        val colors = requireNotNull(WidgetPalette.colors(context))

        assertTrue(
            "night background must not be lighter than the day one",
            luminance(colors.night.background) < luminance(colors.notNight.background)
        )
    }

    /**
     * Each surface is a pair, and the pair has to be legible: a widget is read
     * at a glance from a home screen, over whatever wallpaper it sits on.
     * 4.5:1 is WCAG AA for body text, which the excerpt row is.
     */
    @Test
    fun `both surfaces carry readable text`() {
        WidgetPaletteStore.save(context, ColorPalette.MATERIAL_YOU, ThemeMode.SYSTEM)

        val colors = requireNotNull(WidgetPalette.colors(context))

        listOf("day" to colors.notNight, "night" to colors.night).forEach { (name, surface) ->
            val ratio = contrastRatio(surface.onBackground, surface.background)
            assertTrue("$name contrast was $ratio, below 4.5:1", ratio >= 4.5)
        }
    }

    /**
     * WCAG relative luminance, spelled out here rather than taken from
     * `androidx.core.graphics.ColorUtils`: that arrives on the test classpath
     * only transitively, and a test that pins a colour rule should not be the
     * thing that breaks when an unrelated dependency reshuffles.
     */
    private fun luminance(@androidx.annotation.ColorInt color: Int): Double {
        fun channel(shifted: Int): Double {
            val c = ((color shr shifted) and 0xFF) / 255.0
            return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    /** Both arguments must be opaque — every colour in [WidgetSurface] is. */
    private fun contrastRatio(
        @androidx.annotation.ColorInt foreground: Int,
        @androidx.annotation.ColorInt background: Int
    ): Double {
        val a = luminance(foreground)
        val b = luminance(background)
        return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
    }

    /**
     * The mirror has to survive the app never having been opened. An existing
     * Material You user upgrading, or someone adding a widget straight from the
     * launcher's picker, reaches a widget without `MainActivity` — and either
     * would otherwise see the green default this change exists to remove.
     */
    @Test
    fun `the mirror heals itself from the authoritative settings`() {
        val repository = AppSettingsRepository(InMemoryPreferencesDataStore())
        runBlocking {
            repository.setColorPalette(ColorPalette.MATERIAL_YOU)
            repository.setThemeMode(ThemeMode.DARK)
        }
        // Nothing has written the mirror, which is the state an upgrade or a
        // widget added before the first launch arrives in.
        assertEquals(ColorPalette.MARKLEAF_GREEN, WidgetPaletteStore.palette(context))

        assertTrue(WidgetPaletteStore.syncFromSettings(context, repository))

        assertEquals(ColorPalette.MATERIAL_YOU, WidgetPaletteStore.palette(context))
        assertEquals(ThemeMode.DARK, WidgetPaletteStore.themeMode(context))
        // A second pass has nothing to move, so it asks for no repaint.
        assertFalse(WidgetPaletteStore.syncFromSettings(context, repository))
    }

    /**
     * Below Android 12 there are no `system_accent1_*` resources to read, and
     * `MarkleafTheme` falls back to the green scheme on the same check — a
     * widget that tried anyway would crash the launcher's inflate.
     */
    @Test
    @Config(sdk = [30])
    fun `material you asks for no override before android 12`() {
        WidgetPaletteStore.save(context, ColorPalette.MATERIAL_YOU, ThemeMode.SYSTEM)

        assertNull(WidgetPalette.colors(context))
    }

    /**
     * The secondary role keeps the primary's hue and only loses opacity, the
     * way `?android:attr/textColorSecondaryInverse` does — so a change to the
     * pair cannot silently leave the excerpt a different colour family.
     */
    @Test
    fun `secondary text is the primary at reduced alpha`() {
        val surface = WidgetSurface(
            background = 0xFF102030.toInt(),
            onBackground = 0xFFFFFFFF.toInt()
        )

        assertEquals(0xB3FFFFFF.toInt(), surface.onBackgroundSecondary)
    }

    /**
     * The end-to-end half: that the chosen colours reach the views the launcher
     * inflates, not just the rule that picks them.
     *
     * This is the assertion the defect fails — before the fix, `updateAppWidget`
     * wrote no tint at all and the shape drawable's hardcoded green was the only
     * colour the widget could have.
     */
    @Test
    fun `material you reaches the recent-notes widget`() {
        WidgetPaletteStore.save(context, ColorPalette.MATERIAL_YOU, ThemeMode.SYSTEM)
        val expected = requireNotNull(WidgetPalette.colors(context))

        val view = inflateQuickNoteWidget()

        assertEquals(expected.notNight.background, view.backgroundTintList?.defaultColor)
        assertEquals(
            expected.notNight.onBackground,
            view.findViewById<TextView>(R.id.widget_title).currentTextColor
        )
    }

    /** The counterpart: green leaves the layout's own background untouched. */
    @Test
    fun `markleaf green leaves the recent-notes widget as the layout drew it`() {
        WidgetPaletteStore.save(context, ColorPalette.MARKLEAF_GREEN, ThemeMode.SYSTEM)

        assertNull(inflateQuickNoteWidget().backgroundTintList)
    }

    /**
     * Runs the provider and hands back the view the launcher would show.
     * `getViewFor` is the shadow's inflation of the RemoteViews the widget
     * pushed, so it sees exactly what `updateAppWidget` set and nothing else.
     */
    private fun inflateQuickNoteWidget(): android.view.View {
        val manager = AppWidgetManager.getInstance(context)
        val id = shadowOf(manager)
            .createWidgets(QuickNoteWidget::class.java, R.layout.widget_quick_note, 1)
            .first()
        QuickNoteWidget.updateAppWidget(context, manager, id)
        return shadowOf(manager).getViewFor(id)
    }
}
