package com.markleaf.notes.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The sidecar index format and its cross-device merge (#216).
 *
 * Robolectric rather than a plain JVM test because the encoder uses `org.json`,
 * which the unit-test `android.jar` only stubs.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class SidecarIndexTest {

    @Test
    fun `round-trips every field`() {
        val entries = listOf(
            entry("note-1", "First.md", "aaa", created = 1_000L, pinned = true),
            entry("note-2", "Second.txt", "bbb", archived = true)
        )

        val parsed = SidecarIndex.decode(SidecarIndex.encode("dev1", entries))!!

        assertEquals("dev1", parsed.deviceId)
        assertEquals(entries.sortedBy { it.noteId }, parsed.entries)
    }

    /**
     * The file lands in a folder something else is syncing. If a pass that
     * changed nothing produced different bytes, every save would look like a
     * change worth uploading — the opposite of what this mode is for.
     */
    @Test
    fun `encoding is stable regardless of input order`() {
        val a = entry("z-note", "Z.md", "1")
        val b = entry("a-note", "A.md", "2")

        assertEquals(
            SidecarIndex.encode("dev1", listOf(a, b)),
            SidecarIndex.encode("dev1", listOf(b, a))
        )
    }

    @Test
    fun `refuses input it cannot trust`() {
        assertNull(SidecarIndex.decode("not json at all"))
        assertNull(SidecarIndex.decode("{}"))
        // A schema from a future version may mean anything.
        assertNull(SidecarIndex.decode("""{"version":99,"device":"d","entries":[]}"""))
        // No device id: an index nobody owns cannot be told apart from ours.
        assertNull(SidecarIndex.decode("""{"version":1,"entries":[]}"""))
    }

    @Test
    fun `skips entries missing the fields that identify a file`() {
        val raw = """
            {"version":1,"device":"d","entries":[
              {"id":"ok","file":"A.md","hash":"h"},
              {"id":"no-file","hash":"h"},
              {"file":"B.md","hash":"h"},
              {"id":"no-hash","file":"C.md"}
            ]}
        """.trimIndent()

        val parsed = SidecarIndex.decode(raw)!!

        assertEquals(listOf("ok"), parsed.entries.map { it.noteId })
    }

    /**
     * Our own hash answers "is this file still what *we* wrote". Another
     * device's answers a different question, so it must never stand in.
     */
    @Test
    fun `our own entry wins over another device's`() {
        val ours = ParsedIndex("me", listOf(entry("n", "Ours.md", "our-hash")))
        val theirs = ParsedIndex("them", listOf(entry("n", "Theirs.md", "their-hash")))

        val merged = SidecarIndex.merge("me", listOf(theirs, ours))

        assertEquals("our-hash", merged.getValue("n").contentHash)
        assertEquals("Ours.md", merged.getValue("n").fileName)
    }

    /**
     * The case that matters on a second device: without picking the other
     * device's entry up, the note has no known id and imports as a duplicate.
     */
    @Test
    fun `another device's entry fills a gap we have`() {
        val ours = ParsedIndex("me", listOf(entry("mine", "Mine.md", "h1")))
        val theirs = ParsedIndex("them", listOf(entry("theirs", "Theirs.md", "h2")))

        val merged = SidecarIndex.merge("me", listOf(ours, theirs))

        assertEquals(setOf("mine", "theirs"), merged.keys)
        assertEquals("h2", merged.getValue("theirs").contentHash)
    }

    @Test
    fun `filename lookup is case-insensitive`() {
        val merged = SidecarIndex.merge("me", listOf(ParsedIndex("me", listOf(entry("n", "Notes.md", "h")))))

        assertEquals("n", SidecarIndex.byFileName(merged)["notes.md"]?.noteId)
    }

    @Test
    fun `index files are recognised by name and yield their device`() {
        val name = SidecarIndex.fileNameFor("abc123")

        assertEquals(".markleaf-index-abc123.json", name)
        assertTrue(SidecarIndex.isIndexFile(name))
        assertEquals("abc123", SidecarIndex.deviceIdOf(name))
        // A note file must never be taken for an index.
        assertFalse(SidecarIndex.isIndexFile("Notes.md"))
        assertFalse(SidecarIndex.isIndexFile(".markleaf-index-.json"))
        assertNull(SidecarIndex.deviceIdOf("Notes.md"))
    }

    @Test
    fun `hash distinguishes content and survives a round trip`() {
        assertEquals(SidecarIndex.hashOf("hello"), SidecarIndex.hashOf("hello"))
        assertNotEquals(SidecarIndex.hashOf("hello"), SidecarIndex.hashOf("hello "))
        // Hex SHA-256.
        assertEquals(64, SidecarIndex.hashOf("hello").length)
    }

    private fun entry(
        id: String,
        file: String,
        hash: String,
        created: Long = 0L,
        pinned: Boolean = false,
        archived: Boolean = false
    ) = SidecarEntry(id, file, hash, created, pinned, archived)
}
