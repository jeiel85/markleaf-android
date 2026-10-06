package com.markleaf.notes.feature.settings

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.ui.theme.MarkleafTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The shortcut row above Settings (#517): the page is one long scroll, and a
 * chip per section jumps to its heading. "At the top" is measured against the
 * Appearance heading on first open, which is where every heading should land.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class SettingsSectionShortcutsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun heading(text: String) = composeRule.onNode(hasText(text) and isHeading())
    private fun shortcut(text: String) = composeRule.onNode(hasText(text) and hasClickAction() and !isHeading())
    // The row scrolls sideways on a phone, so bring the chip on screen first.
    private fun jump(text: String) {
        shortcut(text).performScrollTo().performClick()
        composeRule.waitForIdle()
    }
    private fun SemanticsNodeInteraction.top() = fetchSemanticsNode().boundsInRoot.top

    private fun showSettings(): Float {
        composeRule.setContent {
            MarkleafTheme(dynamicColor = false) { SettingsScreen(onBack = {}) }
        }
        return heading("Appearance").top()
    }

    @Test
    fun aShortcutScrollsItsSectionHeadingToTheTop() {
        val pageTop = showSettings()
        heading("Privacy").assertIsNotDisplayed()

        jump("Privacy")

        heading("Privacy").assertIsDisplayed()
        assertEquals(pageTop, heading("Privacy").top(), 1f)
    }

    @Test
    fun focusMovesToTheHeadingAfterTheJump() {
        // #262: the page scrolled, but TalkBack stayed on the chip (on a device,
        // it went back to the top bar), so a screen-reader user had to swipe
        // through the whole page to reach the section they asked for. Focus on
        // the heading is what TalkBack follows.
        showSettings()

        jump("Privacy")

        heading("Privacy").assertIsFocused()
    }

    @Test
    fun focusFollowsEachJumpNotJustTheFirst() {
        showSettings()
        jump("Data")

        jump("Appearance")

        heading("Appearance").assertIsFocused()
    }

    @Test
    fun syncShortcutUsesAShortLabelButLandsOnTheSyncHeading() {
        val pageTop = showSettings()

        jump("Sync")

        assertEquals(pageTop, heading("Multi-device sync (folder mirror)").top(), 1f)
    }

    @Test
    fun goingBackUpWorksFromFurtherDown() {
        val pageTop = showSettings()
        jump("Data")

        jump("Markdown")

        assertEquals(pageTop, heading("Markdown").top(), 1f)
    }

    @Test
    fun theLastSectionScrollsAsFarAsThePageGoes() {
        showSettings()

        // App is too short to reach the top; the page stops at its end and the
        // heading is on screen rather than overshooting into empty space.
        jump("App")

        heading("App").assertIsDisplayed()
    }
}
