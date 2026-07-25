package com.markleaf.notes.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.markleaf.notes.core.text.NoteTitleSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Round-trips the notes-list preferences added for #279 and #280 through the
 * repository's own transform logic. See [AppSettingsRepositoryLockTest] for why
 * the store is injected rather than the production singleton.
 */
class AppSettingsRepositoryListTest {

    private class InMemoryPreferencesDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())
        private val mutex = Mutex()
        override val data: Flow<Preferences> = state

        override suspend fun updateData(
            transform: suspend (t: Preferences) -> Preferences
        ): Preferences = mutex.withLock {
            val updated = transform(state.value)
            state.value = updated
            updated
        }
    }

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
        repo.setNoteTitleSource(NoteTitleSource.FIRST_LINE)
        assertEquals(NoteTitleSource.FIRST_LINE, repo.settings.first().noteTitleSource)

        repo.setNoteTitleSource(NoteTitleSource.FIRST_HEADING)
        assertEquals(NoteTitleSource.FIRST_HEADING, repo.settings.first().noteTitleSource)
    }

    /** Each preference has its own key — writing one must not disturb the other. */
    @Test
    fun `the two settings are stored independently`() = runTest {
        repo.setNotesLayout(NotesLayout.GRID)
        repo.setNoteTitleSource(NoteTitleSource.FIRST_LINE)

        val settings = repo.settings.first()
        assertEquals(NotesLayout.GRID, settings.notesLayout)
        assertEquals(NoteTitleSource.FIRST_LINE, settings.noteTitleSource)
    }
}
