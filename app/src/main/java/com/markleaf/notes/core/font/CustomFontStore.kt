package com.markleaf.notes.core.font

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.UUID

/**
 * The one font file a reader can bring for the writing surface (#510).
 *
 * Input: a document the reader picked with the system file picker (no storage
 * permission, nothing leaves the device). Output: a private copy under
 * `filesDir/fonts/`, named by Markleaf, plus the name the reader knows it by.
 *
 * Core logic: copy first, then check the copy — a picker hands over a stream,
 * not a path, and checking a stream would mean reading it twice. A file that
 * isn't a TrueType/OpenType font is deleted again rather than stored, because
 * Android quietly draws its default font for a file it can't parse; storing
 * one would leave the reader looking at "their" font that isn't.
 *
 * One file only, as #510 asked: Compose draws bold and italic from it by
 * thickening and slanting. Code keeps the monospace font regardless — that is
 * decided where code is drawn, not here.
 */
object CustomFontStore {

    /** Generous enough for a full CJK font (Noto Sans CJK is about 16 MB). */
    const val MAX_BYTES: Long = 32L * 1024 * 1024

    private const val DIRECTORY = "fonts"
    private const val FALLBACK_DISPLAY_NAME = "font"

    sealed interface ImportResult {
        /** Stored as [fileName] inside [directory]; [displayName] is what the picker called it. */
        data class Imported(val fileName: String, val displayName: String) : ImportResult
        data object NotAFont : ImportResult
        data object TooLarge : ImportResult
        data object Unreadable : ImportResult
    }

    fun directory(context: Context): File = File(context.filesDir, DIRECTORY)

    /** The stored font called [fileName], or null when there is none (never imported, or gone). */
    fun file(context: Context, fileName: String?): File? {
        if (fileName.isNullOrEmpty()) return null
        return File(directory(context), fileName).takeIf { it.isFile }
    }

    /** Blocking file and content-resolver I/O: call off the main thread. */
    fun import(context: Context, uri: Uri, maxBytes: Long = MAX_BYTES): ImportResult {
        val resolver = context.contentResolver
        val displayName = displayName(resolver, uri) ?: FALLBACK_DISPLAY_NAME
        val target = File(directory(context).apply { mkdirs() }, "custom-${UUID.randomUUID()}")
        val copied = try {
            val input = resolver.openInputStream(uri) ?: return ImportResult.Unreadable
            input.use { copyAtMost(it, target, maxBytes) }
        } catch (e: IOException) {
            target.delete()
            return ImportResult.Unreadable
        } catch (e: SecurityException) {
            // The grant behind a picked document can be revoked before we read it.
            target.delete()
            return ImportResult.Unreadable
        }
        if (!copied) {
            target.delete()
            return ImportResult.TooLarge
        }
        if (!isFont(target)) {
            target.delete()
            return ImportResult.NotAFont
        }
        return ImportResult.Imported(target.name, displayName)
    }

    /**
     * Removes every stored font except [keep]: a replaced font is not kept
     * around, so the folder only ever holds the one in use.
     */
    fun deleteAllExcept(context: Context, keep: String?) {
        directory(context).listFiles()?.forEach { file ->
            if (file.name != keep) file.delete()
        }
    }

    /**
     * Whether [file] is a font Android can draw. The structure check below runs
     * on every supported API level; from Android 10 the platform's own parser
     * also has to accept it.
     */
    fun isFont(file: File): Boolean {
        val bytes = try {
            file.readBytes()
        } catch (e: IOException) {
            return false
        }
        if (!hasFontStructure(bytes)) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return true
        // From the bytes already read, not the file: a font built from a file
        // maps it, and a mapped file can't be deleted on every platform the
        // tests run on — replacing a font must be able to remove the old one.
        val buffer = ByteBuffer.allocateDirect(bytes.size).put(bytes).apply { flip() }
        return try {
            android.graphics.fonts.Font.Builder(buffer).build()
            true
        } catch (e: IOException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    /**
     * A TrueType/OpenType font (`00 01 00 00`, `true`, `OTTO`), or the first
     * font of a collection (`ttcf`), whose table directory fits inside the file
     * and names the tables every such font needs. A header alone isn't enough:
     * a file that only starts like a font passed the header check, and the
     * platform parser can't be relied on below Android 10.
     */
    private fun hasFontStructure(bytes: ByteArray): Boolean {
        val tag = tagAt(bytes, 0) ?: return false
        val fontOffset = when (tag) {
            "ttcf" -> uint32At(bytes, 12) ?: return false
            SFNT_TRUETYPE, "true", "OTTO" -> 0L
            else -> return false
        }
        if (fontOffset > Int.MAX_VALUE) return false
        return hasTableDirectory(bytes, fontOffset.toInt())
    }

    private fun hasTableDirectory(bytes: ByteArray, start: Int): Boolean {
        val tag = tagAt(bytes, start) ?: return false
        if (tag != SFNT_TRUETYPE && tag != "true" && tag != "OTTO") return false
        val tableCount = uint16At(bytes, start + 4) ?: return false
        if (tableCount == 0) return false
        val tags = HashSet<String>()
        for (index in 0 until tableCount) {
            val record = start + 12 + index * 16
            val tableTag = tagAt(bytes, record) ?: return false
            val offset = uint32At(bytes, record + 8) ?: return false
            val length = uint32At(bytes, record + 12) ?: return false
            if (offset + length > bytes.size) return false
            tags += tableTag
        }
        return tags.containsAll(REQUIRED_TABLES)
    }

    private fun tagAt(bytes: ByteArray, at: Int): String? =
        if (at < 0 || at + 4 > bytes.size) null else String(bytes, at, 4, Charsets.ISO_8859_1)

    private fun uint16At(bytes: ByteArray, at: Int): Int? =
        if (at < 0 || at + 2 > bytes.size) {
            null
        } else {
            ((bytes[at].toInt() and 0xFF) shl 8) or (bytes[at + 1].toInt() and 0xFF)
        }

    private fun uint32At(bytes: ByteArray, at: Int): Long? =
        if (at < 0 || at + 4 > bytes.size) {
            null
        } else {
            (0 until 4).fold(0L) { value, i -> (value shl 8) or (bytes[at + i].toLong() and 0xFF) }
        }

    private const val SFNT_TRUETYPE = "\u0000\u0001\u0000\u0000"

    /** The tables every TrueType and CFF OpenType font carries, per the OpenType spec. */
    private val REQUIRED_TABLES = setOf("cmap", "head", "hhea", "hmtx", "maxp")

    /** Copies at most [maxBytes]; false (and a partial file the caller deletes) when there was more. */
    private fun copyAtMost(input: InputStream, target: File, maxBytes: Long): Boolean {
        var total = 0L
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        target.outputStream().use { output ->
            while (true) {
                val count = input.read(buffer)
                if (count < 0) return true
                total += count
                if (total > maxBytes) return false
                output.write(buffer, 0, count)
            }
        }
    }

    private fun displayName(resolver: ContentResolver, uri: Uri): String? = try {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0)?.takeIf { it.isNotBlank() } else null
        }
    } catch (e: RuntimeException) {
        // A provider that can't answer the name still let us open the stream.
        null
    }
}
