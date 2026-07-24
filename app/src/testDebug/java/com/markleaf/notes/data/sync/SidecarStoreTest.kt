package com.markleaf.notes.data.sync

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * How [SidecarStore] talks to storage (#262).
 *
 * Every note save goes through this class, and on SAF the folder listing is the
 * expensive call. The original version read the whole folder twice per save —
 * once for the merged view and once for our own entries — then wrote the index.
 * These tests pin what is cached and, just as importantly, what is not.
 *
 * Runs over a real temp directory through `DocumentFile.fromFile`, so the file
 * counting below measures actual reads rather than a mock's expectations.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class SidecarStoreTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var folder: DocumentFile

    @Before
    fun setUp() {
        folder = DocumentFile.fromFile(temp.root)
        SidecarStore.forget()
    }

    @After
    fun tearDown() = SidecarStore.forget()

    @Test
    fun `own entries survive a write and a reload from disk`() {
        val entries = SidecarStore.ownEntries(context, folder, DEVICE)
        entries["n1"] = entry("n1", "First.md")
        assertTrue(SidecarStore.write(context, folder, DEVICE, entries.values))

        SidecarStore.forget()
        val reloaded = SidecarStore.ownEntries(context, folder, DEVICE)

        assertEquals(setOf("n1"), reloaded.keys)
        assertEquals("First.md", reloaded.getValue("n1").fileName)
    }

    /**
     * The cached map is the live one a save mutates, so a second call has to
     * hand back the same entries rather than a fresh copy off disk — otherwise
     * a save's record would be dropped before it was written.
     */
    @Test
    fun `own entries are the same live map across calls`() {
        SidecarStore.ownEntries(context, folder, DEVICE)["n1"] = entry("n1", "First.md")

        assertEquals(setOf("n1"), SidecarStore.ownEntries(context, folder, DEVICE).keys)
    }

    /** An unchanged index must not be rewritten — a synced folder should not see it touched. */
    @Test
    fun `writing identical entries twice touches the file once`() {
        val entries = SidecarStore.ownEntries(context, folder, DEVICE)
        entries["n1"] = entry("n1", "First.md")
        SidecarStore.write(context, folder, DEVICE, entries.values)

        val indexFile = File(temp.root, SidecarIndex.fileNameFor(DEVICE))
        val firstWrite = indexFile.lastModified()
        indexFile.setLastModified(firstWrite - 10_000)
        val movedBack = indexFile.lastModified()

        assertTrue(SidecarStore.write(context, folder, DEVICE, entries.values))

        assertEquals(
            "an unchanged index was rewritten",
            movedBack,
            indexFile.lastModified()
        )
    }

    /**
     * Another device's file changes outside this process, so it is read fresh
     * every time. Caching it would attach a note to the wrong file.
     */
    @Test
    fun `another device's index is re-read rather than cached`() {
        writeForeignIndex(entry("theirs", "Theirs.md"))
        assertEquals("Theirs.md", SidecarStore.load(context, folder, DEVICE)["theirs"]?.fileName)

        writeForeignIndex(entry("theirs", "Renamed.md"))

        assertEquals("Renamed.md", SidecarStore.load(context, folder, DEVICE)["theirs"]?.fileName)
    }

    /**
     * The merged view has to agree with what the next write will emit. Our own
     * cached entries can be ahead of the file, and taking the file's version
     * over them would hand a caller a note→file link that is about to change.
     */
    @Test
    fun `merged view prefers our cached entry over the one on disk`() {
        val entries = SidecarStore.ownEntries(context, folder, DEVICE)
        entries["n1"] = entry("n1", "Old.md")
        SidecarStore.write(context, folder, DEVICE, entries.values)
        // A rename recorded in memory, not yet flushed.
        entries["n1"] = entry("n1", "New.md")

        assertEquals("New.md", SidecarStore.load(context, folder, DEVICE)["n1"]?.fileName)
    }

    /**
     * Switching the mode off deletes our index. A cached copy written back on
     * the next save would resurrect a mapping the user asked to remove.
     */
    @Test
    fun `forget drops entries so a deleted index does not come back`() {
        SidecarStore.ownEntries(context, folder, DEVICE)["n1"] = entry("n1", "First.md")
        SidecarStore.forget()

        assertTrue(SidecarStore.ownEntries(context, folder, DEVICE).isEmpty())
    }

    @Test
    fun `an unreadable index is skipped rather than guessed at`() {
        File(temp.root, SidecarIndex.fileNameFor("broken")).writeText("{ not json")

        assertTrue(SidecarStore.readAll(context, folder).isEmpty())
        assertNull(SidecarStore.load(context, folder, DEVICE)["anything"])
    }

    private fun writeForeignIndex(vararg entries: SidecarEntry) {
        File(temp.root, SidecarIndex.fileNameFor(OTHER_DEVICE))
            .writeText(SidecarIndex.encode(OTHER_DEVICE, entries.toList()))
    }

    private fun entry(id: String, file: String) = SidecarEntry(
        noteId = id,
        fileName = file,
        contentHash = SidecarIndex.hashOf(file),
        createdAtMillis = 0L,
        pinned = false,
        archived = false
    )

    private companion object {
        const val DEVICE = "dev1"
        const val OTHER_DEVICE = "dev2"
    }
}
