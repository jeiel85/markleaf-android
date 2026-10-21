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
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.R
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.repository.LocalNoteRepository
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.data.settings.InMemoryPreferencesDataStore
import com.markleaf.notes.domain.model.Note
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * #547 review: a note the user saved holding only `# ` is theirs. The empty-note
 * discard deletes for good, so only a marker this visit seeded may count as
 * empty (#544). The setting is on here, so a pass means provenance — not the
 * setting being off — is what kept the note. One scenario per file for the same
 * reason as [EditorDiscardsBlankNoteTest].
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class EditorKeepsSavedHeadingMarkerTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val repo by lazy { LocalNoteRepository(AppDatabase.getInstance(context)) }

    @Test
    fun aSavedBareHeadingMarkerSurvivesLeavingTheEditor() {
        val noteId = UUID.randomUUID().toString()
        val now = Instant.now()
        runBlocking {
            repo.createNote(
                Note(
                    id = noteId,
                    title = "",
                    contentMarkdown = "# ",
                    excerpt = "",
                    // One clock read: a just-created note has createdAt == updatedAt.
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
        var showEditor by mutableStateOf(true)
        val settings = AppSettingsRepository(InMemoryPreferencesDataStore()).also {
            runBlocking { it.setNewNotesStartWithHeading(true) }
        }

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
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onNodeWithContentDescription(context.getString(R.string.note_content))
                .fetchSemanticsNode().config[SemanticsProperties.EditableText].text == "# "
        }

        showEditor = false
        composeRule.waitForIdle()
        // A discard would land through Room's executor after idle (see
        // EditorDiscardsBlankNoteTest); give it the same window the discard
        // tests wait for before concluding it did not happen.
        Thread.sleep(1_500)
        composeRule.waitForIdle()

        assertEquals("# ", runBlocking { repo.getNote(noteId)?.contentMarkdown })
    }
}
