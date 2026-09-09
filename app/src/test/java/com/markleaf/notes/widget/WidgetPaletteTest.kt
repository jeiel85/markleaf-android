package com.markleaf.notes.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.markleaf.notes.R
import com.markleaf.notes.data.settings.ColorPalette
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
     * The Theme setting decides the end of the palette, not the context's
     * `Configuration`: the app renders dark straight from the setting, while the
     * matching `-night` qualifier arrives through `UiModeManager` (#354) at a
     * moment this code does not control. Robolectric's default configuration is
     * not night, so a Dark that read `uiMode` would come back light here — which
     * is exactly the mismatch a user picking Dark on a light phone would see.
     */
    @Test
    fun `the theme setting picks which end of the palette to take`() {
        WidgetPaletteStore.save(context, ColorPalette.MATERIAL_YOU, ThemeMode.LIGHT)
        val light = requireNotNull(WidgetPalette.colors(context))

        WidgetPaletteStore.save(context, ColorPalette.MATERIAL_YOU, ThemeMode.DARK)
        val dark = requireNotNull(WidgetPalette.colors(context))

        assertNotEquals(light.background, dark.background)
        assertNotEquals(light.onBackground, dark.onBackground)
        assertEquals(context.getColor(android.R.color.system_accent1_600), light.background)
        assertEquals(context.getColor(android.R.color.system_accent1_200), dark.background)
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
        val colors = WidgetColors(background = 0xFF102030.toInt(), onBackground = 0xFFFFFFFF.toInt())

        assertEquals(0xB3FFFFFF.toInt(), colors.onBackgroundSecondary)
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

        assertEquals(expected.background, view.backgroundTintList?.defaultColor)
        assertEquals(
            expected.onBackground,
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
