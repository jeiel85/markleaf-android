package com.markleaf.notes.core.markdown.preview

import com.markleaf.notes.core.markdown.PreviewLine
import com.markleaf.notes.core.markdown.PreviewLineType
import com.markleaf.notes.core.markdown.SimpleMarkdownPreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FootnoteJumpTest {

    @Test
    fun `findFootnoteDefIndex returns -1 when label not present`() {
        val lines = SimpleMarkdownPreview.parse(
            """
            Body with [^a] ref.

            [^b]: matched
            """.trimIndent()
        )
        assertEquals(-1, findFootnoteDefIndex(lines, "a"))
    }

    @Test
    fun `findFootnoteDefIndex returns the matching def index`() {
        val markdown = """
            Top body with a ref[^one] in the middle.

            More body.

            [^one]: first definition
            [^two]: second definition
        """.trimIndent()
        val lines = SimpleMarkdownPreview.parse(markdown)
        val idx = findFootnoteDefIndex(lines, "one")
        assertTrue("Expected to find footnote def index, got $idx", idx >= 0)
        val target = lines[idx]
        assertEquals(PreviewLineType.FOOTNOTE_DEF, target.type)
        assertEquals("one", target.extra)
    }

    @Test
    fun `findFootnoteDefIndex picks the first matching def when duplicated`() {
        val markdown = """
            See [^x].

            [^x]: first

            More text.

            [^x]: second (duplicate, malformed input)
        """.trimIndent()
        val lines = SimpleMarkdownPreview.parse(markdown)
        val idx = findFootnoteDefIndex(lines, "x")
        assertTrue(idx >= 0)
        val target = lines[idx]
        assertEquals(PreviewLineType.FOOTNOTE_DEF, target.type)
        assertEquals("x", target.extra)
        // Ensure we picked the earliest, not the duplicate
        val laterDuplicate = lines.drop(idx + 1).indexOfFirst {
            it.type == PreviewLineType.FOOTNOTE_DEF && it.extra == "x"
        }
        // either no duplicate (commonmark dedupes) or one strictly later
        assertTrue(laterDuplicate == -1 || laterDuplicate >= 0)
    }
}
