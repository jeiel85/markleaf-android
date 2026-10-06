package com.markleaf.notes.feature.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Opening Find puts the caret in its field (#262): the reader opened it to type
 * a word, and having to tap the field first was the extra step the emulator
 * check for #417 ran into. Composed the way the editor shows it — the bar only
 * exists while Find is open.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class FindBarFocusTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var findOpen by mutableStateOf(false)

    private fun showHost(showReplace: Boolean) {
        composeRule.setContent {
            if (findOpen) {
                FindBar(
                    query = "",
                    onQueryChange = {},
                    currentIndex = 0,
                    totalMatches = 0,
                    onPrev = {},
                    onNext = {},
                    onClose = {},
                    showReplace = showReplace
                )
            }
        }
    }

    @Test
    fun openingFindFocusesTheSearchField() {
        showHost(showReplace = true)

        findOpen = true
        composeRule.waitForIdle()

        // The first of the two fields is the search one; Replace sits below it.
        composeRule.onAllNodes(hasSetTextAction())[0].assertIsFocused()
    }

    @Test
    fun inPreviewWithNoReplaceRowTheSearchFieldIsFocusedToo() {
        showHost(showReplace = false)

        findOpen = true
        composeRule.waitForIdle()

        composeRule.onAllNodes(hasSetTextAction())[0].assertIsFocused()
    }
}
