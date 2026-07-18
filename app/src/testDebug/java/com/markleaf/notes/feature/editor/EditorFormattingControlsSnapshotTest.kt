package com.markleaf.notes.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.Density
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.markleaf.notes.ui.theme.MarkleafTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
@OptIn(ExperimentalTestApi::class)
class EditorFormattingControlsSnapshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun collapsedPhone() = snapshot(
        name = "editor_formatting_collapsed_phone",
        state = EditorFormattingUiState()
    )

    @Test
    fun selectedTextPhone() = snapshot(
        name = "editor_formatting_selected_phone",
        state = EditorFormattingUiState(selectionActive = true)
    )

    @Test
    fun expandedPhone() = snapshot(
        name = "editor_formatting_expanded_phone",
        state = EditorFormattingUiState(expanded = true)
    )

    @Test
    fun disabledPhone() = snapshot(
        name = "editor_formatting_disabled_phone",
        state = EditorFormattingUiState(enabled = false)
    )

    @Test
    fun expandedDarkPhone() = snapshot(
        name = "editor_formatting_expanded_dark_phone",
        state = EditorFormattingUiState(expanded = true),
        darkTheme = true
    )

    @Test
    fun expandedLargeTextPhone() = snapshot(
        name = "editor_formatting_expanded_large_text_phone",
        state = EditorFormattingUiState(expanded = true),
        fontScale = 1.5f
    )

    @Test
    @Config(sdk = [33], qualifiers = "w800dp-h600dp-mdpi")
    fun expandedTablet() = snapshot(
        name = "editor_formatting_expanded_tablet",
        state = EditorFormattingUiState(expanded = true)
    )

    @Test
    @Config(sdk = [33], qualifiers = "ko-rKR-w360dp-h640dp-notnight-mdpi")
    fun expandedKoreanPhone() = snapshot(
        name = "editor_formatting_expanded_korean_phone",
        state = EditorFormattingUiState(expanded = true)
    )

    @Test
    fun keyboardFocusedPhone() {
        val expanded = mutableStateOf(false)
        render(state = { EditorFormattingUiState(expanded = expanded.value) }, onExpandedChange = {
            expanded.value = it
        })

        composeRule.onNodeWithContentDescription("Formatting")
            .performSemanticsAction(SemanticsActions.RequestFocus)
            .performKeyInput {
            pressKey(Key.Enter)
        }

        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/snapshots/roborazzi/editor_formatting_keyboard_focused_phone.png"
        )
    }

    private fun snapshot(
        name: String,
        state: EditorFormattingUiState,
        darkTheme: Boolean = false,
        fontScale: Float = 1f
    ) {
        render(state = { state }, darkTheme = darkTheme, fontScale = fontScale)
        composeRule.onRoot().captureRoboImage(
            filePath = "src/test/snapshots/roborazzi/$name.png"
        )
    }

    private fun render(
        state: () -> EditorFormattingUiState,
        onExpandedChange: (Boolean) -> Unit = {},
        darkTheme: Boolean = false,
        fontScale: Float = 1f
    ) {
        composeRule.setContent {
            MarkleafTheme(darkTheme = darkTheme, dynamicColor = false) {
                val currentDensity = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(currentDensity.density, fontScale)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        EditorFormattingControls(
                            state = state(),
                            onExpandedChange = onExpandedChange,
                            onAction = {},
                            backgroundColor = MaterialTheme.colorScheme.background
                        )
                    }
                }
            }
        }
    }
}
