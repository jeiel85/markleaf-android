package com.markleaf.notes.core.markdown.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import com.markleaf.notes.core.markdown.SimpleMarkdownPreview
import com.markleaf.notes.ui.theme.MarkleafTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Visual regression net for MarkdownPreviewList. Renders representative
 * markdown snippets through the same composable the editor uses, then
 * captures the result as a PNG via Roborazzi. PRs that change rendering
 * (intentionally or not) light up as image diffs in CI.
 *
 * To regenerate goldens after an intended change:
 *   ./gradlew recordRoborazziDebug
 *
 * To verify against committed goldens:
 *   ./gradlew verifyRoborazziDebug
 */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class MarkdownPreviewSnapshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val roborazziRule = RoborazziRule(
        composeRule = composeRule,
        captureRoot = composeRule.onRoot(),
        options = RoborazziRule.Options(
            outputDirectoryPath = "src/test/snapshots/roborazzi",
            roborazziOptions = RoborazziOptions(
                // Goldens are now recorded on the Linux CI runner (v2.9.1), so
                // the same OS records and verifies — tighter threshold catches
                // real regressions instead of OS-level font-hinting noise.
                compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.005f)
            )
        )
    )

    @Test
    fun headings_light() = snapshot("headings_light", darkTheme = false) {
        Renders("# Heading 1\n## Heading 2\n### Heading 3\nbody paragraph")
    }

    @Test
    fun headings_dark() = snapshot("headings_dark", darkTheme = true) {
        Renders("# Heading 1\n## Heading 2\n### Heading 3\nbody paragraph")
    }

    @Test
    fun lists_and_checkboxes_light() = snapshot("lists_light", darkTheme = false) {
        Renders(
            """
            - bullet item
            - another bullet
            1. ordered first
            2. ordered second
            - [ ] todo
            - [x] done
            """.trimIndent()
        )
    }

    @Test
    fun inline_styles_light() = snapshot("inline_styles_light", darkTheme = false) {
        Renders("**bold**, *italic*, ~~strike~~, `code`, ***bold-italic***")
    }

    /**
     * Regression net for #141: inline markdown (bold, italic, wikilinks, code)
     * must render *inside* list rows, not fall through to plain text. Before the
     * fix, `- **bold**` showed the literal `**bold**` and `- [[Note]]` showed
     * `[[Note]]` with brackets.
     */
    @Test
    fun list_inline_formatting_light() = snapshot("list_inline_formatting_light", darkTheme = false) {
        Renders(
            """
            - a **bold** bullet
            - link to [[Another Note]]
            1. ordered with *italic*
            2. ordered with `code`
            - [ ] todo with **strong**
            - [x] done with [[Wiki]]
            """.trimIndent()
        )
    }

    @Test
    fun blockquote_and_hr_light() = snapshot("blockquote_hr_light", darkTheme = false) {
        Renders(
            """
            > a regular quote

            ---

            after divider
            """.trimIndent()
        )
    }

    @Test
    fun code_block_light() = snapshot("code_block_light", darkTheme = false) {
        Renders(
            """
            ```kotlin
            // greet the world
            fun main() {
                val name = "Markleaf"
                println("Hello, ${'$'}name!")
            }
            ```
            """.trimIndent()
        )
    }

    @Test
    fun code_block_python_light() = snapshot("code_block_python_light", darkTheme = false) {
        Renders(
            """
            ```python
            # average of a list
            def avg(xs):
                return sum(xs) / len(xs)
            ```
            """.trimIndent()
        )
    }

    @Test
    fun callout_note_light() = snapshot("callout_note_light", darkTheme = false) {
        Renders(
            """
            > [!NOTE]
            > A useful note.
            > Second line.
            """.trimIndent()
        )
    }

    @Test
    fun callout_tip_light() = snapshot("callout_tip_light", darkTheme = false) {
        Renders(
            """
            > [!TIP]
            > Try Cmd+K
            """.trimIndent()
        )
    }

    @Test
    fun callout_warning_light() = snapshot("callout_warning_light", darkTheme = false) {
        Renders(
            """
            > [!WARNING]
            > This is destructive.
            """.trimIndent()
        )
    }

    @Test
    fun callout_warning_dark() = snapshot("callout_warning_dark", darkTheme = true) {
        Renders(
            """
            > [!WARNING]
            > This is destructive.
            """.trimIndent()
        )
    }

    @Test
    fun frontmatter_light() = snapshot("frontmatter_light", darkTheme = false) {
        Renders(
            """
            ---
            title: Hello
            tags: [draft]
            ---
            Body text.
            """.trimIndent()
        )
    }

    @Test
    fun footnotes_light() = snapshot("footnotes_light", darkTheme = false) {
        Renders(
            """
            Some text with a ref[^1] inside.

            [^1]: footnote definition body
            """.trimIndent()
        )
    }

    @Test
    fun mixed_document_light() = snapshot("mixed_document_light", darkTheme = false) {
        Renders(
            """
            ---
            title: Demo
            ---

            # Demo Note

            Body with **bold** and *italic*.

            > [!TIP]
            > Inline `code` works inside callouts too.

            - one
            - two

            See ref[^a].

            [^a]: definition for a
            """.trimIndent()
        )
    }

    @Test
    fun markdown_link_light() = snapshot("markdown_link_light", darkTheme = false) {
        Renders("Visit [our site](https://example.com) for details.")
    }

    @Test
    fun table_light() = snapshot("table_light", darkTheme = false) {
        Renders(
            """
            | Name | Score | Status |
            | :--- | ---: | :---: |
            | Alice | 42 | OK |
            | Bob | 7 | FAIL |
            """.trimIndent()
        )
    }

    /**
     * Regression net for #197: links and inline styles must render *inside*
     * table cells (link color + underline, bold, code), not fall through to
     * plain text.
     */
    @Test
    fun table_links_light() = snapshot("table_links_light", darkTheme = false) {
        Renders(
            """
            | name | link | rating |
            | --- | --- | --- |
            | Restaurant 1 | [address](https://www.restaurant.com) | ***** |
            | Cafe **Two** | [[Another Note]] | `ok` |
            """.trimIndent()
        )
    }

    @Test
    fun mixed_document_dark() = snapshot("mixed_document_dark", darkTheme = true) {
        Renders(
            """
            ---
            title: Demo
            ---

            # Demo Note

            Body with **bold** and *italic*.

            > [!TIP]
            > Inline `code` works inside callouts too.

            - one
            - two

            See ref[^a].

            [^a]: definition for a
            """.trimIndent()
        )
    }

    /**
     * Regression net for #339: a nested item must be a row of its own, at its
     * own indent, with its own marker. Before the fix the whole subtree was
     * folded into the parent row — `- a` with a nested `- b` rendered as the
     * single bullet `ab`, and a nested `- [ ] task` had no checkbox at all.
     */
    @Test
    fun nested_lists_light() = snapshot("nested_lists_light", darkTheme = false) {
        Renders(
            """
            - top item
              - nested item
                - deeper item
            - [ ] parent task
              - [x] child done
              - [ ] child todo

            1. one
               1. one-a
            """.trimIndent()
        )
    }

    /**
     * A loose list — items separated by blank lines — has to render with more
     * air than a tight one, or the blank lines the author typed disappear
     * (#340). Both lists below hold the same three items.
     */
    @Test
    fun loose_and_tight_lists_light() = snapshot("loose_and_tight_lists_light", darkTheme = false) {
        Renders(
            """
            Tight:

            - alpha
            - beta
            - gamma

            Loose:

            - alpha

            - beta

            - gamma
            """.trimIndent()
        )
    }

    /**
     * Regression net for #340: the gap between two paragraphs has to be larger
     * than the gap between two lines of one paragraph, and a heading needs air
     * above it — except at the very top, where there is nothing to separate it
     * from.
     */
    @Test
    fun paragraph_rhythm_light() = snapshot("paragraph_rhythm_light", darkTheme = false) {
        Renders(
            """
            # Heading one

            First paragraph, long enough that it wraps onto a second line in
            the snapshot canvas.

            Second paragraph, which must read as separate from the first.

            ## Heading two

            Another paragraph.
            """.trimIndent()
        )
    }

    private fun snapshot(name: String, darkTheme: Boolean, content: @Composable () -> Unit) {
        composeRule.setContent {
            MarkleafTheme(darkTheme = darkTheme, dynamicColor = false) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(8.dp)
                ) {
                    content()
                }
            }
        }
        composeRule.onRoot().captureRoboImage(filePath = "src/test/snapshots/roborazzi/$name.png")
    }

    @Composable
    private fun Renders(markdown: String) {
        MarkdownPreviewList(
            lines = SimpleMarkdownPreview.parse(markdown),
            contentPadding = PaddingValues(0.dp)
        )
    }
}
