package com.markleaf.notes.ui.theme

import android.graphics.drawable.ColorDrawable
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The activity window background has to follow the theme the app resolved to,
 * not the `-night` resource qualifier (#354).
 *
 * `Theme.Markleaf` is Material Light in `values/` and Material Dark in
 * `values-night/`, so the platform picks between them by the system dark-mode
 * setting alone. With Settings → Appearance → Theme set against the system, the
 * window underneath the app kept the platform's colour and showed through the
 * navigation cross-fade. These assertions pin the colour that used to be wrong.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class ThemeWindowBackgroundTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun darkThemePaintsTheWindowWithTheDarkBackground() {
        composeRule.setContent {
            MarkleafTheme(darkTheme = true) {}
        }
        composeRule.waitForIdle()

        assertEquals(DarkColorScheme.background.toArgb(), windowBackgroundArgb())
    }

    @Test
    fun lightThemePaintsTheWindowWithTheLightBackground() {
        composeRule.setContent {
            MarkleafTheme(darkTheme = false) {}
        }
        composeRule.waitForIdle()

        assertEquals(LightColorScheme.background.toArgb(), windowBackgroundArgb())
    }

    /**
     * The regression itself: the theme changes at runtime — the Settings toggle
     * writes a preference, the app recomposes — so a background applied only at
     * activity start would be stale from the second frame onwards.
     */
    @Test
    fun switchingThemeAtRuntimeRepaintsTheWindow() {
        val dark = mutableStateOf(false)
        composeRule.setContent {
            MarkleafTheme(darkTheme = dark.value) {}
        }
        composeRule.waitForIdle()
        assertEquals(LightColorScheme.background.toArgb(), windowBackgroundArgb())

        dark.value = true
        composeRule.waitForIdle()

        assertEquals(DarkColorScheme.background.toArgb(), windowBackgroundArgb())
    }

    private fun windowBackgroundArgb(): Int {
        val background = composeRule.activity.window.decorView.background
        return (background as ColorDrawable).color
    }
}
