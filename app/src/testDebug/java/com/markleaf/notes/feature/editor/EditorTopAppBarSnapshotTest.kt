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

    /**
     * The locked view-toggle, in both themes. Locking recolours the icon amber
     * to signal the sticky mode (#200), and the amber is now chosen per
     * background — a single one measured 2.60:1 on the light bar. Only the
     * unlocked bar used to be snapshotted, so the indicator could break unseen.
     */
    @Test
    fun lockedPhone() = snapshot(
        name = "editor_top_appbar_locked_phone",
        title = "New Note",
        isViewModeLocked = true
    )

    @Test
    fun lockedDarkPhone() = snapshot(
        name = "editor_top_appbar_locked_dark_phone",
        title = "New Note",
        darkTheme = true,
        isViewModeLocked = true
    )

    private fun snapshot(
        name: String,
        title: String,
        fontScale: Float = 1f,
        darkTheme: Boolean = false,
        isViewModeLocked: Boolean = false
    ) {
        composeRule.setContent {
            MarkleafTheme(darkTheme = darkTheme, dynamicColor = false) {
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
                        modifier = Modifier.testTag("editorTopAppBar"),
                        isViewModeLocked = isViewModeLocked
                    )
                }
            }
        }
        composeRule.onNodeWithTag("editorTopAppBar").captureRoboImage(
            filePath = "src/test/snapshots/roborazzi/$name.png"
        )
    }
}
