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
    fun `outline only ever contains heading levels one through three`() {
        // The preview model only has H1/H2/H3, so however the parser buckets a
        // deeper heading, extractHeadings must never emit a level outside 1..3.
        val md = """
            # Top

            ## Mid

            #### Deeper
        """.trimIndent()
        val headings = extractHeadings(SimpleMarkdownPreview.parse(md))
        assertTrue(headings.isNotEmpty())
        assertTrue(headings.all { it.level in 1..3 })
        assertTrue(headings.any { it.text == "Top" })
    }
}
