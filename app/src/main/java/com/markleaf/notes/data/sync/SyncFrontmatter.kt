package com.markleaf.notes.data.sync

import com.markleaf.notes.domain.model.Note
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Encodes/decodes Markleaf metadata as YAML-style frontmatter at the top of a
 * `.md` file. The format is intentionally a strict subset of standard YAML so
 * that Obsidian, GitHub, VSCode, etc. all parse it gracefully:
 *
 * ```
 * ---
 * markleaf_id: abc-123
 * created_at: 2026-05-08T10:30:00Z
 * updated_at: 2026-05-08T11:00:00Z
 * pinned: false
 * ---
 * # Body...
 * ```
 *
 * We don't ship a YAML parser. Our own five keys are read as `key: value` on a
 * single line, which is all we ever write. Everything else is treated as opaque
 * text: an entry is a top-level line plus every continuation line under it, and
 * it is carried back out byte-for-byte so external tools can keep their own
 * frontmatter through a Markleaf round-trip.
 *
 * That opacity is the point. Reading unknown entries as `key: value` used to
 * drop every continuation line, so the block sequence Obsidian writes tags in
 * by default came back as a bare `tags:` with the items gone, and a nested map
 * was flattened into bogus top-level keys (#226).
 */
object SyncFrontmatter {
    private const val DELIMITER = "---"

    /**
     * UTF-8 byte-order mark. Editors on Windows (and a few Android apps) write
     * one in front of the first `---`, and Kotlin's `trim()` does not remove it
     * — `Char.isWhitespace()` is false for U+FEFF, which is a format character,
     * not a space. Left in place it made [decode] read the whole file as body,
     * so the `markleaf_id` went missing and the mirror forked a new file on
     * every save (#213). Built from its code point rather than written as a
     * literal so the character can't go invisible in this source file.
     */
    private val BOM: String = Char(0xFEFF).toString()

    /** Keys we own and emit explicitly — never echoed back from [Parsed.unknownEntries]. */
    private val RESERVED_KEYS = setOf(
        "markleaf_id", "created_at", "updated_at", "pinned", "archived"
    )

    private val isoFormatter: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT

    data class Parsed(
        val markleafId: String?,
        val createdAt: Instant?,
        val updatedAt: Instant?,
        val pinned: Boolean?,
        val archived: Boolean?,
        val body: String,
        /**
         * Frontmatter entries we don't model, each holding its top-level line and
         * any continuation lines below it, exactly as they were read. Multi-line
         * entries keep their newlines, so a block sequence or a nested map is one
         * element here rather than several lossy ones (#226).
         */
        val unknownEntries: List<String>,
        /**
         * True only when a complete `---` … `---` block was found *and* its
         * contents read as metadata. False for a file with no block, for one
         * whose block never closes — including when the caller simply hasn't
         * read far enough yet — and for a pair of horizontal rules with body
         * text between them. Pair it with [opensFrontmatter] and [blockClosed]
         * to tell those apart: a reader that only peeked at the head must not
         * mistake "not read far enough" for "no metadata here" (#222).
         */
        val hasFrontmatter: Boolean,
        /**
         * True when an opening delimiter was followed by a closing one, whether
         * or not what sat between them was metadata. Lets a reader stop: once
         * the block has closed, reading further cannot change the verdict.
         */
        val blockClosed: Boolean
    )

    /**
     * True when [fileContents] *opens* a frontmatter block — the first line is
     * the delimiter — whether or not the block is closed within this string.
     *
     * The distinction matters to anyone parsing a truncated prefix of a file:
     * `hasFrontmatter == false` alone cannot tell "no metadata here" from "the
     * block runs past what I read", and treating the second as the first
     * discards metadata the file really has.
     */
    fun opensFrontmatter(fileContents: String): Boolean =
        fileContents.removePrefix(BOM).lineSequence().firstOrNull()?.trim() == DELIMITER

    /**
     * @param extraEntries frontmatter entries written by other tools (Obsidian
     *   aliases, tags, cssclasses, comments, …) that we don't model, each as the
     *   verbatim text of one top-level entry. Pass [Parsed.unknownEntries] here
     *   when re-stamping a file we imported so a round-trip through Markleaf
     *   doesn't strip or reshape them. Entries are written out unchanged —
     *   indentation, quoting and blank lines included — because we cannot know
     *   what the owning tool needs. Reserved keys are ignored; we always emit
     *   our own canonical versions.
     */
    fun encode(note: Note, extraEntries: List<String> = emptyList()): String {
        val sb = StringBuilder()
        sb.append(DELIMITER).append('\n')
        sb.append("markleaf_id: ").append(note.id).append('\n')
        sb.append("created_at: ").append(isoFormatter.format(note.createdAt)).append('\n')
        sb.append("updated_at: ").append(isoFormatter.format(note.updatedAt)).append('\n')
        sb.append("pinned: ").append(note.pinned).append('\n')
        sb.append("archived: ").append(note.archived).append('\n')
        extraEntries.forEach { entry ->
            val key = topLevelKeyOf(entry.lineSequence().firstOrNull().orEmpty())
            // A null key is a comment or a line we can't read as `key: value`;
            // we keep those too rather than deciding they don't matter.
            if (key == null || key !in RESERVED_KEYS) {
                sb.append(entry).append('\n')
            }
        }
        sb.append(DELIMITER).append('\n')
        sb.append('\n')
        sb.append(note.contentMarkdown)
        return sb.toString()
    }

    fun decode(fileContents: String): Parsed {
        // A leading BOM is dropped before anything else: it would otherwise make
        // the opening delimiter check below fail and silently turn a perfectly
        // good mirror file into "a file with no frontmatter" (#213).
        val text = fileContents.removePrefix(BOM)
        val lines = text.lines()
        if (lines.firstOrNull()?.trim() != DELIMITER) {
            return Parsed(
                markleafId = null,
                createdAt = null,
                updatedAt = null,
                pinned = null,
                archived = null,
                body = text,
                unknownEntries = emptyList(),
                hasFrontmatter = false,
                blockClosed = false
            )
        }
        val closeOffset = lines.subList(1, lines.size).indexOfFirst { it.trim() == DELIMITER }
        if (closeOffset < 0) {
            return Parsed(
                null, null, null, null, null, text, emptyList(),
                hasFrontmatter = false, blockClosed = false
            )
        }
        val frontmatterLines = lines.subList(1, 1 + closeOffset)
        // `---` is also a Markdown horizontal rule, so an opening delimiter on
        // its own proves nothing. A note whose body starts with a rule and
        // carries another one later used to have everything between them
        // swallowed as unparseable frontmatter and dropped on import (#222).
        if (!looksLikeMetadata(frontmatterLines)) {
            return Parsed(
                null, null, null, null, null, text, emptyList(),
                hasFrontmatter = false, blockClosed = true
            )
        }
        var bodyStart = 1 + closeOffset + 1
        // Skip a single leading blank line after the closing delimiter for cleanliness.
        if (bodyStart < lines.size && lines[bodyStart].isEmpty()) bodyStart++
        val body = lines.subList(bodyStart.coerceAtMost(lines.size), lines.size)
            .joinToString("\n")

        var markleafId: String? = null
        var createdAt: Instant? = null
        var updatedAt: Instant? = null
        var pinned: Boolean? = null
        var archived: Boolean? = null
        val unknownEntries = mutableListOf<String>()

        groupEntries(frontmatterLines).forEach { entry ->
            val key = topLevelKeyOf(entry.first())
            if (key == null || key !in RESERVED_KEYS) {
                unknownEntries += entry.joinToString("\n")
                return@forEach
            }
            // Our own keys are single-line by construction, so the value comes
            // from the opening line; nothing else can be hiding under them.
            val value = valueOf(entry.first())
            when (key) {
                "markleaf_id" -> markleafId = value.takeIf { it.isNotEmpty() }
                "created_at" -> createdAt = parseInstantOrNull(value)
                "updated_at" -> updatedAt = parseInstantOrNull(value)
                "pinned" -> pinned = value.equals("true", ignoreCase = true)
                "archived" -> archived = value.equals("true", ignoreCase = true)
            }
        }

        return Parsed(
            markleafId, createdAt, updatedAt, pinned, archived, body, unknownEntries,
            hasFrontmatter = true, blockClosed = true
        )
    }

    /**
     * Splits frontmatter into top-level entries. A line starting at column 0
     * that isn't a sequence item opens a new entry; indented lines, `-` items
     * and blank lines belong to the entry above them. Zero-indent sequences
     * (`tags:` followed by unindented `- reading`) are legal YAML and common in
     * the wild, which is why a leading `-` counts as a continuation rather than
     * a new entry — the closing `---` never reaches here, it is stripped as the
     * delimiter before this runs.
     *
     * This is not YAML parsing. It is only enough structure to know where one
     * entry ends and the next begins, so we can hand the whole thing back.
     */
    private fun groupEntries(lines: List<String>): List<List<String>> {
        val entries = mutableListOf<MutableList<String>>()
        lines.forEach { line ->
            if (entries.isEmpty() || !isContinuation(line)) {
                entries += mutableListOf(line)
            } else {
                entries.last() += line
            }
        }
        return entries
    }

    private fun isContinuation(line: String): Boolean =
        line.isBlank() || line[0] == ' ' || line[0] == '\t' || line[0] == '-'

    /** The key of a top-level entry, or null if the line isn't `key: …`. */
    private fun topLevelKeyOf(line: String): String? {
        val colon = line.indexOf(':')
        return if (colon > 0) line.substring(0, colon).trim() else null
    }

    private fun valueOf(line: String): String {
        val colon = line.indexOf(':')
        if (colon < 0) return ""
        return line.substring(colon + 1).trim().trim('"').trim('\'')
    }

    /**
     * Whether the lines inside a closed `---` … `---` block are metadata rather
     * than body text that happens to sit between two horizontal rules.
     *
     * The test is the **first non-blank line**: real frontmatter opens with a
     * key. Later lines are deliberately not checked, because a YAML value
     * legitimately continues across lines — `tags:` followed by `  - a` is an
     * ordinary Obsidian block, and demanding that every line look like a key
     * would throw those away, trading one silent loss for a worse one.
     *
     * Requiring the key to carry no whitespace also pins it to column 0, which
     * is where a top-level YAML key belongs, and narrows the ambiguity to prose
     * whose first line is a single word followed by a colon. That case is still
     * read as frontmatter; going further needs a real YAML parser. When in
     * doubt this errs toward *not* frontmatter, because the cost of being wrong
     * that way is a duplicate file, while the other way is deleted text.
     */
    private fun looksLikeMetadata(blockLines: List<String>): Boolean {
        val first = blockLines.firstOrNull { it.isNotBlank() } ?: return false
        val colon = first.indexOf(':')
        if (colon <= 0) return false
        return first.substring(0, colon).none { it.isWhitespace() }
    }

    private fun parseInstantOrNull(value: String): Instant? = runCatching {
        Instant.parse(value)
    }.getOrNull()
}
