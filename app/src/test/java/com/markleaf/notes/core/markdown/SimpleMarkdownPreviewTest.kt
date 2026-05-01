package com.markleaf.notes.core.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimpleMarkdownPreviewTest {
    @Test
    fun parse_parsesHeadingsAndBody() {
        val markdown = """
            # Title
            ## Subtitle
            ### Section
            plain text
        """.trimIndent()

        val lines = SimpleMarkdownPreview.parse(markdown)

        assertEquals(4, lines.size)
        assertEquals(PreviewLineType.H1, lines[0].type)
        assertEquals("Title", lines[0].text)
        assertEquals(PreviewLineType.H2, lines[1].type)
        assertEquals(PreviewLineType.H3, lines[2].type)
        assertEquals(PreviewLineType.BODY, lines[3].type)
    }

    @Test
    fun parse_parsesBulletsAndCheckboxes() {
        val markdown = """
            - bullet
            - [ ] todo item
            - [x] done item
        """.trimIndent()

        val lines = SimpleMarkdownPreview.parse(markdown)

        assertEquals(3, lines.size)
        assertEquals(PreviewLineType.BULLET, lines[0].type)
        assertEquals(PreviewLineType.CHECKBOX_TODO, lines[1].type)
        assertEquals(PreviewLineType.CHECKBOX_DONE, lines[2].type)
    }

    @Test
    fun parse_keepsEmptyLines() {
        val markdown = "a\n\nb"
        val lines = SimpleMarkdownPreview.parse(markdown)

        assertEquals(3, lines.size)
        assertTrue(lines[1].type == PreviewLineType.EMPTY)
    }

    @Test
    fun parse_parsesInlineNoteLinksInBodyText() {
        val lines = SimpleMarkdownPreview.parse("See [[Target Note]] for details")

        assertEquals(PreviewLineType.BODY, lines.first().type)
        assertEquals(3, lines.first().segments.size)
        assertEquals(PreviewInlineType.NOTE_LINK, lines.first().segments[1].type)
        assertEquals("Target Note", lines.first().segments[1].target)
    }

    @Test
    fun parse_parsesInlineMarkdownLinksInBodyText() {
        val lines = SimpleMarkdownPreview.parse("Open [Project](Project Note) next")

        assertEquals(PreviewLineType.BODY, lines.first().type)
        assertEquals(3, lines.first().segments.size)
        assertEquals(PreviewInlineType.MARKDOWN_LINK, lines.first().segments[1].type)
        assertEquals("Project Note", lines.first().segments[1].target)
    }
}
