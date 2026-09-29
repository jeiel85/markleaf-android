package com.markleaf.notes.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Base64

/**
 * Turns the images a note references into `data:` URIs for [ExportPdf] (#474).
 *
 * Input: an image destination from the note's Markdown.
 * Output: a `data:` URI, or null — which the export prints as the preview's
 * `![alt](path)` text.
 *
 * Only files under `filesDir/attachments` resolve, by canonical path, which is
 * the same set the in-app preview draws; a crafted `../` destination cannot
 * pull another app-private file into a PDF.
 *
 * Memory is bounded for the whole document, not per file, because the page is
 * one string: every inlined image stays in it until printing ends, base64 adds
 * a third, and a string holds two bytes per character. Three things keep a
 * note full of camera photos from exhausting the heap:
 * - a small web-safe file (≤ [RAW_MAX_BYTES]) is inlined as it is, anything
 *   larger is decoded at a reduced size (long side ≤ [MAX_EDGE_PX]) and
 *   re-encoded, which a printed page can't tell apart;
 * - each file is read and encoded once however often the note shows it;
 * - [budgetBytes] caps the encoded bytes of all occurrences together, and an
 *   image that would go over it prints as text instead — and is not kept:
 *   its bytes are dropped and only a "doesn't fit" marker is cached, so a
 *   note with hundreds of photos holds at most the budget, not every photo.
 *
 * The decode path is also what gives HEIC, HEIF, AVIF and BMP attachments a
 * format the print WebView can show: the picker accepts them and the preview
 * decodes them, so the PDF should too, on the Android versions that can.
 *
 * One instance per export: the cache and the budget belong to one document.
 */
internal class PdfImageInliner(
    private val attachmentsRoot: File,
    private val filesDir: File,
    private val budgetBytes: Long = DOCUMENT_BUDGET_BYTES,
    private val transcode: (File) -> EncodedImage? = ::downsample
) {
    constructor(context: Context) : this(File(context.filesDir, "attachments"), context.filesDir)

    internal class EncodedImage(val mime: String, val bytes: ByteArray)

    private val encoded = HashMap<String, EncodedImage?>()
    private var usedBytes = 0L

    /** Encoded bytes still held by the cache — for tests of the memory bound. */
    internal val retainedBytes: Long
        get() = encoded.values.sumOf { it?.bytes?.size?.toLong() ?: 0L }

    fun dataUri(destination: String): String? {
        val file = resolve(destination) ?: return null
        val image = if (encoded.containsKey(file.path)) {
            encoded[file.path]
        } else {
            encode(file)
        } ?: run {
            encoded[file.path] = null
            return null
        }
        if (usedBytes + image.bytes.size > budgetBytes) {
            // The budget only grows, so this file will never fit later either.
            encoded[file.path] = null
            return null
        }
        encoded[file.path] = image
        usedBytes += image.bytes.size
        return "data:${image.mime};base64," + Base64.getEncoder().encodeToString(image.bytes)
    }

    private fun resolve(destination: String): File? {
        val relative = destination.trim().removePrefix("./")
        if (relative.isEmpty() || relative.contains(':')) return null
        val file = runCatching { File(filesDir, relative).canonicalFile }.getOrNull() ?: return null
        val root = runCatching { attachmentsRoot.canonicalFile }.getOrNull() ?: return null
        return file.takeIf { it.path.startsWith(root.path + File.separator) && it.isFile }
    }

    private fun encode(file: File): EncodedImage? {
        val extension = file.extension.lowercase()
        val webSafeMime = WEB_SAFE_MIME[extension]
        if (webSafeMime != null && file.length() <= RAW_MAX_BYTES) {
            return runCatching { EncodedImage(webSafeMime, file.readBytes()) }.getOrNull()
        }
        if (webSafeMime == null && extension !in DECODABLE_ONLY) return null
        return runCatching { transcode(file) }.getOrNull()
    }

    internal companion object {
        const val RAW_MAX_BYTES = 1_500_000L
        const val MAX_EDGE_PX = 2048
        const val DOCUMENT_BUDGET_BYTES = 24L * 1024 * 1024

        private val WEB_SAFE_MIME = mapOf(
            "png" to "image/png",
            "jpg" to "image/jpeg",
            "jpeg" to "image/jpeg",
            "webp" to "image/webp",
            "gif" to "image/gif"
        )

        /** Stored under these extensions by `AttachmentManager` (the MIME subtype); decoded, never inlined raw. */
        private val DECODABLE_ONLY = setOf("bmp", "heic", "heif", "avif")

        /**
         * Decodes [file] at the largest power-of-two reduction that keeps its long
         * side within [MAX_EDGE_PX], and re-encodes it as JPEG, or PNG when it has transparent
         * pixels. Null when the platform can't decode it — HEIC before
         * Android 9, AVIF before 12 — which prints as text, like any unresolved image.
         *
         * The EXIF orientation is applied to the pixels before re-encoding.
         * `BitmapFactory` ignores it and the re-encoded image carries no EXIF,
         * while import keeps `TAG_ORIENTATION` and the preview honours it — so
         * without this a portrait camera photo would print sideways.
         */
        fun downsample(file: File): EncodedImage? {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            val longSide = maxOf(bounds.outWidth, bounds.outHeight)
            if (longSide <= 0) return null
            var sample = 1
            while (longSide / sample > MAX_EDGE_PX) sample *= 2
            val bitmap = BitmapFactory.decodeFile(
                file.path,
                BitmapFactory.Options().apply { inSampleSize = sample }
            ) ?: return null
            val oriented = applyExifOrientation(bitmap, file)
            if (oriented !== bitmap) bitmap.recycle()
            return compress(oriented)
        }

        private fun compress(bitmap: Bitmap): EncodedImage {
            try {
                val out = ByteArrayOutputStream()
                return if (hasTransparentPixel(bitmap)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    EncodedImage("image/png", out.toByteArray())
                } else {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                    EncodedImage("image/jpeg", out.toByteArray())
                }
            } finally {
                bitmap.recycle()
            }
        }

        private fun applyExifOrientation(bitmap: Bitmap, file: File): Bitmap {
            val orientation = runCatching {
                ExifInterface(file.path).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
                else -> return bitmap
            }
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }

        /**
         * Whether any pixel is actually see-through. `Bitmap.hasAlpha()` alone is
         * not enough: an RGBA PNG — which is what most screenshots are — reports
         * an alpha channel even when every pixel is opaque, and would otherwise
         * stay a PNG several times the size of the JPEG. Scans a row at a time so
         * the check costs one row of memory, not a second copy of the image.
         */
        private fun hasTransparentPixel(bitmap: Bitmap): Boolean {
            if (!bitmap.hasAlpha()) return false
            val row = IntArray(bitmap.width)
            for (y in 0 until bitmap.height) {
                bitmap.getPixels(row, 0, bitmap.width, 0, y, bitmap.width, 1)
                if (row.any { (it ushr 24) != 0xFF }) return true
            }
            return false
        }

        private const val JPEG_QUALITY = 85
    }
}
