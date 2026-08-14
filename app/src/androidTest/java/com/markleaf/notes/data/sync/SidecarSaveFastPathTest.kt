package com.markleaf.notes.data.sync

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.data.settings.SyncFileExtension
import com.markleaf.notes.domain.model.Note
import java.io.File
import java.time.Instant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The remembered-file path a sidecar save takes on the second and every later
 * save of the same note (#262).
 *
 * Sidecar files carry no header, so the frontmatter path's identity check —
 * peek the file and compare `markleaf_id` — has nothing to read. The index
 * entry's filename is the identity instead, and the fast path is only allowed
 * to write when the document still answers to that name. These cover what goes
 * wrong if that check is ever loosened: a file renamed outside Markleaf must
 * not be written into, and a retitled note must be renamed rather than forked.
 */
@RunWith(AndroidJUnit4::class)
class SidecarSaveFastPathTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var dir: File
    private lateinit var folder: DocumentFile

    @Before
    fun setUp() {
        dir = File(context.cacheDir, "sidecar-fast-path-test").apply {
            deleteRecursively()
            mkdirs()
        }
        folder = DocumentFile.fromFile(dir)
        SidecarStore.forget()
    }

    @After
    fun tearDown() {
        SidecarStore.forget()
        dir.deleteRecursively()
    }

    private fun note(id: String = "note-1", title: String = "Plan", body: String) = Note(
        id = id,
        title = title,
        contentMarkdown = body,
        excerpt = body.take(40),
        createdAt = Instant.ofEpochMilli(1_000),
        updatedAt = Instant.ofEpochMilli(2_000)
    )

    private fun save(n: Note) = NoteFolderMirror.writeNoteInto(
        context,
        folder,
        n,
        SyncFileExtension.MD,
        MirrorMetadata.Sidecar(DEVICE)
    )

    private fun markdownFiles() = dir.listFiles().orEmpty().filter { it.name.endsWith(".md") }

    private fun ownEntries() = SidecarStore.readAll(context, folder)
        .firstOrNull { it.deviceId == DEVICE }
        ?.entries
        .orEmpty()

    @Test
    fun savingTheSameNoteAgainRewritesTheOneFile() {
        assertTrue(save(note(body = "# Plan\n\nfirst")))
        assertTrue(save(note(body = "# Plan\n\nsecond")))

        val files = markdownFiles()
        assertEquals("a repeat save must not fork a second file", 1, files.size)
        assertEquals("# Plan\n\nsecond", files.single().readText())

        val entries = ownEntries()
        assertEquals("one note, one entry", 1, entries.size)
        assertEquals(files.single().name, entries.single().fileName)
        assertEquals(
            "the entry must record the body that was actually written",
            SidecarIndex.hashOf("# Plan\n\nsecond"),
            entries.single().contentHash
        )
    }

    @Test
    fun aFileRenamedOutsideMarkleafIsNotWrittenBlind() {
        assertTrue(save(note(body = "# Plan\n\nfirst")))
        val original = markdownFiles().single()
        val renamed = File(dir, "Renamed by something else.md")
        assertTrue("the rename must succeed for this to test anything", original.renameTo(renamed))
        val bodyAfterRename = renamed.readText()

        assertTrue(save(note(body = "# Plan\n\nsecond")))

        assertEquals(
            "the file we no longer recognise must be left exactly as it was",
            bodyAfterRename,
            renamed.readText()
        )
        val ours = markdownFiles().filter { it.name != renamed.name }
        assertEquals("the note gets a file of its own instead", 1, ours.size)
        assertEquals("# Plan\n\nsecond", ours.single().readText())
    }

    @Test
    fun aRetitledNoteIsRenamedRatherThanForked() {
        assertTrue(save(note(body = "# Plan\n\nfirst")))
        assertTrue(save(note(title = "Renamed plan", body = "# Renamed plan\n\nfirst")))

        val files = markdownFiles()
        assertEquals("a title change must not leave two files behind", 1, files.size)
        assertTrue(
            "the file should carry the new title: ${files.single().name}",
            files.single().name.startsWith("Renamed plan")
        )
        assertEquals(
            "and the index must name the file that exists",
            files.single().name,
            ownEntries().single().fileName
        )
    }

    private companion object {
        const val DEVICE = "test-device"
    }
}
