package com.markleaf.notes.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.data.settings.EditorFont
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * What each [EditorFont] asks the theme for. Sans is `null` rather than
 * [FontFamily.Default] on purpose: it leaves the original Typography object
 * untouched, so existing users and golden images render exactly as before.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class EditorFontFamilyTest {

    /** Compose loads a file font when the family is built, so it has to be a real one. */
    private val readerFont: File by lazy {
        File.createTempFile("reader-font", ".ttf").apply {
            deleteOnExit()
            writeBytes(
                requireNotNull(EditorFontFamilyTest::class.java.classLoader?.getResourceAsStream("fonts/Roboto-Regular.ttf")) {
                    "Roboto-Regular.ttf is expected on the test classpath (Robolectric native runtime)"
                }.use { it.readBytes() }
            )
        }
    }

    @Test
    fun `sans leaves the typography untouched`() {
        assertNull(EditorFont.SANS.bodyFontFamily())
    }

    @Test
    fun `serif and monospace map to their system families`() {
        assertEquals(FontFamily.Serif, EditorFont.SERIF.bodyFontFamily())
        assertEquals(FontFamily.Monospace, EditorFont.MONOSPACE.bodyFontFamily())
    }

    @Test
    fun `your font without a file draws as sans`() {
        assertNull(EditorFont.CUSTOM.bodyFontFamily(customFont = null))
    }

    @Test
    fun `your font with a file gets a family of its own`() {
        val family = EditorFont.CUSTOM.bodyFontFamily(customFont = readerFont)
        assertNotNull(family)
        assertNotEquals(FontFamily.Serif, family)
        assertNotEquals(FontFamily.Monospace, family)
    }

    @Test
    fun `a built-in choice ignores a stored font file`() {
        assertEquals(FontFamily.Serif, EditorFont.SERIF.bodyFontFamily(customFont = readerFont))
        assertNull(EditorFont.SANS.bodyFontFamily(customFont = readerFont))
    }
}
