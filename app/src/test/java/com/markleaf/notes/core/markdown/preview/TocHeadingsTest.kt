package com.markleaf.notes.core.markdown.preview

import com.markleaf.notes.core.markdown.PreviewLineType
import com.markleaf.notes.core.markdown.SimpleMarkdownPreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers [extractHeadings], the table-of-contents outline derived from rendered
 * preview lines. Parses real Markdown via [SimpleMarkdownPreview] (like
 * FootnoteJumpTest) so the indices it returns line up with what the preview
 * LazyColumn actually renders.
 */
class TocHeadingsTest {

    @Test
    fun `body-only note has no headings`() {
        val lines = SimpleMarkdownPreview.parse("just a paragraph\n\nand another")
        assertEquals(emptyList<TocHeading>(), extractHeadings(lines))
    }

    @Test
    fun `extracts h1 h2 h3 in order with levels`() {
        val md = """
            # Title

            intro

            ## Section A

            body

            ### Sub A1

            more
        """.trimIndent()
        val headings = extractHeadings(SimpleMarkdownPreview.parse(md))
        assertEquals(listOf("Title", "Section A", "Sub A1"), headings.map { it.text })
        assertEquals(listOf(1, 2, 3), headings.map { it.level })
    }

    @Test
    fun `heading index points at the matching preview line`() {
        val md = """
            intro paragraph

            ## Findings

            detail
        """.trimIndent()
        val lines = SimpleMarkdownPreview.parse(md)
        val headings = extractHeadings(lines)
        assertEquals(1, headings.size)
        val target = lines[headings.first().index]
        assertEquals(PreviewLineType.H2, target.type)
        assertEquals("Findings", target.text)
    }

    @Test
    fun `a heading deeper than three keeps its own level`() {
        // This asserted `level in 1..3` until #255: the preview model stopped at
        // H3 and bucketed everything deeper into it. Invisible while the preview
        // was the only consumer, and wrong once the outline screen shipped —
        // indentation is its only level cue, so a deeply nested note read flat.
        val md = """
            # Top

            ## Mid

            #### Deeper

            ###### Deepest
        """.trimIndent()
        val headings = extractHeadings(SimpleMarkdownPreview.parse(md))
        assertEquals(listOf("Top", "Mid", "Deeper", "Deepest"), headings.map { it.text })
        assertEquals(listOf(1, 2, 4, 6), headings.map { it.level })
        // Six is the deepest ATX heading commonmark recognises, so nothing
        // should ever land outside this range.
        assertTrue(headings.all { it.level in 1..6 })
    }

    /**
     * A run of `#` with no space is not a heading, and a seventh level does not
     * exist — both stay body text rather than clamping to H6.
     */
    @Test
    fun `hashes that do not form a heading stay out of the outline`() {
        val md = """
            ####### Seven hashes

            #NoSpace
        """.trimIndent()

        assertEquals(emptyList<TocHeading>(), extractHeadings(SimpleMarkdownPreview.parse(md)))
    }

    /**
     * The rendered index only ever locates a heading in the preview. Jumping to
     * one while editing needs the line it occupies in the note's own text, so
     * every heading has to carry it — blank lines and non-heading blocks in
     * between included (#215).
     */
    @Test
    fun `each heading carries the source line it sits on`() {
        val md = """
            # Title

            intro paragraph

            ## Section A

            body

            ### Sub A1
        """.trimIndent()
        val headings = extractHeadings(SimpleMarkdownPreview.parse(md))
        assertEquals(listOf("Title", "Section A", "Sub A1"), headings.map { it.text })
        assertEquals(listOf(0, 4, 8), headings.map { it.sourceLine })
    }

    @Test
    fun `a setext heading reports the line its text is on`() {
        // No `#` to count, so the line can only come from the parser's spans.
        val md = "Title\n=====\n\n## Later\n"
        val headings = extractHeadings(SimpleMarkdownPreview.parse(md))
        assertEquals(listOf("Title", "Later"), headings.map { it.text })
        assertEquals(listOf(0, 3), headings.map { it.sourceLine })
    }
}
