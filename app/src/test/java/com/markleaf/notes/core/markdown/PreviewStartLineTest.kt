package com.markleaf.notes.core.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * `PreviewLine.startLine` (#464): the line of the note's text where the block
 * behind a preview row begins, so that leaving Preview for the editor can put
 * the caret where the reader was. Every assertion pins a row to a line of the
 * source written out below it, so an off-by-one shows up as a wrong line rather
 * than as a number that merely looks plausible.
 */
class PreviewStartLineTest {

    private fun parse(markdown: String) = SimpleMarkdownPreview.parse(markdown.trimIndent())

    @Test
    fun `headings and paragraphs start on the line they are written on`() {
        val lines = parse(
            """
            # Title

            First paragraph.

            ## Section

            Second paragraph,
            wrapped onto a second line.
            """
        )

        assertEquals(
            listOf("Title", "First paragraph.", "Section"),
            lines.take(3).map { it.text }
        )
        assertEquals(listOf(0, 2, 4, 6), lines.map { it.startLine })
    }

    @Test
    fun `each list item starts on its own line, not on the first item's`() {
        val lines = parse(
            """
            - one
            - two
            - three
            """
        )

        assertEquals(listOf(0, 1, 2), lines.map { it.startLine })
    }

    @Test
    fun `nested and ordered list items each know their own line`() {
        val lines = parse(
            """
            1. first
               - nested a
               - nested b
            2. second
            """
        )

        assertEquals(listOf("first", "nested a", "nested b", "second"), lines.map { it.text })
        assertEquals(listOf(0, 1, 2, 3), lines.map { it.startLine })
    }

    @Test
    fun `a paragraph written under a list item starts on its own line`() {
        val lines = parse(
            """
            - item

              continuation paragraph
            """
        )

        assertEquals(listOf("item", "continuation paragraph"), lines.map { it.text })
        assertEquals(listOf(0, 2), lines.map { it.startLine })
    }

    @Test
    fun `code blocks, rules, tables and quotes start where their block opens`() {
        val lines = parse(
            """
            intro

            ```kotlin
            val x = 1
            ```

            ---

            | a | b |
            | - | - |
            | 1 | 2 |

            > quoted
            """
        )

        val byType = lines.associate { it.type to it.startLine }
        assertEquals(2, byType[PreviewLineType.CODE_BLOCK])
        assertEquals(6, byType[PreviewLineType.HORIZONTAL_RULE])
        assertEquals(8, byType[PreviewLineType.TABLE])
        assertEquals(12, byType[PreviewLineType.BLOCKQUOTE])
    }

    @Test
    fun `a callout starts on the line that opens it`() {
        val lines = parse(
            """
            before

            > [!NOTE]
            > body of the callout
            """
        )

        assertEquals(2, lines.single { it.type == PreviewLineType.CALLOUT }.startLine)
    }

    @Test
    fun `front matter starts on the first line`() {
        val lines = parse(
            """
            ---
            title: Hello
            ---

            body
            """
        )

        assertEquals(PreviewLineType.FRONTMATTER, lines.first().type)
        assertEquals(0, lines.first().startLine)
        assertEquals(4, lines.last().startLine)
    }

    @Test
    fun `an image alone in its paragraph starts on its line`() {
        val lines = parse(
            """
            text

            ![alt](pic.png)
            """
        )

        assertEquals(PreviewLineType.IMAGE, lines.last().type)
        assertEquals(2, lines.last().startLine)
    }

    @Test
    fun `rows inside a details section keep the lines they were written on`() {
        val lines = parse(
            """
            <details>
            <summary>More</summary>

            hidden paragraph

            </details>

            after
            """
        )

        val paragraph = lines.first { it.text == "hidden paragraph" }
        assertEquals(3, paragraph.startLine)
        assertEquals(7, lines.last { it.text == "after" }.startLine)
    }

    @Test
    fun `startLine leaves the exact sourceLine contract alone`() {
        // sourceLine stays null for a plain bullet so a tap can never land on a
        // row with no `[ ]` to flip; startLine is a separate, looser field.
        val lines = parse(
            """
            - plain
            - [ ] task
            """
        )

        assertNull(lines[0].sourceLine)
        assertEquals(0, lines[0].startLine)
        assertEquals(1, lines[1].sourceLine)
        assertEquals(1, lines[1].startLine)
    }

    @Test
    fun `the plain-text fallback for a note too deep to parse carries no lines`() {
        // Nothing to place a caret from: it exists precisely because the tree could
        // not be walked. The caret is left alone rather than moved to a guess.
        val rows = CommonMarkPreviewAdapter.plainTextRows("a\nb")

        assertEquals(listOf(null, null), rows.map { it.startLine })
    }
}
