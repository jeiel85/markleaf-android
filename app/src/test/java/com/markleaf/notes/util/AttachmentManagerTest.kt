package com.markleaf.notes.util

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.exifinterface.media.ExifInterface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AttachmentManagerTest {

    private val miniJpeg = byteArrayOf(
        0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x00.toByte(), 0x10.toByte(),
        0x4A.toByte(), 0x46.toByte(), 0x49.toByte(), 0x46.toByte(), 0x00.toByte(), 0x01.toByte(),
        0x01.toByte(), 0x01.toByte(), 0x00.toByte(), 0x60.toByte(), 0x00.toByte(), 0x60.toByte(),
        0x00.toByte(), 0x00.toByte(), 0xFF.toByte(), 0xDB.toByte(), 0x00.toByte(), 0x43.toByte(),
        0x00.toByte(), 0x08.toByte(), 0x06.toByte(), 0x06.toByte(), 0x07.toByte(), 0x06.toByte(),
        0x05.toByte(), 0x08.toByte(), 0x07.toByte(), 0x07.toByte(), 0x07.toByte(), 0x09.toByte(),
        0x09.toByte(), 0x08.toByte(), 0x0A.toByte(), 0x0C.toByte(), 0x14.toByte(), 0x0D.toByte(),
        0x0C.toByte(), 0x0B.toByte(), 0x0B.toByte(), 0x14.toByte(), 0x0F.toByte(), 0x10.toByte(),
        0x0C.toByte(), 0x14.toByte(), 0x1D.toByte(), 0x1A.toByte(), 0x1F.toByte(), 0x1E.toByte(),
        0x1D.toByte(), 0x1A.toByte(), 0x1C.toByte(), 0x1C.toByte(), 0x20.toByte(), 0x24.toByte(),
        0x2E.toByte(), 0x27.toByte(), 0x20.toByte(), 0x22.toByte(), 0x2C.toByte(), 0x23.toByte(),
        0x1C.toByte(), 0x1C.toByte(), 0x28.toByte(), 0x37.toByte(), 0x29.toByte(), 0x2C.toByte(),
        0x30.toByte(), 0x31.toByte(), 0x34.toByte(), 0x34.toByte(), 0x34.toByte(), 0x1F.toByte(),
        0x27.toByte(), 0x39.toByte(), 0x3D.toByte(), 0x38.toByte(), 0x32.toByte(), 0x3C.toByte(),
        0x2E.toByte(), 0x33.toByte(), 0x34.toByte(), 0x32.toByte(), 0x32.toByte(), 0xFF.toByte(), 0xC0.toByte(),
        0x00.toByte(), 0x0B.toByte(), 0x08.toByte(), 0x00.toByte(), 0x01.toByte(), 0x00.toByte(),
        0x01.toByte(), 0x01.toByte(), 0x01.toByte(), 0x11.toByte(), 0x00.toByte(), 0xFF.toByte(),
        0xC4.toByte(), 0x00.toByte(), 0x1F.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(),
        0x05.toByte(), 0x01.toByte(), 0x01.toByte(), 0x01.toByte(), 0x01.toByte(), 0x01.toByte(),
        0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(),
        0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(), 0x02.toByte(), 0x03.toByte(),
        0x04.toByte(), 0x05.toByte(), 0x06.toByte(), 0x07.toByte(), 0x08.toByte(), 0x09.toByte(),
        0x0A.toByte(), 0x0B.toByte(), 0xFF.toByte(), 0xDA.toByte(), 0x00.toByte(), 0x08.toByte(),
        0x01.toByte(), 0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0x3F.toByte(), 0x00.toByte(),
        0x37.toByte(), 0xFF.toByte(), 0xD9.toByte()
    )

    @Test
    fun testCopyImageIntoStorageStripsExifMetadata() = kotlinx.coroutines.test.runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // 1. Create a temporary image file on disk and write some EXIF tags to it
        val tempFile = File(context.cacheDir, "temp_test_image.jpg")
        FileOutputStream(tempFile).use { it.write(miniJpeg) }
        
        val sourceExif = ExifInterface(tempFile.absolutePath)
        sourceExif.setAttribute(ExifInterface.TAG_MAKE, "TestCameraMake")
        sourceExif.setAttribute(ExifInterface.TAG_MODEL, "TestCameraModel")
        sourceExif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, "37/1,30/1,0/1")
        sourceExif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N")
        sourceExif.setAttribute(ExifInterface.TAG_DATETIME, "2026:05:21 12:00:00")
        sourceExif.saveAttributes()

        // Verify tags are initially set
        val checkExif = ExifInterface(tempFile.absolutePath)
        assertEquals("TestCameraMake", checkExif.getAttribute(ExifInterface.TAG_MAKE))
        assertEquals("TestCameraModel", checkExif.getAttribute(ExifInterface.TAG_MODEL))
        assertEquals("2026:05:21 12:00:00", checkExif.getAttribute(ExifInterface.TAG_DATETIME))

        // 2. Import this image using AttachmentManager
        val result = AttachmentManager.copyIntoStorage(
            context = context,
            noteId = "test-note-1",
            sourceUri = Uri.fromFile(tempFile)
        )
        
        assertNotNull(result)
        
        // 3. Resolve the imported file and verify EXIF tags are null/cleared
        val copiedFile = AttachmentManager.resolveFile(context, result!!.relativePath)
        assertNotNull(copiedFile)
        assertTrue(copiedFile!!.exists())
        
        val copiedExif = ExifInterface(copiedFile.absolutePath)
        assertNull(copiedExif.getAttribute(ExifInterface.TAG_MAKE))
        assertNull(copiedExif.getAttribute(ExifInterface.TAG_MODEL))
        assertNull(copiedExif.getAttribute(ExifInterface.TAG_GPS_LATITUDE))
        assertNull(copiedExif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF))
        assertNull(copiedExif.getAttribute(ExifInterface.TAG_DATETIME))
        
        // Cleanup
        tempFile.delete()
        AttachmentManager.deleteAllForNote(context, "test-note-1")
    }
}
