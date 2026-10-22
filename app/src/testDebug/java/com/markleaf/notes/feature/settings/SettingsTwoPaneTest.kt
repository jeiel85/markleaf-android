package com.markleaf.notes.feature.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.ui.theme.MarkleafTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Settings on a wide screen (`WindowWidthSizeClass.Expanded`, the width the
 * notes screen also splits at): the list on the left and the open category's
 * page beside it, instead of one replacing the other.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], qualifiers = "w1280dp-h800dp-mdpi")
class SettingsTwoPaneTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var backs = 0

    private fun row(text: String) = composeRule.onNode(hasText(text) and hasClickAction() and !isHeading())
    private fun paneTitle(text: String) = composeRule.onNode(hasText(text) and isHeading())
    private fun result(title: String) = composeRule.onNode(
        hasClickAction() and SemanticsMatcher("result titled $title") {
            it.config.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text == title
        }
    )

    private fun showSettings() {
        composeRule.setContent {
            MarkleafTheme(dynamicColor = false) {
                SettingsScreen(onBack = { backs++ }, twoPane = true)
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun theListAndTheFirstCategoryShowTogether() {
        showSettings()

        row("Editor").assertIsDisplayed()
        row("About").assertIsDisplayed()
        paneTitle("Appearance").assertIsDisplayed()
        composeRule.onNode(hasText("Theme") and !hasClickAction()).assertIsDisplayed()
        row("Appearance").assertIsSelected()
    }

    @Test
    fun aRowOpensItsPageBesideTheList() {
        showSettings()

        row("Privacy & security").performClick()
        composeRule.waitForIdle()

        paneTitle("Privacy & security").assertIsFocused()
        row("Privacy & security").assertIsSelected()
        row("Appearance").assertIsNotSelected()
        row("Editor").assertIsDisplayed()
    }

    @Test
    fun aSearchResultOpensItsPageBesideTheList() {
        showSettings()
        composeRule.onNode(hasSetTextAction()).performTextInput("notes list layout")
        composeRule.waitForIdle()

        result("Notes list layout").performClick()
        composeRule.waitForIdle()

        composeRule.onNode(hasText("Notes list layout") and !hasClickAction()).assertIsFocused()
        row("Notes").assertIsSelected()
    }

    /** With the list already on screen there is nothing to go back to inside Settings. */
    @Test
    fun theBackArrowLeavesSettingsWithAPageOpen() {
        showSettings()
        row("Sync & export").performClick()
        composeRule.waitForIdle()

        composeRule.onNode(hasContentDescription("Back") and hasClickAction()).performClick()

        assertEquals(1, backs)
    }
}
