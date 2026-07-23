package com.markleaf.notes.feature.editor

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
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
@Config(sdk = [33], qualifiers = "w360dp-h80dp-mdpi")
class EditorTopAppBarSnapshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun phone() = snapshot("editor_top_appbar_phone", "New Note")

    @Test
    fun largeTextPhone() = snapshot(
        name = "editor_top_appbar_large_text_phone",
        title = "New Note",
        fontScale = 1.5f
    )

    private fun snapshot(name: String, title: String, fontScale: Float = 1f) {
        composeRule.setContent {
            MarkleafTheme(dynamicColor = false) {
                val currentDensity = LocalDensity.current
                CompositionLocalProvider(
                    LocalDensity provides Density(currentDensity.density, fontScale)
                ) {
                    EditorTopAppBar(
                        title = title,
                        isPreviewMode = false,
                        isFocusMode = false,
                        showMore = true,
                        moreExpanded = false,
                        onBack = {},
                        onTogglePreview = {},
                        onExitFocusMode = {},
                        onOpenOutline = {},
                        onOpenInfo = {},
                        onOpenMore = {},
                        onDismissMore = {},
                        moreMenuContent = {},
                        modifier = Modifier.testTag("editorTopAppBar")
                    )
                }
            }
        }
        composeRule.onNodeWithTag("editorTopAppBar").captureRoboImage(
            filePath = "src/test/snapshots/roborazzi/$name.png"
        )
    }
}
