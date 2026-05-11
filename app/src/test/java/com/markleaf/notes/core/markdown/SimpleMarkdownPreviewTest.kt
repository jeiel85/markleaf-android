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

        // CommonMark collapses adjacent blocks into one PreviewLine each (no
        // synthetic EMPTY between them) — the renderer adds visual spacing.
        val typesInOrder = lines.map { it.type }
        assertEquals(
            listOf(PreviewLineType.H1, PreviewLineType.H2, PreviewLineType.H3, PreviewLineType.BODY),
            typesInOrder
        )
        assertEquals("Title", lines[0].text)
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
    fun parse_separatesParagraphsByBlankLine() {
        val markdown = "a\n\nb"
        val lines = SimpleMarkdownPreview.parse(markdown)

        // CommonMark consumes the blank line as a paragraph separator —
        // the renderer adds visual spacing without a synthetic EMPTY line.
        assertEquals(2, lines.size)
        assertEquals(PreviewLineType.BODY, lines[0].type)
        assertEquals("a", lines[0].text)
        assertEquals(PreviewLineType.BODY, lines[1].type)
        assertEquals("b", lines[1].text)
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

        val typesInOrder = lines.map { it.type }
        assertEquals(
            listOf(PreviewLineType.BODY, PreviewLineType.CODE_BLOCK, PreviewLineType.BODY),
            typesInOrder
        )
        val codeBlock = lines.first { it.type == PreviewLineType.CODE_BLOCK }
        assertEquals("val a = 1\nval b = 2", codeBlock.text)
        assertEquals("kotlin", codeBlock.extra)
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
            tags: draft
            ---
            Body text
        """.trimIndent()

        val lines = SimpleMarkdownPreview.parse(markdown)

        // CommonMark's YAML front-matter visitor returns key→list shape;
        // the adapter reformats to "key: value" lines for display.
        assertEquals(PreviewLineType.FRONTMATTER, lines[0].type)
        assertTrue("frontmatter contains title", lines[0].text.contains("title: Hello"))
        // First non-frontmatter, non-empty line should be BODY.
        val firstAfterFrontmatter = lines.drop(1).firstOrNull { it.type != PreviewLineType.EMPTY }
        assertEquals(PreviewLineType.BODY, firstAfterFrontmatter?.type)
        assertEquals("Body text", firstAfterFrontmatter?.text)
    }

    @Test
    fun parse_doesNotTreatMidDocumentDashesAsFrontmatter() {
        // Blank lines around `---` make it a horizontal rule per CommonMark.
        // Without blank lines (`Body\n---`), CommonMark would treat it as a
        // Setext H2 instead — that's the correct spec behavior; v2.3 honors it.
        val markdown = "Body\n\n---\n\nMore body"
        val lines = SimpleMarkdownPreview.parse(markdown)

        val types = lines.map { it.type }
        assertTrue("expected a HORIZONTAL_RULE in the output", PreviewLineType.HORIZONTAL_RULE in types)
        assertTrue("expected at least one BODY line", PreviewLineType.BODY in types)
    }

    @Test
    fun parse_parsesCalloutBlocks() {
        // CommonMark requires a blank line to end a blockquote — without it,
        // following text is lazily folded into the same blockquote. The blank
        // line below is what tells the parser the callout ends.
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
        val afterIndex = lines.indexOfFirst { it.type == PreviewLineType.BODY && it.text == "After" }
        assertTrue("expected an 'After' BODY line", afterIndex > 0)
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
    fun parse_emitsLinkSegmentWithHref() {
        val lines = SimpleMarkdownPreview.parse("see [our site](https://example.com) please")

        val linkSegment = lines.first().segments.firstOrNull { it.type == PreviewInlineType.LINK }
        assertEquals("our site", linkSegment?.text)
        assertEquals("https://example.com", linkSegment?.href)
    }

    @Test
    fun parse_parsesFootnoteRefAsInlineSegment() {
        // CommonMark only recognises `[^1]` as a footnote reference when a
        // matching definition exists in the same document — that's per spec.
        val markdown = """
            Body with ref[^1] inside

            [^1]: definition
        """.trimIndent()

        val lines = SimpleMarkdownPreview.parse(markdown)

        val bodyLine = lines.firstOrNull { it.type == PreviewLineType.BODY }
        val ref = bodyLine?.segments?.firstOrNull { it.type == PreviewInlineType.FOOTNOTE_REF }
        assertEquals("1", ref?.text)
    }
}
