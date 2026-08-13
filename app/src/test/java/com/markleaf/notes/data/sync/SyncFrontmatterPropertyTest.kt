package com.markleaf.notes.data.sync

import com.markleaf.notes.domain.model.Note
import java.time.Instant
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Property-based companion to [SyncFrontmatterTest]. The fixed examples there
 * pin known shapes; these sweep the two invariants the parser is supposed to
 * hold for arbitrary input (#262):
 *
 * 1. **Our own output round-trips.** `encode(note, extras)` always decodes back
 *    to the same id, timestamps, flags, body and extras — a user's file written
 *    by Markleaf must never silently change on the next read.
 * 2. **Foreign entries and body are opaque.** For any document Markleaf reads,
 *    re-stamping it with `decode → encode → decode` leaves `unknownEntries` and
 *    `body` unchanged. We don't understand them, so we must not alter them.
 *
 * The generator is a crude hand-rolled source rather than a property library —
 * the project ships no test frameworks beyond JUnit, and the shapes to cover
 * are few: `key: value` lines, indented block sequences, nested maps, quoted
 * values and comments. All are seeded so a failure reproduces byte-for-byte.
 *
 * The generator never emits a line whose `trimEnd()` equals `---` (that would
 * close the block early) and never uses `\r\n` (decode normalizes line
 * endings); both are contract boundaries, not bugs, and are pinned in the
 * fixed-example tests instead.
 */
class SyncFrontmatterPropertyTest {

    private val random = Random(20260813L)

    // --- generators ---------------------------------------------------------

    private val FOREIGN_KEYS = listOf(
        "tags", "aliases", "cssclasses", "obsidian_tag", "title", "author",
        "url", "source", "custom_color", "category", "note_type"
    )

    private fun randomNote(body: String = randomBody()): Note = Note(
        id = "id-${random.nextInt(1_000_000)}",
        title = "T",
        contentMarkdown = body,
        excerpt = "T",
        createdAt = Instant.ofEpochMilli(random.nextLong(1_000_000_000L)),
        updatedAt = Instant.ofEpochMilli(random.nextLong(1_000_000_000L)),
        pinned = random.nextBoolean(),
        archived = random.nextBoolean()
    )

    private fun randomBody(): String {
        val lines = mutableListOf<String>()
        repeat(random.nextInt(0, 6)) {
            when (random.nextInt(3)) {
                0 -> lines += "# " + randomWords(1, 4)
                1 -> lines += "- " + randomWords(2, 6)
                else -> lines += randomWords(3, 9)
            }
        }
        return lines.joinToString("\n")
    }

    private fun randomWords(min: Int, max: Int): String {
        val words = mutableListOf<String>()
        repeat(random.nextInt(min, max + 1)) {
            val len = random.nextInt(1, 9)
            words += buildString { repeat(len) { append((97 + random.nextInt(26)).toChar()) } }
        }
        return words.joinToString(" ")
    }

    /** A single top-level frontmatter entry in one of the shapes we meet in the wild. */
    private fun randomForeignEntry(): String {
        val key = FOREIGN_KEYS[random.nextInt(FOREIGN_KEYS.size)]
        return when (random.nextInt(5)) {
            0 -> "$key: ${randomWords(1, 5)}"
            1 -> "$key:"
                 .let { head ->
                     val items = mutableListOf<String>()
                     repeat(random.nextInt(1, 4)) { items += "  - ${randomWords(1, 4)}" }
                     (listOf(head) + items).joinToString("\n")
                 }
            2 -> "$key:"
                 .let { head ->
                     val items = mutableListOf<String>()
                     repeat(random.nextInt(1, 3)) { items += "  ${FOREIGN_KEYS[random.nextInt(FOREIGN_KEYS.size)]}: ${randomWords(1, 3)}" }
                     (listOf(head) + items).joinToString("\n")
                 }
            3 -> "$key: \"${randomWords(1, 4)}\""
            else -> "# ${randomWords(2, 6)}"
        }
    }

    private fun randomExtras(count: Int = random.nextInt(1, 5)): List<String> =
        (0 until count).map { randomForeignEntry() }

    // --- property 1: our own output always round-trips ----------------------

    @Test
    fun encodeThenDecode_preservesEveryFieldForRandomNotes() {
        repeat(200) {
            val note = randomNote()
            val extras = randomExtras()

            val parsed = SyncFrontmatter.decode(SyncFrontmatter.encode(note, extras))

            assertTrue(parsed.hasFrontmatter)
            assertTrue(parsed.blockClosed)
            assertEquals("note id", note.id, parsed.markleafId)
            assertEquals("created_at", note.createdAt, parsed.createdAt)
            assertEquals("updated_at", note.updatedAt, parsed.updatedAt)
            assertEquals("pinned", note.pinned, parsed.pinned)
            assertEquals("archived", note.archived, parsed.archived)
            assertEquals("body", note.contentMarkdown, parsed.body)
            assertEquals("unknown entries", extras, parsed.unknownEntries)
        }
    }

    @Test
    fun encodeThenDecode_roundTripsARandomEmptyBodyToo() {
        repeat(50) {
            val note = randomNote(body = "")
            val parsed = SyncFrontmatter.decode(SyncFrontmatter.encode(note, randomExtras()))

            assertEquals("", parsed.body)
            assertEquals(note.id, parsed.markleafId)
        }
    }

    // --- property 2: foreign entries and body are opaque --------------------

    @Test
    fun decodeEncodeDecode_leavesUnknownEntriesAndBodyUnchanged() {
        repeat(200) {
            val original = randomFrontmatterDocument()

            val first = SyncFrontmatter.decode(original)
            // Safety net: the generator always emits markleaf_id first, so
            // hasFrontmatter is expected to hold; the guard is for the shape's
            // own sake, not a path this generator reaches.
            if (!first.hasFrontmatter) return@repeat

            val restamped = SyncFrontmatter.encode(randomNote(body = first.body), first.unknownEntries)
            val second = SyncFrontmatter.decode(restamped)

            assertEquals("unknown entries survive re-stamp", first.unknownEntries, second.unknownEntries)
            assertEquals("body survives re-stamp", first.body, second.body)
        }
    }

    private fun randomFrontmatterDocument(): String {
        val sb = StringBuilder()
        sb.append("---\n")
        sb.append("markleaf_id: doc-").append(random.nextInt(100_000)).append('\n')
        // Intentionally not ISO-8601: decode must tolerate a foreign value here
        // without crashing (parseInstantOrNull returns null), and the asserted
        // properties only cover unknownEntries and body.
        if (random.nextBoolean()) {
            sb.append("created_at: ").append(random.nextLong(1_000_000_000L)).append('\n')
        }
        randomExtras(random.nextInt(1, 6)).forEach { entry ->
            sb.append(entry).append('\n')
        }
        sb.append("---\n\n")
        sb.append(randomBody())
        return sb.toString()
    }
}
