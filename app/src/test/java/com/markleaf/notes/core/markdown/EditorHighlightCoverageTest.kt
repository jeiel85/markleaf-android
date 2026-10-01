package com.markleaf.notes.core.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * M4 of `docs/MARKDOWN_SYNTAX_PLAN.md`: the live editor styles what the
 * preview renders.
 *
 * Before: only `- [ ]` at the very start of a line got the task colour, `~~~`
 * fences were never styled, `#`/`**` inside a code block were styled as
 * structure, and `__bold__`, `***both***`, wikilinks and bare URLs were
 * plain text.
 */
class EditorHighlightCoverageTest {

    private val colors = MarkdownSyntaxColors(
        heading = Color.Red,
        emphasis = Color.Blue,
        link = Color.Green,
        syntax = Color.Gray,
        checkbox = Color.Magenta,
        code = Color.Cyan,
        codeBlock = Color.Yellow,
        blockquote = Color.LightGray,
        horizontalRule = Color.Black
    )

    private fun highlight(text: String) = MarkdownSyntaxHighlighter.highlight(text, colors)

    private fun AnnotatedString.spansWith(predicate: (androidx.compose.ui.text.SpanStyle) -> Boolean) =
        spanStyles.filter { predicate(it.item) }

    private fun AnnotatedString.covered(start: Int, end: Int, predicate: (androidx.compose.ui.text.SpanStyle) -> Boolean) =
        spansWith(predicate).any { it.start <= start && it.end >= end }

    // ---- tasks ---------------------------------------------------------------

    @Test
    fun everyTaskThePreviewDrawsIsColoured() {
        val text = "- parent\n  - [ ] nested\n* [x] star\n1. [ ] numbered\n> - [ ] quoted"
        val result = highlight(text)

        for (task in listOf("  - [ ] nested", "* [x] star", "1. [ ] numbered", "> - [ ] quoted")) {
            val start = text.indexOf(task)
            assertTrue(task, result.covered(start, start + task.length) { it.color == Color.Magenta })
        }
    }

    @Test
    fun aBracketThatIsNotATaskIsNotColoured() {
        val result = highlight("see [x] here\n-[ ] no space")

        assertTrue(result.spansWith { it.color == Color.Magenta }.isEmpty())
    }

    // ---- fences --------------------------------------------------------------

    @Test
    fun aTildeFenceIsCode() {
        val text = "~~~\nval x = 1\n~~~"
        val result = highlight(text)

        assertTrue(result.covered(0, text.length) { it.background == Color.Yellow.copy(alpha = 0.1f) })
    }

    @Test
    fun structureInsideAFenceIsNotStyled() {
        val text = "```bash\n# a shell comment\n**not bold** and [x](y)\n- [ ] not a task\n```"
        val result = highlight(text)

        assertTrue("heading", result.spansWith { it.color == Color.Red }.isEmpty())
        assertTrue("bold", result.spansWith { it.fontWeight == FontWeight.Bold }.isEmpty())
        assertTrue("link", result.spansWith { it.color == Color.Green }.isEmpty())
        assertTrue("task", result.spansWith { it.color == Color.Magenta }.isEmpty())
    }

    @Test
    fun anUnclosedFenceRunsToTheEnd() {
        val text = "intro\n```\nstill code\n# still code"
        val result = highlight(text)

        assertTrue(result.covered(text.indexOf("```"), text.length) { it.background != null && it.background != Color.Unspecified })
        assertTrue(result.spansWith { it.color == Color.Red }.isEmpty())
    }

    @Test
    fun aFenceClosesOnlyOnTheSameCharacterAndLength() {
        val text = "````\n```\nstill inside\n````\nafter"
        val ranges = MarkdownSyntaxHighlighter.fencedCodeRanges(text)

        assertEquals(listOf(0 until text.indexOf("\nafter")), ranges)
    }

    // ---- inline --------------------------------------------------------------

    @Test
    fun emphasisInsideInlineCodeIsNotStyled() {
        val result = highlight("run `**x** and _y_` now")

        assertTrue(result.spansWith { it.fontWeight == FontWeight.Bold }.isEmpty())
        assertTrue(result.spansWith { it.fontStyle == FontStyle.Italic }.isEmpty())
    }

    @Test
    fun underscoreBoldIsBold() {
        val text = "a __strong__ word"
        val result = highlight(text)

        assertTrue(result.covered(text.indexOf("__"), text.indexOf(" word")) { it.fontWeight == FontWeight.Bold })
    }

    @Test
    fun tripleStarIsBoldAndItalic() {
        val text = "***both***"
        val result = highlight(text)

        assertTrue(result.covered(0, text.length) { it.fontWeight == FontWeight.Bold && it.fontStyle == FontStyle.Italic })
    }

    @Test
    fun aWikilinkIsALink() {
        val text = "see [[Some Note]] here"
        val result = highlight(text)

        val start = text.indexOf("[[")
        assertTrue(result.covered(start, start + "[[Some Note]]".length) {
            it.color == Color.Green && it.textDecoration == TextDecoration.Underline
        })
    }

    @Test
    fun aBareUrlIsALink() {
        val text = "see https://example.com/a?b=1. and www.example.org"
        val result = highlight(text)

        val links = result.spansWith { it.color == Color.Green }
        assertEquals(
            listOf("https://example.com/a?b=1", "www.example.org"),
            links.map { text.substring(it.start, it.end) }
        )
    }

    @Test
    fun aWrittenLinksDestinationIsNotStyledTwice() {
        val text = "[docs](https://example.com)"
        val result = highlight(text)

        assertEquals(1, result.spansWith { it.color == Color.Green }.size)
    }
}
