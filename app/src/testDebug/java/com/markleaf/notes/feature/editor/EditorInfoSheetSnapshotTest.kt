package com.markleaf.notes.feature.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.ui.theme.MarkleafTheme
import java.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class EditorInfoSheetSnapshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun populatedPhone() = snapshot("editor_info_populated_phone", populatedState())

    @Test
    fun emptyPhone() = snapshot(
        "editor_info_empty_phone",
        EditorInfoUiState(
            statsText = "0 words · 0 chars · 0 min",
            backlinks = emptyList()
        )
    )

    @Test
    @Config(sdk = [33], qualifiers = "ko-rKR-w360dp-h640dp-notnight-mdpi")
    fun populatedKoreanPhone() = snapshot(
        "editor_info_populated_korean_phone",
        populatedState(statsText = "12 단어 · 86자 · 1분")
    )

    @Test
    fun populatedLargeTextPhone() = snapshot(
        "editor_info_populated_large_text_phone",
        populatedState(),
        fontScale = 1.5f
    )

    @Test
    @Config(sdk = [33], qualifiers = "w800dp-h600dp-night-mdpi")
    fun populatedDarkTablet() = snapshot(
        "editor_info_populated_dark_tablet",
        populatedState(),
        darkTheme = true
    )

    private fun snapshot(
        name: String,
        state: EditorInfoUiState,
        darkTheme: Boolean = false,
        fontScale: Float = 1f
    ) {
        composeRule.setContent {
            MarkleafTheme(darkTheme = darkTheme, dynamicColor = false) {
                val currentDensity = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(currentDensity.density, fontScale)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 640.dp)
                                    .testTag("editorInfoSurface"),
                                color = MaterialTheme.colorScheme.surface,
                                shape = MaterialTheme.shapes.extraLarge,
                                tonalElevation = 1.dp
                            ) {
                                EditorInfoSheetContent(
                                    state = state,
                                    onBacklinkClick = {}
                                )
                            }
                        }
                    }
                }
            }
        }
        composeRule.onNodeWithTag("editorInfoSurface").captureRoboImage(
            filePath = "src/test/snapshots/roborazzi/$name.png"
        )
    }

    private fun populatedState(statsText: String = "12 words · 86 chars · 1 min") = EditorInfoUiState(
        statsText = statsText,
        backlinks = listOf(
            note("source-1", "Daily writing ritual"),
            note("source-2", "Markdown field notes")
        )
    )

    private fun note(id: String, title: String) = Note(
        id = id,
        title = title,
        contentMarkdown = "# $title",
        excerpt = "",
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH
    )
}
