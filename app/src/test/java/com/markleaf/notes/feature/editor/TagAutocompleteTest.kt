package com.markleaf.notes.feature.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Covers [detectTagQuery] / [completeTag], the `#tag` autocomplete trigger.
 * The rules deliberately mirror [com.markleaf.notes.util.TagParser] (see
 * TagParserTest) so the dropdown and the persisted tag index agree on what a
 * tag is — these cases parallel that suite on purpose.
 */
class TagAutocompleteTest {

    private fun field(text: String, cursor: Int = text.length) =
        TextFieldValue(text = text, selection = TextRange(cursor))

    // ---- detectTagQuery ----

    @Test
    fun `bare hash returns empty query`() {
        assertEquals("", detectTagQuery(field("note #")))
    }

    @Test
    fun `partial tag returns the typed prefix`() {
        assertEquals("ta", detectTagQuery(field("note #ta")))
    }

    @Test
    fun `tag at start of content is detected`() {
        assertEquals("first", detectTagQuery(field("#first")))
    }

    @Test
    fun `tag after a newline is detected`() {
        assertEquals("ta", detectTagQuery(field("line one\n#ta")))
    }

    @Test
    fun `hierarchical tag in progress is detected`() {
        assertEquals("project/si", detectTagQuery(field("note #project/si")))
    }

    @Test
    fun `korean tag is detected`() {
        assertEquals("한글", detectTagQuery(field("메모 #한글")))
    }

    @Test
    fun `url fragment is not a tag`() {
        assertNull(detectTagQuery(field("see https://example.com#frag")))
    }

    @Test
    fun `double hash is not a tag`() {
        assertNull(detectTagQuery(field("text ##foo")))
    }

    @Test
    fun `mid-word hash is not a tag`() {
        assertNull(detectTagQuery(field("foo#bar")))
    }

    @Test
    fun `heading hash is not a tag`() {
        // "# Heading" — the space after `#` ends any tag candidate.
        assertNull(detectTagQuery(field("# Heading")))
    }

    @Test
    fun `closed tag with following text is not in progress`() {
        // Cursor is after the space, past the completed tag.
        assertNull(detectTagQuery(field("note #work done")))
    }

    @Test
    fun `tag cannot start with a digit`() {
        assertNull(detectTagQuery(field("note #1tag")))
    }

    @Test
    fun `no hash before cursor returns null`() {
        assertNull(detectTagQuery(field("just text")))
    }

    @Test
    fun `query is taken up to the cursor, not the whole word`() {
        // Cursor sits after "wo" inside "#work".
        assertEquals("wo", detectTagQuery(field("note #work", cursor = 8)))
    }

    // ---- completeTag ----

    @Test
    fun `completes at end of line and adds a trailing space`() {
        val result = completeTag(field("note #ta"), "tag")
        assertEquals("note #tag ", result.text)
        assertEquals(TextRange(10), result.selection)
    }

    @Test
    fun `completes a partial query replacing only up to the cursor`() {
        // "#proj" with cursor after "proj"; picking the full nested tag.
        val result = completeTag(field("note #proj"), "project/site")
        assertEquals("note #project/site ", result.text)
        assertEquals(TextRange(19), result.selection)
    }

    @Test
    fun `does not double the space when followed by whitespace`() {
        val value = field("note #ta done", cursor = 8)
        val result = completeTag(value, "tag")
        assertEquals("note #tag done", result.text)
        assertEquals(TextRange(9), result.selection)
    }

    @Test
    fun `completing from a bare hash inserts the tag`() {
        val result = completeTag(field("note #"), "ideas")
        assertEquals("note #ideas ", result.text)
        assertEquals(TextRange(12), result.selection)
    }
}
