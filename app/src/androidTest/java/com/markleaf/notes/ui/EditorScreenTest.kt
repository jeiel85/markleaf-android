package com.markleaf.notes.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.markleaf.notes.R
import com.markleaf.notes.TestHostActivity
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.repository.LocalNoteRepository
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.feature.editor.EditorScreen
import com.markleaf.notes.ui.theme.MarkleafTheme
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class EditorScreenTest {
    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private var scenario: ActivityScenario<TestHostActivity>? = null

    @After
    fun tearDown() {
        scenario?.close()
        TestHostActivity.content = null
    }

    private fun launchEditor(noteId: String? = null) {
        TestHostActivity.content = {
            MarkleafTheme {
                EditorScreen(noteId = noteId, onBack = {})
            }
        }
        scenario = ActivityScenario.launch(TestHostActivity::class.java)
        composeTestRule.waitForIdle()
    }

    @Test
    fun editorScreen_showsTitle() {
        launchEditor()

        val newNoteLabel = context.getString(R.string.new_note)
        composeTestRule.onNodeWithText(newNoteLabel).assertIsDisplayed()
    }

    @Test
    fun editorScreen_persistedEmptyNoteRequestsInitialFocus() {
        val noteId = UUID.randomUUID().toString()
        val repository = LocalNoteRepository(AppDatabase.getInstance(context))
        val now = Instant.now()
        runBlocking {
            repository.createNote(
                Note(
                    id = noteId,
                    title = "",
                    contentMarkdown = "",
                    excerpt = "",
                    createdAt = now,
                    updatedAt = now
                )
            )
        }

        try {
            launchEditor(noteId)

            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.note_content))
                .assertIsFocused()
        } finally {
            runBlocking { repository.deleteForever(noteId) }
        }
    }

    @Test
    fun editorScreen_emptyStateDoesNotExposeEmojiText() {
        launchEditor()

        composeTestRule.onNodeWithText("✏️").assertDoesNotExist()
        composeTestRule.onNodeWithText(context.getString(R.string.editor_empty_title)).assertIsDisplayed()
    }

    @Test
    fun editorScreen_togglePreview() {
        launchEditor()

        val previewLabel = context.getString(R.string.preview)
        val editLabel = context.getString(R.string.edit)

        composeTestRule.onNodeWithContentDescription(previewLabel).performClick()

        composeTestRule.onNodeWithText(previewLabel).assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription(editLabel).assertIsDisplayed()
    }

    // v2.30.0(#215)에서 아웃라인은 "노트 정보" 시트를 떠나 자기 화면으로 갔다.
    // 그래서 시트와 아웃라인을 각각 확인한다 — 하나였던 이 테스트는 그때
    // 깨졌고, `com.markleaf.notes.ui` 가 통째로 제외돼 있어서 아무도 못 봤다(#239).
    @Test
    fun editorScreen_noteInformationShowsStatisticsAndBacklinks() {
        launchEditor()
        val editor = composeTestRule.onNodeWithContentDescription(context.getString(R.string.note_content))

        editor.performTextInput("# Overview\n\nBody")
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.note_information)).performClick()

        composeTestRule.onNodeWithText(context.getString(R.string.note_statistics)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.backlinks_section_title)).assertIsDisplayed()
    }

    @Test
    fun editorScreen_outlineHeadingKeepsYouInTheEditor() {
        launchEditor()
        val editor = composeTestRule.onNodeWithContentDescription(context.getString(R.string.note_content))

        editor.performTextInput("# Overview\n\nBody")
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.table_of_contents)).performClick()

        composeTestRule.onNodeWithText(context.getString(R.string.table_of_contents)).assertIsDisplayed()
        composeTestRule.onNodeWithText("Overview").performClick()

        // #215 의 계약: 편집 중 헤딩을 누르면 작성 중인 것을 버리고 읽기 모드로
        // 넘어가는 대신 에디터에 남는다. 그래서 토글은 여전히 "미리보기"를
        // 가리킨다 — "편집"을 가리키면 프리뷰로 넘어갔다는 뜻이고, 그게 #215
        // 이전의 동작이다.
        composeTestRule.onNodeWithText(context.getString(R.string.table_of_contents)).assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.note_content)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.preview)).assertIsDisplayed()
    }

    @Test
    fun editorScreen_quickInsertAddsHeading() {
        launchEditor()
        val editor = composeTestRule.onNodeWithContentDescription(context.getString(R.string.note_content))

        editor.performTextInput("/")
        composeTestRule.onNodeWithText(context.getString(R.string.quick_insert_title)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.quick_insert_heading_1)).performClick()

        editor.assertTextEquals("# ")
        composeTestRule.onNodeWithText(context.getString(R.string.quick_insert_title)).assertDoesNotExist()
    }

    @Test
    fun editorScreen_formattingPanelAppliesBold() {
        launchEditor()
        val editor = composeTestRule.onNodeWithContentDescription(context.getString(R.string.note_content))
        val formattingLabel = context.getString(R.string.formatting)

        composeTestRule.onNodeWithContentDescription(formattingLabel).performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.formatting_inline)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.bold)).performClick()

        editor.assertTextEquals("**bold**")
        composeTestRule.onNodeWithText(context.getString(R.string.formatting_inline)).assertDoesNotExist()
    }

    @Test
    fun editorScreen_quickInsertPreemptsFormattingControls() {
        launchEditor()
        val editor = composeTestRule.onNodeWithContentDescription(context.getString(R.string.note_content))
        val formattingLabel = context.getString(R.string.formatting)

        composeTestRule.onNodeWithContentDescription(formattingLabel).assertIsDisplayed()
        editor.performTextInput("/")

        composeTestRule.onNodeWithText(context.getString(R.string.quick_insert_title)).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(formattingLabel).assertDoesNotExist()
    }

    /**
     * "Show formatting button" off (#331) has to remove the standing `Aa` row
     * without taking the selection actions with it. Those are two different
     * things sharing one composable: the entry is ambient chrome above the
     * keyboard, while bold / italic / link only appear because you selected
     * text, which is how you asked for them. A gate that dropped both would
     * leave no touch path to formatting at all and still look like it worked.
     */
    @Test
    fun editorScreen_hidingTheFormattingButtonKeepsSelectionActions() {
        val settings = AppSettingsRepository(context.applicationContext)
        runBlocking { settings.setShowFormattingButton(false) }
        try {
            launchEditor()
            val editor = composeTestRule.onNodeWithContentDescription(
                context.getString(R.string.note_content)
            )

            composeTestRule.onNodeWithContentDescription(context.getString(R.string.formatting))
                .assertDoesNotExist()

            editor.performTextInput("selectable")
            editor.performSemanticsAction(SemanticsActions.SetSelection) { setSelection ->
                setSelection(0, "selectable".length, true)
            }

            composeTestRule.onNodeWithContentDescription(context.getString(R.string.bold))
                .assertIsDisplayed()
            composeTestRule.onNodeWithContentDescription(context.getString(R.string.formatting))
                .assertDoesNotExist()
        } finally {
            runBlocking { settings.setShowFormattingButton(true) }
        }
    }

    /**
     * The whole of #360 in one pass: Markleaf autosaves, so typing over a
     * selected note used to be permanent as soon as the debounce fired. Undo
     * has to put the text back *and* land that version in the row — an undo the
     * note is not saved with would be no undo at all.
     *
     * `EditorUndoHistoryTest` covers what counts as a step; this covers that
     * `EditorScreen` joins the stack, the button and the autosave.
     */
    @Test
    fun editorScreen_undoRestoresTextTypedOverTheWholeNoteAndSavesIt() {
        val noteId = UUID.randomUUID().toString()
        val repository = LocalNoteRepository(AppDatabase.getInstance(context))
        val body = "A note the writer would hate to lose."
        val now = Instant.now()
        runBlocking {
            repository.createNote(
                Note(
                    id = noteId,
                    title = body,
                    contentMarkdown = body,
                    excerpt = body,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }

        try {
            launchEditor(noteId)
            val editor = composeTestRule.onNodeWithContentDescription(
                context.getString(R.string.note_content)
            )

            editor.performTextReplacement("x")
            editor.assertTextEquals("x")
            // Wait for the damage to actually reach the row, so the assertion
            // after the undo cannot pass on a save that never happened.
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                runBlocking { repository.getNote(noteId)?.contentMarkdown == "x" }
            }

            composeTestRule.onNodeWithContentDescription(context.getString(R.string.undo))
                .assertIsDisplayed()
                .performClick()

            editor.assertTextEquals(body)
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                runBlocking { repository.getNote(noteId)?.contentMarkdown == body }
            }
        } finally {
            runBlocking { repository.deleteForever(noteId) }
        }
    }

    /**
     * "Show formatting button" off (#331) takes the standing `Aa` handle away.
     * It must not take undo with it: the row is the only touch path back, so it
     * mounts for undo alone once there is a step to go back to (#360).
     */
    @Test
    fun editorScreen_undoStaysReachableWithTheFormattingButtonOff() {
        val settings = AppSettingsRepository(context.applicationContext)
        runBlocking { settings.setShowFormattingButton(false) }
        try {
            launchEditor()
            val editor = composeTestRule.onNodeWithContentDescription(
                context.getString(R.string.note_content)
            )
            val undoLabel = context.getString(R.string.undo)

            // Nothing typed yet, so there is nothing for the row to offer.
            composeTestRule.onNodeWithContentDescription(undoLabel).assertDoesNotExist()

            editor.performTextInput("typed by mistake")

            composeTestRule.onNodeWithContentDescription(context.getString(R.string.formatting))
                .assertDoesNotExist()
            composeTestRule.onNodeWithContentDescription(undoLabel)
                .assertIsDisplayed()
                .performClick()

            composeTestRule.onNodeWithText(context.getString(R.string.editor_empty_title))
                .assertIsDisplayed()
        } finally {
            runBlocking { settings.setShowFormattingButton(true) }
        }
    }

    @Test
    fun editorScreen_urlDoesNotOpenQuickInsert() {
        launchEditor()

        composeTestRule.onNodeWithContentDescription(context.getString(R.string.note_content))
            .performTextInput("https://")

        composeTestRule.onNodeWithText(context.getString(R.string.quick_insert_title)).assertDoesNotExist()
    }

    /**
     * The wiring #262 asked for: a preview checkbox tap has to reach the saved
     * note through this screen's own callback and autosave, not through the
     * three steps called by hand.
     *
     * `MarkdownTaskToggleClickTest` covers the tap reporting a source line and
     * `MarkdownEditActionsTest` the string edit; `PreviewToggleReachesMirrorTest`
     * covers repository → mirror file. What none of them covered is that
     * `EditorScreen` joins them — that the callback updates `editorState`, that
     * the save is triggered, and that the debounce lands it. This drives the
     * real screen, so it fails if any of those three stop happening.
     *
     * It stops at the repository on purpose: the mirror write resolves its
     * folder with `DocumentFile.fromTreeUri`, and a tree Uri needs a SAF grant
     * that a person has to tap. The seam between the two tests is that
     * boundary rather than a gap.
     */
    @Test
    fun editorScreen_previewCheckboxTapReachesTheSavedNote() {
        val noteId = UUID.randomUUID().toString()
        val repository = LocalNoteRepository(AppDatabase.getInstance(context))
        val body = "# Plan\n\n- [ ] first\n- [ ] second"
        val now = Instant.now()
        runBlocking {
            repository.createNote(
                Note(
                    id = noteId,
                    title = "Plan",
                    contentMarkdown = body,
                    excerpt = body.take(40),
                    createdAt = now,
                    updatedAt = now
                )
            )
        }

        try {
            launchEditor(noteId)

            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.preview))
                .performClick()

            // Only the marker toggles — the label stays selectable (#219), which
            // is why this clicks the left edge rather than the node's centre.
            composeTestRule.onNodeWithText("☐ second").performTouchInput {
                click(centerLeft.copy(x = 4f))
            }

            // The autosave is debounced by a second, so this waits for the note
            // rather than asserting immediately.
            composeTestRule.waitUntil(timeoutMillis = 15_000) {
                runBlocking {
                    repository.getNote(noteId)?.contentMarkdown?.contains("- [x] second") == true
                }
            }

            val saved = runBlocking { repository.getNote(noteId) }
            assertTrue(
                "the untouched row must not move: ${saved?.contentMarkdown}",
                saved?.contentMarkdown?.contains("- [ ] first") == true
            )
        } finally {
            runBlocking { repository.deleteForever(noteId) }
        }
    }
}
