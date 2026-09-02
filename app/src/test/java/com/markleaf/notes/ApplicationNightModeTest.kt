package com.markleaf.notes

import android.app.UiModeManager
import com.markleaf.notes.data.settings.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Which night mode the app hands the system for each Theme (#354).
 *
 * This is what reaches the *starting window* — the splash the system draws
 * before any app code runs, and the one thing the in-app window repaint could
 * not touch. Getting a value wrong here does not fail anything at build time
 * and shows up only as a splash in the wrong colour on the launch *after* the
 * setting is changed, which is a slow way to find out.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ApplicationNightModeTest {

    @Test
    fun `dark asks the system for night`() {
        assertEquals(UiModeManager.MODE_NIGHT_YES, ThemeMode.DARK.toApplicationNightMode())
    }

    @Test
    fun `light asks the system for notnight`() {
        assertEquals(UiModeManager.MODE_NIGHT_NO, ThemeMode.LIGHT.toApplicationNightMode())
    }

    /**
     * The one that is not obvious. There is no `getApplicationNightMode`, and
     * `MODE_NIGHT_AUTO` is documented in terms of location and sensors rather
     * than as "release the app override" — so this is the value that had to be
     * established on a device rather than read off the API, and the one a future
     * reader is most likely to talk themselves out of.
     */
    @Test
    fun `system releases the override instead of leaving the last one in place`() {
        assertEquals(UiModeManager.MODE_NIGHT_AUTO, ThemeMode.SYSTEM.toApplicationNightMode())
    }

    /** Every mode maps to something; a new one must not fall through. */
    @Test
    fun `every theme mode has a night mode`() {
        val modes = ThemeMode.entries.map { it.toApplicationNightMode() }

        assertEquals(ThemeMode.entries.size, modes.distinct().size)
    }
}
