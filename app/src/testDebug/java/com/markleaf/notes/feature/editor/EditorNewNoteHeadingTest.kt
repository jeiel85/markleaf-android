package com.markleaf.notes.feature.editor

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextRange
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
 * "Start new notes with a heading" (#544) on the real editor: a new (empty) note
 * opens on `# ` with the caret after it, the seed is not written until the user
 * types, and what they type lands after it. A note with text opens as stored.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class EditorNewNoteHeadingTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val repo by lazy { LocalNoteRepository(AppDatabase.getInstance(context)) }

    private fun createNote(content: String, importedEmpty: Boolean = false): String {
        val noteId = UUID.randomUUID().toString()
        val now = Instant.now()
        runBlocking {
            repo.createNote(
                Note(
                    id = noteId,
                    title = "",
                    contentMarkdown = content,
                    excerpt = "",
                    // One clock read: a just-created note has createdAt == updatedAt.
                    createdAt = now,
                    updatedAt = now,
                    // What MirrorImport leaves on a note it brought in empty.
                    lastImportedAt = if (importedEmpty) now else null
                )
            )
        }
        return noteId
    }

    /**
     * #547 review: only a note the new-note flow just made is seeded. An empty
     * note imported from the sync folder is one the user kept, and "existing
     * notes are not changed".
     */
    @Test
    fun anEmptyImportedNoteIsNotSeeded() {
        show(createNote("", importedEmpty = true), startWithHeading = true)

        assertEquals("", fieldText())
    }

    private fun show(noteId: String, startWithHeading: Boolean) {
        val settings = AppSettingsRepository(InMemoryPreferencesDataStore()).also {
            runBlocking { it.setNewNotesStartWithHeading(startWithHeading) }
        }
        composeRule.setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                EditorScreen(noteId = noteId, onBack = {}, settingsRepository = settings)
            }
        }
        composeRule.waitForIdle()
    }

    private fun field() = composeRule.onNodeWithContentDescription(context.getString(R.string.note_content))

    private fun fieldText() = field().fetchSemanticsNode().config[SemanticsProperties.EditableText].text

    @Test
    fun aNewNoteOpensOnTheHeadingAndWhatIsTypedFollowsIt() {
        val noteId = createNote("")
        show(noteId, startWithHeading = true)

        composeRule.waitUntil(timeoutMillis = 5_000) { fieldText() == "# " }
        assertEquals(TextRange(2), field().fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange])
        // Shown, not saved: opening and leaving must change nothing.
        assertEquals("", runBlocking { repo.getNote(noteId)?.contentMarkdown })

        field().performTextInput("Groceries")
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { repo.getNote(noteId)?.contentMarkdown } == "# Groceries"
        }
        assertEquals("Groceries", runBlocking { repo.getNote(noteId)?.title })
    }

    /**
     * #547 review: the seed is not an edit, so a version the sync folder imports
     * into the row while the seeded note is open is shown, not held back as if
     * the user had unsaved text (#428).
     */
    @Test
    fun anImportWhileTheSeedIsUntouchedIsShown() {
        val noteId = createNote("")
        show(noteId, startWithHeading = true)
        composeRule.waitUntil(timeoutMillis = 5_000) { fieldText() == "# " }

        runBlocking {
            val note = repo.getNote(noteId)!!
            repo.updateNote(note.copy(contentMarkdown = "from the folder", updatedAt = Instant.now()))
        }

        composeRule.waitUntil(timeoutMillis = 5_000) { fieldText() == "from the folder" }
    }

    /**
     * #547 review: typing and deleting back to the seed must not save it. The
     * row goes back to empty, so nothing (a mirror write, a process death)
     * can keep a blank `# ` note around.
     */
    @Test
    fun deletingBackToTheSeedSavesAnEmptyNote() {
        val noteId = createNote("")
        show(noteId, startWithHeading = true)
        composeRule.waitUntil(timeoutMillis = 5_000) { fieldText() == "# " }

        field().performTextInput("x")
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { repo.getNote(noteId)?.contentMarkdown } == "# x"
        }
        field().performTextReplacement("# ")
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { repo.getNote(noteId)?.contentMarkdown } == ""
        }
        assertEquals("# ", fieldText())
    }

    /**
     * #547 review: once the folder's version is on screen it is the note, even
     * when that version is exactly `# `, so the next import is adopted rather
     * than refused as if `# ` were still the seed.
     */
    @Test
    fun anImportedHeadingMarkerIsNoLongerTheSeed() {
        val noteId = createNote("")
        show(noteId, startWithHeading = true)
        composeRule.waitUntil(timeoutMillis = 5_000) { fieldText() == "# " }

        runBlocking {
            val note = repo.getNote(noteId)!!
            repo.updateNote(note.copy(contentMarkdown = "# ", updatedAt = Instant.now()))
        }
        composeRule.waitForIdle()
        runBlocking {
            val note = repo.getNote(noteId)!!
            repo.updateNote(note.copy(contentMarkdown = "from the folder", updatedAt = Instant.now()))
        }

        composeRule.waitUntil(timeoutMillis = 5_000) { fieldText() == "from the folder" }
    }

    @Test
    fun withTheSettingOffANewNoteOpensEmpty() {
        show(createNote(""), startWithHeading = false)

        assertEquals("", fieldText())
    }

    @Test
    fun aNoteWithTextOpensAsStored() {
        show(createNote("milk\neggs"), startWithHeading = true)

        composeRule.waitUntil(timeoutMillis = 5_000) { fieldText() == "milk\neggs" }
    }
}
