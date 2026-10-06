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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

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

    /**
     * Every table record intact, every table's data zeroed: the structure check
     * passes it, so only the platform parser can refuse it (#513 review).
     */
    private fun fontWithZeroedTables(): ByteArray {
        val bytes = realFontBytes()
        val tableCount = ((bytes[4].toInt() and 0xFF) shl 8) or (bytes[5].toInt() and 0xFF)
        for (index in 12 + tableCount * 16 until bytes.size) bytes[index] = 0
        return bytes
    }

    @Test
    fun aFontWithBrokenTableDataIsRejected() {
        val result = CustomFontStore.import(context, pickable("broken-tables.ttf", fontWithZeroedTables()))

        assertEquals(CustomFontStore.ImportResult.NotAFont, result)
        assertEquals(emptyList<String>(), storedFiles())
    }

    /** Android 8–9 have no Font.Builder; Typeface.Builder has to do the refusing there. */
    @Test
    @Config(sdk = [28])
    fun onAndroid9AFontWithBrokenTableDataIsRejected() {
        val result = CustomFontStore.import(context, pickable("broken-tables.ttf", fontWithZeroedTables()))

        assertEquals(CustomFontStore.ImportResult.NotAFont, result)
        // The Android 9 loader keeps the file mapped after refusing it. Android
        // and the Linux CI delete a mapped file; a Windows host can't, so only
        // there is the leftover check skipped — the refusal above still runs.
        if (!System.getProperty("os.name").orEmpty().startsWith("Windows")) {
            assertEquals(emptyList<String>(), storedFiles())
        }
    }

    @Test
    @Config(sdk = [28])
    fun onAndroid9ARealFontStillImports() {
        val result = CustomFontStore.import(context, pickable("Reader.ttf", realFontBytes()))

        assertTrue("expected the font to import, got $result", result is CustomFontStore.ImportResult.Imported)
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
    fun twoOverlappingReplacementsLeaveTheRecordedFontOnDisk() = runBlocking {
        // #513 review: a second pick while the first import is still being
        // recorded. Without serialising the two, the first one's cleanup ran
        // after the second was recorded and deleted the font in use, so the
        // setting pointed at a file that no longer existed.
        val bytes = realFontBytes()
        var recorded: String? = null
        val firstRecording = CompletableDeferred<Unit>()

        val first = async(Dispatchers.Default) {
            CustomFontStore.replace(context, pickable("A.ttf", bytes)) { imported ->
                firstRecording.complete(Unit)
                delay(300) // a slow preference write
                recorded = imported.fileName
            }
        }
        firstRecording.await()
        val second = async(Dispatchers.Default) {
            CustomFontStore.replace(context, pickable("B.ttf", bytes)) { imported ->
                recorded = imported.fileName
            }
        }
        first.await()
        second.await()

        val stored = storedFiles()
        assertEquals("expected exactly the recorded font to remain", listOf(recorded), stored)
        assertNotNull(CustomFontStore.file(context, recorded))
    }

    @Test
    fun aCollectionPointingFarOutsideTheFileIsRefusedNotACrash() {
        // #513 review: `ttcf` with a first-font offset of 0x7fffffff overflowed
        // the bounds check (`at + 4` went negative) and threw from String().
        val bytes = "ttcf".toByteArray(Charsets.ISO_8859_1) +
            byteArrayOf(0, 1, 0, 0, 0, 0, 0, 1, 0x7f, 0xff.toByte(), 0xff.toByte(), 0xff.toByte()) +
            ByteArray(64)

        val result = CustomFontStore.import(context, pickable("huge-offset.ttc", bytes))

        assertEquals(CustomFontStore.ImportResult.NotAFont, result)
        assertEquals(emptyList<String>(), storedFiles())
    }

    @Test
    fun leavingWhileAFontIsBeingRecordedStillFinishesTheReplacement() = runBlocking {
        // #513 review: leaving Settings cancels the screen's scope. The import
        // had already written its file; cancelling then skipped both recording
        // it and cleaning up, leaving an orphan of up to 32 MB per attempt.
        val recordingStarted = CompletableDeferred<Unit>()
        var recorded: String? = null
        val replacement = launch(Dispatchers.Default) {
            CustomFontStore.replace(context, pickable("Reader.ttf", realFontBytes())) { imported ->
                recordingStarted.complete(Unit)
                delay(200) // a slow preference write
                recorded = imported.fileName
            }
        }
        recordingStarted.await()
        replacement.cancelAndJoin()

        assertNotNull("expected the picked font to be recorded despite the cancellation", recorded)
        assertEquals(listOf(recorded), storedFiles())
    }

    @Test
    fun noStoredNameMeansNoFile() {
        assertNull(CustomFontStore.file(context, null))
        assertNull(CustomFontStore.file(context, ""))
        assertNull(CustomFontStore.file(context, "custom-never-imported"))
    }
}
