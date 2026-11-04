package com.markleaf.notes.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.ui.theme.MarkleafTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The preset swatches in the widget colour dialog (#262): TalkBack has to hear
 * a swatch as something to choose, and which one the current colour is —
 * a bare hex code says neither.
 *
 * The row is tested on its own. The whole dialog never goes idle under
 * Robolectric — it fails the same way with the swatches as they were before
 * this change — while on an emulator it draws no frames once open, so that is
 * Robolectric's, not the dialog's.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class WidgetColorPickerTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun swatch(hex: String) = composeRule.onNode(hasContentDescription(hex))
    private val isRadioButton = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)

    private fun showSwatches(initial: Int) {
        composeRule.setContent {
            var color by mutableIntStateOf(initial)
            MarkleafTheme(dynamicColor = false) {
                PresetSwatches(selectedColor = color, onPick = { color = it })
            }
        }
    }

    @Test
    fun swatchesAreRadioButtonsAndTheCurrentColourIsSelected() {
        showSwatches(GREEN)

        swatch("#4CAF50").assert(isRadioButton).assertIsSelected()
        swatch("#1E88E5").assert(isRadioButton).assertIsNotSelected()
    }

    @Test
    fun tappingASwatchMovesTheSelection() {
        showSwatches(GREEN)

        swatch("#1E88E5").performClick()

        swatch("#1E88E5").assertIsSelected()
        swatch("#4CAF50").assertIsNotSelected()
    }

    @Test
    fun aColourThatIsNoPresetSelectsNone() {
        showSwatches(0xFF123456.toInt())

        swatch("#4CAF50").assertIsNotSelected()
        swatch("#1E88E5").assertIsNotSelected()
    }

    private companion object {
        val GREEN = 0xFF4CAF50.toInt()
    }
}
