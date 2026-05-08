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
 * We don't ship a YAML parser — everything is `key: value` line-by-line.
 * Unrecognized keys are preserved opaquely so external tools can add their
 * own without us stomping on them on round-trip.
 */
object SyncFrontmatter {
    private const val DELIMITER = "---"

    private val isoFormatter: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT

    data class Parsed(
        val markleafId: String?,
        val createdAt: Instant?,
        val updatedAt: Instant?,
        val pinned: Boolean?,
        val archived: Boolean?,
        val body: String,
        val unknownKeys: Map<String, String>
    )

    fun encode(note: Note): String {
        val sb = StringBuilder()
        sb.append(DELIMITER).append('\n')
        sb.append("markleaf_id: ").append(note.id).append('\n')
        sb.append("created_at: ").append(isoFormatter.format(note.createdAt)).append('\n')
        sb.append("updated_at: ").append(isoFormatter.format(note.updatedAt)).append('\n')
        sb.append("pinned: ").append(note.pinned).append('\n')
        sb.append("archived: ").append(note.archived).append('\n')
        sb.append(DELIMITER).append('\n')
        sb.append('\n')
        sb.append(note.contentMarkdown)
        return sb.toString()
    }

    fun decode(fileContents: String): Parsed {
        val lines = fileContents.lines()
        if (lines.firstOrNull()?.trim() != DELIMITER) {
            return Parsed(
                markleafId = null,
                createdAt = null,
                updatedAt = null,
                pinned = null,
                archived = null,
                body = fileContents,
                unknownKeys = emptyMap()
            )
        }
        val closeOffset = lines.subList(1, lines.size).indexOfFirst { it.trim() == DELIMITER }
        if (closeOffset < 0) {
            return Parsed(null, null, null, null, null, fileContents, emptyMap())
        }
        val frontmatterLines = lines.subList(1, 1 + closeOffset)
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
        val unknownKeys = mutableMapOf<String, String>()

        frontmatterLines.forEach { line ->
            val colon = line.indexOf(':')
            if (colon <= 0) return@forEach
            val key = line.substring(0, colon).trim()
            val value = line.substring(colon + 1).trim().trim('"').trim('\'')
            when (key) {
                "markleaf_id" -> markleafId = value.takeIf { it.isNotEmpty() }
                "created_at" -> createdAt = parseInstantOrNull(value)
                "updated_at" -> updatedAt = parseInstantOrNull(value)
                "pinned" -> pinned = value.equals("true", ignoreCase = true)
                "archived" -> archived = value.equals("true", ignoreCase = true)
                else -> unknownKeys[key] = value
            }
        }

        return Parsed(markleafId, createdAt, updatedAt, pinned, archived, body, unknownKeys)
    }

    private fun parseInstantOrNull(value: String): Instant? = runCatching {
        Instant.parse(value)
    }.getOrNull()
}
