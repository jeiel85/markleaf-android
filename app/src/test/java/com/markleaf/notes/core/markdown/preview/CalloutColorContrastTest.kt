package com.markleaf.notes.core.markdown.preview

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.markleaf.notes.core.markdown.CalloutKind
import com.markleaf.notes.ui.theme.DarkColorScheme
import com.markleaf.notes.ui.theme.LightColorScheme
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins every callout's text to a contrast floor against its own fill, in the
 * schemes Markleaf ships and in a Material You Monochrome scheme (#473).
 *
 * The Monochrome scheme is the one that broke: a Galaxy A55 on a monochrome
 * wallpaper palette showed a NOTE callout as `#3C3C3C` on a `#FAFAFA` page,
 * with the body text in the page's near-black. Those are tones 25 and 98 of a
 * neutral palette, which is exactly how Material's `SchemeMonochrome` places
 * `primaryContainer` and `background` in light mode. Dynamic colour needs a
 * device, so the scheme is rebuilt here from those tones; only the roles a
 * callout reads are set, the rest are Material defaults and unused.
 *
 * The threshold is WCAG 2.1 1.4.3 for text, the same floor
 * `EditorColorContrastTest` uses.
 */
class CalloutColorContrastTest {

    @Test
    fun calloutText_clearsContrast_inMarkleafLight() = assertCalloutContrast(LightColorScheme, "light")

    @Test
    fun calloutText_clearsContrast_inMarkleafDark() = assertCalloutContrast(DarkColorScheme, "dark")

    @Test
    fun calloutText_clearsContrast_inMonochromeLight() =
        assertCalloutContrast(MonochromeLight, "Monochrome light")

    @Test
    fun calloutText_clearsContrast_inMonochromeDark() =
        assertCalloutContrast(MonochromeDark, "Monochrome dark")

    /**
     * Guards the test itself: if the rebuilt scheme stopped reproducing the
     * report, the two tests above would pass without proving anything.
     */
    @Test
    fun monochromeScheme_reproducesTheReportedPairing() {
        listOf("light" to MonochromeLight, "dark" to MonochromeDark).forEach { (theme, scheme) ->
            val ratio = contrastRatio(scheme.onBackground, scheme.primaryContainer)
            assertTrue(
                "page text on the NOTE fill is ${ratio.format()}:1 in Monochrome $theme; " +
                    "the scheme no longer reproduces #473",
                ratio < TEXT_MIN
            )
        }
    }

    private fun assertCalloutContrast(scheme: ColorScheme, theme: String) {
        (CalloutKind.entries + listOf<CalloutKind?>(null)).forEach { kind ->
            val colors = calloutColors(kind, scheme)
            val ratio = contrastRatio(colors.content, colors.container)
            assertTrue(
                "${kind ?: "untyped"} callout text is ${ratio.format()}:1 on its fill in the " +
                    "$theme scheme, under the $TEXT_MIN:1 floor for text",
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

        /** A neutral (chroma 0) tone as sRGB grey. */
        fun tone(t: Int): Color = Color(
            when (t) {
                0 -> 0xFF000000
                10 -> 0xFF1B1B1B
                20 -> 0xFF303030
                25 -> 0xFF3B3B3B
                60 -> 0xFF919191
                85 -> 0xFFD4D4D4
                90 -> 0xFFE2E2E2
                98 -> 0xFFF9F9F9
                100 -> 0xFFFFFFFF
                else -> error("tone $t not tabulated")
            }
        )

        val MonochromeLight = lightColorScheme(
            primary = tone(0),
            primaryContainer = tone(25),
            onPrimaryContainer = tone(100),
            secondary = tone(20),
            secondaryContainer = tone(85),
            onSecondaryContainer = tone(10),
            tertiary = tone(25),
            tertiaryContainer = tone(60),
            onTertiaryContainer = tone(0),
            background = tone(98),
            onBackground = tone(10),
            surfaceVariant = tone(90),
            onSurfaceVariant = tone(25),
        )

        val MonochromeDark = darkColorScheme(
            primary = tone(100),
            primaryContainer = tone(85),
            onPrimaryContainer = tone(0),
            secondary = tone(85),
            secondaryContainer = tone(25),
            onSecondaryContainer = tone(90),
            tertiary = tone(90),
            tertiaryContainer = tone(60),
            onTertiaryContainer = tone(0),
            background = tone(10),
            onBackground = tone(90),
            surfaceVariant = tone(25),
            onSurfaceVariant = tone(85),
        )
    }
}
