package com.markleaf.notes.ui.theme

import androidx.compose.ui.text.font.FontFamily
import com.markleaf.notes.data.settings.EditorFont
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * What each [EditorFont] asks the theme for. Sans is `null` rather than
 * [FontFamily.Default] on purpose: it leaves the original Typography object
 * untouched, so existing users and golden images render exactly as before.
 */
class EditorFontFamilyTest {

    @Test
    fun `sans leaves the typography untouched`() {
        assertNull(EditorFont.SANS.bodyFontFamily())
    }

    @Test
    fun `serif and monospace map to their system families`() {
        assertEquals(FontFamily.Serif, EditorFont.SERIF.bodyFontFamily())
        assertEquals(FontFamily.Monospace, EditorFont.MONOSPACE.bodyFontFamily())
    }
}
