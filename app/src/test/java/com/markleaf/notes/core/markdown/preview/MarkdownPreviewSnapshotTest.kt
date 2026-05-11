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
