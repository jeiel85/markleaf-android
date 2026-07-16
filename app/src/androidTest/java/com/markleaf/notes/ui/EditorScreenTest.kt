package com.markleaf.notes.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.markleaf.notes.R
import com.markleaf.notes.TestHostActivity
import com.markleaf.notes.feature.editor.EditorScreen
import com.markleaf.notes.ui.theme.MarkleafTheme
import org.junit.After
import org.junit.Rule
import org.junit.Test

class EditorScreenTest {
    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private var scenario: ActivityScenario<TestHostActivity>? = null

    @After
    fun tearDown() {
        scenario?.close()
        TestHostActivity.content = null
    }

    private fun launchEditor() {
        TestHostActivity.content = {
            MarkleafTheme {
                EditorScreen(onBack = {})
            }
        }
        scenario = ActivityScenario.launch(TestHostActivity::class.java)
        composeTestRule.waitForIdle()
    }

    @Test
    fun editorScreen_showsTitle() {
        launchEditor()

        val newNoteLabel = context.getString(R.string.new_note)
        composeTestRule.onNodeWithText(newNoteLabel).assertIsDisplayed()
    }

    @Test
    fun editorScreen_togglePreview() {
        launchEditor()

        val previewLabel = context.getString(R.string.preview)
        val editLabel = context.getString(R.string.edit)

        composeTestRule.onNodeWithContentDescription(previewLabel).performClick()

        composeTestRule.onNodeWithText(previewLabel).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription(editLabel).assertIsDisplayed()
    }

    @Test
    fun editorScreen_noteInformationNavigatesOutlineFromEditMode() {
        launchEditor()
        val editor = composeTestRule.onNodeWithContentDescription(context.getString(R.string.note_content))

        editor.performTextInput("# Overview\n\nBody")
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.note_information)).performClick()

        composeTestRule.onNodeWithText(context.getString(R.string.note_statistics)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.table_of_contents)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.backlinks_section_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText("Overview").performClick()

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.edit)).assertIsDisplayed()
    }

    @Test
    fun editorScreen_quickInsertAddsHeading() {
        launchEditor()
        val editor = composeTestRule.onNodeWithContentDescription(context.getString(R.string.note_content))

        editor.performTextInput("/")
        composeTestRule.onNodeWithText(context.getString(R.string.quick_insert_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.quick_insert_heading_1)).performClick()

        editor.assertTextEquals("# ")
        composeTestRule.onNodeWithText(context.getString(R.string.quick_insert_title)).assertDoesNotExist()
    }

    @Test
    fun editorScreen_formattingPanelAppliesBold() {
        launchEditor()
        val editor = composeTestRule.onNodeWithContentDescription(context.getString(R.string.note_content))
        val formattingLabel = context.getString(R.string.formatting)

        composeTestRule.onNodeWithContentDescription(formattingLabel).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.formatting_inline)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.bold)).performClick()

        editor.assertTextEquals("**bold**")
        composeTestRule.onNodeWithText(context.getString(R.string.formatting_inline)).assertDoesNotExist()
    }

    @Test
    fun editorScreen_quickInsertPreemptsFormattingControls() {
        launchEditor()
        val editor = composeTestRule.onNodeWithContentDescription(context.getString(R.string.note_content))
        val formattingLabel = context.getString(R.string.formatting)

        composeTestRule.onNodeWithContentDescription(formattingLabel).assertIsDisplayed()
        editor.performTextInput("/")

        composeTestRule.onNodeWithText(context.getString(R.string.quick_insert_title)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(formattingLabel).assertDoesNotExist()
    }

    @Test
    fun editorScreen_urlDoesNotOpenQuickInsert() {
        launchEditor()

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.note_content))
            .performTextInput("https://")

        composeTestRule.onNodeWithText(context.getString(R.string.quick_insert_title)).assertDoesNotExist()
    }
}
