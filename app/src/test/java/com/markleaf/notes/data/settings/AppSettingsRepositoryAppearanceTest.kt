package com.markleaf.notes.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Round-trips the appearance preferences added for #345 (theme mode) and #346
 * (editor font size) through the repository's own transform logic. See
 * [AppSettingsRepositoryLockTest] for why the store is injected rather than
 * the production singleton.
 */
class AppSettingsRepositoryAppearanceTest {

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

        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertEquals(EditorFontSize.MEDIUM, settings.editorFontSize)
    }

    @Test
    fun `theme mode round-trips`() = runTest {
        repo.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, repo.settings.first().themeMode)

        repo.setThemeMode(ThemeMode.SYSTEM)
        assertEquals(ThemeMode.SYSTEM, repo.settings.first().themeMode)
    }

    @Test
    fun `editor font size round-trips`() = runTest {
        repo.setEditorFontSize(EditorFontSize.EXTRA_LARGE)
        assertEquals(EditorFontSize.EXTRA_LARGE, repo.settings.first().editorFontSize)

        repo.setEditorFontSize(EditorFontSize.MEDIUM)
        assertEquals(EditorFontSize.MEDIUM, repo.settings.first().editorFontSize)
    }

    /** Each preference has its own key — writing one must not disturb the other. */
    @Test
    fun `the two settings are stored independently`() = runTest {
        repo.setThemeMode(ThemeMode.LIGHT)
        repo.setEditorFontSize(EditorFontSize.LARGE)

        val settings = repo.settings.first()
        assertEquals(ThemeMode.LIGHT, settings.themeMode)
        assertEquals(EditorFontSize.LARGE, settings.editorFontSize)
    }
}
