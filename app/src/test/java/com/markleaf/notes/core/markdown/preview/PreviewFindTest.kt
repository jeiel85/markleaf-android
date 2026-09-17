package com.markleaf.notes.core.markdown.preview

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.markleaf.notes.core.markdown.PreviewLineType
import com.markleaf.notes.core.markdown.SimpleMarkdownPreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers find-in-note in preview (#417): matching runs over the text the
 * preview draws rather than the Markdown source, and a match inside a collapsed
 * `<details>` section can still be navigated to. Parses real Markdown so the
 * rows under test are the ones the renderer actually receives.
 */
class PreviewFindTest {

    @Test
    fun `matches the rendered text, not the Markdown syntax`() {
        val lines = SimpleMarkdownPreview.parse("Some **bold** text and a [label](https://example.com)")

        assertEquals(1, findInPreview(lines, "bold text").size)
        assertEquals(0, findInPreview(lines, "**bold**").size)
        assertEquals(1, findInPreview(lines, "label").size)
        assertEquals(0, findInPreview(lines, "example.com").size)
    }

    @Test
    fun `is case-insensitive and counts every occurrence in reading order`() {
        val lines = SimpleMarkdownPreview.parse("# Apple\n\napple and APPLE")
        val matches = findInPreview(lines, "apple")

        val headingIndex = lines.indexOfFirst { it.type == PreviewLineType.H1 }
        val bodyIndex = lines.indexOfFirst { it.type == PreviewLineType.BODY }
        assertEquals(
            listOf(
                PreviewFindMatch(headingIndex, 0),
                PreviewFindMatch(bodyIndex, 0),
                PreviewFindMatch(bodyIndex, 1)
            ),
            matches
        )
    }

    @Test
    fun `list markers are not part of the searchable text`() {
        val lines = SimpleMarkdownPreview.parse("1. first\n2. second")

        assertEquals(0, findInPreview(lines, "1.").size)
        assertEquals(1, findInPreview(lines, "second").size)
    }

    @Test
    fun `an empty query matches nothing`() {
        assertEquals(emptyList<PreviewFindMatch>(), findInPreview(SimpleMarkdownPreview.parse("text"), ""))
    }

    @Test
    fun `table cells and code blocks are searched`() {
        val lines = SimpleMarkdownPreview.parse(
            """
            | Name | Note |
            |------|------|
            | kiwi | **kiwi** pie |

            ```
            val kiwi = 1
            ```
            """.trimIndent()
        )

        val table = lines.indexOfFirst { it.type == PreviewLineType.TABLE }
        val code = lines.indexOfFirst { it.type == PreviewLineType.CODE_BLOCK }
        assertEquals(
            listOf(PreviewFindMatch(table, 0), PreviewFindMatch(table, 1), PreviewFindMatch(code, 0)),
            findInPreview(lines, "kiwi")
        )
    }

    @Test
    fun `a match in a collapsed section is found and navigating expands it`() {
        val lines = SimpleMarkdownPreview.parse(
            """
            <details>
            <summary>More</summary>

            hidden needle
            </details>
            """.trimIndent()
        )
        val match = findInPreview(lines, "needle").single()
        assertTrue(match.lineIndex !in visiblePreviewLineIndices(lines, emptySet()))

        val expanded = expandSectionsFor(lines, emptySet(), match.lineIndex)

        assertTrue(match.lineIndex in visiblePreviewLineIndices(lines, expanded))
    }

    @Test
    fun `expanding leaves an already open section as the user set it`() {
        val lines = SimpleMarkdownPreview.parse(
            """
            <details open>
            <summary>More</summary>

            shown needle
            </details>
            """.trimIndent()
        )
        val match = findInPreview(lines, "needle").single()

        assertEquals(emptySet<Int>(), expandSectionsFor(lines, emptySet(), match.lineIndex))
    }

    @Test
    fun `occurrence offsets stay on the drawn text when case folding changes length`() {
        // "İ".lowercase() is two chars; offsets must still point into the original.
        assertEquals(listOf(1), findOccurrences("İab", "ab"))
    }

    @Test
    fun `highlight marks every occurrence after the marker and the current one differently`() {
        val match = SpanStyle(fontWeight = FontWeight.Bold)
        val current = SpanStyle(textDecoration = TextDecoration.Underline)
        val text = AnnotatedString("1. ab ab")

        val result = highlightFindMatches(
            text,
            PreviewFindHighlight(query = "ab", current = 1),
            matchStyle = match,
            currentStyle = current,
            searchStart = 3
        )

        assertEquals(
            listOf(Triple(match, 3, 5), Triple(current, 6, 8)),
            result.spanStyles.map { Triple(it.item, it.start, it.end) }
        )
    }

    @Test
    fun `after shifts the current occurrence into a later piece of the row`() {
        val highlight = PreviewFindHighlight(query = "a", current = 2)

        assertEquals(0, highlight.after(2).current)
        assertEquals(null, highlight.after(3).current)
    }
}
