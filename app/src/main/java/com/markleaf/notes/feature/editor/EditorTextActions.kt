package com.markleaf.notes.feature.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.markleaf.notes.data.settings.OpenNotesAt
import kotlin.math.ceil
import kotlin.math.max

internal const val MAX_WIKILINK_SUGGESTIONS = 8
internal const val MAX_TAG_SUGGESTIONS = 8

/**
 * How long the note has to sit still before its position is written (#214).
 * Long enough that typing a paragraph costs one write rather than dozens,
 * short enough that putting the phone down mid-note records where you were.
 */
internal const val POSITION_WRITE_DEBOUNCE_MS = 1_000L
internal const val SAVE_DEBOUNCE_MS = 1_000L

/**
 * How long the note-open path waits for DataStore before opening on defaults.
 *
 * Generous rather than tight: the read normally completes in milliseconds, and
 * a device slow enough to need a second is a device where opening on the wrong
 * mode is worse than waiting. This is a stuck-forever guard, not a latency
 * budget (#204).
 */
internal const val SETTINGS_READ_TIMEOUT_MS = 2_000L

/**
 * Whether a note opens in preview: only when the setting is on *and* the note
 * has something to preview.
 *
 * The content check is not a nicety. A brand-new note is created first and then
 * opened with a real id (the FAB path in `MarkleafNavHost`), so the id alone
 * cannot tell a new note from an existing one — without this an empty note
 * would land in preview with no editor and no keyboard, and "new notes still
 * open for editing" would be broken (#200).
 *
 * Extracted so that rule has a test rather than living only in a load effect
 * that needs a device to exercise (#204).
 */
internal fun opensInPreview(openNotesInPreview: Boolean, content: String): Boolean =
    openNotesInPreview && content.isNotEmpty()

/**
 * Whether this screen may record where the note was left.
 *
 * The setting has to be on, and — the part that is easy to miss — the note must
 * not have opened on fallback settings. When the settings read times out the
 * note opens at the top on defaults; DataStore may then emit the real settings
 * a moment later, flipping `openNotesAt` to `LAST_POSITION` and starting the
 * recorder from that fallback state. Its first debounced write would store
 * caret 0 and overwrite the position the user actually left, without them
 * having touched anything (#204).
 *
 * Skipping the write is the conservative answer: we could not read what the
 * user asked for, so we do not overwrite what we already knew. Reopening the
 * note takes the normal path.
 */
internal fun recordsPosition(
    openNotesAt: OpenNotesAt,
    openedOnFallbackSettings: Boolean
): Boolean = openNotesAt == OpenNotesAt.LAST_POSITION && !openedOnFallbackSettings

/**
 * Resolves the caret offset to restore for a note, clamping it to the current
 * content length and reporting whether the saved position was usable as-is
 * (#262).
 *
 * The note can be shorter than when the position was recorded — edited in
 * another app, or a smaller version brought in by sync — so the saved offset
 * is never trusted, only clamped. A null saved offset (no row yet) is a
 * dropped restore. The status lets the caller leave a debug breadcrumb
 * instead of failing silently.
 */
internal enum class RestoreStatus { OK, CLAMPED, DROPPED }

internal fun resolveRestoredCaret(saved: Int?, contentLength: Int): Pair<Int, RestoreStatus> {
    if (saved == null) return 0 to RestoreStatus.DROPPED
    val clamped = saved.coerceIn(0, contentLength)
    return clamped to if (clamped == saved) RestoreStatus.OK else RestoreStatus.CLAMPED
}

/**
 * The same contract for the preview surface: the saved block index is clamped
 * to the rows the note actually renders, and the caller is told whether it had
 * to be (#262).
 *
 * A note opened straight into preview restores through this rather than through
 * the caret, so without it the surface that did the visible restoring is the one
 * that reports nothing. There is no `DROPPED` counterpart here on purpose: no
 * saved row means no scroll request is made at all, and the caret resolver
 * reports that case from the same row.
 */
