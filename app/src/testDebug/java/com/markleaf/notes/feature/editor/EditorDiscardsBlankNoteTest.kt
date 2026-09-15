package com.markleaf.notes.feature.editor

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.repository.LocalNoteRepository
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.data.settings.InMemoryPreferencesDataStore
import com.markleaf.notes.domain.model.Note
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * #405: a note that was never written into does not survive leaving the
 * editor.
 *
 * Drives the real leave-the-editor path -- removing EditorScreen from
 * composition, exactly what the back icon, system back, and switching to a
 * different note all do -- rather than calling the cleanup directly, because
 * the risk this guards against is the cleanup not running at all (a coroutine
 * scope cancelled by the same teardown that was supposed to launch it).
 *
 * One scenario per file rather than three methods in one class: `AppDatabase`
 * is a JVM-wide singleton ([AppDatabase.getInstance]) that EditorScreen is
 * not able to take an override for the way it can for
 * [AppSettingsRepository], and Robolectric's per-class sandbox -- not
 * per-method -- is what keeps that singleton from going stale between
 * scenarios.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class EditorDiscardsBlankNoteTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val repo by lazy { LocalNoteRepository(AppDatabase.getInstance(context)) }

    @Test
    fun blankNoteIsRemovedWhenTheEditorIsLeft() {
        val noteId = UUID.randomUUID().toString()
        runBlocking {
            repo.createNote(
                Note(
                    id = noteId,
                    title = "",
                    contentMarkdown = "",
                    excerpt = "",
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
            )
        }
        var showEditor by mutableStateOf(true)
        // The production DataStore delegate is a JVM-wide singleton too (see
        // the comment on EditorScreen's settingsRepository parameter); an
        // in-memory store keeps this test from touching it at all.
        val settings = AppSettingsRepository(InMemoryPreferencesDataStore())

        composeRule.setContent {
            val hostScope = rememberCoroutineScope()
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                if (showEditor) {
                    EditorScreen(
                        noteId = noteId,
                        onBack = {},
                        settingsRepository = settings,
                        hostScope = hostScope
                    )
                }
            }
        }
        composeRule.waitForIdle()

        showEditor = false
        composeRule.waitForIdle()

        assertNull(
            "A note nobody ever wrote into should not survive leaving the editor",
            runBlocking { repo.getNote(noteId) }
        )
    }
}
