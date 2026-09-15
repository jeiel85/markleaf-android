package com.markleaf.notes.feature.editor

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
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
 * Regression for a Codex review finding on PR #407: `collapsibleId` is
 * assigned by a section's position among every `<details>` in the note, so
 * inserting a new one above an already-toggled section reassigns ids out
 * from under the toggle set. Before the fix, closing "Keep" (which starts
 * expanded via `<details open>`) and then typing a brand new section above it
 * left the toggle attached to whatever now held id 0 -- the *new* section --
 * silently expanding content the user never asked to see, while "Keep"
 * reverted to looking untouched.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class EditorCollapsibleToggleSurvivesUnrelatedEditsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val repo by lazy { LocalNoteRepository(AppDatabase.getInstance(context)) }

    @Test
    fun insertingANewSectionDoesNotInheritAnotherSectionsStaleToggle() {
        val original = "<details open>\n<summary>Keep</summary>\n\nkeep body\n</details>"
        val noteId = UUID.randomUUID().toString()
        runBlocking {
            repo.createNote(
                Note(
                    id = noteId,
                    title = "Keep",
                    contentMarkdown = original,
                    excerpt = "",
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
            )
        }
        val settings = AppSettingsRepository(InMemoryPreferencesDataStore())

        composeRule.setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                EditorScreen(noteId = noteId, onBack = {}, settingsRepository = settings)
            }
        }
        composeRule.waitForIdle()

        // Enter preview and close "Keep", which starts expanded.
        composeRule.onNodeWithContentDescription(context.getString(R.string.preview)).performClick()
        composeRule.onNodeWithText("keep body").assertExists()
        composeRule.onNodeWithText("Keep").performClick()
        composeRule.onNodeWithText("keep body").assertDoesNotExist()

        // Back to edit, and insert a brand new (collapsed-by-default) section
        // above "Keep" -- this is the structural change that reassigns ids.
        composeRule.onNodeWithContentDescription(context.getString(R.string.edit)).performClick()
        val withNewSection = "<details>\n<summary>New</summary>\n\nnew body\n</details>\n\n$original"
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.note_content)
        ).performTextReplacement(withNewSection)

        composeRule.onNodeWithContentDescription(context.getString(R.string.preview)).performClick()

        // The stale toggle must not have leaked onto "New": with no `open`
        // attribute of its own, it starts collapsed like any other section
        // that was never touched.
        composeRule.onNodeWithText("new body").assertDoesNotExist()
    }
}
