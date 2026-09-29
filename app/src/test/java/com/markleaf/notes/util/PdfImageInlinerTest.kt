package com.markleaf.notes.util

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * The rules that decide which images reach a PDF and how much memory they may
 * take (#474). The decoder is replaced by a counting fake here, so these run on
 * the plain JVM; [PdfImageDownsampleTest] covers the real decode.
 */
class PdfImageInlinerTest {

    @get:Rule
    val temp = TemporaryFolder()

    private lateinit var filesDir: File
    private lateinit var root: File
    private var transcodes = 0

    private fun inliner(
        budget: Long = PdfImageInliner.DOCUMENT_BUDGET_BYTES,
        result: (File) -> PdfImageInliner.EncodedImage? = {
            PdfImageInliner.EncodedImage("image/jpeg", byteArrayOf(9, 9))
        }
    ): PdfImageInliner {
        filesDir = temp.root.resolve("files").apply { mkdirs() }
        root = File(filesDir, "attachments").apply { mkdirs() }
        File(root, "n1").mkdirs()
        return PdfImageInliner(root, filesDir, budget) { transcodes++; result(it) }
    }

    @Test
    fun `a small web-safe image is inlined as it is`() {
        val inliner = inliner()
        File(root, "n1/a.png").writeBytes(byteArrayOf(1, 2, 3))

        assertEquals("data:image/png;base64,AQID", inliner.dataUri("attachments/n1/a.png"))
        assertEquals("data:image/png;base64,AQID", inliner.dataUri("./attachments/n1/a.png"))
        assertEquals(0, transcodes)
    }

    /** A camera photo goes through the decoder, not straight into the page. */
    @Test
    fun `a large image is re-encoded instead of inlined raw`() {
        val inliner = inliner()
        File(root, "n1/photo.jpg").writeBytes(ByteArray((PdfImageInliner.RAW_MAX_BYTES + 1).toInt()))

        assertEquals("data:image/jpeg;base64,CQk=", inliner.dataUri("attachments/n1/photo.jpg"))
        assertEquals(1, transcodes)
    }

    /** Formats the picker accepts but the WebView may not render are decoded too. */
    @Test
    fun `heic, heif, avif and bmp attachments are decoded whatever their size`() {
        val inliner = inliner()
        listOf("heic", "heif", "avif", "bmp").forEach { ext ->
            File(root, "n1/p.$ext").writeBytes(byteArrayOf(1))
            assertNotNull(ext, inliner.dataUri("attachments/n1/p.$ext"))
        }
        assertEquals(4, transcodes)
    }

    /** An image the platform can't decode (HEIC before Android 9) prints as text. */
    @Test
    fun `an image the decoder rejects resolves to nothing`() {
        val inliner = inliner(result = { null })
        File(root, "n1/p.heic").writeBytes(byteArrayOf(1))

        assertNull(inliner.dataUri("attachments/n1/p.heic"))
    }

    @Test
    fun `a file shown several times is decoded once`() {
        val inliner = inliner()
        File(root, "n1/p.heic").writeBytes(byteArrayOf(1))

        repeat(3) { assertNotNull(inliner.dataUri("attachments/n1/p.heic")) }
        assertEquals(1, transcodes)
    }

    /**
     * The budget is for the whole document: every occurrence stays in the page
     * string, so the one that would go over it prints as text instead.
     */
    @Test
    fun `images past the document budget print as text`() {
        val inliner = inliner(budget = 5)
        File(root, "n1/a.png").writeBytes(byteArrayOf(1, 2))
        File(root, "n1/b.png").writeBytes(byteArrayOf(3, 4))
        File(root, "n1/c.png").writeBytes(byteArrayOf(5, 6))

        assertNotNull(inliner.dataUri("attachments/n1/a.png"))
        assertNotNull(inliner.dataUri("attachments/n1/b.png"))
        assertNull(inliner.dataUri("attachments/n1/c.png"))
        assertNull("a repeat is charged again", inliner.dataUri("attachments/n1/a.png"))
    }

    /** Only the attachment folder, only images, only local paths. */
    @Test
    fun `anything outside the attachment folder or not an image resolves to nothing`() {
        val inliner = inliner()
        File(root, "n1/notes.txt").writeText("secret")
        File(filesDir, "shared_prefs").mkdirs()
        File(filesDir, "shared_prefs/x.png").writeBytes(byteArrayOf(1))

        assertNull(inliner.dataUri("attachments/n1/missing.png"))
        assertNull(inliner.dataUri("attachments/n1/notes.txt"))
        assertNull(inliner.dataUri("attachments/../shared_prefs/x.png"))
        assertNull(inliner.dataUri("shared_prefs/x.png"))
        assertNull(inliner.dataUri("https://example.com/a.png"))
        assertNull(inliner.dataUri("content://x/attachments/a.png"))
        assertNull(inliner.dataUri(""))
        assertEquals("a non-image is never handed to the decoder", 0, transcodes)
    }

    @Test
    fun `the default budget leaves room for dozens of downsampled photos`() {
        // A 2048 px JPEG at quality 85 is typically 0.3–0.8 MB.
        assertTrue(PdfImageInliner.DOCUMENT_BUDGET_BYTES / 800_000 >= 30)
    }
}
