package com.markleaf.notes.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TagParserTest {

    @Test
    fun `parseTags returns empty list for content without tags`() {
        val content = "This is a note without any tags"
        val tags = TagParser.parseTags(content)
        assertEquals(emptyList<String>(), tags)
    }

    @Test
    fun `parseTags extracts single tag`() {
        val content = "This is a note #tag1"
        val tags = TagParser.parseTags(content)
        assertEquals(listOf("tag1"), tags)
    }

    @Test
    fun `parseTags extracts multiple tags`() {
        val content = "Note with #tag1 and #tag2 here"
        val tags = TagParser.parseTags(content)
        assertEquals(listOf("tag1", "tag2"), tags)
    }

    @Test
    fun `parseTags does not extract heading as tag`() {
        val content = "# Heading not a tag\nThis is a note #realTag"
        val tags = TagParser.parseTags(content)
        assertEquals(listOf("realTag"), tags)
    }

    @Test
    fun `parseTags does not extract URL fragment as tag`() {
        val content = "Link: https://example.com#fragment and #realTag"
        val tags = TagParser.parseTags(content)
        assertEquals(listOf("realTag"), tags)
    }

    @Test
    fun `parseTags supports Korean tags`() {
        val content = "노트 #태그1 and #한글태그"
        val tags = TagParser.parseTags(content)
        assertEquals(listOf("태그1", "한글태그"), tags)
    }

    @Test
    fun `parseTags handles multiple hash symbols correctly`() {
        val content = "Note with ##notATag and #realTag"
        val tags = TagParser.parseTags(content)
        assertEquals(listOf("realTag"), tags)
    }

    @Test
    fun `parseTags excludes empty tag names`() {
        val content = "Note with # and #realTag"
        val tags = TagParser.parseTags(content)
        assertEquals(listOf("realTag"), tags)
    }

    @Test
    fun `parseTags works at start of content`() {
        val content = "#firstTag is the first word"
        val tags = TagParser.parseTags(content)
        assertEquals(listOf("firstTag"), tags)
    }

    @Test
    fun `normalizeTagName lowercases and trims`() {
        val tag = "  TagName  "
        val normalized = TagParser.normalizeTagName(tag)
        assertEquals("tagname", normalized)
    }
}