internal fun resolveRestoredPreviewIndex(saved: Int, lastIndex: Int): Pair<Int, RestoreStatus> {
    val clamped = saved.coerceIn(0, lastIndex)
    return clamped to if (clamped == saved) RestoreStatus.OK else RestoreStatus.CLAMPED
}

/**
 * A preview scroll waiting for the preview to be built. [animate] separates the
 * two callers: restoring where a note was left should already be there when the
 * note appears, while a jump the user asked for reads better as movement.
 * [restore] marks the one caller whose clamp is worth reporting — see the scroll
 * effect.
 */
internal data class PreviewScrollRequest(
    val index: Int,
    val animate: Boolean,
    val restore: Boolean = false
)

/**
 * If the cursor sits inside an *unclosed* `[[…` wikilink (no `]]` between
 * the opening `[[` and the cursor, no newline either), return the partial
 * query text. Returns null when there's nothing to autocomplete.
 */
internal fun detectWikilinkQuery(value: TextFieldValue): String? {
    val cursor = value.selection.start.coerceIn(0, value.text.length)
    val before = value.text.substring(0, cursor)
    val openIdx = before.lastIndexOf("[[")
    if (openIdx < 0) return null
    val between = before.substring(openIdx + 2)
    if (between.contains("]]") || between.contains("\n")) return null
    return between
}

/**
 * Replace the open `[[query` segment ending at the cursor with `[[Title]]`
 * and place the cursor just after `]]`. Used when the user picks a wikilink
 * autocomplete suggestion.
 */
internal fun completeWikilink(value: TextFieldValue, title: String): TextFieldValue {
    val cursor = value.selection.start.coerceIn(0, value.text.length)
    val before = value.text.substring(0, cursor)
    val openIdx = before.lastIndexOf("[[")
    if (openIdx < 0) return value
    val replacement = "[[$title]]"
    val newText = value.text.substring(0, openIdx) + replacement + value.text.substring(cursor)
    val newCursor = openIdx + replacement.length
    return value.copy(text = newText, selection = TextRange(newCursor))
}

// A tag body may contain letters/digits (any script), `_`, `/` (hierarchy), and
// `-`; a tag must *start* with a letter or `_`. These mirror TagParser's regex so
// the autocomplete trigger and the persisted tag index agree on what a tag is.
private fun isTagBodyChar(c: Char): Boolean =
    c.isLetterOrDigit() || c == '_' || c == '/' || c == '-'

private fun isTagStartChar(c: Char): Boolean =
    c.isLetter() || c == '_'

/**
 * If the cursor sits inside an *in-progress* `#tag` (a `#` that starts the
 * content or follows whitespace, with only valid tag characters between it and
 * the cursor), return the partial tag text (possibly empty right after `#`).
 * Returns null otherwise. Mirrors [com.markleaf.notes.util.TagParser]'s rules so
 * URL fragments (`…com#frag`), `##`, and mid-word `a#b` never autocomplete.
 */
internal fun detectTagQuery(value: TextFieldValue): String? {
    val cursor = value.selection.start.coerceIn(0, value.text.length)
    val before = value.text.substring(0, cursor)
    val hashIdx = before.lastIndexOf('#')
    if (hashIdx < 0) return null
    if (hashIdx > 0 && !before[hashIdx - 1].isWhitespace()) return null
    val query = before.substring(hashIdx + 1)
    // Whitespace or punctuation between the `#` and the cursor means the tag
    // already closed — there is nothing to complete.
    if (query.any { !isTagBodyChar(it) }) return null
    // A tag cannot start with a digit, `/`, or `-`.
    if (query.isNotEmpty() && !isTagStartChar(query[0])) return null
    return query
}

/**
 * Replace the open `#query` segment ending at the cursor with `#tag ` (a
 * trailing space closes the tag so the dropdown dismisses and the writer keeps
 * typing). Used when the user picks a tag autocomplete suggestion.
 */
