package com.markleaf.notes.data.sync

import com.markleaf.notes.data.sync.NoteFolderMirror.Reconcile
import com.markleaf.notes.domain.model.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * The "(copy from another device …)" note that appears with nobody else
 * writing to the folder (#434), and the `body_sha256` digest that stops it.
 *
 * ## The sequence, as reported
 *
 * @ray4423 syncs one phone to pCloud through rclone crypt (RSAF), on a link
 * slow enough that uploads don't always finish. Occasionally a note they had
 * just edited gained a conflict copy — and the copy held an **older** version
 * of the note, with no second device anywhere in the picture.
 *
 * Three things have to line up for the pass to take a copy, and an unfinished
 * write supplies all three:
 *
 * 1. The file's body differs from the note — the newer text never landed.
 * 2. The file looks *newer* than the note. Its own `updated_at` is old, so the
 *    only thing that can say this is the filesystem's modified time, which a
 *    write that started (or a client that re-uploaded) moves regardless.
 * 3. The note counts as locally edited since the last **confirmed** write —
 *    and confirmation only happens when the write returns successfully.
 *
 * ## Why the timestamps alone can't settle it
 *
 * The mtime fallback exists for a real case: an editor that rewrites the body
 * and leaves Markleaf's block alone keeps the old `updated_at`, and without the
 * fallback that edit would never be imported — a silent loss, worse than a
 * spare copy. From the header alone, that case and this one are identical.
 *
 * What tells them apart is whether the body is the one the header was written
 * for, which is what the digest answers. These tests drive the production
 * encoder, decoder and reconcile — no reimplementation of the rule.
 */
class UnfinishedWriteConflictTest {

    private val t0 = Instant.parse("2026-09-19T10:00:00Z")   // last confirmed write
    private val t2 = Instant.parse("2026-09-19T10:05:00Z")   // the local edit that never landed
    private val mtimeNow = Instant.parse("2026-09-19T10:06:00Z")

    private fun note(content: String, updatedAt: Instant, lastImportedAt: Instant?) = Note(
        id = "n1",
        title = "TITLE",
        contentMarkdown = content,
        excerpt = content,
        createdAt = t0,
        updatedAt = updatedAt,
        lastImportedAt = lastImportedAt
    )

    /** The file as Markleaf last wrote it: header and body from the same moment. */
    private fun fileAsWritten(content: String, updatedAt: Instant) =
        SyncFrontmatter.decode(
            SyncFrontmatter.encode(note(content, updatedAt, lastImportedAt = updatedAt))
        )

    @Test
    fun ourOwnUnwrittenVersionIsNotAReadOfAnotherDevice() {
        // The folder still holds what we last wrote; the note has moved on.
        val file = fileAsWritten("the old text", t0)
        val onDevice = note("the text I just typed", updatedAt = t2, lastImportedAt = t0)

        assertTrue("the file must verify against its own header", SyncFrontmatter.bodyIsSelfVerified(file))

        val fileTs = MirrorReconcile.effectiveFileTimestamp(
            frontmatterUpdatedAt = file.updatedAt,
            fileModifiedAt = mtimeNow,
            bodyChanged = file.body != onDevice.contentMarkdown,
            bodyIsSelfVerified = SyncFrontmatter.bodyIsSelfVerified(file)
        )

        assertEquals("the file's own timestamp stands, not the mtime", t0, fileTs)
        assertEquals(Reconcile.Skip, MirrorReconcile.reconcileAction(onDevice, fileTs))
    }

    @Test
    fun withoutTheDigestTheSameFileIsStillReadAsARemoteEdit() {
        // The same situation on a file written before `body_sha256` existed.
        // Kept as a test rather than a comment: it is the behaviour every
        // existing file in every existing folder still gets, and the fix is
        // only meaningful against it.
        val file = fileAsWritten("the old text", t0).copy(bodySha256 = null)
        val onDevice = note("the text I just typed", updatedAt = t2, lastImportedAt = t0)

        assertFalse(SyncFrontmatter.bodyIsSelfVerified(file))

        val fileTs = MirrorReconcile.effectiveFileTimestamp(
            frontmatterUpdatedAt = file.updatedAt,
            fileModifiedAt = mtimeNow,
            bodyChanged = file.body != onDevice.contentMarkdown,
            bodyIsSelfVerified = SyncFrontmatter.bodyIsSelfVerified(file)
        )

        assertEquals(mtimeNow, fileTs)
        assertEquals(Reconcile.Conflict, MirrorReconcile.reconcileAction(onDevice, fileTs))
    }

    @Test
    fun anEditByAnotherAppIsStillSeenEvenWhenItLeavesTheHeaderAlone() {
        // The case the mtime fallback exists for, and the one the fix must not
        // break: the body moved under a header that was not rewritten, so the
        // digest no longer matches and the mtime is believed again.
        val written = SyncFrontmatter.encode(note("the old text", t0, lastImportedAt = t0))
        val editedElsewhere = SyncFrontmatter.decode(written.replace("the old text", "edited in vim"))
        val onDevice = note("the old text", updatedAt = t0, lastImportedAt = t0)

        assertFalse(
            "a body edited under our header must not verify",
            SyncFrontmatter.bodyIsSelfVerified(editedElsewhere)
        )

        val fileTs = MirrorReconcile.effectiveFileTimestamp(
            frontmatterUpdatedAt = editedElsewhere.updatedAt,
            fileModifiedAt = mtimeNow,
            bodyChanged = editedElsewhere.body != onDevice.contentMarkdown,
            bodyIsSelfVerified = SyncFrontmatter.bodyIsSelfVerified(editedElsewhere)
        )

        assertEquals("the mtime is what makes a foreign edit visible", mtimeNow, fileTs)
        assertEquals(Reconcile.Overwrite, MirrorReconcile.reconcileAction(onDevice, fileTs))
    }

    @Test
    fun aGenuineConflictBetweenTwoDevicesIsStillTakenAsOne() {
        // A second device wrote a real new version: its header verifies *and*
        // is newer than ours, and we have an unsynced local edit. Verification
        // must not swallow this one — it is what conflict copies are for.
        val fromOtherDevice = fileAsWritten("their version", t2)
        val onDevice = note("my version", updatedAt = t0, lastImportedAt = Instant.EPOCH)

        val fileTs = MirrorReconcile.effectiveFileTimestamp(
            frontmatterUpdatedAt = fromOtherDevice.updatedAt,
            fileModifiedAt = mtimeNow,
            bodyChanged = fromOtherDevice.body != onDevice.contentMarkdown,
            bodyIsSelfVerified = SyncFrontmatter.bodyIsSelfVerified(fromOtherDevice)
        )

        assertEquals(t2, fileTs)
        assertEquals(Reconcile.Conflict, MirrorReconcile.reconcileAction(onDevice, fileTs))
    }

    @Test
    fun aVerifiedFileThatIsGenuinelyNewerIsStillImported() {
        // Same as above but with no local edit outstanding: the newer version
        // is taken, not copied.
        val fromOtherDevice = fileAsWritten("their version", t2)
        val onDevice = note("the old text", updatedAt = t0, lastImportedAt = t0)

        val fileTs = MirrorReconcile.effectiveFileTimestamp(
            frontmatterUpdatedAt = fromOtherDevice.updatedAt,
            fileModifiedAt = mtimeNow,
            bodyChanged = fromOtherDevice.body != onDevice.contentMarkdown,
            bodyIsSelfVerified = SyncFrontmatter.bodyIsSelfVerified(fromOtherDevice)
        )

        assertEquals(t2, fileTs)
        assertEquals(Reconcile.Overwrite, MirrorReconcile.reconcileAction(onDevice, fileTs))
    }

    @Test
    fun aTruncatedWriteDoesNotVerifyAndIsStillTreatedAsForeign() {
        // Honest about the limit: this fixes the case where the *previous*
        // content is intact, not one where a partial write left a fragment
        // behind. A fragment doesn't match the digest, so it goes down the old
        // path and can still produce a copy — which at least keeps the
        // fragment visible rather than silently dropping it.
        val written = SyncFrontmatter.encode(note("the old text", t0, lastImportedAt = t0))
        val truncated = SyncFrontmatter.decode(written.replace("the old text", "the old te"))

        assertFalse(SyncFrontmatter.bodyIsSelfVerified(truncated))
    }

    @Test
    fun theDigestSurvivesARoundTripThroughTheFile() {
        val file = fileAsWritten("# Heading\n\nBody with **bold** and a `tick`.\n", t0)

        assertEquals("# Heading\n\nBody with **bold** and a `tick`.\n", file.body)
        assertEquals(SidecarIndex.hashOf(file.body), file.bodySha256)
    }

    @Test
    fun theDigestIsNotEchoedBackAsSomebodyElsesFrontmatter() {
        // `body_sha256` is ours, so a re-stamp must emit exactly one of them
        // rather than carrying the file's old copy through as an unknown entry.
        val file = fileAsWritten("the old text", t0)
        val restamped = SyncFrontmatter.encode(
            note("the old text", t0, lastImportedAt = t0),
            extraEntries = file.unknownEntries
        )

        assertTrue(file.unknownEntries.none { it.startsWith("body_sha256") })
        assertEquals(1, restamped.split("body_sha256:").size - 1)
    }
}
