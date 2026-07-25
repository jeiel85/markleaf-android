package com.markleaf.notes.core.text

import junit.framework.TestCase.assertEquals
import org.junit.Test

class TitleExtractorTest {

    @Test
    fun `extractTitle returns Untitled for blank content`() {
        assertEquals("Untitled", TitleExtractor.extractTitle(""))
        assertEquals("Untitled", TitleExtractor.extractTitle("   "))
        assertEquals("Untitled", TitleExtractor.extractTitle("\n\n"))
    }

    @Test
    fun `extractTitle uses first heading`() {
        val content = "# My Heading\nSome content here"
        assertEquals("My Heading", TitleExtractor.extractTitle(content))
    }

    @Test
    fun `extractTitle uses first non-empty line when no heading`() {
        val content = "\n\nFirst line\nSecond line"
        assertEquals("First line", TitleExtractor.extractTitle(content))
    }

    @Test
    fun `extractTitle limits to 80 chars`() {
        val longTitle = "# " + "a".repeat(100)
        val result = TitleExtractor.extractTitle(longTitle)
        assertEquals(80, result.length)
    }

    @Test
    fun `extractTitle removes heading markers`() {
        val content = "###  Deep Heading  "
        assertEquals("Deep Heading", TitleExtractor.extractTitle(content))
    }

    @Test
    fun `generateExcerpt returns empty for blank`() {
        assertEquals("", TitleExtractor.generateExcerpt(""))
    }

    @Test
    fun `generateExcerpt truncates long content`() {
        val longContent = "# Title\n" + "a".repeat(200)
        val excerpt = TitleExtractor.generateExcerpt(longContent)
        assertEquals(103, excerpt.length) // 100 + "..."
    }

    @Test
    fun `generateExcerpt removes markdown syntax`() {
        val content = "# Note\n**bold** and *italic* and ~~strike~~ and `code`"
        val excerpt = TitleExtractor.generateExcerpt(content)
        assertEquals("bold and italic and strike and code", excerpt)
    }

    @Test
    fun `generateExcerpt skips heading used as the title`() {
        val content = "# Project brief\n\nBody content follows here"
        assertEquals("Body content follows here", TitleExtractor.generateExcerpt(content))
    }

    @Test
    fun `generateExcerpt skips first line when no heading`() {
        val content = "First line title\nSecond line body"
        assertEquals("Second line body", TitleExtractor.generateExcerpt(content))
    }

    @Test
    fun `generateExcerpt is empty when the note is only a title`() {
        assertEquals("", TitleExtractor.generateExcerpt("# Only title"))
    }

    /**
     * The behaviour #280 reported: with the default rule, a heading anywhere in
     * the note outranks the line the note actually starts with.
     */
    @Test
    fun `first heading wins over an earlier plain line by default`() {
        val content = "Some plain text\n## Details\nmore"
        assertEquals("Details", TitleExtractor.extractTitle(content))
        assertEquals(
            "Details",
            TitleExtractor.extractTitle(content, NoteTitleSource.FIRST_HEADING)
        )
    }

    @Test
    fun `first line mode ignores a heading further down`() {
        val content = "Some plain text\n## Details\nmore"
        assertEquals(
            "Some plain text",
            TitleExtractor.extractTitle(content, NoteTitleSource.FIRST_LINE)
        )
    }

    @Test
    fun `first line mode still strips markers when the first line is a heading`() {
        val content = "## My Note\nbody"
        assertEquals(
            "My Note",
            TitleExtractor.extractTitle(content, NoteTitleSource.FIRST_LINE)
        )
    }

    @Test
    fun `first line mode skips leading blank lines`() {
        val content = "\n\n  Started here\n# Later heading"
        assertEquals(
            "Started here",
            TitleExtractor.extractTitle(content, NoteTitleSource.FIRST_LINE)
        )
    }

    @Test
    fun `first line mode is Untitled for blank content`() {
        assertEquals("Untitled", TitleExtractor.extractTitle("", NoteTitleSource.FIRST_LINE))
        assertEquals("Untitled", TitleExtractor.extractTitle("\n \n", NoteTitleSource.FIRST_LINE))
    }

    /** The excerpt drops whichever line became the title, under either rule. */
    @Test
    fun `excerpt skips the line the title came from in first line mode`() {
        val content = "Some plain text\n## Details\nmore"
        assertEquals(
            "Details\nmore",
            TitleExtractor.generateExcerpt(content, NoteTitleSource.FIRST_LINE)
        )
        assertEquals(
            "Some plain text\nmore",
            TitleExtractor.generateExcerpt(content, NoteTitleSource.FIRST_HEADING)
        )
    }
}
