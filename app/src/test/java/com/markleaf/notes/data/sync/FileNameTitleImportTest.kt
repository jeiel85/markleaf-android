package com.markleaf.notes.data.sync

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import androidx.test.core.app.ApplicationProvider
import com.markleaf.notes.core.text.NoteTitleSource
import com.markleaf.notes.data.settings.SyncFileExtension
import com.markleaf.notes.domain.model.Note
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.Instant

/**
 * The folder side of the File name title rule (D088): a file's name is the
 * title it brings in, and a file renamed on the computer renames its note.
 *
 * Real [DocumentFile] trees over a temp directory, as in
 * [SidecarExternalRenameTest], so the writes, the matching and the import are
 * the production ones.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FileNameTitleImportTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var dir: File
    private lateinit var folder: DocumentFile

    @Before
    fun setUp() {
        dir = temporaryFolder.newFolder("file-name-title-test")
        folder = DocumentFile.fromFile(dir)
        SidecarStore.forget()
    }

    @After
    fun tearDown() {
        SidecarStore.forget()
    }

    /** A note whose last write reached the folder, as after any confirmed save. */
    private fun note(title: String = "groceries", body: String = "# Shopping on Saturday\n\nmilk") = Note(
        id = "note-1",
        title = title,
        contentMarkdown = body,
        excerpt = "",
        createdAt = Instant.ofEpochMilli(1_000),
        updatedAt = Instant.ofEpochMilli(2_000),
        lastImportedAt = Instant.ofEpochMilli(2_000),
        remoteSeenAt = Instant.ofEpochMilli(2_000)
    )

    private class Imported(
        val result: NoteFolderMirror.ImportResult,
        val updated: List<Note>,
        val created: List<Note>
    )

    private fun import(
        existing: List<Note>,
        metadata: MirrorMetadata = MirrorMetadata.Frontmatter,
        source: NoteTitleSource = NoteTitleSource.FILE_NAME
    ): Imported = runBlocking {
        val updated = mutableListOf<Note>()
        val created = mutableListOf<Note>()
        val result = NoteFolderMirror.importChangesFrom(
            context = context,
            folder = folder,
            existing = existing,
            applyUpdate = { updated.add(it) },
            applyCreate = { created.add(it) },
            metadata = metadata,
            titleSource = source
        )
        Imported(result, updated, created)
    }

    private fun save(n: Note, metadata: MirrorMetadata = MirrorMetadata.Frontmatter) =
        assertTrue(NoteFolderMirror.writeNoteInto(context, folder, n, SyncFileExtension.MD, metadata, keepFileName = true))

    private fun rename(from: String, to: String) =
        assertTrue("the rename must succeed for this to test anything", File(dir, from).renameTo(File(dir, to)))

    @Test
    fun aNewNoteWritesItsNameAsTheFileName() {
        save(note())
        assertEquals(listOf("groceries.md"), dir.list()!!.filter { it.endsWith(".md") })
    }

    @Test
    fun aDroppedFileIsTitledByItsNameNotItsFirstLine() {
        File(dir, "Frankenstein.md").writeText("# Chapter 1\n\nYou will rejoice to hear")
        val imported = import(emptyList())
        assertEquals("Frankenstein", imported.created.single().title)
    }

    @Test
    fun aFileRenamedOnTheComputerRenamesItsNoteAndNothingElse() {
        val n = note()
        save(n)
        rename("groceries.md", "shopping.md")

        val imported = import(listOf(n))
        val renamed = imported.updated.single()
        assertEquals("shopping", renamed.title)
        assertEquals("the body is not touched", n.contentMarkdown, renamed.contentMarkdown)
        assertEquals("a rename is not an edit: the note keeps its place", n.updatedAt, renamed.updatedAt)
        assertTrue(imported.created.isEmpty())
    }

    @Test
    fun aSidecarFileRenamedOnTheComputerRenamesItsNote() {
        val n = note()
        val sidecar = MirrorMetadata.Sidecar("device-a")
        save(n, sidecar)
        rename("groceries.md", "shopping.md")

        val imported = import(listOf(n), metadata = sidecar)
        assertEquals("shopping", imported.updated.single().title)
        assertTrue(imported.created.isEmpty())
    }

    @Test
    fun aRenameMadeInTheAppIsNotUndoneByAFileThatMissedIt() {
        // Renamed in the app to "groceries", but the write never reached the
        // folder, so the file still has the old name and the note is ahead.
        File(dir, "shopping.md").writeText(SyncFrontmatter.encode(note(title = "shopping")))
        val ahead = note().copy(updatedAt = Instant.ofEpochMilli(3_000))
        val imported = import(listOf(ahead))
        assertTrue("the old name on disk must not win", imported.updated.isEmpty())
    }

    @Test
    fun caseAloneIsNotARename() {
        val n = note()
        save(n)
        rename("groceries.md", "Groceries.md")
        assertTrue(import(listOf(n)).updated.isEmpty())
    }

    @Test
    fun underTheDerivedRulesAFileNameIsStillIgnored() {
        val n = note(title = "Shopping on Saturday")
        save(n)
        rename("Shopping on Saturday.md", "shopping.md")
        val imported = import(listOf(n), source = NoteTitleSource.FIRST_HEADING)
        assertTrue(imported.updated.isEmpty())
        assertEquals(1, imported.result.skipped)
    }
}
