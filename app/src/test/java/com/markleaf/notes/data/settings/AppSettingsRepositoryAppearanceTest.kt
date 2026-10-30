package com.markleaf.notes.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Round-trips the appearance preferences added for #345 (theme mode), #346
 * (editor font size) and #561 (line spacing) through the repository's own
 * transform logic. See
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
        assertEquals(EditorFont.SANS, settings.editorFont)
        assertEquals(EditorLineSpacing.NORMAL, settings.editorLineSpacing)
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

    @Test
    fun `line spacing round-trips every option`() = runTest {
        for (spacing in EditorLineSpacing.entries) {
            repo.setEditorLineSpacing(spacing)
            assertEquals(spacing, repo.settings.first().editorLineSpacing)
        }
    }

    /** A value this build doesn't know — written by a newer one — reads as the default. */
    @Test
    fun `an unknown line spacing reads as normal`() = runTest {
        store.updateData { preferences ->
            preferences.toMutablePreferences().apply {
                this[stringPreferencesKey("editor_line_spacing")] = "EXTRA_COMPACT"
            }
        }
        assertEquals(EditorLineSpacing.NORMAL, repo.settings.first().editorLineSpacing)
    }

    /** Monospace is a stored choice like serif — #432. */
    @Test
    fun `editor font round-trips every option`() = runTest {
        for (font in EditorFont.entries) {
            repo.setEditorFont(font)
            assertEquals(font, repo.settings.first().editorFont)
        }
    }

    /** #510: a picked font is recorded and selected in one write, and survives switching away. */
    @Test
    fun `a custom font is selected with its file and kept when switching away`() = runTest {
        repo.setCustomFont(fileName = "custom-1", displayName = "Atkinson.ttf")
        val picked = repo.settings.first()
        assertEquals(EditorFont.CUSTOM, picked.editorFont)
        assertEquals("custom-1", picked.customFontFile)
        assertEquals("Atkinson.ttf", picked.customFontName)

        repo.setEditorFont(EditorFont.SERIF)
        val switched = repo.settings.first()
        assertEquals(EditorFont.SERIF, switched.editorFont)
        assertEquals("custom-1", switched.customFontFile)

        repo.setEditorFont(EditorFont.CUSTOM)
        assertEquals("custom-1", repo.settings.first().customFontFile)
    }

    /**
     * #513 review: a built-in font chosen while an import was still running
     * must survive the import finishing — the file is recorded, not selected.
     */
    @Test
    fun `a font recorded without selecting it leaves the current choice alone`() = runTest {
        repo.setEditorFont(EditorFont.SERIF)

        repo.setCustomFont(fileName = "custom-2", displayName = "Late.ttf", select = false)

        val settings = repo.settings.first()
        assertEquals(EditorFont.SERIF, settings.editorFont)
        assertEquals("custom-2", settings.customFontFile)
        assertEquals("Late.ttf", settings.customFontName)
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
