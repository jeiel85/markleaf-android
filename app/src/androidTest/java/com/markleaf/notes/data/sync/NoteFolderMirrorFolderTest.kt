package com.markleaf.notes.data.sync

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.data.settings.SyncFileExtension
import com.markleaf.notes.domain.model.Note
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Instant

/**
 * Exercises the mirror's real IO path — `listFiles`, the frontmatter head read,
 * adoption, in-place rename, `ContentResolver` writes — over a live
 * [DocumentFile] tree. Everything else about the mirror is pure logic covered by
 * JVM tests; this covers the part that was only ever checked by hand (#222).
 *
 * The tree is [DocumentFile.fromFile] over a temp directory rather than a
 * user-granted SAF folder, because a grant needs a person to tap it. That is
 * faithful for reads, writes, listing and rename — all measured — with **one**
 * divergence: `RawDocumentFile.createFile` appends an extension derived from the
 * MIME type unconditionally (`Note.md` becomes `Note.md.md`), where a real
 * provider strips a matching one first. So nothing here asserts on a name that
 * `createFile` produced; the cases that would are written as "the file we must
 * not touch is untouched", which is the property that actually matters. Name
 * generation itself is covered by `MirrorFileNamesTest`.
 */
@RunWith(AndroidJUnit4::class)
class NoteFolderMirrorFolderTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var dir: File
    private lateinit var folder: DocumentFile

    @Before
    fun setUp() {
        dir = File(context.cacheDir, "mirror-folder-test").apply {
            deleteRecursively()
            mkdirs()
        }
        folder = DocumentFile.fromFile(dir)
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    // --- helpers ------------------------------------------------------------

    private fun note(
        id: String = "note-1",
        title: String = "My Note",
        body: String = "# My Note\n\nlocal body",
        updatedAtMs: Long = 2_000,
        lastImportMs: Long? = 2_000,
        remoteSeenMs: Long? = null
    ) = Note(
        id = id,
        title = title,
        contentMarkdown = body,
        excerpt = body.take(40),
        createdAt = Instant.ofEpochMilli(1_000),
        updatedAt = Instant.ofEpochMilli(updatedAtMs),
        lastImportedAt = lastImportMs?.let { Instant.ofEpochMilli(it) },
        remoteSeenAt = remoteSeenMs?.let { Instant.ofEpochMilli(it) }
    )

    private fun seed(name: String, contents: String): File =
        File(dir, name).apply { writeText(contents) }

    private fun write(note: Note, extension: SyncFileExtension = SyncFileExtension.MD) =
        NoteFolderMirror.writeNoteInto(context, folder, note, extension)

    private fun files() = dir.listFiles()?.map { it.name }?.sorted().orEmpty()

    // --- the note's own file is rewritten, never duplicated ------------------

    @Test
    fun writesIntoTheFileCarryingTheNotesId() {
        seed("My Note.md", "---\nmarkleaf_id: note-1\nupdated_at: 1970-01-01T00:00:01Z\n---\n\nstale")

        assertTrue(write(note()))

        assertEquals(listOf("My Note.md"), files())
        val text = File(dir, "My Note.md").readText()
        assertTrue("body was rewritten: $text", text.contains("local body"))
        assertFalse("stale body is gone: $text", text.contains("stale"))
    }

    @Test
    fun renamesInPlaceWhenTheTitleChanges() {
        seed("Old Title.md", "---\nmarkleaf_id: note-1\n---\n\nold")

        assertTrue(write(note(title = "New Title", body = "# New Title\n\nfresh")))

        // Renamed, not deleted-and-recreated: a mid-flight sync client must
        // never see the note vanish and reappear.
        assertEquals(listOf("New Title.md"), files())
        assertTrue(File(dir, "New Title.md").readText().contains("fresh"))
    }

    // --- #213: a lost id must not fork a new file every save ----------------

    @Test
    fun adoptsAnUnclaimedFileThatAlreadyCarriesTheNotesName() {
        // What another app leaves behind when it rewrites a file without
        // keeping our block. Before the fix this forked "My Note (2).md", then
        // "(3)", on every auto-save.
        seed("My Note.md", "# My Note\n\nwritten by another app")

        assertTrue(write(note()))

        assertEquals(listOf("My Note.md"), files())
        val text = File(dir, "My Note.md").readText()
        assertTrue("id was stamped back in: $text", text.contains("markleaf_id: note-1"))
    }

    @Test
    fun neverTakesAFileClaimedByAnotherNote() {
        val other = "---\nmarkleaf_id: someone-else\n---\n\ntheir body"
        seed("My Note.md", other)

        assertTrue(write(note()))

        // The safety property is that the other note's file is untouched — not
        // what the new file ends up called (see the class comment).
        assertEquals(other, File(dir, "My Note.md").readText())
        assertEquals(2, files().size)
    }

    @Test
    fun preservesFrontmatterKeysWrittenByOtherTools() {
        seed(
            "My Note.md",
            "---\nmarkleaf_id: note-1\nobsidian_tag: review\ncustom_color: blue\n---\n\nold"
        )

        assertTrue(write(note()))

        val text = File(dir, "My Note.md").readText()
        assertTrue("obsidian_tag survived: $text", text.contains("obsidian_tag: review"))
        assertTrue("custom_color survived: $text", text.contains("custom_color: blue"))
    }

    // --- #222: a header we cannot read to the end is left alone -------------

    @Test
    fun leavesAFileAloneWhenItsHeaderRunsPastTheReadCap() {
        // An open block bigger than the cap reads as "we cannot say who owns
        // this". Claiming it would rewrite metadata never read; a duplicate file
        // is the safe answer.
        val huge = buildString {
            append("---\n")
            while (length < NoteFolderMirror.FRONTMATTER_MAX_BYTES + 4096) {
                append("padding_key: padding value that goes on and on\n")
            }
        }
        seed("My Note.md", huge)

        assertTrue(write(note()))

        assertEquals(huge, File(dir, "My Note.md").readText())
        assertEquals(2, files().size)
    }

    // --- #222: the remembered-document fast path ----------------------------
    //
    // Saving twice in a row is the ordinary case, and the second save takes the
    // cached route. These cover what the cache can get wrong; it is verified by
    // re-reading the file's id, so every case below has to end up correct
    // whichever route it took.

    @Test
    fun repeatedSavesKeepLandingInTheSameFile() {
        seed("My Note.md", "---\nmarkleaf_id: note-1\n---\n\nfirst")

        assertTrue(write(note(body = "second")))
        assertTrue(write(note(body = "third")))
        assertTrue(write(note(body = "fourth")))

        assertEquals(listOf("My Note.md"), files())
        assertTrue(File(dir, "My Note.md").readText().contains("fourth"))
    }

    @Test
    fun saveAfterTheFileIsRenamedBehindOurBack() {
        seed("My Note.md", "---\nmarkleaf_id: note-1\n---\n\nfirst")
        assertTrue(write(note(body = "second")))

        // Another app renames it. The remembered name is now wrong, but the
        // file is still ours — the id read is what settles it.
        File(dir, "My Note.md").renameTo(File(dir, "Renamed By Someone.md"))

        assertTrue(write(note(body = "third")))

        // Whichever route it took, the content must be in the file that carries
        // our id — never in a fresh duplicate.
        val ours = dir.listFiles()!!.filter { it.readText().contains("markleaf_id: note-1") }
        assertEquals(1, ours.size)
        assertTrue(ours[0].readText().contains("third"))
    }

    @Test
    fun saveAfterTheFileIsDeletedBehindOurBack() {
        seed("My Note.md", "---\nmarkleaf_id: note-1\n---\n\nfirst")
        assertTrue(write(note(body = "second")))

        File(dir, "My Note.md").delete()

        // The remembered document is gone; the write must not fail or throw.
        assertTrue(write(note(body = "third")))
        val ours = dir.listFiles()!!.filter { it.readText().contains("markleaf_id: note-1") }
        assertEquals(1, ours.size)
        assertTrue(ours[0].readText().contains("third"))
    }

    @Test
    fun saveAfterAnotherNoteTakesOverTheRememberedName() {
        // The nastiest shape: our file is replaced by a *different* note's file
        // under the same name. A cache that trusted its entry would overwrite
        // someone else's note.
        seed("My Note.md", "---\nmarkleaf_id: note-1\n---\n\nfirst")
        assertTrue(write(note(body = "second")))

        File(dir, "My Note.md").writeText("---\nmarkleaf_id: someone-else\n---\n\ntheirs")

        assertTrue(write(note(body = "third")))

        assertTrue(
            "the other note's file is untouched",
            File(dir, "My Note.md").readText().contains("theirs")
        )
    }

    // --- import ------------------------------------------------------------

    @Test
    fun importStampsOurIdIntoAFileThatHadNone() = runBlocking {
        seed("Dropped In.md", "# Dropped In\n\nhand-dropped")
        val created = mutableListOf<Note>()

        val result = NoteFolderMirror.importChangesFrom(
            context, folder, existing = emptyList(),
            applyUpdate = { }, applyCreate = { created += it }
        )

        assertEquals(1, result.created)
        assertEquals(1, created.size)
        // Without the write-back the next pass would import it again as another
        // new note — the #140 "same note appears 4-5 times" duplication.
        val text = File(dir, "Dropped In.md").readText()
        assertTrue("id stamped back: $text", text.contains("markleaf_id: ${created[0].id}"))
    }

    @Test
    fun conflictKeepsTheLocalNoteAndSettlesOnTheNextPass() = runBlocking {
        // Remote moved to 9s; local was edited at 5s after a 2s import.
        seed(
            "My Note.md",
            "---\nmarkleaf_id: note-1\nupdated_at: 1970-01-01T00:00:09Z\n---\n\nremote body"
        )
        var local = note(updatedAtMs = 5_000, lastImportMs = 2_000)
        val created = mutableListOf<Note>()

        val first = NoteFolderMirror.importChangesFrom(
            context, folder, existing = listOf(local),
            applyUpdate = { local = it }, applyCreate = { created += it }
        )

        assertEquals(1, first.conflicts)
        assertEquals(1, created.size)
        assertTrue("copy is flagged", created[0].isConflictCopy)
        assertEquals("remote body", created[0].contentMarkdown)
        // The user's own note keeps its content and its edit time (#222).
        assertEquals("# My Note\n\nlocal body", local.contentMarkdown)
        assertEquals(Instant.ofEpochMilli(5_000), local.updatedAt)
        assertNotNull("the resolved remote version is recorded", local.remoteSeenAt)

        // #217: the same pass used to fire again every minute, without end.
        val second = NoteFolderMirror.importChangesFrom(
            context, folder, existing = listOf(local),
            applyUpdate = { local = it }, applyCreate = { created += it }
        )

        assertEquals(0, second.conflicts)
        assertEquals(1, created.size)
    }
}
