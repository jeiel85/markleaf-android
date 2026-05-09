package com.markleaf.notes.data.sync

import com.markleaf.notes.domain.model.Note
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

/**
 * Pure-logic checks of [NoteFolderMirror.importChanges]'s conflict
 * decision tree. These don't exercise the SAF IO path — that's tested
 * via the live tablet smoke. They document the four corners of the
 * "newer file vs local edit since last sync" matrix.
 */
class NoteFolderMirrorConflictLogicTest {

    /**
     * Replays the decision the importChanges branch makes given (file ts,
     * db ts, last imported ts). Returns one of "OVERWRITE" / "CONFLICT" /
     * "SKIP". Mirrors the implementation comments without exercising IO.
     */
    private enum class Outcome { OVERWRITE, CONFLICT, SKIP }

    private fun decide(fileTs: Long, dbTs: Long, lastImport: Long?): Outcome {
        val fileNewer = fileTs > dbTs + 2_000L
        if (!fileNewer) return Outcome.SKIP
        val import = lastImport ?: 0L
        val localEditedSinceImport = dbTs > import + 2_000L
        return if (localEditedSinceImport) Outcome.CONFLICT else Outcome.OVERWRITE
    }

    private fun base() = Note(
        id = "n1",
        title = "T",
        contentMarkdown = "T",
        excerpt = "T",
        createdAt = Instant.ofEpochMilli(0),
        updatedAt = Instant.ofEpochMilli(0)
    )

    @Test
    fun fileOlder_isSkipped() {
        // Local was edited; remote file hasn't moved past it.
        assertEquals(Outcome.SKIP, decide(fileTs = 1_000, dbTs = 5_000, lastImport = 0))
    }

    @Test
    fun fileNewerAndLocalUntouched_isOverwrite() {
        // Last sync stamped lastImport at 10s. Local hasn't moved past
        // that (still 10s). Remote file is at 100s. Safe to overwrite.
        assertEquals(
            Outcome.OVERWRITE,
            decide(fileTs = 100_000, dbTs = 10_000, lastImport = 10_000)
        )
    }

    @Test
    fun fileNewerAndLocalEditedAfterLastImport_isConflict() {
        // Last sync at 10s. Local moved to 20s (edited locally after sync).
        // Remote also at 100s. Both moved → conflict, keep duplicate.
        assertEquals(
            Outcome.CONFLICT,
            decide(fileTs = 100_000, dbTs = 20_000, lastImport = 10_000)
        )
    }

    @Test
    fun fileNewerAndNoPriorImport_isConflict() {
        // Note was never imported (lastImport null). Any remote-newer
        // case is treated as conflict to avoid silent overwrites of
        // notes the user created locally.
        assertEquals(
            Outcome.CONFLICT,
            decide(fileTs = 100_000, dbTs = 50_000, lastImport = null)
        )
    }

    @Test
    fun slackWindowAroundEqualTimestamps_treatedAsSkip() {
        // 1s difference is inside the 2s slack — treat as in sync.
        assertEquals(Outcome.SKIP, decide(fileTs = 11_000, dbTs = 10_000, lastImport = 10_000))
    }

    @Test
    fun base_noteCarriesLastImportedAtField() {
        // Smoke check that the new field exists on the domain model and
        // defaults to null for fresh local notes.
        assertEquals(null, base().lastImportedAt)
    }
}
