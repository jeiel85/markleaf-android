package com.markleaf.notes.feature.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.ui.theme.MarkleafTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
@OptIn(ExperimentalTestApi::class)
class EditorFormattingControlsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun collapsedEntryOpensCompletePanel() {
        val expanded = mutableStateOf(false)
        render(
            state = { EditorFormattingUiState(expanded = expanded.value) },
            onExpandedChange = { expanded.value = it }
        )

        composeRule.onNodeWithContentDescription("Formatting")
            .assertIsDisplayed()
            .assertIsEnabled()
            .assert(hasStateDescription("Collapsed"))
            .performClick()

        composeRule.onNodeWithContentDescription("Formatting")
            .assert(hasStateDescription("Expanded"))
        composeRule.onNodeWithText("Inline").assertIsDisplayed()
        composeRule.onNodeWithText("Bold").assertIsDisplayed()
        composeRule.onNodeWithText("Insert image").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun selectedTextShowsContextActionsAndInvokesBold() {
        var picked: EditorFormattingAction? = null
        render(
            state = { EditorFormattingUiState(selectionActive = true) },
            onAction = { picked = it }
        )

        composeRule.onNodeWithContentDescription("Bold").assertIsDisplayed().performClick()

        assertEquals(EditorFormattingAction.BOLD, picked)
        composeRule.onNodeWithContentDescription("More options").assertIsDisplayed()
    }

    @Test
    fun panelActionClosesPanelAndInvokesAction() {
        val expanded = mutableStateOf(true)
        var picked: EditorFormattingAction? = null
        render(
            state = { EditorFormattingUiState(expanded = expanded.value) },
            onExpandedChange = { expanded.value = it },
            onAction = { picked = it }
        )

        composeRule.onNodeWithText("Code block").performScrollTo().performClick()

        assertEquals(EditorFormattingAction.CODE_BLOCK, picked)
        assertEquals(false, expanded.value)
    }

    @Test
    fun keyboardOpenMovesFocusToFirstPanelAction() {
        val expanded = mutableStateOf(false)
        render(
            state = { EditorFormattingUiState(expanded = expanded.value) },
            onExpandedChange = { expanded.value = it }
        )

        composeRule.onNodeWithContentDescription("Formatting")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .assertIsFocused()
            .performKeyInput {
            pressKey(Key.Enter)
        }

        assertEquals(true, expanded.value)
        composeRule.onNodeWithText("Bold").assertIsFocused()
    }

