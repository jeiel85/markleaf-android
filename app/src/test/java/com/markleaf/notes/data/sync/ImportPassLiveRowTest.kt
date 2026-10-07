package com.markleaf.notes.data.sync

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import androidx.test.core.app.ApplicationProvider
import com.markleaf.notes.domain.model.Note
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.Instant

/**
 * The second way #434's "(copy from another device …)" note appeared, after
 * the `body_sha256` digest had closed the first.
 *
 * The reporter came back on v2.51.0+ with the copy rarer but not gone, and this
 * time it held the **whole** older note — not a fragment — in a file whose
 * digest verifies. The digest rule cannot produce that from a stale file: a
 * verified file is judged by its own `updated_at`, which is older than the
 * note. What it can be fooled by is a stale *note*.
 *
 * The pass takes its note set once, when the app resumes, and then reads every
 * file in the folder — slowly, over a provider that fetches each one from the
 * network. The user is meanwhile typing in the note they came back to, and each
 * save writes the file and confirms it. When the pass reaches that file, it
 * holds a version newer than the row the pass started with, so against the
 * snapshot it looks like somebody else's edit:
 *
 * - if that starting row was itself still unconfirmed (a write in flight, or
 *   one that failed earlier), the pass calls it a conflict and copies in the
 *   version this phone wrote a moment ago — older than what the user now has;
 * - either way, the write that follows is `snapshot.copy(...)`, which puts the
 *   start-of-pass text back over the user's newest save.
 *
 * These drive the real pass over a real folder; the only thing varied is
 * whether it reads the row live or from the snapshot.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ImportPassLiveRowTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val t1 = Instant.parse("2026-10-07T01:00:00Z")
    private val t2 = Instant.parse("2026-10-07T01:00:30Z")
    private val t3 = Instant.parse("2026-10-07T01:01:00Z")

    private fun note(
        content: String,
        updatedAt: Instant,
        lastImportedAt: Instant?,
        id: String = "n1"
    ) = Note(
        id = id,
        title = "TITLE",
        contentMarkdown = content,
        excerpt = content,
        createdAt = t1.minusSeconds(3600),
        updatedAt = updatedAt,
        lastImportedAt = lastImportedAt
    )

    private class Recorder {
        val updates = mutableListOf<Note>()
        val creates = mutableListOf<Note>()
    }

    private fun folderHolding(vararg written: Note): DocumentFile {
        val dir = tmp.newFolder("mirror")
        written.forEach { File(dir, "${it.title}.md").writeText(SyncFrontmatter.encode(it)) }
        return DocumentFile.fromFile(dir)
    }

    private fun runPass(
        folder: DocumentFile,
        snapshot: List<Note>,
        live: Map<String, Note>?,
        recorder: Recorder
    ): NoteFolderMirror.ImportResult = runBlocking {
        NoteFolderMirror.importChangesFrom(
            context = context,
            folder = folder,
            existing = snapshot,
            applyUpdate = { recorder.updates += it },
            applyCreate = { recorder.creates += it },
            currentNote = live?.let { rows ->
                val read: suspend (String) -> Note? = { id -> rows[id] }
                read
            }
        )
    }

    @Test
    fun `a version this phone wrote during the pass is not copied in as another device's`() {
        // Start of pass: the row holds C1, whose write has not been confirmed.
        val snapshot = note("C1", updatedAt = t1, lastImportedAt = t1.minusSeconds(60))
        // While the pass reads other files: C2 saved, written and confirmed;
        // then C3 saved, its write still on the way.
        val written = note("C2", updatedAt = t2, lastImportedAt = t2)
        val now = note("C3", updatedAt = t3, lastImportedAt = t2)
        val folder = folderHolding(written)

        // The snapshot alone reproduces the report — this is the case, not a
        // neighbouring one.
        val stale = Recorder()
        val staleResult = runPass(folder, listOf(snapshot), live = null, recorder = stale)
        assertEquals(1, staleResult.conflicts)
        assertEquals("C2", stale.creates.single().contentMarkdown)
        assertTrue(stale.creates.single().isConflictCopy)
        assertEquals("C1", stale.updates.single().contentMarkdown)

        val fresh = Recorder()
        val result = runPass(folder, listOf(snapshot), live = mapOf("n1" to now), recorder = fresh)
        assertEquals(0, result.conflicts)
        assertEquals(1, result.skipped)
        assertTrue(fresh.creates.isEmpty())
        assertTrue("nothing may be written over C3", fresh.updates.isEmpty())
    }

    @Test
    fun `a save made during the pass is not overwritten by the one before it`() {
        // Start of pass: everything confirmed, so the file reads as a clean
        // remote update rather than a conflict — and the overwrite is silent.
        val snapshot = note("C1", updatedAt = t1, lastImportedAt = t1)
        val written = note("C2", updatedAt = t2, lastImportedAt = t2)
        val now = note("C3", updatedAt = t3, lastImportedAt = t2)
        val folder = folderHolding(written)

        val stale = Recorder()
        runPass(folder, listOf(snapshot), live = null, recorder = stale)
        assertEquals("C2", stale.updates.single().contentMarkdown)

        val fresh = Recorder()
        val result = runPass(folder, listOf(snapshot), live = mapOf("n1" to now), recorder = fresh)
        assertEquals(0, result.updated)
        assertTrue("C3 must survive the pass", fresh.updates.isEmpty())
    }

    @Test
    fun `a note deleted for good during the pass is not brought back`() {
        val snapshot = note("C1", updatedAt = t1, lastImportedAt = t1)
        val folder = folderHolding(snapshot)

        val recorder = Recorder()
        val result = runPass(folder, listOf(snapshot), live = emptyMap(), recorder = recorder)

        assertEquals(1, result.skipped)
        assertTrue(recorder.creates.isEmpty())
        assertTrue(recorder.updates.isEmpty())
    }

    @Test
    fun `a note created during the pass is matched, not inserted over`() {
        // The pass never saw this note; the editor created and wrote it while
        // the pass was reading other files. Create would insert with REPLACE.
        val created = note("Fresh", updatedAt = t3, lastImportedAt = t3, id = "n2")
        val folder = folderHolding(created)

        val recorder = Recorder()
        val result = runPass(folder, emptyList(), live = mapOf("n2" to created), recorder = recorder)

        assertEquals(0, result.created)
        assertEquals(1, result.skipped)
        assertTrue(recorder.creates.isEmpty())
    }

    @Test
    fun `a note nobody touched still takes a newer file from the folder`() {
        // The live read must not swallow the ordinary update it exists beside.
        val row = note("C1", updatedAt = t1, lastImportedAt = t1)
        val fromElsewhere = note("C2", updatedAt = t3, lastImportedAt = t3)
        val folder = folderHolding(fromElsewhere)

        val recorder = Recorder()
        val result = runPass(folder, listOf(row), live = mapOf("n1" to row), recorder = recorder)

        assertEquals(1, result.updated)
        assertEquals("C2", recorder.updates.single().contentMarkdown)
    }
}
