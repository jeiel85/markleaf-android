package com.markleaf.notes.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Minimal faithful [DataStore]: serialized transforms over a state flow.
 *
 * Why tests inject one instead of using the production store: the
 * `preferencesDataStore` delegate caches a single instance per JVM, so
 * Robolectric's per-method data directories can never work with it, and
 * DataStore 1.0's file writes replace the target via `File.renameTo`, which
 * fails on Windows for every write after the first (#158). What is ours — which
 * keys are written, cleared and consulted, and in what order — runs identically
 * here on both platforms; durability of the file is the library's guarantee.
 *
 * The same seam is what lets a Roborazzi test put a screen into a chosen
 * settings state: [com.markleaf.notes.feature.editor.EditorScreen] takes the
 * repository as a parameter for the "Show formatting button" golden (#331).
 */
class InMemoryPreferencesDataStore : DataStore<Preferences> {
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
