package com.markleaf.notes.feature.editor

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.ui.theme.MarkleafTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The view-toggle icon carries two gestures (#200): a tap flips this note's
 * preview mode, a long-press flips the persistent lock. They must stay
 * independent — a tap must never fire the lock, and vice versa.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h80dp-mdpi")
class EditorTopAppBarLockTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tapFlipsPreviewAndLongPressFlipsLockIndependently() {
        var previewToggles = 0
        var lockToggles = 0
        composeRule.setContent {
            MarkleafTheme(dynamicColor = false) {
                EditorTopAppBar(
                    title = "New Note",
                    isPreviewMode = false,
                    isFocusMode = false,
                    showMore = true,
                    moreExpanded = false,
                    onBack = {},
                    onTogglePreview = { previewToggles++ },
                    onExitFocusMode = {},
                    onOpenInfo = {},
                    onOpenMore = {},
                    onDismissMore = {},
                    moreMenuContent = {},
                    isViewModeLocked = false,
                    onToggleLock = { lockToggles++ }
                )
            }
        }

        // While in edit mode the toggle offers "Preview".
        val toggle = composeRule.onNodeWithContentDescription("Preview")

        toggle.performClick()
        assertEquals(1, previewToggles)
        assertEquals(0, lockToggles)

        toggle.performTouchInput { longClick() }
        assertEquals(1, lockToggles)
        assertEquals(1, previewToggles)
    }
}
