package com.markleaf.notes.feature.editor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.core.markdown.preview.TocHeading
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.ui.theme.MarkleafTheme
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class EditorInfoSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun populatedSheetShowsEveryInformationSection() {
        render(populatedState())

        composeRule.onNodeWithText("Note information").assertIsDisplayed()
        composeRule.onNodeWithText("Statistics").assertIsDisplayed()
        composeRule.onNodeWithText("12 words · 86 chars · 1 min").assertIsDisplayed()
        composeRule.onNodeWithText("Table of contents").assertIsDisplayed()
        composeRule.onNodeWithText("Overview").assertIsDisplayed()
        composeRule.onNodeWithText("Linked from").assertIsDisplayed()
        composeRule.onNodeWithText("Source note").assertIsDisplayed()
    }

    @Test
    fun headingAndBacklinkRowsInvokeNavigation() {
        var headingIndex: Int? = null
        var backlinkId: String? = null
        render(
            state = populatedState(),
            onHeadingClick = { headingIndex = it },
            onBacklinkClick = { backlinkId = it }
        )

        composeRule.onNodeWithText("Overview").performClick()
        composeRule.onNodeWithText("Source note").performClick()

        assertEquals(3, headingIndex)
        assertEquals("source-note", backlinkId)
    }

    @Test
    fun emptyOutlineAndBacklinksRemainUnderstandable() {
        render(
            EditorInfoUiState(
                statsText = "0 words · 0 chars · 0 min",
                headings = emptyList(),
                backlinks = emptyList()
            )
        )

        composeRule.onNodeWithText("No headings yet.").assertIsDisplayed()
        composeRule.onNodeWithText("No notes link here yet.").assertIsDisplayed()
    }

    private fun render(
        state: EditorInfoUiState,
        onHeadingClick: (Int) -> Unit = {},
        onBacklinkClick: (String) -> Unit = {}
    ) {
        composeRule.setContent {
            MarkleafTheme(dynamicColor = false) {
                EditorInfoSheet(
                    state = state,
                    onHeadingClick = onHeadingClick,
                    onBacklinkClick = onBacklinkClick,
                    onDismiss = {}
                )
            }
        }
    }

    private fun populatedState() = EditorInfoUiState(
        statsText = "12 words · 86 chars · 1 min",
        headings = listOf(TocHeading(index = 3, text = "Overview", level = 1)),
        backlinks = listOf(
            Note(
                id = "source-note",
                title = "Source note",
                contentMarkdown = "# Source note",
                excerpt = "",
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH
            )
        )
    )
}
