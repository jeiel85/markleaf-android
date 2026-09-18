package com.markleaf.notes.data.sync

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import androidx.test.core.app.ApplicationProvider
import com.markleaf.notes.domain.model.Note
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * The nested-folder spike (#424): what it costs to let the *import* direction
 * see the `.md` files in a user's subdirectories.
 *
 * Two halves, deliberately:
 *
 * - The rule functions are pure and tested as such, the same split
 *   [MirrorFileNames] uses — a skip rule that is wrong is much easier to read
 *   about in a one-line assertion than in a tree walk.
 * - The walk itself runs over a real [DocumentFile] tree built from a temp
 *   directory. `DocumentFile.fromFile` gives a `RawDocumentFile`, whose
 *   `listFiles`/`isFile`/`isDirectory` go straight to `java.io.File`, so the
 *   traversal under test is the real one and not a stub of it. What this cannot
 *   show is SAF's *cost*: a `RawDocumentFile` listing is a filesystem call,
 *   where the real thing is a ContentProvider query. [MirrorTraversal.MirrorWalk.listCalls]
 *   is here so that cost can be counted on a device; these tests pin what it
 *   counts, not what it costs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MirrorTraversalTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    /**
     * A fresh directory per test method, rather than one under `cacheDir` that
     * every method clears on the way in.
     *
     * The shared-directory version of this class failed
     * `listCalls counts one listing per directory entered` once and then passed
     * four runs in a row — the signature of one method seeing a directory
     * another one made. These assertions are absolute counts of what the walk
     * listed, so a single leftover directory silently changes the answer, and
     * `File.deleteRecursively()` reports that kind of failure only in a return
     * value nobody reads. A rule that hands out a new directory removes the
     * shared state instead of cleaning it.
     */
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var dir: File
    private lateinit var folder: DocumentFile

    @Before
    fun setUp() {
        dir = temporaryFolder.newFolder("mirror-traversal-test")
        folder = DocumentFile.fromFile(dir)
    }

    private fun seed(path: String, contents: String = "# Note\n\nbody"): File =
        File(dir, path).apply {
            parentFile?.mkdirs()
            writeText(contents)
        }

    private fun walk(maxDepth: Int) = MirrorTraversal.walk(folder, maxDepth)

    private fun paths(maxDepth: Int) = walk(maxDepth).files.map { it.relativePath }.sorted()

    // --- pure rules ---------------------------------------------------------

    @Test
    fun `childPath leaves a root-level name bare`() {
        // A leading slash would read as an absolute path to anything that later
        // tried to resolve one of these against the folder.
        assertEquals("note.md", MirrorTraversal.childPath("", "note.md"))
        assertEquals("projects/note.md", MirrorTraversal.childPath("projects", "note.md"))
        assertEquals("a/b/note.md", MirrorTraversal.childPath("a/b", "note.md"))
    }

    @Test
    fun `directoryVerdict stops at the depth cap`() {
        assertEquals(
            MirrorTraversal.DirectoryVerdict.SKIP,
            MirrorTraversal.directoryVerdict("projects", depth = 0, maxDepth = 0)
        )
        assertEquals(
            MirrorTraversal.DirectoryVerdict.DESCEND,
            MirrorTraversal.directoryVerdict("projects", depth = 0, maxDepth = 1)
        )
        assertEquals(
            MirrorTraversal.DirectoryVerdict.SKIP,
            MirrorTraversal.directoryVerdict("deeper", depth = 1, maxDepth = 1)
        )
    }

    @Test
    fun `directoryVerdict refuses hidden directories`() {
        // Sync clients and editors keep state in dot-directories. Syncthing's
        // .stversions in particular holds every version of every deleted file —
        // importing it would resurrect notes the user threw away.
        for (name in listOf(".git", ".obsidian", ".stversions", ".trash")) {
            assertEquals(
                "expected $name to be skipped",
                MirrorTraversal.DirectoryVerdict.SKIP,
                MirrorTraversal.directoryVerdict(name, depth = 0, maxDepth = 5)
            )
        }
    }

    @Test
    fun `directoryVerdict refuses the root attachments directory Markleaf owns`() {
        assertEquals(
            MirrorTraversal.DirectoryVerdict.SKIP,
            MirrorTraversal.directoryVerdict("attachments", depth = 0, maxDepth = 5)
        )
        // Case-insensitively, because a synced folder can land on exFAT or a
        // Windows share where the name comes back in another case.
        assertEquals(
            MirrorTraversal.DirectoryVerdict.SKIP,
            MirrorTraversal.directoryVerdict("Attachments", depth = 0, maxDepth = 5)
        )
    }

    @Test
    fun `directoryVerdict descends into a nested attachments directory`() {
        // `MirrorWrite.mirrorAttachments` resolves `attachments/` on the linked
        // folder itself, so Markleaf's storage is only ever at the root. A
        // `projects/attachments/` is the user's own directory, and refusing it
        // by name would drop whatever notes are in it — the silent loss this
        // whole spike is about.
        assertEquals(
            MirrorTraversal.DirectoryVerdict.DESCEND,
            MirrorTraversal.directoryVerdict("attachments", depth = 1, maxDepth = 5)
        )
    }

    @Test
    fun `a nested attachments directory keeps its notes`() {
        seed("attachments/note-1/scan.md")
        seed("projects/attachments/meeting.md")

        // The root one is Markleaf's and stays out; the nested one is the
        // user's and its notes arrive.
        assertEquals(listOf("projects/attachments/meeting.md"), paths(maxDepth = 5))
    }

    @Test
    fun `directoryVerdict reports a nameless directory as unknown, not skipped`() {
        // `getName` is `queryForString(_display_name, null)`, so a null name is
        // the provider declining to describe the entry — not Markleaf choosing
        // to pass over it. Calling it a skip would put a provider failure in a
        // figure documented as Markleaf's own decisions.
        assertEquals(
            MirrorTraversal.DirectoryVerdict.UNKNOWN,
            MirrorTraversal.directoryVerdict(null, depth = 0, maxDepth = 5)
        )
    }

    @Test
    fun `canDescendFrom is false once the cap is reached`() {
        assertTrue(MirrorTraversal.canDescendFrom(depth = 0, maxDepth = 1))
        assertFalse(MirrorTraversal.canDescendFrom(depth = 0, maxDepth = 0))
        assertFalse(MirrorTraversal.canDescendFrom(depth = 1, maxDepth = 1))
    }

    @Test
    fun `effectiveDepth forces sidecar mode flat`() {
        // Not conservatism: the sidecar index keys notes by bare filename, so
        // two `note.md` files in different directories collapse onto one entry.
        assertEquals(0, MirrorTraversal.effectiveDepth(MirrorMetadata.Sidecar("device-1"), 4))
        assertEquals(4, MirrorTraversal.effectiveDepth(MirrorMetadata.Frontmatter, 4))
        assertEquals(0, MirrorTraversal.effectiveDepth(MirrorMetadata.Frontmatter, -1))
    }

    // --- depth 0 is exactly what the mirror did before ----------------------

    @Test
    fun `depth 0 lists the root and nothing under it`() {
        seed("top.md")
        seed("projects/alpha.md")
        seed("projects/nested/beta.md")

        assertEquals(listOf("top.md"), paths(maxDepth = 0))
    }

    @Test
    fun `depth 0 costs exactly one listing`() {
        seed("top.md")
        seed("projects/alpha.md")

        // The old code was a single `folder.listFiles()`. If this ever moves,
        // every existing caller silently got more expensive.
        assertEquals(1, walk(maxDepth = 0).listCalls)
    }

    @Test
    fun `depth 0 asks nothing that the flat listing did not ask`() {
        // The regression this pins is invisible to the RawDocumentFile tests
        // above: on a SAF folder every one of `isFile`, `name` and
        // `isDirectory` is its own ContentProvider query, and an earlier
        // version of `walk` tested `isDirectory` first — one extra round trip
        // per entry, on every import and survey, for a recursion that is
        // switched off. Mocks are used here precisely because the count is the
        // subject; nothing else in this class needs them.
        // Stubbed with `doReturn(...).when(...)` and only where the walk must
        // read something, so the properties under verification are never
        // touched by the setup itself.
        val note = mock(DocumentFile::class.java)
        doReturn(true).`when`(note).isFile
        doReturn("note.md").`when`(note).name

        // Nothing stubbed: an unstubbed `isFile` already answers false, which
        // is what a directory would answer.
        val subdirectory = mock(DocumentFile::class.java)

        val root = mock(DocumentFile::class.java)
        doReturn(arrayOf(note, subdirectory)).`when`(root).listFiles()

        val result = MirrorTraversal.walk(root, maxDepth = 0)

        assertEquals(listOf("note.md"), result.files.map { it.relativePath })
        // The question that costs a query and cannot change the outcome at
        // depth 0 is never asked — of either entry.
        verify(note, never()).isDirectory
        verify(subdirectory, never()).isDirectory
        // And the directory is dismissed on its `isFile` alone, without its
        // name being fetched.
        verify(subdirectory, never()).name
    }

    @Test
    fun `a file's name is fetched once, not once per use`() {
        // `getName()` is a provider query like the rest, and the walk needs the
        // name twice — to decide the entry is a mirror file, and to build its
        // relative path. Calling `MirrorFileLookup.isMirrorEntry` and then
        // reading `name` again cost two queries per file where the flat listing
        // cost one, which is the same regression as the `isDirectory` one in a
        // second place.
        val note = mock(DocumentFile::class.java)
        doReturn(true).`when`(note).isFile
        doReturn("note.md").`when`(note).name

        val root = mock(DocumentFile::class.java)
        doReturn(arrayOf(note)).`when`(root).listFiles()

        val result = MirrorTraversal.walk(root, maxDepth = 0)

        assertEquals(listOf("note.md"), result.files.map { it.relativePath })
        verify(note, times(1)).name
        verify(note, times(1)).isFile
    }

    @Test
    fun `a directory's name is fetched once on the descend path`() {
        val subdirectory = mock(DocumentFile::class.java)
        doReturn(true).`when`(subdirectory).isDirectory
        doReturn("projects").`when`(subdirectory).name
        doReturn(emptyArray<DocumentFile>()).`when`(subdirectory).listFiles()

        val root = mock(DocumentFile::class.java)
        doReturn(arrayOf(subdirectory)).`when`(root).listFiles()

        MirrorTraversal.walk(root, maxDepth = 1)

        // Once for the skip rules, reused for the path — not once for each.
        verify(subdirectory, times(1)).name
    }

    @Test
    fun `a depth that can descend does ask whether an entry is a directory`() {
        // The other half of the trade-off: once recursion is possible the
        // question is worth its query, so this is not "never ask", it is "ask
        // only when the answer matters".
        val subdirectory = mock(DocumentFile::class.java)
        doReturn(true).`when`(subdirectory).isDirectory
        doReturn("projects").`when`(subdirectory).name
        doReturn(emptyArray<DocumentFile>()).`when`(subdirectory).listFiles()

        val root = mock(DocumentFile::class.java)
        doReturn(arrayOf(subdirectory)).`when`(root).listFiles()

        MirrorTraversal.walk(root, maxDepth = 1)

        verify(subdirectory, atLeastOnce()).isDirectory
    }

    @Test
    fun `depth 0 reports no skipped directories because it never identifies any`() {
        // Documented rather than incidental: `directoriesSkipped` counts
        // rule-based skips, and at depth 0 recognising a directory would cost
        // the query the cap exists to avoid. A future change that makes this
        // number non-zero has reintroduced that cost.
        seed("note.md")
        seed("projects/alpha.md")
        seed(".git/hidden.md")

        assertEquals(0, walk(maxDepth = 0).directoriesSkipped)
    }

    @Test
    fun `depth 0 reports nothing in either counter, including for a failed entry`() {
        // Both figures are instruments for the recursion experiment, not a
        // health check on the flat pass. At depth 0 nothing past the file test
        // runs, so an entry the provider would not describe — `isFile` and
        // `isDirectory` both answering false — is passed over as quietly as a
        // `.png` would be, and neither counter moves. Saying so here stops the
        // zeros being read as "the folder was fine".
        val opaque = mock(DocumentFile::class.java)

        val root = mock(DocumentFile::class.java)
        doReturn(arrayOf(opaque)).`when`(root).listFiles()

        val result = MirrorTraversal.walk(root, maxDepth = 0)

        assertEquals(0, result.directoriesSkipped)
        assertEquals(0, result.entriesUnclassified)
        // …and the entry cost only the one query the flat listing would have.
        verify(opaque, times(1)).isFile
        verify(opaque, never()).isDirectory
    }

    @Test
    fun `depth 0 keeps ignoring non-mirror files`() {
        seed("note.md")
        seed("note.txt")
        seed("photo.png")
        seed("README")

        assertEquals(listOf("note.md", "note.txt"), paths(maxDepth = 0))
    }

    // --- what the spike actually buys ---------------------------------------

    @Test
    fun `depth 1 finds files one directory down and records where they were`() {
        seed("top.md")
        seed("projects/alpha.md")

        assertEquals(listOf("projects/alpha.md", "top.md"), paths(maxDepth = 1))
    }

    @Test
    fun `depth 1 does not reach the second level`() {
        seed("projects/alpha.md")
        seed("projects/nested/beta.md")

        assertEquals(listOf("projects/alpha.md"), paths(maxDepth = 1))
    }

    @Test
    fun `a deeper cap reaches further down`() {
        seed("a/b/c/deep.md")

        assertEquals(emptyList<String>(), paths(maxDepth = 2))
        assertEquals(listOf("a/b/c/deep.md"), paths(maxDepth = 3))
    }

    @Test
    fun `two files with the same name in different directories stay distinct`() {
        // The property the sidecar index cannot express, which is why
        // `effectiveDepth` refuses that mode. Here it is, stated as a fact about
        // the traversal so the limitation above is anchored to something real.
        seed("work/meeting.md")
        seed("home/meeting.md")

        val found = paths(maxDepth = 1)
        assertEquals(listOf("home/meeting.md", "work/meeting.md"), found)
        assertEquals(2, found.toSet().size)
    }

    @Test
    fun `the walk skips hidden and attachment directories even when allowed to descend`() {
        seed("real.md")
        seed(".obsidian/config.md")
        seed("attachments/note-1/scan.md")

        assertEquals(listOf("real.md"), paths(maxDepth = 5))
    }

    @Test
    fun `skipped directories are counted rather than silently dropped`() {
        seed(".git/notes.md")
        seed("attachments/note-1/scan.md")

        val result = walk(maxDepth = 5)
        assertEquals(2, result.directoriesSkipped)
        // One call for the root; neither skipped directory was listed.
        assertEquals(1, result.listCalls)
    }

    @Test
    fun `listCalls counts one listing per directory entered`() {
        seed("projects/alpha.md")
        seed("archive/old.md")

        // Root + two subdirectories. This is the number that becomes a SAF
        // round trip on a device, and it grows with the user's tree, not with
        // their note count.
        assertEquals(3, walk(maxDepth = 1).listCalls)
    }

    @Test
    fun `a listFiles that throws costs one directory, not the whole walk`() {
        // This guards a `DocumentFile` subclass that throws, and nothing more.
        // It is **not** how an unreadable SAF directory behaves:
        // `TreeDocumentFile.listFiles` catches `Exception` around its provider
        // query and `RawDocumentFile.listFiles` returns empty when
        // `File.listFiles()` gives null, so a real one never reaches this
        // branch. A mock is the only way into it — which is the reason the walk
        // must not claim to *detect* unreadable directories, and why the
        // companion test below pins what actually happens.
        val broken = mock(DocumentFile::class.java)
        doReturn(true).`when`(broken).isDirectory
        doReturn("projects").`when`(broken).name
        doThrow(SecurityException("no access")).`when`(broken).listFiles()

        val note = mock(DocumentFile::class.java)
        doReturn(true).`when`(note).isFile
        doReturn("note.md").`when`(note).name

        val root = mock(DocumentFile::class.java)
        doReturn(arrayOf(broken, note)).`when`(root).listFiles()

        val result = MirrorTraversal.walk(root, maxDepth = 1)

        // The rest of the folder is still the user's, so it still arrives.
        assertEquals(listOf("note.md"), result.files.map { it.relativePath })
        // Not a rule-based skip, so not in that figure — otherwise the number
        // would mean two things depending on the implementation underneath.
        assertEquals(0, result.directoriesSkipped)
        assertEquals(2, result.listCalls)
    }

    @Test
    fun `an entry the provider will not describe is unclassified, not skipped`() {
        // `isDirectory` is `"…/directory".equals(getRawType(uri))`, so a failed
        // MIME query answers "not a directory" and the entry — with whatever
        // subtree hangs off it — vanishes. The count is the only trace left,
        // and it must not land in the figure that means "a rule refused this".
        val opaque = mock(DocumentFile::class.java)
        // isFile and isDirectory both answer false, which is what a provider
        // that failed both queries looks like from here.

        val root = mock(DocumentFile::class.java)
        doReturn(arrayOf(opaque)).`when`(root).listFiles()

        val result = MirrorTraversal.walk(root, maxDepth = 1)

        assertTrue(result.files.isEmpty())
        assertEquals(0, result.directoriesSkipped)
        assertEquals(1, result.entriesUnclassified)
    }

    @Test
    fun `a directory with no name is unclassified, not skipped`() {
        val nameless = mock(DocumentFile::class.java)
        doReturn(true).`when`(nameless).isDirectory
        // `name` left unstubbed: null, the value a failed display-name query
        // produces.

        val root = mock(DocumentFile::class.java)
        doReturn(arrayOf(nameless)).`when`(root).listFiles()

        val result = MirrorTraversal.walk(root, maxDepth = 1)

        assertEquals(0, result.directoriesSkipped)
        assertEquals(1, result.entriesUnclassified)
    }

    @Test
    fun `a rule-based skip stays out of the unclassified count`() {
        // The other direction: the two buckets must not bleed into each other.
        val hidden = mock(DocumentFile::class.java)
        doReturn(true).`when`(hidden).isDirectory
        doReturn(".obsidian").`when`(hidden).name

        val root = mock(DocumentFile::class.java)
        doReturn(arrayOf(hidden)).`when`(root).listFiles()

        val result = MirrorTraversal.walk(root, maxDepth = 1)

        assertEquals(1, result.directoriesSkipped)
        assertEquals(0, result.entriesUnclassified)
    }

    @Test
    fun `an unreadable directory is indistinguishable from an empty one`() {
        // The shape a real provider failure takes: not an exception, an empty
        // listing. The walk cannot tell this from a directory with nothing in
        // it, reports nothing, and counts nothing — which is a silent drop of
        // the same kind this spike exists to describe, and is written up as a
        // finding rather than papered over here.
        val unreadable = mock(DocumentFile::class.java)
        doReturn(true).`when`(unreadable).isDirectory
        doReturn("projects").`when`(unreadable).name
        doReturn(emptyArray<DocumentFile>()).`when`(unreadable).listFiles()

        val root = mock(DocumentFile::class.java)
        doReturn(arrayOf(unreadable)).`when`(root).listFiles()

        val result = MirrorTraversal.walk(root, maxDepth = 1)

        assertTrue(result.files.isEmpty())
        assertEquals(0, result.directoriesSkipped)
        assertEquals(2, result.listCalls)
    }

    @Test
    fun `an empty folder walks without finding anything`() {
        assertTrue(walk(maxDepth = 3).files.isEmpty())
    }

    // --- end to end: the import direction ------------------------------------

    @Test
    fun `import at depth 0 leaves a subfolder note invisible`() = runBlocking {
        seed("projects/alpha.md", "# Alpha\n\nfrom a subfolder")

        val created = mutableListOf<Note>()
        val result = NoteFolderMirror.importChangesFrom(
            context = context,
            folder = folder,
            existing = emptyList(),
            applyUpdate = { },
            applyCreate = { created.add(it) }
        )

        // Today's behaviour, stated out loud: no error, no skip, no count —
        // the file simply is not there as far as Markleaf is concerned. This is
        // what the reporter on #424 is running into.
        assertEquals(0, result.created)
        assertEquals(0, result.errors)
        assertTrue(created.isEmpty())
    }

    @Test
    fun `import at depth 1 takes in a subfolder note`() = runBlocking {
        seed("projects/alpha.md", "# Alpha\n\nfrom a subfolder")

        val created = mutableListOf<Note>()
        val result = NoteFolderMirror.importChangesFrom(
            context = context,
            folder = folder,
            existing = emptyList(),
            applyUpdate = { },
            applyCreate = { created.add(it) },
            maxDepth = 1
        )

        assertEquals(1, result.created)
        assertEquals("Alpha", created.single().title)
    }

    @Test
    fun `a sidecar import stays flat however deep it is asked to go`() = runBlocking {
        seed("projects/alpha.md", "# Alpha\n\nfrom a subfolder")

        val created = mutableListOf<Note>()
        val result = NoteFolderMirror.importChangesFrom(
            context = context,
            folder = folder,
            existing = emptyList(),
            applyUpdate = { },
            applyCreate = { created.add(it) },
            metadata = MirrorMetadata.Sidecar("device-1"),
            maxDepth = 3
        )

        // The refusal in `effectiveDepth`, observed from the outside. A sidecar
        // folder must not half-adopt a tree its index cannot describe.
        assertEquals(0, result.created)
        assertTrue(created.isEmpty())
    }

    // --- the survey must keep promising what the import delivers -------------

    @Test
    fun `the survey counts the subfolder files the import will take`() = runBlocking {
        seed("top.md")
        seed("projects/alpha.md")

        val survey = NoteFolderMirror.surveyFolderIn(
            context, folder, existing = emptyList(), maxDepth = 1
        )
        val result = NoteFolderMirror.importChangesFrom(
            context = context,
            folder = folder,
            existing = emptyList(),
            applyUpdate = { },
            applyCreate = { },
            maxDepth = 1
        )

        // #372's lesson, carried into the tree: this number is shown to the user
        // before they agree to the import, so a survey that walks differently
        // from the import promises arrivals that never come.
        assertEquals(2, survey.newFiles)
        assertEquals(survey.newFiles, result.created)
    }

    @Test
    fun `the sidecar survey is refused the same depth the sidecar import is`() {
        seed("top.md")
        seed("projects/alpha.md")

        val survey = NoteFolderMirror.surveyFolderIn(
            context,
            folder,
            existing = emptyList(),
            metadata = MirrorMetadata.Sidecar("device-1"),
            maxDepth = 3
        )

        // Both sides resolve the depth through `effectiveDepth`, so the refusal
        // cannot land on one pass and not the other.
        assertEquals(1, survey.newFiles)
    }
}
