package com.markleaf.notes.data.sync

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import androidx.test.core.app.ApplicationProvider
import com.markleaf.notes.domain.model.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
 * [NoteFolderMirror.noteIdForFileNameIn] — the lookup a preview or file-viewer
 * local Markdown link (#414) resolves through, over a real [DocumentFile]
 * tree (`DocumentFile.fromFile`, the same technique [MirrorTraversalTest]
 * uses, since a SAF tree `Uri` needs a person to tap it). Both
 * [MirrorMetadata] shapes are covered here — this was the "verify frontmatter
 * and sidecar mode navigation" gap named when #414 was reopened.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NoteFolderMirrorLocalLinkTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var dir: File
    private lateinit var folder: DocumentFile

    @Before
    fun setUp() {
        dir = temporaryFolder.newFolder("local-link-test")
        folder = DocumentFile.fromFile(dir)
    }

    private fun note(id: String = "note-1") = Note(
        id = id,
        title = "Target",
        contentMarkdown = "# Target\n\nbody",
        excerpt = "body",
        createdAt = Instant.ofEpochMilli(0),
        updatedAt = Instant.ofEpochMilli(0)
    )

    @Test
    fun frontmatterMode_resolvesByTheIdInsideTheFile() {
        File(dir, "target.md").writeText(SyncFrontmatter.encode(note("note-1")))

        val resolved = NoteFolderMirror.noteIdForFileNameIn(
            context, folder, "target.md", MirrorMetadata.Frontmatter
        )

        assertEquals("note-1", resolved)
    }

    @Test
    fun frontmatterMode_aFileWithNoHeaderResolvesToNothing() {
        // A local link may name a real file that simply isn't a Markleaf note
        // yet — no id to find, not an error.
        File(dir, "plain.md").writeText("# Plain\n\njust text, never synced")

        val resolved = NoteFolderMirror.noteIdForFileNameIn(
            context, folder, "plain.md", MirrorMetadata.Frontmatter
        )

        assertNull(resolved)
    }

    @Test
    fun sidecarMode_resolvesThroughTheDeviceIndexByFileName() {
        File(dir, "target.md").writeText("# Target\n\nbody")
        SidecarStore.write(
            context, folder, "device-1",
            listOf(
                SidecarEntry(
                    noteId = "note-1",
                    fileName = "target.md",
                    contentHash = "irrelevant",
                    createdAtMillis = 0,
                    pinned = false,
                    archived = false
                )
            )
        )

        val resolved = NoteFolderMirror.noteIdForFileNameIn(
            context, folder, "target.md", MirrorMetadata.Sidecar("device-1")
        )

        assertEquals("note-1", resolved)
    }

    @Test
    fun sidecarMode_aFileNoEntryNamesResolvesToNothing() {
        File(dir, "orphan.md").writeText("# Orphan\n\nno entry for this one")

        val resolved = NoteFolderMirror.noteIdForFileNameIn(
            context, folder, "orphan.md", MirrorMetadata.Sidecar("device-1")
        )

        assertNull(resolved)
    }

    @Test
    fun aFileThatDoesNotExistResolvesToNothingInEitherMode() {
        assertNull(
            NoteFolderMirror.noteIdForFileNameIn(context, folder, "missing.md", MirrorMetadata.Frontmatter)
        )
        assertNull(
            NoteFolderMirror.noteIdForFileNameIn(context, folder, "missing.md", MirrorMetadata.Sidecar("device-1"))
        )
    }
}
