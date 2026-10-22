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
 * Settings as a list of categories, each opening its own page of grouped
 * settings. It replaced the single long scroll and its row of shortcut chips,
 * which hid most sections off the side of a phone screen.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class SettingsNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var backs = 0

    // A row merges its name with the summary under it; hasText matches either.
    private fun row(text: String) = composeRule.onNode(hasText(text) and hasClickAction() and !isHeading())
    private fun heading(text: String) = composeRule.onNode(hasText(text) and isHeading())
    private fun noNode(text: String) =
        assertTrue(composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isEmpty())
    private fun backArrow() = composeRule.onNode(hasContentDescription("Back") and hasClickAction())
    private fun systemBack() {
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }

    private fun showSettings(
        initialSection: SettingsCategory? = null,
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

    /** Every category is on the first screen of a phone, which the chip row never managed. */
    @Test
    fun everyCategoryHasARowOnScreen() {
        showSettings()

        listOf("Appearance", "Editor", "Notes", "Privacy & security", "Sync & export", "About")
            .forEach { row(it).assertIsDisplayed() }
    }

    /** Each row says where its main settings stand, from a fresh install's defaults. */
    @Test
    fun eachRowSummarisesItsSettings() {
        showSettings()

        row("Appearance").assertIsDisplayed()
        composeRule.onNode(hasText("System · Markleaf green")).assertIsDisplayed()
        composeRule.onNode(hasText("Sans · Medium · Comfortable")).assertIsDisplayed()
        composeRule.onNode(hasText("List · First heading · Top")).assertIsDisplayed()
        composeRule.onNode(hasText("No locks on")).assertIsDisplayed()
        composeRule.onNode(hasText("Not syncing")).assertIsDisplayed()
        composeRule.onNode(hasText("Open source · Apache License 2.0")).assertIsDisplayed()
    }

    @Test
    fun aRowOpensItsPageWithItsTitleFocused() {
        showSettings()

        open("Privacy & security")

        // #262: TalkBack carries on from the page's title, not the row that
        // is gone or the top of the app bar.
        heading("Privacy & security").assertIsFocused()
        composeRule.onNode(hasText(SCREENSHOTS)).assertIsDisplayed()
        noNode("Appearance")
    }

    /** Groups on a page carry headings, so the page reads as parts rather than one run. */
    @Test
    fun aPageShowsItsGroupHeadings() {
        showSettings()

        open("Privacy & security")

        heading("Lock").assertIsDisplayed()
        heading("Screenshots & shortcuts").assertIsDisplayed()
    }

    @Test
    fun systemBackFromAPageReturnsToTheList() {
        showSettings()
        open("Sync & export")

        systemBack()

        row("Sync & export").assertIsDisplayed()
        row("Appearance").assertIsDisplayed()
        assertEquals(0, backs)
    }

    @Test
    fun theBackArrowOnAPageReturnsToTheList() {
        showSettings()
        open("Editor")

        backArrow().performClick()
        composeRule.waitForIdle()

        row("Editor").assertIsDisplayed()
        assertEquals(0, backs)
    }

    @Test
    fun theBackArrowOnTheListLeavesSettings() {
        showSettings()

        backArrow().performClick()

        assertEquals(1, backs)
    }

    /** Sync and export are one page now: the folder mirror first, export below it. */
    @Test
    fun syncAndExportShareAPage() {
        showSettings()

        open("Sync & export")

        heading("Multi-device sync (folder mirror)").assertIsDisplayed()
        composeRule.onNode(hasText("Choose sync folder") and hasClickAction()).assertIsDisplayed()
        composeRule.onNode(hasText("Export notes by tag…")).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun eachPageStartsAtItsOwnTop() {
        showSettings()
        open("Notes")
        val firstHeading = heading("Notes list")
        val top = firstHeading.fetchSemanticsNode().boundsInRoot.top
        // Leave this page scrolled well down.
        composeRule.onNode(hasText("Start new notes with a heading")).performScrollTo()
        assertTrue(firstHeading.fetchSemanticsNode().boundsInRoot.top < top)
        backArrow().performClick()
        composeRule.waitForIdle()

        open("Notes")

        assertEquals(top, firstHeading.fetchSemanticsNode().boundsInRoot.top, 1f)
    }

    /**
     * The Locked space's "set a passcode" opens Privacy & security at the
     * passcode (the route is `NavRoutes.SETTINGS_PASSCODE`), and Back goes
     * back there rather than to a list the reader never saw.
     */
    @Test
    fun aDeepLinkLandsOnItsSettingAndBackLeaves() {
        showSettings(SettingsCategory.PRIVACY, SettingsItem.LOCKED_PASSCODE)

        composeRule.onNode(hasText("Locked notes passcode") and !hasClickAction()).assertIsFocused()
        heading("Privacy & security").assertIsDisplayed()

        backArrow().performClick()

        assertEquals(1, backs)
    }

    private companion object {
        const val SCREENSHOTS = "Block screenshots and Recents preview"
    }
}
