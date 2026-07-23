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
    fun encode_preservesExtraKeysAndStampsMarkleafId() {
        // When we re-stamp an imported file (#140 write-back), any frontmatter
        // keys the file already carried — e.g. an Obsidian tag — must survive
        // the round-trip rather than being silently dropped.
        val note = sampleNote()
        val extras = mapOf("obsidian_tag" to "review", "custom_color" to "blue")

        val parsed = SyncFrontmatter.decode(SyncFrontmatter.encode(note, extras))

        assertEquals(note.id, parsed.markleafId)
        assertEquals("review", parsed.unknownKeys["obsidian_tag"])
        assertEquals("blue", parsed.unknownKeys["custom_color"])
        assertEquals(note.contentMarkdown, parsed.body)
    }

    @Test
    fun encode_ignoresReservedKeysPassedAsExtras() {
        // A defensive caller could pass a reserved key in the extras map; encode
        // must emit our canonical markleaf_id once and never echo the stray one.
        val note = sampleNote()

        val encoded = SyncFrontmatter.encode(note, mapOf("markleaf_id" to "STRAY"))

        assertEquals(1, encoded.split("markleaf_id:").size - 1)
        assertTrue(encoded.contains("markleaf_id: ${note.id}"))
        assertFalse(encoded.contains("STRAY"))
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
    fun decode_ignoresLeadingByteOrderMark() {
        // #213: a UTF-8 BOM in front of the opening `---` used to defeat the
        // delimiter check (Kotlin's trim() leaves U+FEFF alone), so the file
        // parsed as "no frontmatter", the note lost its markleaf_id, and the
        // mirror forked a new file on every save.
        val bom = Char(0xFEFF).toString()
        val raw = bom + """
            ---
            markleaf_id: bom-id
            updated_at: 2026-05-08T11:00:00Z
            ---
            body
        """.trimIndent()

        val parsed = SyncFrontmatter.decode(raw)

        assertEquals("bom-id", parsed.markleafId)
        assertNotNull(parsed.updatedAt)
        assertEquals("body", parsed.body)
    }

    @Test
    fun decode_stripsByteOrderMarkFromPlainBodyToo() {
        // A BOM'd file with no frontmatter still round-trips cleanly: the mark
        // must not survive into the note body, or it shows up as an invisible
        // first character in the editor.
        val bom = Char(0xFEFF).toString()

        val parsed = SyncFrontmatter.decode(bom + "# Just a body")

        assertNull(parsed.markleafId)
        assertEquals("# Just a body", parsed.body)
    }

    // --- #222: a horizontal rule is not frontmatter -------------------------

    @Test
    fun decode_keepsBodyTextBetweenTwoHorizontalRules() {
        // The reported loss: `---` opens a Markdown rule as readily as it opens
        // frontmatter. Everything between the two rules used to be swallowed as
        // "frontmatter we could not parse" and dropped on import.
        val raw = "---\nSome text\n---\nMore body"

        val parsed = SyncFrontmatter.decode(raw)

        assertNull(parsed.markleafId)
        assertFalse(parsed.hasFrontmatter)
        assertEquals(raw, parsed.body)
    }

    @Test
    fun decode_keepsAnEmptyRulePair() {
        val raw = "---\n---\nBody"

        val parsed = SyncFrontmatter.decode(raw)

        assertFalse(parsed.hasFrontmatter)
        assertEquals(raw, parsed.body)
    }

    @Test
    fun decode_stillAcceptsYamlWhoseValueSpansLines() {
        // The guard must not be stricter than YAML. An Obsidian block with a
        // list value has lines that look nothing like `key: value`; rejecting it
        // would trade one silent loss for a worse one.
        val raw = """
            ---
            tags:
              - review
              - draft
            markleaf_id: x
            ---
            body
        """.trimIndent()

        val parsed = SyncFrontmatter.decode(raw)

        assertTrue(parsed.hasFrontmatter)
        assertEquals("x", parsed.markleafId)
        assertEquals("body", parsed.body)
    }

    @Test
    fun decode_blockClosedIsTrueEvenWhenTheBlockIsNotMetadata() {
        // Lets the mirror's head reader stop instead of chasing a closed rule
        // pair to its read cap on every save.
        val rules = SyncFrontmatter.decode("---\nSome text\n---\nMore")
        assertTrue(rules.blockClosed)
        assertFalse(rules.hasFrontmatter)

        val unterminated = SyncFrontmatter.decode("---\nmarkleaf_id: x\nstill open")
        assertFalse(unterminated.blockClosed)
    }

    @Test
    fun decode_ourOwnOutputIsAlwaysRecognised() {
        // encode() always emits markleaf_id first, so the guard can never reject
        // a file Markleaf itself wrote.
        val parsed = SyncFrontmatter.decode(SyncFrontmatter.encode(sampleNote()))

        assertTrue(parsed.hasFrontmatter)
        assertEquals(sampleNote().id, parsed.markleafId)
    }

    // --- #222: telling "no metadata" from "not read far enough" -------------

    @Test
    fun hasFrontmatter_trueOnlyForAClosedBlock() {
        assertTrue(SyncFrontmatter.decode(SyncFrontmatter.encode(sampleNote())).hasFrontmatter)
        assertFalse(SyncFrontmatter.decode("# Plain body").hasFrontmatter)
        assertFalse(SyncFrontmatter.decode("---\nmarkleaf_id: x\nstill open").hasFrontmatter)
    }

    @Test
    fun opensFrontmatter_separatesAnOpenBlockFromNoBlock() {
        // A truncated read of a file whose block runs long yields
        // hasFrontmatter == false, exactly like a file with no block. Only
        // opensFrontmatter tells the two apart — and mistaking the first for
        // the second is what let the mirror overwrite metadata it never read.
        val truncated = "---\nmarkleaf_id: x\nalias: something-very-"

        assertFalse(SyncFrontmatter.decode(truncated).hasFrontmatter)
        assertTrue(SyncFrontmatter.opensFrontmatter(truncated))
        assertFalse(SyncFrontmatter.opensFrontmatter("# Plain body"))
    }

    @Test
    fun opensFrontmatter_looksPastAByteOrderMark() {
        val bom = Char(0xFEFF).toString()

        assertTrue(SyncFrontmatter.opensFrontmatter(bom + "---\nmarkleaf_id: x\n"))
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
