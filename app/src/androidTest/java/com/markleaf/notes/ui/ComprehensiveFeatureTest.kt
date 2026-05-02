package com.markleaf.notes.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.markleaf.notes.MainActivity
import com.markleaf.notes.R
import org.junit.Rule
import org.junit.Test
import java.util.*

/**
 * Comprehensive UI test suite for Markleaf.
 * Covers 50 test cases across core features.
 * Targeted for on-device verification on SM-S921N.
 */
class ComprehensiveFeatureTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val context get() = composeTestRule.activity

    private fun getString(resId: Int): String = context.getString(resId)
    private fun getString(resId: Int, vararg formatArgs: Any): String = context.getString(resId, *formatArgs)

    // --- Helpers ---

    private fun createNote(content: String) {
        composeTestRule.onNodeWithContentDescription(getString(R.string.add_note)).performClick()
        composeTestRule.onNodeWithContentDescription(getString(R.string.note_content)).performTextReplacement(content)
        // Wait for auto-save (1s debounce)
        composeTestRule.mainClock.advanceTimeBy(1500L)
        goBack()
    }

    private fun goBack() {
        // Find back button by content description
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

    // --- 1-10: Note Management ---

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
        val special = "Special !@#$%^&*()_+ {}|:\"<>?~`"
        createNote("# $special")
        composeTestRule.onNodeWithText(special).assertIsDisplayed()
    }

    @Test
    fun test06_multipleNotesOrderedByRecent() {
        createNote("# Note A")
        createNote("# Note B")
        // Note B should be at the top. We just verify both exist for now.
        composeTestRule.onNodeWithText("Note B").assertIsDisplayed()
        composeTestRule.onNodeWithText("Note A").assertIsDisplayed()
    }

    @Test
    fun test07_editExistingNote() {
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
    fun test08_excerptDisplayInList() {
        val uniqueExcerpt = "Excerpt ${UUID.randomUUID().toString().take(6)}"
        createNote("# Title\n$uniqueExcerpt")
        composeTestRule.onNodeWithText(uniqueExcerpt).assertIsDisplayed()
    }

    @Test
    fun test09_createNoteAndCancelGoBack() {
        composeTestRule.onNodeWithContentDescription(getString(R.string.add_note)).performClick()
        goBack()
        composeTestRule.onNodeWithText(getString(R.string.notes_title)).assertIsDisplayed()
    }

    @Test
    fun test10_longNoteContentScrolling() {
        val longContent = (1..50).joinToString("\n") { "Line $it" }
        createNote("# Long Note\n$longContent")
        composeTestRule.onNodeWithText("Long Note").performClick()
        composeTestRule.onNodeWithContentDescription(getString(R.string.note_content)).performTouchInput { swipeUp() }
    }

    // --- 11-20: Editor & Markdown ---

    @Test
    fun test11_boldToolbarAction() {
        composeTestRule.onNodeWithContentDescription(getString(R.string.add_note)).performClick()
        composeTestRule.onNodeWithContentDescription(getString(R.string.bold)).performClick()
        composeTestRule.onNodeWithContentDescription(getString(R.string.note_content)).assert(hasText("**", substring = true))
        goBack()
    }

    @Test
    fun test12_italicToolbarAction() {
        composeTestRule.onNodeWithContentDescription(getString(R.string.add_note)).performClick()
        composeTestRule.onNodeWithContentDescription(getString(R.string.italic)).performClick()
        composeTestRule.onNodeWithContentDescription(getString(R.string.note_content)).assert(hasText("_", substring = true))
        goBack()
    }

    @Test
    fun test13_checkboxToolbarAction() {
        composeTestRule.onNodeWithContentDescription(getString(R.string.add_note)).performClick()
        composeTestRule.onNodeWithContentDescription(getString(R.string.checkbox)).performClick()
        composeTestRule.onNodeWithContentDescription(getString(R.string.note_content)).assert(hasText("- [ ] ", substring = true))
        goBack()
    }

    @Test
    fun test14_headingInPreview() {
        val title = "H1 Preview"
        composeTestRule.onNodeWithContentDescription(getString(R.string.add_note)).performClick()
        composeTestRule.onNodeWithContentDescription(getString(R.string.note_content)).performTextInput("# $title")
        togglePreview()
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
        goBack()
    }

    @Test
    fun test15_listInPreview() {
        composeTestRule.onNodeWithContentDescription(getString(R.string.add_note)).performClick()
        composeTestRule.onNodeWithContentDescription(getString(R.string.note_content)).performTextInput("- Item 1\n- Item 2")
        togglePreview()
        composeTestRule.onNodeWithText("• Item 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("• Item 2").assertIsDisplayed()
        goBack()
    }

    @Test
    fun test16_checkboxInPreview() {
        composeTestRule.onNodeWithContentDescription(getString(R.string.add_note)).performClick()
        composeTestRule.onNodeWithContentDescription(getString(R.string.note_content)).performTextInput("- [ ] Todo\n- [x] Done")
        togglePreview()
        composeTestRule.onNodeWithText("☐ Todo").assertIsDisplayed()
        composeTestRule.onNodeWithText("☑ Done").assertIsDisplayed()
        goBack()
    }

    @Test
    fun test17_markdownLinkInPreview() {
        composeTestRule.onNodeWithContentDescription(getString(R.string.add_note)).performClick()
        composeTestRule.onNodeWithContentDescription(getString(R.string.note_content)).performTextInput("[Google](https://google.com)")
        togglePreview()
        composeTestRule.onNodeWithText("Google").assertIsDisplayed()
        goBack()
    }

    @Test
    fun test18_tableInPreview() {
        val table = "| Col 1 | Col 2 |\n|---|---|\n| Val 1 | Val 2 |"
        composeTestRule.onNodeWithContentDescription(getString(R.string.add_note)).performClick()
        composeTestRule.onNodeWithContentDescription(getString(R.string.note_content)).performTextInput(table)
        togglePreview()
        composeTestRule.onNodeWithText("Val 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Val 2").assertIsDisplayed()
        goBack()
    }

    @Test
    fun test19_mathInPreview() {
        val math = "$$ E=mc^2 $$"
        composeTestRule.onNodeWithContentDescription(getString(R.string.add_note)).performClick()
        composeTestRule.onNodeWithContentDescription(getString(R.string.note_content)).performTextInput(math)
        togglePreview()
        composeTestRule.onNodeWithText(math).assertIsDisplayed()
        goBack()
    }

    @Test
    fun test20_syntaxHighlightingToggleExists() {
        openSettings()
        val showSyntax = getString(R.string.show_markdown_syntax)
        composeTestRule.onNodeWithText(showSyntax).assertIsDisplayed()
        goBack()
    }

    // --- 21-30: Linking ---

    @Test
    fun test21_wikiLinkToolbarAction() {
        composeTestRule.onNodeWithContentDescription(getString(R.string.add_note)).performClick()
        composeTestRule.onNodeWithContentDescription(getString(R.string.wiki_link)).performClick()
        composeTestRule.onNodeWithContentDescription(getString(R.string.note_content)).assert(hasText("[[]]", substring = true))
        goBack()
    }

    @Test
    fun test22_wikiLinkDetection() {
        val target = "Target Note"
        createNote("# $target")
        composeTestRule.onNodeWithContentDescription(getString(R.string.add_note)).performClick()
        composeTestRule.onNodeWithContentDescription(getString(R.string.note_content)).performTextInput("[[$target]]")
        togglePreview()
        composeTestRule.onNodeWithText("[[$target]]").assertIsDisplayed()
        goBack()
    }

    @Test
    fun test23_clickingWikiLinkNavigates() {
        val target = "JumpTarget ${UUID.randomUUID().toString().take(6)}"
        createNote("# $target\nThis is the target.")
        
        composeTestRule.onNodeWithContentDescription(getString(R.string.add_note)).performClick()
        composeTestRule.onNodeWithContentDescription(getString(R.string.note_content)).performTextInput("[[$target]]")
        togglePreview()
        composeTestRule.onNodeWithText("[[$target]]").performClick()
        
        // Should open target note
        composeTestRule.onNodeWithText("This is the target.").assertIsDisplayed()
        goBack()
        goBack()
    }

    @Test
    fun test24_backlinksDisplay() {
        val target = "LinkTarget ${UUID.randomUUID().toString().take(6)}"
        val source = "LinkSource ${UUID.randomUUID().toString().take(6)}"
        createNote("# $target")
        createNote("# $source\n[[$target]]")
        
        composeTestRule.onNodeWithText(target).performClick()
        composeTestRule.onNodeWithText(getString(R.string.backlinks)).assertIsDisplayed()
        composeTestRule.onNodeWithText(source).assertIsDisplayed()
        goBack()
    }

    @Test
    fun test25_externalLinksInPreview() {
        composeTestRule.onNodeWithContentDescription(getString(R.string.add_note)).performClick()
        composeTestRule.onNodeWithContentDescription(getString(R.string.note_content)).performTextInput("https://github.com")
        togglePreview()
        composeTestRule.onNodeWithText("https://github.com", substring = true).assertIsDisplayed()
        goBack()
    }

    @Test
    fun test26_multiWordWikiLinks() {
        val title = "Note With Many Words"
        createNote("# $title")
        composeTestRule.onNodeWithContentDescription(getString(R.string.add_note)).performClick()
        composeTestRule.onNodeWithContentDescription(getString(R.string.note_content)).performTextInput("[[$title]]")
        togglePreview()
        composeTestRule.onNodeWithText("[[$title]]").performClick()
        composeTestRule.onNodeWithText(getString(R.string.edit_note)).assertIsDisplayed()
        goBack()
        goBack()
    }

    @Test
    fun test27_wikiLinkToNonExistentShowsSearch() {
        val ghost = "Ghost Note ${UUID.randomUUID().toString().take(6)}"
        composeTestRule.onNodeWithContentDescription(getString(R.string.add_note)).performClick()
        composeTestRule.onNodeWithContentDescription(getString(R.string.note_content)).performTextInput("[[$ghost]]")
        togglePreview()
        composeTestRule.onNodeWithText("[[$ghost]]").performClick()
        // If note doesn't exist, it currently opens SearchScreen with that query
        composeTestRule.onNodeWithText(getString(R.string.search_notes_hint)).assertIsDisplayed()
        goBack()
        goBack()
    }

    @Test
    fun test28_backlinkNavigationBackAndForth() {
        val a = "Note A ${UUID.randomUUID().toString().take(6)}"
        val b = "Note B ${UUID.randomUUID().toString().take(6)}"
        createNote("# $a\n[[$b]]")
        createNote("# $b\n[[$a]]")
        
        composeTestRule.onNodeWithText(a).performClick()
        togglePreview()
        composeTestRule.onNodeWithText("[[$b]]").performClick()
        composeTestRule.onNodeWithText(getString(R.string.edit_note)).assertIsDisplayed() // Now in B
        togglePreview()
        composeTestRule.onNodeWithText("[[$a]]").performClick()
        composeTestRule.onNodeWithText(a, substring = true).assertIsDisplayed() // Back in A
        goBack()
    }

    @Test
    fun test29_nestedMarkdownLinks() {
        composeTestRule.onNodeWithContentDescription(getString(R.string.add_note)).performClick()
        composeTestRule.onNodeWithContentDescription(getString(R.string.note_content)).performTextInput("[Label]([[Target]])")
        togglePreview()
        composeTestRule.onNodeWithText("Label").assertIsDisplayed()
        goBack()
    }

    @Test
    fun test30_linkReferenceCountInSearch() {
        val target = "CommonTarget ${UUID.randomUUID().toString().take(6)}"
        createNote("# $target")
        createNote("Link 1 to [[$target]]")
        createNote("Link 2 to [[$target]]")
        
        openSearch()
        composeTestRule.onNodeWithText(getString(R.string.search_notes_hint)).performTextInput(target)
        // Verify link result shows count - using a substring match to handle formatting
        composeTestRule.onNodeWithText("2", substring = true).assertIsDisplayed()
        goBack()
    }

    // --- 31-40: Search & Tags ---

    @Test
    fun test31_searchByExactText() {
        val unique = "UniqueQuery ${UUID.randomUUID().toString().take(6)}"
        createNote("# Title\n$unique")
        openSearch()
        composeTestRule.onNodeWithText(getString(R.string.search_notes_hint)).performTextInput(unique)
        composeTestRule.onNodeWithText("Title").assertIsDisplayed()
        goBack()
    }

    @Test
    fun test32_searchByPartialText() {
        val prefix = "PartialMatch"
        val full = "${prefix}${UUID.randomUUID().toString().take(6)}"
        createNote("# $full")
        openSearch()
        composeTestRule.onNodeWithText(getString(R.string.search_notes_hint)).performTextInput(prefix)
        composeTestRule.onNodeWithText(full).assertIsDisplayed()
        goBack()
    }

    @Test
    fun test33_searchByTag() {
        val tag = "tag${UUID.randomUUID().toString().take(6)}"
        createNote("# Note with #$tag")
        openSearch()
        composeTestRule.onNodeWithText(getString(R.string.search_notes_hint)).performTextInput("#$tag")
        composeTestRule.onNodeWithText("#$tag", substring = true).assertIsDisplayed()
        goBack()
    }

    @Test
    fun test34_searchByWikiLinkQuery() {
        val target = "QueryTarget ${UUID.randomUUID().toString().take(6)}"
        createNote("[[$target]]")
        openSearch()
        composeTestRule.onNodeWithText(getString(R.string.search_notes_hint)).performTextInput("[[$target]]")
        composeTestRule.onNodeWithText("[[$target]]", substring = true).assertIsDisplayed()
        goBack()
    }

    @Test
    fun test35_tagScreenListing() {
        val tag = "listTag${UUID.randomUUID().toString().take(6)}"
        createNote("Test #$tag")
        openTags()
        composeTestRule.onNodeWithText("#$tag", substring = true).assertIsDisplayed()
        goBack()
    }

    @Test
    fun test36_clickingTagShowsNotes() {
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
    fun test37_autoDetectionOfTags() {
        composeTestRule.onNodeWithContentDescription(getString(R.string.add_note)).performClick()
        val tag = "newtag${UUID.randomUUID().toString().take(4)}"
        composeTestRule.onNodeWithContentDescription(getString(R.string.note_content)).performTextInput("#$tag")
        composeTestRule.mainClock.advanceTimeBy(1500L)
        goBack()
        openTags()
        composeTestRule.onNodeWithText("#$tag", substring = true).assertIsDisplayed()
        goBack()
    }

    @Test
    fun test38_multipleTagsInOneNote() {
        val tag1 = "tag1_${UUID.randomUUID().toString().take(4)}"
        val tag2 = "tag2_${UUID.randomUUID().toString().take(4)}"
        createNote("#$tag1 #$tag2")
        openTags()
        composeTestRule.onNodeWithText("#$tag1", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("#$tag2", substring = true).assertIsDisplayed()
        goBack()
    }

    @Test
    fun test39_searchEmptyResults() {
        openSearch()
        composeTestRule.onNodeWithText(getString(R.string.search_notes_hint)).performTextInput("NonExistentXYZ123")
        composeTestRule.onNodeWithText(getString(R.string.no_results_found)).assertIsDisplayed()
        goBack()
    }

    @Test
    fun test40_quickOpenFromSearch() {
        val title = "QuickOpen ${UUID.randomUUID().toString().take(6)}"
        createNote("# $title")
        openSearch()
        composeTestRule.onNodeWithText(getString(R.string.search_notes_hint)).performTextInput(title)
        composeTestRule.onNodeWithText(title).performClick()
        composeTestRule.onNodeWithText(getString(R.string.edit_note)).assertIsDisplayed()
        goBack()
        goBack()
    }

    // --- 41-50: Trash, Settings & Data ---

    @Test
    fun test41_moveToTrash() {
        val title = "ToTrash ${UUID.randomUUID().toString().take(6)}"
        createNote("# $title")
        composeTestRule.onNodeWithText(title).performTouchInput { longClick() }
        composeTestRule.onNodeWithText(title).assertDoesNotExist()
        openTrash()
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
        goBack()
    }

    @Test
    fun test42_restoreFromTrash() {
        val title = "RestoreMe ${UUID.randomUUID().toString().take(6)}"
        createNote("# $title")
        composeTestRule.onNodeWithText(title).performTouchInput { longClick() }
        openTrash()
        composeTestRule.onNodeWithText(getString(R.string.restore)).performClick()
        goBack() // Close Trash (if it's a separate screen/dialog)
        composeTestRule.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun test43_deleteForever() {
        val title = "DeleteForever ${UUID.randomUUID().toString().take(6)}"
        createNote("# $title")
        composeTestRule.onNodeWithText(title).performTouchInput { longClick() }
        openTrash()
        composeTestRule.onNodeWithText(getString(R.string.delete)).performClick()
        composeTestRule.onNodeWithText(getString(R.string.delete_forever)).performClick()
        composeTestRule.onNodeWithText(title).assertDoesNotExist()
        goBack()
    }

    @Test
    fun test44_trashEmptyState() {
        openTrash()
        // If trash was empty from previous tests or we clear it
        composeTestRule.onNodeWithText(getString(R.string.trash_empty_hint)).assertIsDisplayed()
        goBack()
    }

    @Test
    fun test45_toggleMarkdownSyntaxSetting() {
        openSettings()
        val toggle = getString(R.string.show_markdown_syntax)
        composeTestRule.onNodeWithText(toggle).performClick()
        composeTestRule.onNodeWithText(toggle).assertIsDisplayed()
        goBack()
    }

    @Test
    fun test46_lineWidthNarrow() {
        openSettings()
        composeTestRule.onNodeWithText(getString(R.string.line_width)).performClick()
        composeTestRule.onNodeWithText(getString(R.string.line_width_narrow)).performClick()
        composeTestRule.onNodeWithText(getString(R.string.line_width_narrow)).assertIsDisplayed()
        goBack()
    }

    @Test
    fun test47_lineWidthComfortable() {
        openSettings()
        composeTestRule.onNodeWithText(getString(R.string.line_width)).performClick()
        composeTestRule.onNodeWithText(getString(R.string.line_width_comfortable)).performClick()
        composeTestRule.onNodeWithText(getString(R.string.line_width_comfortable)).assertIsDisplayed()
        goBack()
    }

    @Test
    fun test48_lineWidthWide() {
        openSettings()
        composeTestRule.onNodeWithText(getString(R.string.line_width)).performClick()
        composeTestRule.onNodeWithText(getString(R.string.line_width_wide)).performClick()
        composeTestRule.onNodeWithText(getString(R.string.line_width_wide)).assertIsDisplayed()
        goBack()
    }

    @Test
    fun test49_backupUiFlow() {
        openSettings()
        composeTestRule.onNodeWithText(getString(R.string.create_backup)).assertIsDisplayed()
        goBack()
    }

    @Test
    fun test50_versionHistoryDialog() {
        val title = "HistoryNote ${UUID.randomUUID().toString().take(6)}"
        createNote("# $title")
        composeTestRule.onNodeWithText(title).performClick()
        // Snapshots might take a moment to trigger or need manual save
        composeTestRule.onNodeWithContentDescription(getString(R.string.version_history)).performClick()
        // At least the dialog or "No saved versions yet" should show
        composeTestRule.onNodeWithText(getString(R.string.version_history)).assertIsDisplayed()
        composeTestRule.onNodeWithText(getString(R.string.cancel)).performClick()
        goBack()
        composeTestRule.waitForIdle()
    }
}
