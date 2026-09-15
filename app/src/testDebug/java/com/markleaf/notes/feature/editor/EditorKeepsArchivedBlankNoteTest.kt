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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Regression for a self-review finding on PR #406: the #405 discard-on-exit
 * check originally only knew about ONE way a note reaches Trash -- the
 * delete-confirm dialog in this same file, tracked by a local
 * `wasSentToTrash` flag. On the tablet's two-pane layout the note list sits
 * open beside the editor and can archive, lock, or trash the very note that
 * is showing (blank) in the editor pane through a completely different path
 * (`NotesListScreen` -> `NotesViewModel`) that never touched that flag --
 * silently converting a deliberate "keep this, just tucked away" action into
 * a permanent delete. The fix re-reads the note's own persisted
 * trashed/archived/locked/pinned flags instead of trusting a flag this
 * screen set itself, which covers every surface by construction. This test
 * exercises archived specifically; trashed/locked/pinned share the same
 * guard.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class EditorKeepsArchivedBlankNoteTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val repo by lazy { LocalNoteRepository(AppDatabase.getInstance(context)) }

    @Test
    fun archivingABlankNoteFromElsewhereSurvivesLeavingTheEditor() {
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
            // Simulates the tablet list pane archiving this same note while
            // it happens to be open (blank) in the editor pane -- a
            // completely different call path than this file's own delete
            // dialog, reaching NoteRepository directly the way
            // NotesViewModel.setArchived does.
            repo.setArchived(noteId, true)
        }
        var showEditor by mutableStateOf(true)
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

        val stored = runBlocking { repo.getNote(noteId) }
        assertNotNull("an archived note must survive leaving the editor even while blank", stored)
        assertEquals(true, stored?.archived)
    }
}
