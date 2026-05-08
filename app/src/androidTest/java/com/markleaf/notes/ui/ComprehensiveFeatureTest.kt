package com.markleaf.notes.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.markleaf.notes.R
import com.markleaf.notes.TestHostActivity
import com.markleaf.notes.data.local.AppDatabase
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.*

/**
 * UI test suite covering Markleaf's lightweight feature set:
 * note CRUD, the trim Markdown preview, search, tags, trash, and settings.
 */
class ComprehensiveFeatureTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: AppDatabase
    private var scenario: ActivityScenario<TestHostActivity>? = null

    @Before
    fun setUp() {
        database = createInMemoryMarkleafDatabase()
        TestHostActivity.content = markleafTestContent(database)
        scenario = ActivityScenario.launch(TestHostActivity::class.java)
        composeTestRule.waitForIdle()
    }

    @After
    fun tearDown() {
        scenario?.close()
        TestHostActivity.content = null
        database.close()
    }

    private fun getString(resId: Int): String = context.getString(resId)
    private fun getString(resId: Int, vararg formatArgs: Any): String = context.getString(resId, *formatArgs)

    // --- Helpers ---

    private fun createNote(content: String) {
        composeTestRule.onNodeWithContentDescription(getString(R.string.add_note)).performClick()
        composeTestRule.onNodeWithContentDescription(getString(R.string.note_content)).performTextReplacement(content)
        composeTestRule.mainClock.advanceTimeBy(1500L)
        goBack()
    }

    private fun goBack() {
        composeTestRule.onNodeWithContentDescription(getString(R.string.back)).performClick()
    }

    private fun togglePreview() {
        composeTestRule.onNodeWithText(getString(R.string.preview)).performClick()
    }

    private fun openSearch() {
        composeTestRule.onNodeWithContentDescription(getString(R.string.search)).performClick()
    }

    private fun openTags() {
        composeTestRule.onNodeWithContentDescription(getString(R.string.tags)).performClick()
    }

    private fun openTrash() {
        composeTestRule.onNodeWithContentDescription(getString(R.string.trash)).performClick()
    }

    private fun openSettings() {
        composeTestRule.onNodeWithContentDescription(getString(R.string.settings)).performClick()
    }

    private fun trashFromList(title: String) {
        composeTestRule.onNodeWithText(title).performTouchInput { longClick() }
        composeTestRule.onNodeWithText(getString(R.string.move_to_trash)).performClick()
    }

    // --- Note Management ---

    @Test
    fun test01_createNoteAndVerifyInList() {
        val title = "Note ${UUID.randomUUID().toString().take(6)}"
        createNote("# $title\nContent")
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun test02_noteAutoSavesContent() {
        val uniqueContent = "AutoSave ${UUID.randomUUID().toString().take(6)}"
        composeTestRule.onNodeWithContentDescription(getString(R.string.add_note)).performClick()
        composeTestRule.onNodeWithContentDescription(getString(R.string.note_content)).performTextInput(uniqueContent)
        composeTestRule.mainClock.advanceTimeBy(1500L)
        goBack()
        composeTestRule.onNodeWithText(uniqueContent).assertIsDisplayed()
    }

    @Test
    fun test03_h1BecomesTitle() {
        val title = "Title ${UUID.randomUUID().toString().take(6)}"
        createNote("# $title")
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun test04_emptyNoteIsUntitled() {
        createNote("")
        composeTestRule.onNodeWithText(getString(R.string.untitled)).assertIsDisplayed()
    }

    @Test
    fun test05_specialCharactersInNote() {
        val special = "Special !@#\$%^&*()_+ {}|:\"<>?~`"
        createNote("# $special")
        composeTestRule.onNodeWithText(special).assertIsDisplayed()
    }

    @Test
    fun test06_editExistingNote() {
        val originalTitle = "Original ${UUID.randomUUID().toString().take(6)}"
        val newTitle = "Modified ${UUID.randomUUID().toString().take(6)}"
        createNote("# $originalTitle")
        composeTestRule.onNodeWithText(originalTitle).performClick()
        composeTestRule.onNodeWithContentDescription(getString(R.string.note_content)).performTextReplacement("# $newTitle")
        composeTestRule.mainClock.advanceTimeBy(1500L)
        goBack()
        composeTestRule.onNodeWithText(newTitle).assertIsDisplayed()
    }

    @Test
    fun test07_excerptDisplayInList() {
        val uniqueExcerpt = "Excerpt ${UUID.randomUUID().toString().take(6)}"
        createNote("# Title\n$uniqueExcerpt")
        composeTestRule.onNodeWithText(uniqueExcerpt).assertIsDisplayed()
    }

    // --- Editor & Markdown ---

    @Test
    fun test10_boldToolbarAction() {
        composeTestRule.onNodeWithContentDescription(getString(R.string.add_note)).performClick()
        composeTestRule.onNodeWithContentDescription(getString(R.string.bold)).performClick()
        composeTestRule.onNodeWithContentDescription(getString(R.string.note_content)).assert(hasText("**", substring = true))
        goBack()
    }

    @Test
    fun test11_italicToolbarAction() {
        composeTestRule.onNodeWithContentDescription(getString(R.string.add_note)).performClick()
        composeTestRule.onNodeWithContentDescription(getString(R.string.italic)).performClick()
        composeTestRule.onNodeWithContentDescription(getString(R.string.note_content)).assert(hasText("*", substring = true))
        goBack()
    }

    @Test
    fun test12_checkboxToolbarAction() {
        composeTestRule.onNodeWithContentDescription(getString(R.string.add_note)).performClick()
        composeTestRule.onNodeWithContentDescription(getString(R.string.checkbox)).performClick()
        composeTestRule.onNodeWithContentDescription(getString(R.string.note_content)).assert(hasText("- [ ] ", substring = true))
        goBack()
    }

    @Test
    fun test13_headingInPreview() {
        val title = "H1 Preview"
        composeTestRule.onNodeWithContentDescription(getString(R.string.add_note)).performClick()
        composeTestRule.onNodeWithContentDescription(getString(R.string.note_content)).performTextInput("# $title")
        togglePreview()
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
        goBack()
    }

    @Test
    fun test14_listInPreview() {
        composeTestRule.onNodeWithContentDescription(getString(R.string.add_note)).performClick()
        composeTestRule.onNodeWithContentDescription(getString(R.string.note_content)).performTextInput("- Item 1\n- Item 2")
        togglePreview()
        composeTestRule.onNodeWithText("• Item 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("• Item 2").assertIsDisplayed()
        goBack()
    }

    @Test
    fun test15_checkboxInPreview() {
        composeTestRule.onNodeWithContentDescription(getString(R.string.add_note)).performClick()
        composeTestRule.onNodeWithContentDescription(getString(R.string.note_content)).performTextInput("- [ ] Todo\n- [x] Done")
        togglePreview()
        composeTestRule.onNodeWithText("☐ Todo").assertIsDisplayed()
        composeTestRule.onNodeWithText("☑ Done").assertIsDisplayed()
        goBack()
    }

    @Test
    fun test16_syntaxHighlightingToggleExists() {
        openSettings()
        val showSyntax = getString(R.string.show_markdown_syntax)
        composeTestRule.onNodeWithText(showSyntax).assertIsDisplayed()
        goBack()
    }

    // --- Search & Tags ---

    @Test
    fun test20_searchByExactText() {
        val unique = "UniqueQuery ${UUID.randomUUID().toString().take(6)}"
        createNote("# Title\n$unique")
        openSearch()
        composeTestRule.onNodeWithText(getString(R.string.search_notes_hint)).performTextInput(unique)
        composeTestRule.onNodeWithText("Title").assertIsDisplayed()
        goBack()
    }

    @Test
    fun test21_searchByPartialText() {
        val prefix = "PartialMatch"
        val full = "${prefix}${UUID.randomUUID().toString().take(6)}"
        createNote("# $full")
        openSearch()
        composeTestRule.onNodeWithText(getString(R.string.search_notes_hint)).performTextInput(prefix)
        composeTestRule.onNodeWithText(full).assertIsDisplayed()
        goBack()
    }

    @Test
    fun test22_tagScreenListing() {
        val tag = "listTag${UUID.randomUUID().toString().take(6)}"
        createNote("Test #$tag")
        openTags()
        composeTestRule.onNodeWithText("#$tag", substring = true).assertIsDisplayed()
        goBack()
    }

    @Test
    fun test23_clickingTagShowsNotes() {
        val tag = "clickTag${UUID.randomUUID().toString().take(6)}"
        val title = "Tagged Note"
        createNote("# $title\n#$tag")
        openTags()
        composeTestRule.onNodeWithText("#$tag", substring = true).performClick()
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
        goBack()
        goBack()
    }

    @Test
    fun test24_searchEmptyResults() {
        openSearch()
        composeTestRule.onNodeWithText(getString(R.string.search_notes_hint)).performTextInput("NonExistentXYZ123")
        composeTestRule.onNodeWithText(getString(R.string.no_results_found)).assertIsDisplayed()
        goBack()
    }

    // --- Trash, Settings ---

    @Test
    fun test30_moveToTrashFromList() {
        val title = "ToTrash ${UUID.randomUUID().toString().take(6)}"
        createNote("# $title")
        trashFromList(title)
        composeTestRule.onNodeWithText(title).assertDoesNotExist()
        openTrash()
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
        goBack()
    }

    @Test
    fun test31_moveToTrashFromEditor() {
        val title = "EditorTrash ${UUID.randomUUID().toString().take(6)}"
        createNote("# $title")
        composeTestRule.onNodeWithText(title).performClick()
        composeTestRule.onNodeWithContentDescription(getString(R.string.move_to_trash)).performClick()
        composeTestRule.onNodeWithText(getString(R.string.move_to_trash)).performClick()
        composeTestRule.onNodeWithText(title).assertDoesNotExist()
        openTrash()
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
        goBack()
    }

    @Test
    fun test32_restoreFromTrash() {
        val title = "RestoreMe ${UUID.randomUUID().toString().take(6)}"
        createNote("# $title")
        trashFromList(title)
        openTrash()
        composeTestRule.onNodeWithText(getString(R.string.restore)).performClick()
        goBack()
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun test33_deleteForever() {
        val title = "DeleteForever ${UUID.randomUUID().toString().take(6)}"
        createNote("# $title")
        trashFromList(title)
        openTrash()
        composeTestRule.onNodeWithText(getString(R.string.delete)).performClick()
        composeTestRule.onNodeWithText(getString(R.string.delete_forever)).performClick()
        composeTestRule.onNodeWithText(title).assertDoesNotExist()
        goBack()
    }

    @Test
    fun test34_trashEmptyState() {
        openTrash()
        composeTestRule.onNodeWithText(getString(R.string.trash_empty_hint)).assertIsDisplayed()
        goBack()
    }

    @Test
    fun test40_lineWidthOptions() {
        openSettings()
        composeTestRule.onNodeWithText(getString(R.string.line_width_narrow)).assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(R.string.line_width_comfortable)).assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(R.string.line_width_wide)).assertIsDisplayed()
        goBack()
    }
}
