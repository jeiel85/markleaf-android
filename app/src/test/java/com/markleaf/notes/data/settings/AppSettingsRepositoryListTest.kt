package com.markleaf.notes.data.settings

import com.markleaf.notes.core.text.NoteTitleSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Round-trips the notes-list preferences added for #279 and #280 through the
 * repository's own transform logic. See [AppSettingsRepositoryLockTest] for why
 * the store is injected rather than the production singleton.
 */
class AppSettingsRepositoryListTest {

    private val store = InMemoryPreferencesDataStore()
    private val repo = AppSettingsRepository(store)

    @Test
    fun `an untouched store reports the pre-existing behaviour`() = runTest {
        val settings = repo.settings.first()

        assertEquals(NotesLayout.LIST, settings.notesLayout)
        assertEquals(NoteTitleSource.FIRST_HEADING, settings.noteTitleSource)
    }

    @Test
    fun `notes layout round-trips`() = runTest {
        repo.setNotesLayout(NotesLayout.GRID)
        assertEquals(NotesLayout.GRID, repo.settings.first().notesLayout)

        repo.setNotesLayout(NotesLayout.LIST)
        assertEquals(NotesLayout.LIST, repo.settings.first().notesLayout)
    }

    @Test
    fun `note title source round-trips`() = runTest {
        repo.beginRetitle(NoteTitleSource.FIRST_LINE)
        assertEquals(NoteTitleSource.FIRST_LINE, repo.settings.first().noteTitleSource)

        repo.beginRetitle(NoteTitleSource.FIRST_HEADING)
        assertEquals(NoteTitleSource.FIRST_HEADING, repo.settings.first().noteTitleSource)
    }

    /**
     * The sidecar mode and its conversion marker land in one write, for the same
     * reason (#262). The mode is selected before the folder is converted, so a
     * death between two separate writes would leave the mode switched with
     * nothing recording that the folder is still unconverted — and the settings
     * screen's own same-mode guard would then refuse to finish it.
     */
    @Test
    fun `beginSidecarMigration selects the mode and marks the conversion owed together`() = runTest {
        val before = repo.settings.first()
        assertEquals(SyncMetadataMode.FRONTMATTER, before.syncMetadataMode)
        assertEquals(false, before.sidecarMigrationPending)

        repo.beginSidecarMigration()

        val settings = repo.settings.first()
        assertEquals(SyncMetadataMode.SIDECAR, settings.syncMetadataMode)
        assertEquals(true, settings.sidecarMigrationPending)

        repo.setSidecarMigrationPending(false)
        assertEquals(false, repo.settings.first().sidecarMigrationPending)
        assertEquals(
            "clearing the marker must not undo the mode",
            SyncMetadataMode.SIDECAR,
            repo.settings.first().syncMetadataMode
        )
    }

    /**
     * The rule and the marker that says a pass is owed land in one write (#262).
     * Written separately, a process death between them selects the new rule with
     * the flag still false — no resume, and a list showing titles from two
     * rules, which is the window the flag exists to close.
     */
    @Test
    fun `beginRetitle selects the rule and marks the pass pending together`() = runTest {
        assertEquals(false, repo.settings.first().retitlePending)

        repo.beginRetitle(NoteTitleSource.FIRST_LINE)

        val settings = repo.settings.first()
        assertEquals(NoteTitleSource.FIRST_LINE, settings.noteTitleSource)
        assertEquals(true, settings.retitlePending)
    }

    @Test
    fun `retitle pending flag round-trips and defaults to false`() = runTest {
        assertEquals(false, repo.settings.first().retitlePending)

        repo.setRetitlePending(true)
        assertEquals(true, repo.settings.first().retitlePending)

        repo.setRetitlePending(false)
        assertEquals(false, repo.settings.first().retitlePending)
    }

    /** Each preference has its own key — writing one must not disturb the other. */
    @Test
    fun `the two settings are stored independently`() = runTest {
        repo.setNotesLayout(NotesLayout.GRID)
        repo.beginRetitle(NoteTitleSource.FIRST_LINE)

        val settings = repo.settings.first()
        assertEquals(NotesLayout.GRID, settings.notesLayout)
        assertEquals(NoteTitleSource.FIRST_LINE, settings.noteTitleSource)
    }
}
