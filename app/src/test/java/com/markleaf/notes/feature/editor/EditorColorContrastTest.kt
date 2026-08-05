package com.markleaf.notes.feature.editor

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.markleaf.notes.core.markdown.markdownSyntaxColors
import com.markleaf.notes.ui.theme.DarkColorScheme
import com.markleaf.notes.ui.theme.LightColorScheme
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the editor's colours to a contrast floor against the background they are
 * drawn on.
 *
 * Written with the fix for the `MarkdownSyntaxColors` grey defaults: blockquote
 * text and `---` rules fell back to a fixed `Color.Gray` that could not know the
 * theme, and measured 3.41:1 on the light background — legible enough to pass a
 * glance, under the threshold on a meter. A golden image would not have caught
 * it either, because the render was exactly what the code asked for. Only a
 * number catches a number.
 *
 * The thresholds are WCAG 2.1: 1.4.3 for text, 1.4.11 for a meaningful icon.
 */
class EditorColorContrastTest {

    @Test
    fun markdownSyntaxColors_clearTextContrast_onLightBackground() =
        assertSyntaxContrast(LightColorScheme, "light")

    @Test
    fun markdownSyntaxColors_clearTextContrast_onDarkBackground() =
        assertSyntaxContrast(DarkColorScheme, "dark")

    @Test
    fun viewModeLockedTint_clearsIconContrast_onBothTopBars() {
        listOf("light" to LightColorScheme, "dark" to DarkColorScheme).forEach { (theme, scheme) ->
            // The top bar's container colour is colorScheme.background, which is
            // both what the tint is chosen against and what it is drawn on.
            val ratio = contrastRatio(viewModeLockedTint(scheme.background), scheme.background)
            assertTrue(
                "locked view-toggle tint is ${ratio.format()}:1 on the $theme top bar, " +
                    "under the $ICON_MIN:1 floor for a meaningful icon",
                ratio >= ICON_MIN
            )
        }
    }

    private fun assertSyntaxContrast(scheme: ColorScheme, theme: String) {
        val colors = markdownSyntaxColors(scheme)
        // `codeBlock` is left out deliberately: it is read only as a 10%-alpha
        // wash behind a fenced block, so a text threshold says nothing about it.
        val textRoles = mapOf(
            "heading" to colors.heading,
            "emphasis" to colors.emphasis,
            "link" to colors.link,
            "syntax" to colors.syntax,
            "checkbox" to colors.checkbox,
            "code" to colors.code,
            "blockquote" to colors.blockquote,
            "horizontalRule" to colors.horizontalRule
        )
        textRoles.forEach { (role, color) ->
            val ratio = contrastRatio(color, scheme.background)
            assertTrue(
                "$role is ${ratio.format()}:1 on the $theme background, " +
                    "under the $TEXT_MIN:1 floor for text",
                ratio >= TEXT_MIN
            )
        }
    }

    /** WCAG relative-luminance contrast: (lighter + 0.05) / (darker + 0.05). */
    private fun contrastRatio(foreground: Color, background: Color): Float {
        val one = foreground.luminance()
        val other = background.luminance()
        return (maxOf(one, other) + 0.05f) / (minOf(one, other) + 0.05f)
    }

    private fun Float.format(): String = "%.2f".format(this)

    private companion object {
        const val TEXT_MIN = 4.5f
        const val ICON_MIN = 3f
    }
}
