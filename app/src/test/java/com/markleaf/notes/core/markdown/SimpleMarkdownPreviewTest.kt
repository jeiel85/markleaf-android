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

    @Test
    fun parse_parsesFrontmatter() {
        val markdown = """
            ---
            title: Hello
            tags: [draft]
            ---
            Body text
        """.trimIndent()

        val lines = SimpleMarkdownPreview.parse(markdown)

        assertEquals(PreviewLineType.FRONTMATTER, lines[0].type)
        assertEquals("title: Hello\ntags: [draft]", lines[0].text)
        assertEquals(PreviewLineType.BODY, lines[1].type)
        assertEquals("Body text", lines[1].text)
    }

    @Test
    fun parse_doesNotTreatMidDocumentDashesAsFrontmatter() {
        val markdown = "Body\n---\nMore body"
        val lines = SimpleMarkdownPreview.parse(markdown)

        // First line is body; the `---` should still parse as a horizontal rule.
        assertEquals(PreviewLineType.BODY, lines[0].type)
        assertEquals(PreviewLineType.HORIZONTAL_RULE, lines[1].type)
    }

    @Test
    fun parse_parsesCalloutBlocks() {
        val markdown = """
            > [!NOTE]
            > A useful note.
            > Second line.
            After
        """.trimIndent()

        val lines = SimpleMarkdownPreview.parse(markdown)

        assertEquals(PreviewLineType.CALLOUT, lines[0].type)
        assertEquals("NOTE", lines[0].extra)
        assertEquals("A useful note.\nSecond line.", lines[0].text)
        assertEquals(PreviewLineType.BODY, lines[1].type)
    }

    @Test
    fun parse_calloutKindAcceptsCommonAliases() {
        assertEquals(CalloutKind.WARNING, CalloutKind.parse("warn"))
        assertEquals(CalloutKind.WARNING, CalloutKind.parse("WARNING"))
        assertEquals(CalloutKind.CAUTION, CalloutKind.parse("danger"))
        assertEquals(CalloutKind.TIP, CalloutKind.parse("tip"))
        assertEquals(null, CalloutKind.parse("UNKNOWN"))
    }

    @Test
    fun parse_parsesFootnoteDefinition() {
        val lines = SimpleMarkdownPreview.parse("[^1]: A footnote body.")

        assertEquals(PreviewLineType.FOOTNOTE_DEF, lines[0].type)
        assertEquals("1", lines[0].extra)
        assertEquals("A footnote body.", lines[0].text)
    }

    @Test
    fun parse_parsesFootnoteRefAsInlineSegment() {
        val lines = SimpleMarkdownPreview.parse("Body with ref[^1] inside")

        assertEquals(PreviewLineType.BODY, lines[0].type)
        val ref = lines[0].segments.firstOrNull { it.type == PreviewInlineType.FOOTNOTE_REF }
        assertEquals("1", ref?.text)
    }
}
