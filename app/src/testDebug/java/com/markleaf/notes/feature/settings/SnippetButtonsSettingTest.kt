package com.markleaf.notes.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.R
import com.markleaf.notes.ui.theme.MarkleafTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The snippet list in Settings says when the stored list couldn't be read
 * (#262): otherwise the empty list looks like "none yet", and the first
 * snippet the user adds writes over the ones they had.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class SnippetButtonsSettingTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val notice = ApplicationProvider.getApplicationContext<android.content.Context>()
        .getString(R.string.snippet_buttons_unreadable)

    private fun show(unreadable: Boolean) {
        composeRule.setContent {
            MarkleafTheme(dynamicColor = false) {
                Column {
                    SnippetButtonsSetting(buttons = emptyList(), onChange = {}, storedListUnreadable = unreadable)
                }
            }
        }
    }

    @Test
    fun anUnreadableStoredListIsExplained() {
        show(unreadable = true)

        composeRule.onNodeWithText(notice).assertExists()
    }

    @Test
    fun anEmptyListSaysNothingExtra() {
        show(unreadable = false)

        composeRule.onNodeWithText(notice).assertDoesNotExist()
    }
}
