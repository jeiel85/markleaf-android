package com.markleaf.notes.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.exifinterface.media.ExifInterface
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The real decode behind [PdfImageInliner] (#474), with Robolectric's native
 * graphics so BitmapFactory actually decodes and compresses.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class PdfImageDownsampleTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun write(name: String, width: Int, height: Int, alpha: Boolean): File {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(if (alpha) Color.TRANSPARENT else Color.rgb(40, 120, 60))
        val file = temp.newFile(name)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file
    }

    private fun decodedSize(image: PdfImageInliner.EncodedImage): Pair<Int, Int> {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(image.bytes, 0, image.bytes.size, options)
        return options.outWidth to options.outHeight
    }

    @Test
    fun `a large opaque image comes back as a jpeg within the edge limit`() {
        val image = requireNotNull(PdfImageInliner.downsample(write("big.png", 5000, 1200, alpha = false)))

        assertEquals("image/jpeg", image.mime)
        val (w, h) = decodedSize(image)
        assertTrue("long side $w", w <= PdfImageInliner.MAX_EDGE_PX)
        assertEquals("aspect ratio kept", 5000.0 / 1200, w.toDouble() / h, 0.05)
    }

    /** JPEG has no alpha; a transparent image would print with a black box. */
    @Test
    fun `an image with transparency stays png`() {
        val image = requireNotNull(PdfImageInliner.downsample(write("t.png", 300, 300, alpha = true)))

        assertEquals("image/png", image.mime)
        assertEquals(300 to 300, decodedSize(image))
    }

    /**
     * Import keeps the EXIF orientation and the preview honours it, but the
     * re-encoded image has no EXIF — so the rotation has to be in the pixels.
     */
    @Test
    fun `the exif orientation is applied before re-encoding`() {
        val bitmap = Bitmap.createBitmap(400, 100, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
        val file = temp.newFile("portrait.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        ExifInterface(file.path).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }

        val image = requireNotNull(PdfImageInliner.downsample(file))

        assertEquals(100 to 400, decodedSize(image))
    }

    @Test
    fun `a file that is not an image decodes to nothing`() {
        val file = temp.newFile("fake.heic").apply { writeText("not an image") }

        assertNull(PdfImageInliner.downsample(file))
    }
}
