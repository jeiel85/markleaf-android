package com.markleaf.notes.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.ui.theme.MarkleafTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The search results sit over a page that stays composed underneath (#517),
 * so a tap on them must never reach a setting below — including the empty
 * "no match" state, which is one line of text over the whole area (#526
 * review).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class SettingsSearchOverlayTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun tapsReachingThePage(results: List<SettingsSearchEntry>): Int {
        val taps = mutableIntStateOf(0)
        composeRule.setContent {
            MarkleafTheme(dynamicColor = false) {
                Box(Modifier.fillMaxSize()) {
                    // Stands in for the page: one control filling the area.
                    Button(onClick = { taps.intValue++ }, modifier = Modifier.fillMaxSize()) {
                        Text("Underneath")
                    }
                    SettingsSearchResults(
                        results = results,
                        onPick = {},
                        sectionTitle = { it.name },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        composeRule.onRoot().performTouchInput {
            click(center)
            click(bottomCenter.copy(y = bottom - 10f))
        }
        composeRule.waitForIdle()
        return taps.intValue
    }

    @Test
    fun tapsOnTheEmptyStateDoNotReachThePage() {
        assertEquals(0, tapsReachingThePage(emptyList()))
    }

    @Test
    fun tapsBelowTheLastResultDoNotReachThePage() {
        val one = SettingsSearchEntry(SettingsShortcut.APPEARANCE, SettingsItem.THEME, "Theme", listOf("Theme"))
        assertEquals(0, tapsReachingThePage(listOf(one)))
    }
}
