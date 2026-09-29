package com.markleaf.notes.widget

import com.markleaf.notes.feature.settings.formatHexColor
import com.markleaf.notes.feature.settings.parseHexColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The two rules behind a hand-picked widget colour (#469) that need no
 * framework: which text colour goes on it, and the hex form the picker
 * reads and writes.
 */
class WidgetCustomColorTest {

    /**
     * Whatever is picked, the title must stay readable. Swept over the whole
     * RGB cube in steps of 17 (4 913 colours) rather than a few samples,
     * because the colours where white and black are nearly tied — mid greys,
     * saturated greens and oranges — are the ones a hand-picked list misses.
     */
    @Test
    fun `the chosen text colour clears 4_5 to 1 on every opaque colour`() {
        for (r in 0..255 step 17) for (g in 0..255 step 17) for (b in 0..255 step 17) {
            val background = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            val text = readableTextColorOn(background)
            val ratio = contrast(text, background)
            assertTrue(
                "${formatHexColor(text)} on ${formatHexColor(background)} is $ratio:1",
                ratio >= 4.5
            )
        }
    }

    @Test
    fun `dark colours get white text and light colours black`() {
        assertEquals(WHITE, readableTextColorOn(0xFF212121.toInt()))
        assertEquals(WHITE, readableTextColorOn(0xFF0D47A1.toInt()))
        // A medium blue reads better in black (5.7:1) than white (3.7:1), however blue it looks.
        assertEquals(BLACK, readableTextColorOn(0xFF1E88E5.toInt()))
        assertEquals(BLACK, readableTextColorOn(0xFFFAFAFA.toInt()))
        assertEquals(BLACK, readableTextColorOn(0xFFFDD835.toInt()))
    }

    @Test
    fun `hex parses with or without the hash, in any case`() {
        assertEquals(0xFF1E88E5.toInt(), parseHexColor("#1E88E5"))
        assertEquals(0xFF1E88E5.toInt(), parseHexColor("1e88e5"))
        assertEquals(0xFF1E88E5.toInt(), parseHexColor("  #1e88E5 "))
        assertEquals(0xFF000000.toInt(), parseHexColor("#000000"))
    }

    /** The background is always opaque; opacity is its own setting. */
    @Test
    fun `short, alpha and non-hex forms are rejected`() {
        assertNull(parseHexColor(""))
        assertNull(parseHexColor("#FFF"))
        assertNull(parseHexColor("#801E88E5"))
        assertNull(parseHexColor("#1E88EG"))
        assertNull(parseHexColor("##1E88E5"))
    }

    @Test
    fun `format and parse round-trip`() {
        listOf(0xFF000000, 0xFFFFFFFF, 0xFF4CAF50, 0xFF0A0B0C).map { it.toInt() }.forEach { color ->
            assertEquals(color, parseHexColor(formatHexColor(color)))
        }
        assertEquals("#0A0B0C", formatHexColor(0xFF0A0B0C.toInt()))
    }

    private fun contrast(a: Int, b: Int): Double {
        val one = relativeLuminance(a)
        val other = relativeLuminance(b)
        return (maxOf(one, other) + 0.05) / (minOf(one, other) + 0.05)
    }

    private companion object {
        const val WHITE = 0xFFFFFFFF.toInt()
        const val BLACK = 0xFF000000.toInt()
    }
}
