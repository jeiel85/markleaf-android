package com.markleaf.notes.data.sync

import com.markleaf.notes.domain.model.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SyncFrontmatterTest {

    private fun sampleNote() = Note(
        id = "abc-123-def",
        title = "Hello",
        contentMarkdown = "# Hello\n\nBody with **bold** content.",
        excerpt = "Hello",
        createdAt = Instant.parse("2026-05-08T10:30:00Z"),
        updatedAt = Instant.parse("2026-05-08T11:00:00Z"),
        pinned = true,
        archived = false
    )

    @Test
    fun encode_emitsExpectedFrontmatterShape() {
        val encoded = SyncFrontmatter.encode(sampleNote())

        assertTrue("starts with delimiter", encoded.startsWith("---\n"))
        assertTrue("contains markleaf_id", encoded.contains("markleaf_id: abc-123-def"))
        assertTrue("contains created_at iso", encoded.contains("created_at: 2026-05-08T10:30:00Z"))
        assertTrue("contains updated_at iso", encoded.contains("updated_at: 2026-05-08T11:00:00Z"))
        assertTrue("contains pinned true", encoded.contains("pinned: true"))
        assertTrue("contains archived false", encoded.contains("archived: false"))
        assertTrue("body preserved verbatim", encoded.endsWith("# Hello\n\nBody with **bold** content."))
    }

    @Test
    fun decode_roundTripsAllFields() {
        val original = sampleNote()
        val parsed = SyncFrontmatter.decode(SyncFrontmatter.encode(original))

        assertEquals(original.id, parsed.markleafId)
        assertEquals(original.createdAt, parsed.createdAt)
        assertEquals(original.updatedAt, parsed.updatedAt)
        assertEquals(true, parsed.pinned)
        assertEquals(false, parsed.archived)
        assertEquals(original.contentMarkdown, parsed.body)
    }

    @Test
    fun decode_handlesFileWithoutFrontmatter() {
        val raw = "# Just a body\n\nNo frontmatter here."

        val parsed = SyncFrontmatter.decode(raw)

        assertNull(parsed.markleafId)
        assertNull(parsed.createdAt)
        assertNull(parsed.updatedAt)
        assertEquals(raw, parsed.body)
    }

    @Test
    fun decode_handlesUnclosedFrontmatterAsRawBody() {
        // A `---` line at the top is sometimes a horizontal rule, not frontmatter.
        // Without a closing `---`, the whole thing should fall through to body.
        val raw = "---\nnot frontmatter\nstill body"

        val parsed = SyncFrontmatter.decode(raw)

        assertNull(parsed.markleafId)
        assertEquals(raw, parsed.body)
    }

    @Test
    fun decode_preservesUnknownKeysForRoundTripFriendliness() {
        val raw = """
            ---
            markleaf_id: x
            created_at: 2026-05-08T10:30:00Z
            updated_at: 2026-05-08T11:00:00Z
            obsidian_tag: review
            custom_color: blue
            ---
            body
        """.trimIndent()

        val parsed = SyncFrontmatter.decode(raw)

        assertEquals("x", parsed.markleafId)
        assertEquals("review", parsed.unknownKeys["obsidian_tag"])
        assertEquals("blue", parsed.unknownKeys["custom_color"])
    }

    @Test
    fun decode_strippedQuotesFromValues() {
        val raw = """
            ---
            markleaf_id: "quoted-id"
            updated_at: '2026-05-08T11:00:00Z'
            ---
            body
        """.trimIndent()

        val parsed = SyncFrontmatter.decode(raw)

        assertEquals("quoted-id", parsed.markleafId)
        assertNotNull(parsed.updatedAt)
    }

    @Test
    fun decode_pinnedFalseWhenAbsent() {
        val raw = """
            ---
            markleaf_id: x
            ---
            body
        """.trimIndent()

        val parsed = SyncFrontmatter.decode(raw)

        // When the key is missing entirely, parsed.pinned is null (not false) —
        // caller decides whether to default to existing DB value or false.
        assertNull("missing key returns null, not false", parsed.pinned)
    }

    @Test
    fun decode_pinnedFalseExplicit() {
        val raw = """
            ---
            markleaf_id: x
            pinned: false
            ---
            body
        """.trimIndent()

        val parsed = SyncFrontmatter.decode(raw)

        assertEquals(false, parsed.pinned)
        assertFalse(parsed.pinned ?: true)
    }
}
