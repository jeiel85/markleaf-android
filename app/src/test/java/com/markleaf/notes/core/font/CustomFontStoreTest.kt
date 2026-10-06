package com.markleaf.notes.core.font

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Importing a reader's own font file (#510). The real font is Roboto, read from
 * the Robolectric native runtime already on the test classpath, so the
 * repository carries no font file of its own and no font licence with it.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class CustomFontStoreTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun startEmpty() {
        CustomFontStore.directory(context).deleteRecursively()
    }

    private fun realFontBytes(): ByteArray =
        requireNotNull(javaClass.classLoader?.getResourceAsStream("fonts/Roboto-Regular.ttf")) {
            "Roboto-Regular.ttf is expected on the test classpath (Robolectric native runtime)"
        }.use { it.readBytes() }

    /** A picked document stands outside app storage; copy it to the cache like one. */
    private fun pickable(name: String, bytes: ByteArray): Uri {
        val file = File(context.cacheDir, name).apply { writeBytes(bytes) }
        return Uri.fromFile(file)
    }

    private fun storedFiles(): List<String> =
        CustomFontStore.directory(context).listFiles()?.map { it.name }.orEmpty()

    @Test
    fun aRealFontIsCopiedIntoAppStorage() {
        val source = realFontBytes()

        val result = CustomFontStore.import(context, pickable("Reader.ttf", source))

        assertTrue("expected the font to import, got $result", result is CustomFontStore.ImportResult.Imported)
        val imported = result as CustomFontStore.ImportResult.Imported
        val stored = CustomFontStore.file(context, imported.fileName)
        assertNotNull(stored)
        assertTrue(stored!!.readBytes().contentEquals(source))
        assertTrue(CustomFontStore.isFont(stored))
    }

    @Test
    fun aFileThatIsNotAFontIsRejectedAndNotKept() {
        val result = CustomFontStore.import(context, pickable("notes.ttf", "# Not a font".toByteArray()))

        assertEquals(CustomFontStore.ImportResult.NotAFont, result)
        assertEquals(emptyList<String>(), storedFiles())
    }

    @Test
    fun aFileWithAFontHeaderButNoFontIsRejected() {
        // Starts like TrueType and stops there: the header check alone would pass it.
        val bytes = byteArrayOf(0, 1, 0, 0) + ByteArray(64)

        val result = CustomFontStore.import(context, pickable("broken.ttf", bytes))

        assertEquals(CustomFontStore.ImportResult.NotAFont, result)
        assertEquals(emptyList<String>(), storedFiles())
    }

    @Test
    fun aFontOverTheSizeLimitIsRejectedAndNotKept() {
        val result = CustomFontStore.import(
            context,
            pickable("Reader.ttf", realFontBytes()),
            maxBytes = 1024
        )

        assertEquals(CustomFontStore.ImportResult.TooLarge, result)
        assertEquals(emptyList<String>(), storedFiles())
    }

    @Test
    fun aDocumentThatCannotBeOpenedIsUnreadable() {
        val missing = Uri.fromFile(File(context.cacheDir, "gone.ttf"))

        assertEquals(CustomFontStore.ImportResult.Unreadable, CustomFontStore.import(context, missing))
    }

    @Test
    fun replacingAFontLeavesOnlyTheNewOne() {
        val bytes = realFontBytes()
        val first = CustomFontStore.import(context, pickable("A.ttf", bytes)) as CustomFontStore.ImportResult.Imported
        val second = CustomFontStore.import(context, pickable("B.ttf", bytes)) as CustomFontStore.ImportResult.Imported

        CustomFontStore.deleteAllExcept(context, second.fileName)

        assertEquals(listOf(second.fileName), storedFiles())
        assertNull(CustomFontStore.file(context, first.fileName))
        assertFalse(first.fileName == second.fileName)
    }

    @Test
    fun noStoredNameMeansNoFile() {
        assertNull(CustomFontStore.file(context, null))
        assertNull(CustomFontStore.file(context, ""))
        assertNull(CustomFontStore.file(context, "custom-never-imported"))
    }
}
