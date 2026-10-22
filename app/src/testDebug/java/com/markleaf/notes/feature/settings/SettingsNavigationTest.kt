package com.markleaf.notes.feature.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.ui.theme.MarkleafTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Settings as a list of sections, each opening its own page. It replaced the
 * single long scroll and its row of shortcut chips, which hid most sections
 * off the side of a phone screen.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class SettingsNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var backs = 0

    private fun row(text: String) = composeRule.onNode(hasText(text) and hasClickAction() and !isHeading())
    private fun title(text: String) = composeRule.onNode(hasText(text) and isHeading())
    private fun noNode(text: String) =
        assertTrue(composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isEmpty())
    private fun backArrow() = composeRule.onNode(hasContentDescription("Back") and hasClickAction())
    private fun systemBack() {
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }

    private fun showSettings(
        initialSection: SettingsShortcut? = null,
        initialItem: SettingsItem? = null
    ) {
        composeRule.setContent {
            MarkleafTheme(dynamicColor = false) {
                SettingsScreen(
                    onBack = { backs++ },
                    initialSection = initialSection,
                    initialItem = initialItem
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun open(text: String) {
        row(text).performClick()
        composeRule.waitForIdle()
    }

    /** Every section is on the first screen of a phone, which the chip row never managed. */
    @Test
    fun everySectionHasARowOnScreen() {
        showSettings()

        listOf("Appearance", "Markdown", "Notes & search", "Privacy", "Data", "Sync", "Open source", "App")
            .forEach { row(it).assertIsDisplayed() }
    }

    @Test
    fun aRowOpensItsPageWithItsTitleFocused() {
        showSettings()

        open("Privacy")

        // #262: TalkBack carries on from the page's title, not the row that
        // is gone or the top of the app bar.
        title("Privacy").assertIsFocused()
        composeRule.onNode(hasText(SCREENSHOTS)).assertIsDisplayed()
        noNode("Appearance")
    }

    @Test
    fun systemBackFromAPageReturnsToTheList() {
        showSettings()
        open("Data")

        systemBack()

        row("Data").assertIsDisplayed()
        row("Appearance").assertIsDisplayed()
        assertEquals(0, backs)
    }

    @Test
    fun theBackArrowOnAPageReturnsToTheList() {
        showSettings()
        open("Markdown")

        backArrow().performClick()
        composeRule.waitForIdle()

        row("Markdown").assertIsDisplayed()
        assertEquals(0, backs)
    }

    @Test
    fun theBackArrowOnTheListLeavesSettings() {
        showSettings()

        backArrow().performClick()

        assertEquals(1, backs)
    }

    /** The row and the app bar use the short word; the page still opens on the sync block. */
    @Test
    fun syncOpensUnderItsShortName() {
        showSettings()

        open("Sync")

        title("Sync").assertIsDisplayed()
        composeRule.onNode(hasText("Choose sync folder") and hasClickAction()).assertIsDisplayed()
    }

    @Test
    fun eachPageStartsAtItsOwnTop() {
        showSettings()
        open("Notes & search")
        val firstSetting = composeRule.onNode(hasText("Show note previews") and !hasClickAction())
        val top = firstSetting.fetchSemanticsNode().boundsInRoot.top
        // Leave this page scrolled well down.
        composeRule.onNode(hasText("First line")).performScrollTo()
        assertTrue(firstSetting.fetchSemanticsNode().boundsInRoot.top < top)
        backArrow().performClick()
        composeRule.waitForIdle()

        open("Notes & search")

        assertEquals(top, firstSetting.fetchSemanticsNode().boundsInRoot.top, 1f)
    }

    /**
     * The Locked space's "set a passcode" opens Privacy at the passcode (the
     * route is `NavRoutes.SETTINGS_PASSCODE`), and Back goes back there rather
     * than to a list the reader never saw.
     */
    @Test
    fun aDeepLinkLandsOnItsSettingAndBackLeaves() {
        showSettings(SettingsShortcut.PRIVACY, SettingsItem.LOCKED_PASSCODE)

        composeRule.onNode(hasText("Locked notes passcode") and !hasClickAction()).assertIsFocused()
        title("Privacy").assertIsDisplayed()

        backArrow().performClick()

        assertEquals(1, backs)
    }

    private companion object {
        const val SCREENSHOTS = "Block screenshots and Recents preview"
    }
}
