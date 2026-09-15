package com.markleaf.notes.core.markdown.preview

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.R
import com.markleaf.notes.core.markdown.SimpleMarkdownPreview
import com.markleaf.notes.ui.theme.MarkleafTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * #403: tapping a `<details>` section's `<summary>` row expands or collapses
 * it. Owns the toggle state itself (a `remember` wrapping the real call
 * site), the way `EditorScreen` does — `MarkdownPreviewList` takes
 * `toggledSectionIds` from its caller rather than holding it, so a test that
 * only inspects the `onToggleSection` callback would miss whether the row
 * clicked next actually changed.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class MarkdownCollapsibleSectionClickTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun preview(markdown: String) {
        composeRule.setContent {
            MarkleafTheme(darkTheme = false, dynamicColor = false) {
                var toggled by remember { mutableStateOf<Set<Int>>(emptySet()) }
                MarkdownPreviewList(
                    lines = remember { SimpleMarkdownPreview.parse(markdown) },
                    toggledSectionIds = toggled,
                    onToggleSection = { id ->
                        toggled = if (id in toggled) toggled - id else toggled + id
                    }
                )
            }
        }
    }

    @Test
    fun tappingACollapsedSummaryRevealsItsBody() {
        preview("<details>\n<summary>Toggle me</summary>\n\nhidden body\n</details>")

        composeRule.onNodeWithText("hidden body").assertDoesNotExist()
        composeRule.onNodeWithText("Toggle me").performClick()
        composeRule.onNodeWithText("hidden body").assertExists()
    }

    @Test
    fun tappingAnOpenSummaryHidesItsBodyAgain() {
        preview("<details open>\n<summary>Toggle me</summary>\n\nshown body\n</details>")

        composeRule.onNodeWithText("shown body").assertExists()
        composeRule.onNodeWithText("Toggle me").performClick()
        composeRule.onNodeWithText("shown body").assertDoesNotExist()
    }

    @Test
    fun togglingOneSectionLeavesASiblingSectionAlone() {
        val markdown = """
            <details>
            <summary>First</summary>

            first body
            </details>
            <details open>
            <summary>Second</summary>

            second body
            </details>
        """.trimIndent()
        preview(markdown)

        composeRule.onNodeWithText("First").performClick()

        composeRule.onNodeWithText("first body").assertExists()
        composeRule.onNodeWithText("second body").assertExists()
    }

    @Test
    fun aSectionWithNoSummaryTagShowsTheLocalizedFallbackTitle() {
        preview("<details>\n\nbody\n</details>")

        val fallback = context.getString(R.string.collapsible_section_default_summary)
        composeRule.onNodeWithText(fallback).assertExists()
    }
}