internal fun completeTag(value: TextFieldValue, tag: String): TextFieldValue {
    val cursor = value.selection.start.coerceIn(0, value.text.length)
    val before = value.text.substring(0, cursor)
    val hashIdx = before.lastIndexOf('#')
    if (hashIdx < 0) return value
    val after = value.text.substring(cursor)
    // Close the tag with a trailing space so the dropdown dismisses, unless the
    // next character is already whitespace (avoids a doubled space mid-line).
    val trailing = if (after.firstOrNull()?.isWhitespace() == true) "" else " "
    val replacement = "#$tag$trailing"
    val newText = value.text.substring(0, hashIdx) + replacement + after
    val newCursor = hashIdx + replacement.length
    return value.copy(text = newText, selection = TextRange(newCursor))
}

/**
 * Rewrite the alt text of `![oldAlt](path)` (or `![](path)`) to use [newAlt]
 * while keeping [path] intact. If the same path appears multiple times in the
 * body, only the first occurrence is updated — rare in practice because every
 * inserted attachment uses a UUID filename.
 */
internal fun replaceImageAlt(value: TextFieldValue, path: String, newAlt: String): TextFieldValue {
    val pattern = Regex("""!\[[^\[\]\n]*]\(${Regex.escape(path)}\)""")
    val match = pattern.find(value.text) ?: return value
    val replacement = "![${newAlt}](${path})"
    val newText = value.text.substring(0, match.range.first) +
        replacement +
        value.text.substring(match.range.last + 1)
    val delta = replacement.length - match.value.length
    val newCursor = value.selection.start + delta
    return value.copy(
        text = newText,
        selection = TextRange(newCursor.coerceIn(0, newText.length))
    )
}

internal fun findAllRanges(text: String, query: String): List<IntRange> {
    if (query.isEmpty() || text.isEmpty()) return emptyList()
    val lower = text.lowercase()
    val q = query.lowercase()
    val ranges = mutableListOf<IntRange>()
    var idx = 0
    while (idx <= lower.length - q.length) {
        val found = lower.indexOf(q, idx)
        if (found < 0) break
        ranges += found until (found + q.length)
        idx = found + q.length.coerceAtLeast(1)
    }
    return ranges
}

internal fun replaceRange(
    state: TextFieldValue,
    range: IntRange,
    replacement: String
): TextFieldValue {
    val text = state.text
    val start = range.first.coerceIn(0, text.length)
    val endExclusive = (range.last + 1).coerceIn(start, text.length)
    val newText = text.substring(0, start) + replacement + text.substring(endExclusive)
    val caret = (start + replacement.length).coerceIn(0, newText.length)
    return TextFieldValue(text = newText, selection = TextRange(caret))
}

internal fun replaceAllRanges(
    state: TextFieldValue,
    ranges: List<IntRange>,
    replacement: String
): TextFieldValue {
    if (ranges.isEmpty()) return state
    val text = state.text
    val sorted = ranges.sortedBy { it.first }
    val builder = StringBuilder()
    var cursor = 0
    for (range in sorted) {
        val start = range.first.coerceIn(0, text.length)
        val endExclusive = (range.last + 1).coerceIn(start, text.length)
        if (start > cursor) {
            builder.append(text, cursor, start)
        }
        builder.append(replacement)
        cursor = endExclusive
    }
    if (cursor < text.length) {
        builder.append(text, cursor, text.length)
    }
    val newText = builder.toString()
    return TextFieldValue(text = newText, selection = TextRange(newText.length.coerceAtLeast(0)))
}

internal data class EditorStats(val words: Int, val chars: Int, val readMinutes: Int)

internal fun computeStats(text: String): EditorStats {
    val chars = text.length
    val words = text.split(Regex("\\s+")).count { it.isNotBlank() }
    // Mixed-language heuristic: 200 wpm OR 500 chars/min, whichever is larger.
    val wordMinutes = words / 200.0
    val charMinutes = chars / 500.0
    val minutes = max(1, ceil(max(wordMinutes, charMinutes)).toInt())
    val finalMinutes = if (chars == 0) 0 else minutes
    return EditorStats(words = words, chars = chars, readMinutes = finalMinutes)
}
