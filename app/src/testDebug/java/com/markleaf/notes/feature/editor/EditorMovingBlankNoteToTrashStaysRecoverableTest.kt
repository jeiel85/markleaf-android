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
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Regression for a Codex review finding on PR #406: the #405 empty-note
 * discard on leaving the editor must not turn an explicit "Move to trash" on
 * a blank note into a permanent delete. Before this fix, confirming the trash
 * dialog called `repo.moveToTrash` and then `onBack()` in the same breath,
 * which disposed the editor while the note was still blank and had the
 * discard-on-exit cleanup call `deleteForever` right behind it — the note
 * never reached Trash at all, with no way back.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class EditorMovingBlankNoteToTrashStaysRecoverableTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val repo by lazy { LocalNoteRepository(AppDatabase.getInstance(context)) }

    @Test
    fun movingABlankNoteToTrashLeavesItInTrashNotPermanentlyDeleted() {
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
        val settings = AppSettingsRepository(InMemoryPreferencesDataStore())

        composeRule.setContent {
            val hostScope = rememberCoroutineScope()
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                if (showEditor) {
                    EditorScreen(
                        noteId = noteId,
                        onBack = { showEditor = false },
                        settingsRepository = settings,
                        hostScope = hostScope
                    )
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(
            context.getString(R.string.more_options)
        ).performClick()
        composeRule.onNodeWithText(context.getString(R.string.move_to_trash)).performClick()
        // The dialog's confirm button shares the same label as the menu item
        // that opened it; the menu has already closed by this point, so only
        // the dialog's own button matches.
        composeRule.onNodeWithText(context.getString(R.string.move_to_trash)).performClick()
        composeRule.waitForIdle()

        val stored = runBlocking { repo.getNote(noteId) }
        assertTrue("the note must still exist, just trashed", stored != null)
        assertEquals(true, stored?.trashed)
    }
}
