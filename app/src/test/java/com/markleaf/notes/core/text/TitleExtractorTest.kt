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
}
