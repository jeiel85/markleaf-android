package com.markleaf.notes.util

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.markleaf.notes.data.sync.SyncFrontmatter
import java.io.InputStream
import java.time.Instant

/**
 * One text file that lives outside Markleaf's own storage — the `.md`/`.txt` a
 * user shares in (#139) or opens for reading (#326).
 *
 * Two callers need the same three things: read the text, find out what the file
 * is called, and turn it into a note body. The rules behind each are subtle
 * enough that a second copy would drift — the size cap is what keeps a huge or
 * binary file from OOM/ANR-ing the cold-start path, and the title seeding is
 * what makes an imported file keep its own name instead of being titled by
 * whatever its first line happens to say (#134).
 */
object ExternalFile {

    /**
     * How much of a file is read. Real note files are a few KB; two million
     * characters is a generous ceiling that still bounds the worst case when a
     * file manager hands over something that is neither small nor text.
     */
    const val MAX_CHARS = 2_000_000

    /** A file that was read successfully. [displayName] is null when the provider withholds it. */
    data class Document(
        val displayName: String?,
        val text: String,
        val lastModifiedAt: Instant? = null
    )

    /** The body and timestamps to use when a document is explicitly kept as a note. */
    data class NoteSeed(
        val body: String,
        val createdAt: Instant?,
        val updatedAt: Instant?
    )

    /**
     * Read [uri] as UTF-8 text, capped at [MAX_CHARS]. Returns null when the
     * file cannot be opened (moved, deleted, or a permission that has since
     * lapsed) or holds nothing but whitespace — callers treat both the same
     * way, as "there is nothing here to show".
     *
     * Does I/O: call from a background dispatcher wherever the caller can.
     */
    fun read(context: Context, uri: Uri): Document? {
        // Capture provider metadata before opening the stream. Some cloud
        // providers update their remote/cache metadata as a file is read; the
        // import must retain the timestamp that existed when the user picked it.
        val name = displayName(context, uri)
        val lastModified = lastModifiedAt(context, uri)
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.use(::readCapped)
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: return null
        return Document(
            displayName = name,
            text = text,
            lastModifiedAt = lastModified
        )
    }

    /** The note body [document] becomes when it is kept as a note. */
    fun noteBody(document: Document): String = noteBody(document.text, document.displayName)

    /**
     * Build the metadata for an explicit import without changing the source.
     * Markleaf files carry both timestamps in frontmatter; ordinary providers
     * generally expose only LAST_MODIFIED, which is used for both note dates.
     */
    fun noteSeed(document: Document): NoteSeed {
        val parsed = SyncFrontmatter.decode(document.text)
        val fallback = document.lastModifiedAt
        return NoteSeed(
            body = noteBody(document),
            createdAt = parsed.createdAt ?: fallback,
            updatedAt = parsed.updatedAt ?: fallback ?: parsed.createdAt
        )
    }

    /**
     * Seed a title from the file name when the text does not already carry one.
     * A leading `#` (heading) or `---` (frontmatter) means the file titles
     * itself, and prepending anything there would fight whatever wrote it.
     */
    internal fun noteBody(text: String, displayName: String?): String {
        val name = displayName
            ?.substringBeforeLast('.')
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        val trimmed = text.trimStart()
        val alreadyTitled = trimmed.startsWith("#") || trimmed.startsWith("---")
        return if (name != null && !alreadyTitled) "# $name\n\n$text" else text
    }

    /** The file's own name, e.g. `meeting-notes.md`, or null when it has none. */
    fun displayName(context: Context, uri: Uri): String? {
        if (uri.scheme == "file") return uri.lastPathSegment
        return runCatching {
            context.contentResolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
    }

    /** The provider's original modification time, if it exposes one. */
    fun lastModifiedAt(context: Context, uri: Uri): Instant? {
        val millis = if (uri.scheme == "file") {
            uri.path?.let { java.io.File(it).lastModified() }
        } else {
            runCatching {
                context.contentResolver.query(
                    uri, arrayOf(DocumentsContract.Document.COLUMN_LAST_MODIFIED), null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                        if (index >= 0 && !cursor.isNull(index)) cursor.getLong(index) else null
                    } else {
                        null
                    }
                }
            }.getOrNull()
        }
        return millis?.takeIf { it >= 0L }?.let(Instant::ofEpochMilli)
    }

    internal fun readCapped(input: InputStream): String {
        val reader = input.bufferedReader(Charsets.UTF_8)
        val sb = StringBuilder()
        val buf = CharArray(8192)
        var total = 0
        while (total < MAX_CHARS) {
            val read = reader.read(buf)
            if (read < 0) break
            val take = minOf(read, MAX_CHARS - total)
            sb.append(buf, 0, take)
            total += take
        }
        return sb.toString()
    }
}
