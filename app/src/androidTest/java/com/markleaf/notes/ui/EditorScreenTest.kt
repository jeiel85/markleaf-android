package com.markleaf.notes.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.markleaf.notes.feature.editor.EditorScreen
import com.markleaf.notes.ui.theme.MarkleafTheme
import org.junit.Rule
import org.junit.Test

class EditorScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun editorScreen_showsTitle() {
        composeTestRule.setContent {
            MarkleafTheme {
                EditorScreen(onBack = {})
            }
        }

        composeTestRule.onNodeWithText("New Note").assertIsDisplayed()
    }

    @Test
    fun editorScreen_togglePreview() {
        composeTestRule.setContent {
            MarkleafTheme {
                EditorScreen(onBack = {})
            }
        }

        // Click Preview button
        composeTestRule.onNodeWithText("Preview").performClick()
        
        // Check if "Preview" text (from TopAppBar title) is displayed
        composeTestRule.onNodeWithText("Preview").assertIsDisplayed()
        
        // Button text should change to "Edit"
        composeTestRule.onNodeWithText("Edit").assertIsDisplayed()
    }
}
