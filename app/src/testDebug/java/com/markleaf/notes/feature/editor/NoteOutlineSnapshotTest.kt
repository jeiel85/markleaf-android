package com.markleaf.notes.feature.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.markleaf.notes.core.markdown.preview.TocHeading
import com.markleaf.notes.ui.theme.MarkleafTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Locks in what the outline screen looks like (#215) — uniform rows with
 * indentation as the only level cue. The readability complaint that produced
 * this screen was about the rendering itself, so the rendering is what gets a
 * golden image.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class NoteOutlineSnapshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun populatedPhone() = snapshot("note_outline_populated_phone", headings())

    @Test
    fun emptyPhone() = snapshot("note_outline_empty_phone", emptyList())

    @Test
    fun populatedLargeTextPhone() =
        snapshot("note_outline_populated_large_text_phone", headings(), fontScale = 1.5f)

    @Test
    @Config(sdk = [33], qualifiers = "w800dp-h600dp-night-mdpi")
    fun populatedDarkTablet() =
        snapshot("note_outline_populated_dark_tablet", headings(), darkTheme = true)

    private fun snapshot(
        name: String,
        headings: List<TocHeading>,
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
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("noteOutlineSurface"),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Column {
                            NoteOutlineTopBar(onClose = {})
                            NoteOutlineContent(headings = headings, onHeadingClick = {})
                        }
                    }
                }
            }
        }
        composeRule.onNodeWithTag("noteOutlineSurface").captureRoboImage(
            filePath = "src/test/snapshots/roborazzi/$name.png"
        )
    }

    private fun headings() = listOf(
        TocHeading(index = 0, text = "Project brief", level = 1, sourceLine = 0),
        TocHeading(index = 3, text = "Background", level = 2, sourceLine = 6),
        TocHeading(index = 5, text = "Prior art", level = 3, sourceLine = 11),
        TocHeading(index = 9, text = "Next steps", level = 2, sourceLine = 18),
        TocHeading(index = 12, text = "References", level = 1, sourceLine = 26)
    )
}
