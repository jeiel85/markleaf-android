package com.markleaf.notes.feature.settings

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.ui.theme.MarkleafTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Settings search (#517): the shortcut row reaches a section, search reaches
 * the setting itself — scrolled to the top of the page, with focus on it, the
 * same landing a shortcut gives its heading.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class SettingsSearchScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun searchButton() = composeRule.onNode(hasContentDescription("Search settings") and hasClickAction())
    private fun field() = composeRule.onNode(hasSetTextAction())
    // A result row merges its title with its section's name, so match on the
    // title alone: "Privacy" is also the section line under every privacy row.
    private fun result(title: String) = composeRule.onNode(
        hasClickAction() and SemanticsMatcher("result titled $title") {
            it.config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text == title
        }
    )
    private fun SemanticsNodeInteraction.top() = fetchSemanticsNode().boundsInRoot.top

    private fun showSettings(): Float {
        composeRule.setContent {
            MarkleafTheme(dynamicColor = false) { SettingsScreen(onBack = {}) }
        }
        return composeRule.onNode(hasText("Appearance") and isHeading()).top()
    }

    private fun search(query: String) {
        searchButton().performClick()
        field().performTextInput(query)
        composeRule.waitForIdle()
    }

    @Test
    fun aResultScrollsItsSettingToTheTopAndFocusesIt() {
        val pageTop = showSettings()
        search("screenshots")

        result(SCREENSHOTS).performClick()
        composeRule.waitForIdle()

        val setting = composeRule.onNode(hasText(SCREENSHOTS) and !hasClickAction())
        setting.assertIsDisplayed()
        setting.assertIsFocused()
        assertEquals(pageTop, setting.top(), 1f)
    }

    @Test
    fun pickingAResultClosesSearchAndBringsTheShortcutsBack() {
        showSettings()
        search("screenshots")

        result(SCREENSHOTS).performClick()
        composeRule.waitForIdle()

        assertTrue(composeRule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isEmpty())
        composeRule.onNode(hasText("Markdown") and hasClickAction() and !isHeading()).assertIsDisplayed()
    }

    @Test
    fun anOptionLabelFindsItsSetting() {
        showSettings()

        search("dark")

        result("Theme").assertIsDisplayed()
    }

    @Test
    fun aSectionResultLandsOnItsHeading() {
        val pageTop = showSettings()
        search("privacy")

        result("Privacy").performClick()
        composeRule.waitForIdle()

        val heading = composeRule.onNode(hasText("Privacy") and isHeading())
        heading.assertIsFocused()
        assertEquals(pageTop, heading.top(), 1f)
    }

    @Test
    fun aQueryWithNoMatchSaysSo() {
        showSettings()

        search("zzzz")

        composeRule.onNode(hasText("No settings match")).assertIsDisplayed()
    }

    private companion object {
        const val SCREENSHOTS = "Block screenshots and Recents preview"
    }
}
