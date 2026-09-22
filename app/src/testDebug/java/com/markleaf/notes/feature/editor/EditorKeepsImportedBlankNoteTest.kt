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
 * #443: a note emptied by sync -- cleared on another device, truncated by a
 * sync client, or emptied by an outside editor -- must not be permanently
 * deleted just because the editor happens to be opened and closed on it
 * afterward.
 *
 * The folder reconcile imports the empty version before this test ever opens
 * the editor, so by the time [EditorScreen] loads this note it looks exactly
 * like [EditorDiscardsBlankNoteTest]'s never-touched note or
 * [EditorDiscardsClearedNoteTest]'s hand-cleared one: blank on load, blank on
 * leave. What distinguishes it is `lastImportedAt == updatedAt` -- the stamp
 * [com.markleaf.notes.data.sync.MirrorImport] leaves on an import, which
 * neither of those two other scenarios ever sets. See [EditorScreen]'s
 * discard-on-dispose effect for why that stamp is the signal rather than a
 * timestamp comparison.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class EditorKeepsImportedBlankNoteTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val repo by lazy { LocalNoteRepository(AppDatabase.getInstance(context)) }

    @Test
    fun noteEmptiedByImportSurvivesLeavingTheEditor() {
        val noteId = UUID.randomUUID().toString()
        val importedAt = Instant.now()
        runBlocking {
            // Simulates MirrorImport.Reconcile.Overwrite landing an emptied
            // file on a note that used to have real content: contentMarkdown
            // is blank, and lastImportedAt is stamped equal to updatedAt
            // because both come from the same import pass.
            repo.createNote(
                Note(
                    id = noteId,
                    title = "",
                    contentMarkdown = "",
                    excerpt = "",
                    createdAt = importedAt.minusSeconds(3600),
                    updatedAt = importedAt,
                    lastImportedAt = importedAt
                )
            )
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
        assertNotNull("a note emptied by import must survive leaving the editor", stored)
        assertEquals("", stored?.contentMarkdown)
    }
}
