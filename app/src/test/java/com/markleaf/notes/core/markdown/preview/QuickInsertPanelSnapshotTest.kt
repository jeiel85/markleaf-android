package com.markleaf.notes.core.markdown.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.markleaf.notes.feature.editor.QuickInsertCommand
import com.markleaf.notes.feature.editor.QuickInsertDisplayItem
import com.markleaf.notes.feature.editor.QuickInsertPanel
import com.markleaf.notes.feature.editor.quickInsertDisplayItems
import com.markleaf.notes.ui.theme.MarkleafTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class QuickInsertPanelSnapshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun panelShowsSelectionAndInvokesPickedCommand() {
        var picked: QuickInsertCommand? = null
        composeRule.setContent {
            MarkleafTheme(dynamicColor = false) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    QuickInsertPanel(
                        items = displayItems(),
                        selectedIndex = 0,
                        onPick = { picked = it }
                    )
                }
            }
        }

        composeRule.onNodeWithText("Heading 1").assertIsDisplayed().assertIsSelected()
        composeRule.onNodeWithText("Code block").performClick()
        assertEquals(QuickInsertCommand.CODE_BLOCK, picked)
        composeRule.onRoot().captureRoboImage(
            filePath = "build/reports/quick-insert/quick-insert-panel-phone.png"
        )
    }

    @Test
    @Config(sdk = [33], qualifiers = "ko-rKR-w360dp-h640dp-notnight-mdpi")
    fun panelRendersKoreanLabelsWithoutClipping() {
        composeRule.setContent {
            MarkleafTheme(dynamicColor = false) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    QuickInsertPanel(
                        items = quickInsertDisplayItems(),
                        selectedIndex = 0,
                        onPick = {}
                    )
                }
            }
        }

        composeRule.onNodeWithText("제목 1").assertIsDisplayed().assertIsSelected()
        composeRule.onRoot().captureRoboImage(
            filePath = "build/reports/quick-insert/quick-insert-panel-korean.png"
        )
    }

    @Test
    @Config(sdk = [33], qualifiers = "w800dp-h600dp-mdpi")
    fun panelRendersAtTabletWidth() {
        composeRule.setContent {
            MarkleafTheme(dynamicColor = false) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    QuickInsertPanel(
                        items = displayItems(),
                        selectedIndex = 3,
                        onPick = {}
                    )
                }
            }
        }

        composeRule.onNodeWithText("Link to note").assertIsDisplayed().assertIsSelected()
        composeRule.onRoot().captureRoboImage(
            filePath = "build/reports/quick-insert/quick-insert-panel-tablet.png"
        )
    }

    private fun displayItems(): List<QuickInsertDisplayItem> = listOf(
        QuickInsertDisplayItem(QuickInsertCommand.HEADING_1, "Heading 1", "#"),
        QuickInsertDisplayItem(QuickInsertCommand.CODE_BLOCK, "Code block", "```"),
        QuickInsertDisplayItem(QuickInsertCommand.TABLE, "Table", "| |"),
        QuickInsertDisplayItem(QuickInsertCommand.WIKILINK, "Link to note", "[[ ]]"),
        QuickInsertDisplayItem(QuickInsertCommand.IMAGE, "Image", "![]()"),
        QuickInsertDisplayItem(QuickInsertCommand.DATE, "Today’s date", "yyyy-MM-dd")
    )
}
