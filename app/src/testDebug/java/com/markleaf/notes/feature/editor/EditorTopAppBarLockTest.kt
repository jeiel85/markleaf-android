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

    private var previewToggles = 0
    private var lockToggles = 0

    /**
     * Renders the bar and returns the view-toggle node. In edit mode the toggle
     * offers "Preview"; in preview mode it offers "Edit" — the label names the
     * destination, not the current state.
     */
    private fun toggle(isPreviewMode: Boolean = false, isViewModeLocked: Boolean = false) = run {
        composeRule.setContent {
            MarkleafTheme(dynamicColor = false) {
                EditorTopAppBar(
                    title = "New Note",
                    isPreviewMode = isPreviewMode,
                    isFocusMode = false,
                    showMore = true,
                    moreExpanded = false,
                    onBack = {},
                    onTogglePreview = { previewToggles++ },
                    onExitFocusMode = {},
                    onOpenOutline = {},
                    onOpenInfo = {},
                    onOpenMore = {},
                    onDismissMore = {},
                    moreMenuContent = {},
                    isViewModeLocked = isViewModeLocked,
                    onToggleLock = { lockToggles++ }
                )
            }
        }
        composeRule.onNodeWithContentDescription(if (isPreviewMode) "Edit" else "Preview")
    }

    @Test
    fun tapFlipsPreviewAndLongPressFlipsLockIndependently() {
        val toggle = toggle()

        toggle.performClick()
        assertEquals(1, previewToggles)
        assertEquals(0, lockToggles)

        toggle.performTouchInput { longClick() }
        assertEquals(1, lockToggles)
        assertEquals(1, previewToggles)
    }

    /**
     * The other order, which is the one at risk. The lock gesture runs in the
     * Initial pass and consumes the release so the underlying IconButton does
     * not also see a tap — if that consumption leaked past its own gesture, the
     * *next* tap would be swallowed and the button would look dead.
     */
    @Test
    fun aTapAfterALongPressStillFlipsPreview() {
        val toggle = toggle()

        toggle.performTouchInput { longClick() }
        assertEquals(1, lockToggles)
        assertEquals(0, previewToggles)

        toggle.performClick()
        assertEquals(1, previewToggles)
        assertEquals(1, lockToggles)
    }

    /** Locking pins the mode; it does not disable the control that flips it. */
    @Test
    fun tapAndLongPressStillWorkWhileLocked() {
        val toggle = toggle(isViewModeLocked = true)

        toggle.performClick()
        assertEquals(1, previewToggles)

        // Long-pressing a locked toggle unlocks it — same gesture, same handler.
        toggle.performTouchInput { longClick() }
        assertEquals(1, lockToggles)
        assertEquals(1, previewToggles)
    }

    /**
     * Repeated long-presses each register. The lock is a toggle, so a second
     * press has to reach the handler rather than being eaten as a repeat.
     */
    @Test
    fun repeatedLongPressesEachFire() {
        val toggle = toggle()

        toggle.performTouchInput { longClick() }
        toggle.performTouchInput { longClick() }
        toggle.performTouchInput { longClick() }

        assertEquals(3, lockToggles)
        assertEquals(0, previewToggles)
    }

    /**
     * The control keeps its label in preview mode, which is what a screen
     * reader announces before either gesture. Losing it would leave the lock
     * reachable only by sighted trial and error.
     */
    @Test
    fun theToggleIsReachableByItsLabelInPreviewMode() {
        val toggle = toggle(isPreviewMode = true)

        toggle.performTouchInput { longClick() }
        assertEquals(1, lockToggles)

        toggle.performClick()
        assertEquals(1, previewToggles)
    }
}
