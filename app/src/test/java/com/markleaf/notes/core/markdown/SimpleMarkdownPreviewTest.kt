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

    @Test
    fun parse_parsesMarkdownTablesAndSkipsDivider() {
        val markdown = """
            | Name | Count |
            | --- | ---: |
            | Ideas | 3 |
            | Drafts | 2 |
        """.trimIndent()

        val lines = SimpleMarkdownPreview.parse(markdown)

        assertEquals(3, lines.size)
        assertEquals(PreviewLineType.TABLE_HEADER, lines[0].type)
        assertEquals(listOf("Name", "Count"), lines[0].cells)
        assertEquals(PreviewLineType.TABLE_ROW, lines[1].type)
        assertEquals(listOf("Ideas", "3"), lines[1].cells)
        assertEquals(PreviewLineType.TABLE_ROW, lines[2].type)
    }

    @Test
    fun parse_parsesInlineMathSegments() {
        val lines = SimpleMarkdownPreview.parse("Use \$a^2 + b^2 = c^2\$ in notes")

        assertEquals(PreviewLineType.BODY, lines.first().type)
        assertEquals(3, lines.first().segments.size)
        assertEquals(PreviewInlineType.INLINE_MATH, lines.first().segments[1].type)
        assertEquals("a^2 + b^2 = c^2", lines.first().segments[1].text)
    }

    @Test
    fun parse_parsesBoldInline() {
        val lines = SimpleMarkdownPreview.parse("This is **bold** text")

        assertEquals(PreviewLineType.BODY, lines.first().type)
        assertEquals(3, lines.first().segments.size)
        assertEquals(PreviewInlineType.TEXT, lines.first().segments[0].type)
        assertEquals(PreviewInlineType.BOLD, lines.first().segments[1].type)
        assertEquals("bold", lines.first().segments[1].text)
        assertEquals(PreviewInlineType.TEXT, lines.first().segments[2].type)
    }

    @Test
    fun parse_parsesItalicStarInline() {
        val lines = SimpleMarkdownPreview.parse("This is *italic* text")

        assertEquals(PreviewLineType.BODY, lines.first().type)
        assertEquals(3, lines.first().segments.size)
        assertEquals(PreviewInlineType.ITALIC, lines.first().segments[1].type)
        assertEquals("italic", lines.first().segments[1].text)
    }

    @Test
    fun parse_parsesItalicUnderscoreInline() {
        val lines = SimpleMarkdownPreview.parse("This is _italic_ text")

        assertEquals(PreviewLineType.BODY, lines.first().type)
        assertEquals(3, lines.first().segments.size)
        assertEquals(PreviewInlineType.ITALIC, lines.first().segments[1].type)
        assertEquals("italic", lines.first().segments[1].text)
    }

    @Test
    fun parse_parsesStrikethroughInline() {
        val lines = SimpleMarkdownPreview.parse("This is ~~deleted~~ text")

        assertEquals(PreviewLineType.BODY, lines.first().type)
        assertEquals(3, lines.first().segments.size)
        assertEquals(PreviewInlineType.STRIKETHROUGH, lines.first().segments[1].type)
        assertEquals("deleted", lines.first().segments[1].text)
    }

    @Test
    fun parse_parsesInlineCode() {
        val lines = SimpleMarkdownPreview.parse("Use `code` here")

        assertEquals(PreviewLineType.BODY, lines.first().type)
        assertEquals(3, lines.first().segments.size)
        assertEquals(PreviewInlineType.INLINE_CODE, lines.first().segments[1].type)
        assertEquals("code", lines.first().segments[1].text)
    }

    @Test
    fun parse_parsesBoldItalicCombined() {
        val lines = SimpleMarkdownPreview.parse("This is ***bold-italic*** text")

        assertEquals(PreviewLineType.BODY, lines.first().type)
        assertEquals(3, lines.first().segments.size)
        assertEquals(PreviewInlineType.BOLD_ITALIC, lines.first().segments[1].type)
        assertEquals("bold-italic", lines.first().segments[1].text)
    }

    @Test
    fun parse_parsesMixedInlineFormats() {
        val lines = SimpleMarkdownPreview.parse("Start **bold** and *italic* and `code` end")

        assertEquals(PreviewLineType.BODY, lines.first().type)
        val segments = lines.first().segments
        assertEquals(7, segments.size)
        assertEquals(PreviewInlineType.BOLD, segments[1].type)
        assertEquals(PreviewInlineType.ITALIC, segments[3].type)
        assertEquals(PreviewInlineType.INLINE_CODE, segments[5].type)
    }

    @Test
    fun parse_parsesBlockquote() {
        val lines = SimpleMarkdownPreview.parse("> This is a quote")

        assertEquals(1, lines.size)
        assertEquals(PreviewLineType.BLOCKQUOTE, lines[0].type)
        assertEquals("This is a quote", lines[0].text)
    }

    @Test
    fun parse_parsesOrderedList() {
        val lines = SimpleMarkdownPreview.parse("1. First item")

        assertEquals(1, lines.size)
        assertEquals(PreviewLineType.ORDERED_LIST, lines[0].type)
        assertEquals("First item", lines[0].text)
        assertEquals("1", lines[0].extra)
    }

    @Test
    fun parse_parsesHorizontalRule() {
        val lines = SimpleMarkdownPreview.parse("---")

        assertEquals(1, lines.size)
        assertEquals(PreviewLineType.HORIZONTAL_RULE, lines[0].type)
    }

    @Test
    fun parse_parsesHorizontalRuleAsterisks() {
        val lines = SimpleMarkdownPreview.parse("***")

        assertEquals(1, lines.size)
        assertEquals(PreviewLineType.HORIZONTAL_RULE, lines[0].type)
    }

    @Test
    fun parse_parsesDisplayMathBlocks() {
        val markdown = """
            Before
            $$
            E = mc^2
            $$
            After
        """.trimIndent()

        val lines = SimpleMarkdownPreview.parse(markdown)

        assertEquals(3, lines.size)
        assertEquals(PreviewLineType.BODY, lines[0].type)
        assertEquals(PreviewLineType.MATH_BLOCK, lines[1].type)
        assertEquals("E = mc^2", lines[1].text)
        assertEquals(PreviewLineType.BODY, lines[2].type)
    }

    @Test
    fun parse_parsesFencedCodeBlocks() {
        val markdown = """
            Before
            ```kotlin
            val a = 1
            val b = 2
            ```
            After
        """.trimIndent()

        val lines = SimpleMarkdownPreview.parse(markdown)

        assertEquals(3, lines.size)
        assertEquals(PreviewLineType.BODY, lines[0].type)
        assertEquals(PreviewLineType.CODE_BLOCK, lines[1].type)
        assertEquals("val a = 1\nval b = 2", lines[1].text)
        assertEquals("kotlin", lines[1].extra)
        assertEquals(PreviewLineType.BODY, lines[2].type)
    }

    @Test
    fun parse_parsesFencedCodeBlocksWithoutLanguage() {
        val markdown = """
            ```
            plain text
            ```
        """.trimIndent()

        val lines = SimpleMarkdownPreview.parse(markdown)

        assertEquals(1, lines.size)
        assertEquals(PreviewLineType.CODE_BLOCK, lines[0].type)
        assertEquals("plain text", lines[0].text)
        assertEquals(null, lines[0].extra)
    }
}
