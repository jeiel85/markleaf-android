package com.markleaf.notes.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WikilinkExtractorTest {

    @Test
    fun extract_findsSingleLink() {
        assertEquals(listOf("Project A"), WikilinkExtractor.extract("see [[Project A]] please"))
    }

    @Test
    fun extract_findsMultipleLinksInSourceOrder() {
        val targets = WikilinkExtractor.extract("[[A]] then [[B]] then [[A]] again")
        assertEquals(listOf("A", "B", "A"), targets)
    }

    @Test
    fun extract_trimsWhitespaceInsideBrackets() {
        assertEquals(listOf("Project A"), WikilinkExtractor.extract("see [[  Project A  ]]"))
    }

    @Test
    fun extract_ignoresMalformedBrackets() {
        // single brackets, missing close, line break inside — none should match
        assertEquals(emptyList<String>(), WikilinkExtractor.extract("[A] [[B [[C\n]]"))
    }

    @Test
    fun hasAny_isCheaperShortcut() {
        assertTrue(WikilinkExtractor.hasAny("plain [[link]] text"))
        assertFalse(WikilinkExtractor.hasAny("plain text"))
    }

    @Test
    fun normalize_lowercasesAndTrims() {
        assertEquals("project a", WikilinkExtractor.normalize("  Project A  "))
    }
}
