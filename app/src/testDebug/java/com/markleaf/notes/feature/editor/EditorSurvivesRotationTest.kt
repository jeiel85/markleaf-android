package com.markleaf.notes.feature.editor

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Rotating the phone keeps the reader where they were (#499).
 *
 * A rotation recreates the activity, so the editor screen is composed again
 * from nothing and reloads its note. Before the fix that reload decided the
 * mode and the scroll afresh, so a note read in Preview came back in Edit. The
 * tests use [StateRestorationTester], which disposes the composition and builds
 * it again from the saved-instance-state bundle — the same path a rotation
 * takes — while the database, which a rotation does not touch, stays as it is.
 *
 * Each assertion is made only after the note's text is back on screen. The
 * mode flips in the very step that finishes loading, so asserting earlier would
 * pass against the broken code too: the restored value is there for the first
 * frame either way.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class EditorSurvivesRotationTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val repo by lazy { LocalNoteRepository(AppDatabase.getInstance(context)) }

    private fun createNote(content: String): String {
        val noteId = UUID.randomUUID().toString()
        runBlocking {
            repo.createNote(
                Note(
                    id = noteId,
                    title = "Rotating",
                    contentMarkdown = content,
                    excerpt = "",
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
            )
        }
        return noteId
    }

    private fun StateRestorationTester.showEditor(noteId: String, settings: AppSettingsRepository) {
        setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                EditorScreen(noteId = noteId, onBack = {}, settingsRepository = settings)
            }
        }
        composeRule.waitForIdle()
    }

    /**
     * Waits for the reload to put [text] back on screen, in whichever mode.
     * A substring match, because in Edit the whole note is one text field and
     * only a part of it is [text]; an exact match would wait on Preview alone
     * and time out on the very mode the test is checking is kept.
     */
    private fun awaitText(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
    }

    private fun assertInPreview() {
        // The toggle offers the mode you are not in.
        composeRule.onNodeWithContentDescription(context.getString(R.string.edit)).assertExists()
        composeRule.onNodeWithContentDescription(context.getString(R.string.preview)).assertDoesNotExist()
    }

    private fun assertInEdit() {
        composeRule.onNodeWithContentDescription(context.getString(R.string.preview)).assertExists()
        composeRule.onNodeWithContentDescription(context.getString(R.string.edit)).assertDoesNotExist()
    }

    @Test
    fun aNoteReadInPreviewStaysInPreviewAfterARotation() {
        val noteId = createNote("# Rotating\n\nbody text")
        val settings = AppSettingsRepository(InMemoryPreferencesDataStore())
        val restoration = StateRestorationTester(composeRule)
        restoration.showEditor(noteId, settings)

        composeRule.onNodeWithContentDescription(context.getString(R.string.preview)).performClick()
        assertInPreview()

        restoration.emulateSavedInstanceStateRestore()
        awaitText("body text")

        assertInPreview()
    }

    @Test
    fun aNoteBeingEditedStaysInEditAfterARotationEvenWhenNotesOpenInPreview() {
        // The reverse of the report: with "open notes in preview" on, the reload
        // used to put a note the reader had switched to Edit back into Preview.
        val noteId = createNote("# Rotating\n\nbody text")
        val settings = AppSettingsRepository(InMemoryPreferencesDataStore())
        runBlocking { settings.setOpenNotesInPreview(true) }
        val restoration = StateRestorationTester(composeRule)
        restoration.showEditor(noteId, settings)
        awaitText("body text")
        assertInPreview()

        composeRule.onNodeWithContentDescription(context.getString(R.string.edit)).performClick()
        assertInEdit()

        restoration.emulateSavedInstanceStateRestore()
        awaitText("body text")

        assertInEdit()
    }

    @Test
    fun theFirstOpenStillFollowsTheOpenInPreviewSetting() {
        // The other half of the contract: only a recreation keeps the mode. A
        // note opened fresh must still read the setting, or "open notes in
        // preview" would stop working at all.
        val noteId = createNote("# Rotating\n\nbody text")
        val settings = AppSettingsRepository(InMemoryPreferencesDataStore())
        runBlocking { settings.setOpenNotesInPreview(true) }
        val restoration = StateRestorationTester(composeRule)
        restoration.showEditor(noteId, settings)
        awaitText("body text")

        assertInPreview()
    }

    @Test
    fun thePreviewScrollPositionSurvivesARotation() {
        val content = (0 until 150).joinToString("\n\n") { "Paragraph $it" }
        val noteId = createNote(content)
        val settings = AppSettingsRepository(InMemoryPreferencesDataStore())
        val restoration = StateRestorationTester(composeRule)
        restoration.showEditor(noteId, settings)

        composeRule.onNodeWithContentDescription(context.getString(R.string.preview)).performClick()
        composeRule.onNode(hasScrollToIndexAction()).performScrollToIndex(80)
        composeRule.onNodeWithText("Paragraph 80").assertExists()
        composeRule.onNodeWithText("Paragraph 0").assertDoesNotExist()

        restoration.emulateSavedInstanceStateRestore()
        awaitText("Paragraph 80")

        assertInPreview()
        // Still down the note, not back at its top: the first rows are not even
        // composed, which is how a lazy list shows it is scrolled past them.
        composeRule.onNodeWithText("Paragraph 0").assertDoesNotExist()
    }

    @Test
    fun aSectionOpenedInPreviewStaysOpenAfterARotation() {
        // The preview index is a count of *visible* rows, so restoring it
        // against sections that all closed again would land on a different row.
        val noteId = createNote("# Rotating\n\n<details>\n<summary>More</summary>\n\nhidden pear\n</details>")
        val settings = AppSettingsRepository(InMemoryPreferencesDataStore())
        val restoration = StateRestorationTester(composeRule)
        restoration.showEditor(noteId, settings)

        composeRule.onNodeWithContentDescription(context.getString(R.string.preview)).performClick()
        composeRule.onNodeWithText("hidden pear").assertDoesNotExist()
        composeRule.onNodeWithText("More").performClick()
        composeRule.onNodeWithText("hidden pear").assertExists()

        restoration.emulateSavedInstanceStateRestore()
        awaitText("More")

        assertInPreview()
        composeRule.onNodeWithText("hidden pear").assertExists()
    }
}
