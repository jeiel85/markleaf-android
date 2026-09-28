package com.markleaf.notes.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.markleaf.notes.R
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.data.settings.ColorPalette
import com.markleaf.notes.data.settings.InMemoryPreferencesDataStore
import com.markleaf.notes.data.settings.ThemeMode
import com.markleaf.notes.data.settings.WidgetOpacity
import kotlin.math.abs
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Settings → Appearance → Widget background reaching the widgets (#469).
 *
 * The request was to let the wallpaper show through. What these pin: the
 * mirror carries the step, each step's drawable is the widget green at that
 * alpha and nothing else, and the step reaches the view the launcher shows —
 * including the way back to 100%, which a launcher that re-applies onto its
 * existing view would otherwise never see.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetOpacityTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `an unmirrored opacity reads as opaque`() {
        assertEquals(WidgetOpacity.OPAQUE, WidgetPaletteStore.opacity(context))
    }

    /** Opacity alone moving has to repaint, or the setting would do nothing until a palette change. */
    @Test
    fun `an opacity change alone is reported as a change`() {
        WidgetPaletteStore.save(context, ColorPalette.MARKLEAF_GREEN, ThemeMode.SYSTEM, WidgetOpacity.OPAQUE)

        assertTrue(
            WidgetPaletteStore.save(context, ColorPalette.MARKLEAF_GREEN, ThemeMode.SYSTEM, WidgetOpacity.HALF)
        )
        assertEquals(WidgetOpacity.HALF, WidgetPaletteStore.opacity(context))
        assertFalse(
            WidgetPaletteStore.save(context, ColorPalette.MARKLEAF_GREEN, ThemeMode.SYSTEM, WidgetOpacity.HALF)
        )
    }

    /** The self-healing path the factories run must carry the new setting too. */
    @Test
    fun `the mirror heals opacity from the authoritative settings`() {
        val repository = AppSettingsRepository(InMemoryPreferencesDataStore())
        runBlocking { repository.setWidgetOpacity(WidgetOpacity.LOW) }

        assertTrue(WidgetPaletteStore.syncFromSettings(context, repository))

        assertEquals(WidgetOpacity.LOW, WidgetPaletteStore.opacity(context))
    }

    /**
     * Every step is `@color/widget_background` at its percentage — the same hue
     * as the opaque card, so the only thing the setting changes is how much
     * wallpaper shows through. ±1 because the platform rounds `alpha * 255`.
     */
    @Test
    fun `each step draws the widget green at its own alpha`() {
        val green = context.getColor(R.color.widget_background)

        WidgetOpacity.entries.forEach { opacity ->
            val color = solidColor(context.getDrawable(opacity.backgroundRes()))

            assertEquals("$opacity hue", green and 0x00FFFFFF, color and 0x00FFFFFF)
            assertAlpha("$opacity alpha", opacity.percent, color)
        }
    }

    @Test
    fun `a translucent step reaches the recent-notes widget`() {
        WidgetPaletteStore.save(context, ColorPalette.MARKLEAF_GREEN, ThemeMode.SYSTEM, WidgetOpacity.HALF)

        assertAlpha("background", 50, solidColor(inflateQuickNoteWidget().background))
    }

    /**
     * The Material You tint and a translucent step compose: the tint recolours
     * the card and the drawable underneath still carries the step's alpha,
     * which the tint's default `SRC_IN` mode keeps.
     */
    @Test
    fun `material you keeps its tint on a translucent step`() {
        WidgetPaletteStore.save(context, ColorPalette.MATERIAL_YOU, ThemeMode.SYSTEM, WidgetOpacity.LOW)

        val view = inflateQuickNoteWidget()

        assertNotNull(view.backgroundTintList)
        assertAlpha("background", 25, solidColor(view.background))
    }

    /**
     * The regression this guards: the host re-applies a new `RemoteViews` onto
     * the view it already shows when the layout is unchanged (Robolectric's
     * shadow does the same), so a 100% that wrote nothing would leave the
     * widget at whatever step it was last given.
     */
    @Test
    fun `moving back to opaque repaints the widget opaque`() {
        val manager = AppWidgetManager.getInstance(context)
        val id = shadowOf(manager)
            .createWidgets(QuickNoteWidget::class.java, R.layout.widget_quick_note, 1)
            .first()

        WidgetPaletteStore.save(context, ColorPalette.MARKLEAF_GREEN, ThemeMode.SYSTEM, WidgetOpacity.NONE)
        QuickNoteWidget.updateAppWidget(context, manager, id)
        assertAlpha("after 0%", 0, solidColor(shadowOf(manager).getViewFor(id).background))

        WidgetPaletteStore.save(context, ColorPalette.MARKLEAF_GREEN, ThemeMode.SYSTEM, WidgetOpacity.OPAQUE)
        QuickNoteWidget.updateAppWidget(context, manager, id)
        assertAlpha("after 100%", 100, solidColor(shadowOf(manager).getViewFor(id).background))
    }

    @Test
    fun `a translucent step reaches the single-note widget`() {
        WidgetPaletteStore.save(context, ColorPalette.MARKLEAF_GREEN, ThemeMode.SYSTEM, WidgetOpacity.HIGH)
        val manager = AppWidgetManager.getInstance(context)
        val id = shadowOf(manager)
            .createWidgets(SingleNoteWidget::class.java, R.layout.widget_single_note, 1)
            .first()

        SingleNoteWidget.updateAppWidget(context, manager, id)

        assertAlpha("background", 75, solidColor(shadowOf(manager).getViewFor(id).background))
    }

    private fun inflateQuickNoteWidget(): View {
        val manager = AppWidgetManager.getInstance(context)
        val id = shadowOf(manager)
            .createWidgets(QuickNoteWidget::class.java, R.layout.widget_quick_note, 1)
            .first()
        QuickNoteWidget.updateAppWidget(context, manager, id)
        return shadowOf(manager).getViewFor(id)
    }

    /** The shape's own fill, before any tint — which is where the step's alpha lives. */
    private fun solidColor(drawable: android.graphics.drawable.Drawable?): Int {
        val shape = drawable as GradientDrawable
        return requireNotNull(shape.color).defaultColor
    }

    private fun assertAlpha(message: String, percent: Int, color: Int) {
        val expected = percent * 255 / 100.0
        val actual = Color.alpha(color)
        assertTrue("$message: expected ~$expected, was $actual", abs(actual - expected) <= 1.0)
    }
}