    @Test
    fun keyboardCanInvokeFocusedPanelAction() {
        val expanded = mutableStateOf(false)
        var picked: EditorFormattingAction? = null
        render(
            state = { EditorFormattingUiState(expanded = expanded.value) },
            onExpandedChange = { expanded.value = it },
            onAction = { picked = it }
        )

        composeRule.onNodeWithContentDescription("Formatting")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithText("Bold")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.Enter) }

        assertEquals(EditorFormattingAction.BOLD, picked)
        assertEquals(false, expanded.value)
    }

    @Test
    fun formattingShortcutWorksWhilePanelHasKeyboardFocus() {
        val expanded = mutableStateOf(false)
        var picked: EditorFormattingAction? = null
        render(
            state = { EditorFormattingUiState(expanded = expanded.value) },
            onExpandedChange = { expanded.value = it },
            onAction = { picked = it }
        )

        composeRule.onNodeWithContentDescription("Formatting")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithText("Bold")
            .assertIsFocused()
            .performKeyInput {
                keyDown(Key.CtrlLeft)
                pressKey(Key.I)
                keyUp(Key.CtrlLeft)
            }

        assertEquals(EditorFormattingAction.ITALIC, picked)
        assertEquals(false, expanded.value)
    }

    @Test
    fun keyboardNavigationWrapsInsideExpandedPanel() {
        val expanded = mutableStateOf(false)
        render(
            state = { EditorFormattingUiState(expanded = expanded.value) },
            onExpandedChange = { expanded.value = it }
        )

        composeRule.onNodeWithContentDescription("Formatting")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Enter) }
        composeRule.onNodeWithText("Bold")
            .assertIsFocused()
            .performKeyInput { pressKey(Key.DirectionUp) }

        composeRule.onNodeWithText("Insert image").assertIsFocused()
    }

    @Test
    fun escapeDismissesExpandedPanel() {
        val expanded = mutableStateOf(true)
        render(
            state = { EditorFormattingUiState(expanded = expanded.value) },
            onExpandedChange = { expanded.value = it }
        )

        composeRule.onNodeWithText("Bold")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput { pressKey(Key.Escape) }

        assertEquals(false, expanded.value)
    }

    @Test
    fun selectedContextActionClosesAnOpenPanel() {
        val expanded = mutableStateOf(true)
        var picked: EditorFormattingAction? = null
        render(
            state = { EditorFormattingUiState(selectionActive = true, expanded = expanded.value) },
            onExpandedChange = { expanded.value = it },
            onAction = { picked = it }
        )

        composeRule.onNodeWithContentDescription("Bold").performClick()

        assertEquals(EditorFormattingAction.BOLD, picked)
        assertEquals(false, expanded.value)
    }

    @Test
    fun disabledEntryCannotOpenPanel() {
        val expanded = mutableStateOf(false)
        render(
            state = { EditorFormattingUiState(expanded = expanded.value, enabled = false) },
            onExpandedChange = { expanded.value = it }
        )

        composeRule.onNodeWithContentDescription("Formatting")
            .assertIsNotEnabled()
            .performClick()

        assertEquals(false, expanded.value)
    }

    @Test
    fun undoAndRedoAreDisabledUntilThereIsSomethingToStepTo() {
        var undone = 0
        render(state = { EditorFormattingUiState() }, onUndo = { undone++ })

        composeRule.onNodeWithContentDescription("Undo")
            .assertIsDisplayed()
            .assertIsNotEnabled()
            .performClick()
        composeRule.onNodeWithContentDescription("Redo").assertIsNotEnabled()

        assertEquals(0, undone)
    }

    @Test
    fun undoAndRedoInvokeTheirCallbacks() {
        var undone = 0
        var redone = 0
        render(
            state = { EditorFormattingUiState(canUndo = true, canRedo = true) },
            onUndo = { undone++ },
            onRedo = { redone++ }
        )

        composeRule.onNodeWithContentDescription("Undo").assertIsEnabled().performClick()
        composeRule.onNodeWithContentDescription("Redo").assertIsEnabled().performClick()

        assertEquals(1, undone)
        assertEquals(1, redone)
    }

    @Test
    fun undoStaysAvailableWhileTextIsSelected() {
        render(state = { EditorFormattingUiState(selectionActive = true, canUndo = true) })

        composeRule.onNodeWithContentDescription("Undo").assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithContentDescription("Bold").assertIsDisplayed()
    }

    /** The row mounted for undo alone: "Show formatting button" is off (#331). */
    @Test
    fun theRowCanCarryUndoWithoutTheFormattingEntry() {
        render(
            state = {
                EditorFormattingUiState(canUndo = true, showFormattingEntry = false)
            }
        )

        composeRule.onNodeWithContentDescription("Undo").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Redo").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Formatting").assertDoesNotExist()
    }

    @Test
    fun undoIsDisabledWhileTheNoteIsStillLoading() {
        render(state = { EditorFormattingUiState(enabled = false, canUndo = true) })

        composeRule.onNodeWithContentDescription("Undo").assertIsNotEnabled()
    }

    private fun render(
        state: () -> EditorFormattingUiState,
        onExpandedChange: (Boolean) -> Unit = {},
        onAction: (EditorFormattingAction) -> Unit = {},
        onUndo: () -> Unit = {},
        onRedo: () -> Unit = {}
    ) {
        composeRule.setContent {
            MarkleafTheme(dynamicColor = false) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.BottomStart
                ) {
                    EditorFormattingControls(
                        state = state(),
                        onExpandedChange = onExpandedChange,
                        onAction = onAction,
                        modifier = Modifier,
                        backgroundColor = MaterialTheme.colorScheme.background,
                        onUndo = onUndo,
                        onRedo = onRedo
                    )
                }
            }
        }
    }
}
