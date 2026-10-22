package com.markleaf.notes.feature.settings

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assert
import androidx.compose.ui.text.AnnotatedString
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
 * Settings search (#517) on the list of sections: a result opens the page
 * that holds the setting, with the setting at the top and focus on it — the
 * same place the page's first setting sits when the page is opened from its
 * row.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class SettingsSearchScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun field() = composeRule.onNode(hasSetTextAction())
    private fun row(text: String) = composeRule.onNode(hasText(text) and hasClickAction() and !isHeading())
    private fun backArrow() = composeRule.onNode(hasContentDescription("Back") and hasClickAction())
    // A result row merges its title with its section's name, so match on the
    // title alone: "Privacy" is also the section line under every privacy row.
    private fun result(title: String) = composeRule.onNode(
        hasClickAction() and SemanticsMatcher("result titled $title") {
            it.config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text == title
        }
    )
    private fun label(text: String) = composeRule.onNode(hasText(text) and !hasClickAction())
    private fun SemanticsNodeInteraction.top() = fetchSemanticsNode().boundsInRoot.top

    private fun showSettings() {
        composeRule.setContent {
            MarkleafTheme(dynamicColor = false) { SettingsScreen(onBack = {}) }
        }
        composeRule.waitForIdle()
    }

    /**
     * Where a page's first setting sits when the page is opened from its row:
     * the top every landing should match. Leaves the list showing again.
     */
    private fun pageTop(section: String, firstLabel: String): Float {
        row(section).performClick()
        composeRule.waitForIdle()
        val top = label(firstLabel).top()
        backArrow().performClick()
        composeRule.waitForIdle()
        return top
    }

    private fun search(query: String) {
        field().performTextInput(query)
        composeRule.waitForIdle()
    }

    private fun pick(title: String) {
        result(title).performClick()
        composeRule.waitForIdle()
    }

    /**
     * A setting with more than a screen of page below it, so it can reach the
     * top. A setting near the end of a page stops where the page ends, as the
     * old single scroll did for its last section.
     */
    @Test
    fun aResultOpensItsPageWithTheSettingAtTheTopAndFocused() {
        showSettings()
        val top = pageTop("Notes & search", "Show note previews")
        search("reopen last note")

        pick(REOPEN)

        val setting = label(REOPEN)
        setting.assertIsDisplayed()
        setting.assertIsFocused()
        assertEquals(top, setting.top(), 1f)
    }

    /** A page too short to scroll still shows and focuses the setting that was asked for. */
    @Test
    fun aResultOnAShortPageIsShownAndFocused() {
        showSettings()
        search("screenshots")

        pick(SCREENSHOTS)

        val setting = label(SCREENSHOTS)
        setting.assertIsDisplayed()
        setting.assertIsFocused()
    }

    @Test
    fun pickingAResultClearsTheQueryForTheWayBack() {
        showSettings()
        search("screenshots")
        pick(SCREENSHOTS)

        backArrow().performClick()
        composeRule.waitForIdle()

        // EditableText, not assertTextEquals: Text also holds the placeholder.
        field().assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        row("Markdown").assertIsDisplayed()
    }

    @Test
    fun anOptionLabelFindsItsSetting() {
        showSettings()

        search("dark")

        result("Theme").assertIsDisplayed()
    }

    @Test
    fun aSectionResultOpensThePageAtItsTitle() {
        showSettings()
        search("privacy")

        pick("Privacy")

        composeRule.onNode(hasText("Privacy") and isHeading()).assertIsFocused()
    }

    /** "Folder mirror" is only in Sync's long heading, which search still reads. */
    @Test
    fun theLongSyncHeadingStillFindsTheSyncPage() {
        showSettings()

        search("folder mirror")

        result("Multi-device sync (folder mirror)").assertIsDisplayed()
    }

    /** A result names the page it opens by that page's title, Sync's short word included. */
    @Test
    fun aResultNamesItsPageByThePageTitle() {
        showSettings()

        search("note metadata")

        composeRule.onNode(hasText("Note metadata") and hasText("Sync")).assertIsDisplayed()
    }

    /** Snippet buttons are edited only here, so search is how they're found (#436). */
    @Test
    fun snippetButtonsAreFoundByName() {
        showSettings()
        val top = pageTop("Markdown", "Show Markdown syntax")
        search("snippet")

        pick("Snippet buttons")

        val setting = label("Snippet buttons")
        setting.assertIsFocused()
        assertEquals(top, setting.top(), 1f)
    }

    @Test
    fun aQueryWithNoMatchSaysSo() {
        showSettings()

        search("zzzz")

        composeRule.onNode(hasText("No settings match")).assertIsDisplayed()
    }

    @Test
    fun clearingTheQueryBringsTheSectionsBack() {
        showSettings()
        search("zzzz")

        composeRule.onNode(hasContentDescription("Close") and hasClickAction()).performClick()
        composeRule.waitForIdle()

        row("Appearance").assertIsDisplayed()
        assertTrue(composeRule.onAllNodes(hasText("No settings match")).fetchSemanticsNodes().isEmpty())
    }

    /** The field is on screen every time Settings opens; it must not raise the keyboard by itself. */
    @Test
    fun openingSettingsDoesNotFocusTheSearchField() {
        showSettings()

        field().assertIsNotFocused()
    }

    private companion object {
        const val SCREENSHOTS = "Block screenshots and Recents preview"
        const val REOPEN = "Reopen last note on launch"
    }
}
