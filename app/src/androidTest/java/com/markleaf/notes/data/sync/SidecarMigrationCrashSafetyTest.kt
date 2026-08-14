package com.markleaf.notes.data.sync

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.domain.model.Note
import java.io.File
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The one state a converting folder must never be in: a mirror file with no
 * header *and* no index entry (#262, #140).
 *
 * Such a file carries no id in either place, so the next import mints a fresh
 * one — `entry?.noteId ?: parsed.markleafId ?: UUID.randomUUID()` — and the note
 * comes back as a second copy of itself. `SidecarMigration.toSidecar` therefore
 * records and flushes every entry before it strips anything, so a pass that dies
 * halfway leaves files that are headed, indexed, or both, and never neither.
 *
 * These run over a live [DocumentFile] tree for the same reason
 * [NoteFolderMirrorFolderTest] does: the property is about real reads, writes
 * and listings, and the ordering is only meaningful against a folder that keeps
 * what was written to it.
 */
@RunWith(AndroidJUnit4::class)
class SidecarMigrationCrashSafetyTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var dir: File
    private lateinit var folder: DocumentFile

    @Before
    fun setUp() {
        dir = File(context.cacheDir, "sidecar-migration-test").apply {
            deleteRecursively()
            mkdirs()
        }
        folder = DocumentFile.fromFile(dir)
        // The store caches this device's index per folder in a process-wide map.
        // Without this a previous test's entries answer this one's lookups.
        SidecarStore.forget()
    }

    @After
    fun tearDown() {
        SidecarStore.forget()
        dir.deleteRecursively()
    }

    private fun note(id: String, title: String, body: String) = Note(
        id = id,
        title = title,
        contentMarkdown = body,
        excerpt = body.take(40),
        createdAt = Instant.ofEpochMilli(1_000),
        updatedAt = Instant.ofEpochMilli(2_000),
        lastImportedAt = Instant.ofEpochMilli(2_000)
    )

    /** Seeds [count] mirror files that carry a Markleaf header, plus their notes. */
    private fun seedHeadedNotes(count: Int): List<Note> = (1..count).map { i ->
        val body = "# Note $i\n\nbody $i"
        val n = note("note-$i", "Note $i", body)
        File(dir, "Note $i.md").writeText(SyncFrontmatter.encode(n))
        n
    }

    private suspend fun importSidecar(existing: List<Note>): Pair<List<Note>, List<Note>> {
        val created = mutableListOf<Note>()
        val updated = mutableListOf<Note>()
        NoteFolderMirror.importChangesFrom(
            context = context,
            folder = folder,
            existing = existing,
            applyUpdate = { updated += it },
            applyCreate = { created += it },
            metadata = MirrorMetadata.Sidecar(DEVICE)
        )
        return created to updated
    }

    @Test
    fun conversionInterruptedMidPassDuplicatesNothing() = runBlocking {
        val notes = seedHeadedNotes(4)

        // Stand in for the process dying: the hook runs after each file is
        // rewritten, so this dies with some files stripped and some still headed.
        var stripped = 0
        runCatching {
            SidecarMigration.toSidecarIn(context, folder, DEVICE) {
                stripped++
                if (stripped == 2) throw RuntimeException("process death mid-pass")
            }
        }
        assertEquals("the pass must have been interrupted, not completed", 2, stripped)

        // Drop the in-memory index. Without this the test proves nothing: the
        // store caches this device's entries per folder, the migration mutates
        // that cache, and the import would read the entries out of memory even
        // when the flush that was supposed to make them durable never happened.
        // A real process death takes the cache with it — this is what stands in
        // for that, and reinstating the defect showed the test passing without
        // it.
        SidecarStore.forget()

        // The folder is now mixed — which is the point. Both kinds must import
        // as the notes they already are.
        val headed = dir.listFiles().orEmpty().filter {
            it.name.endsWith(".md") && it.readText().startsWith("---")
        }
        assertTrue("some files must still be headed for this to be the mixed case", headed.isNotEmpty())

        val (created, _) = importSidecar(notes)
        assertEquals("an interrupted conversion must not create a single note", 0, created.size)
    }

    @Test
    fun everyStrippedFileHasItsEntryOnDiskBeforeTheStrip() = runBlocking {
        seedHeadedNotes(4)

        runCatching {
            SidecarMigration.toSidecarIn(context, folder, DEVICE) {
                throw RuntimeException("process death after the first file")
            }
        }

        // Read the index back from disk rather than from the cache, because it
        // is the on-disk copy that survives the death being simulated.
        SidecarStore.forget()
        val onDisk = SidecarStore.readAll(context, folder)
            .firstOrNull { it.deviceId == DEVICE }
            ?.entries
            ?.associateBy { it.fileName }
            .orEmpty()

        val strippedNames = dir.listFiles().orEmpty()
            .filter { it.name.endsWith(".md") && !it.readText().startsWith("---") }
            .map { it.name }
        assertTrue("at least one file must have been stripped", strippedNames.isNotEmpty())
        strippedNames.forEach { name ->
            assertTrue("stripped file $name has no entry on disk — this is the #140 state", name in onDisk)
        }
    }

    /**
     * The rule the ordering exists to respect, pinned directly: a stripped file
     * that no index knows about *is* a new note to the importer. If this ever
     * stops being true the two-phase order can be reconsidered — until then it
     * is what makes it necessary.
     */
    @Test
    fun aStrippedFileWithNoEntryIsImportedAsANewNote() = runBlocking {
        val n = note("note-1", "Orphan", "# Orphan\n\nbody")
        File(dir, "Orphan.md").writeText(n.contentMarkdown)

        val (created, _) = importSidecar(listOf(n))

        assertEquals("a stripped, unindexed file is indistinguishable from a new note", 1, created.size)
        assertTrue("and it arrives under a fresh id, which is the duplicate", created.single().id != n.id)
    }

    private companion object {
        const val DEVICE = "test-device"
    }
}
