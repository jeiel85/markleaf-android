package com.markleaf.notes.feature.editor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.core.markdown.preview.TocHeading
import com.markleaf.notes.ui.theme.MarkleafTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The outline on its own screen (#215): every level listed, every row tappable,
 * and the tap carrying back the whole heading — the source line is what lets
 * the editor jump without flipping into preview.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class NoteOutlineScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun listsEveryHeadingLevel() {
        render(headings())

        composeRule.onNodeWithText("Project brief").assertIsDisplayed()
        composeRule.onNodeWithText("Next steps").assertIsDisplayed()
        composeRule.onNodeWithText("References").assertIsDisplayed()
    }

    @Test
    fun tappingAnEntryReportsTheWholeHeading() {
        var tapped: TocHeading? = null
        render(headings(), onHeadingClick = { tapped = it })

        composeRule.onNodeWithText("Next steps").performClick()

        assertEquals("Next steps", tapped?.text)
        assertEquals(3, tapped?.index)
        // Without this the editor cannot place a caret, and the jump falls back
        // to flipping the note into preview.
        assertEquals(9, tapped?.sourceLine)
    }

    @Test
    fun aNoteWithoutHeadingsSaysSo() {
        render(emptyList())

        composeRule.onNodeWithText("No headings yet.").assertIsDisplayed()
    }

    @Test
    fun theBarOffersAWayBack() {
        var closed = false
        composeRule.setContent {
            MarkleafTheme(dynamicColor = false) {
                NoteOutlineTopBar(onClose = { closed = true })
            }
        }

        composeRule.onNodeWithText("Table of contents").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Close").performClick()

        assertEquals(true, closed)
    }

    private fun render(
        headings: List<TocHeading>,
        onHeadingClick: (TocHeading) -> Unit = {}
    ) {
        composeRule.setContent {
            MarkleafTheme(dynamicColor = false) {
                NoteOutlineContent(headings = headings, onHeadingClick = onHeadingClick)
            }
        }
    }

    private fun headings() = listOf(
        TocHeading(index = 0, text = "Project brief", level = 1, sourceLine = 0),
        TocHeading(index = 3, text = "Next steps", level = 2, sourceLine = 9),
        TocHeading(index = 7, text = "References", level = 3, sourceLine = 21)
    )
}
