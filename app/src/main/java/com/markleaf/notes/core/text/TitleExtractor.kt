package com.markleaf.notes.core.text

/**
 * Which line of a note becomes its title (#280).
 *
 * Kept here rather than beside the other settings enums because [TitleExtractor]
 * is what has to understand it, and `core` must not depend on `data`.
 */
enum class NoteTitleSource {
    /**
     * The first Markdown heading anywhere in the note, falling back to the first
     * non-empty line when there is no heading at all. The original behaviour and
     * the default — which also means a note whose heading sits *below* a
     * paragraph is titled by that heading, the inconsistency #280 reported.
     */
    FIRST_HEADING,

    /**
     * The first non-empty line, heading or not — its `#` markers are stripped
     * when that line happens to be a heading. Predictable at the price of
     * ignoring a heading further down.
     */
    FIRST_LINE
}

object TitleExtractor {

    private val HEADING_MARKER = Regex("#+\\s*")
    private val INLINE_MARKERS = Regex("\\*+|__|~~|`+")

    private fun isHeadingLine(trimmed: String): Boolean =
        trimmed.startsWith("#") && trimmed.length > 1 && (trimmed[1] == ' ' || trimmed[1] == '#')

    /**
     * Index of the line that title extraction consumes, or -1 when there is
     * none. Shared by [extractTitle] and [generateExcerpt] so the excerpt never
     * repeats the line already shown as the note title — under either [source].
     */
    private fun titleLineIndex(lines: List<String>, source: NoteTitleSource): Int {
        if (source == NoteTitleSource.FIRST_HEADING) {
            val headingIndex = lines.indexOfFirst { isHeadingLine(it.trim()) }
            if (headingIndex >= 0) return headingIndex
        }
        return lines.indexOfFirst { it.trim().isNotEmpty() }
    }

    /**
     * Extract a title from markdown content, following [source]:
     * [NoteTitleSource.FIRST_HEADING] takes the first heading and only falls
     * back to the first non-empty line, [NoteTitleSource.FIRST_LINE] always
     * takes the first non-empty line. Either way heading markers are stripped,
     * the result is capped at 80 characters, and an empty one becomes
     * "Untitled".
     *
     * [source] defaults to the historical rule so a caller with no user setting
     * to hand — a starter note, an export preview — behaves as it always did.
     */
    fun extractTitle(
        content: String,
        source: NoteTitleSource = NoteTitleSource.FIRST_HEADING
    ): String {
        if (content.isBlank()) return "Untitled"

        val lines = content.lines()
        val index = titleLineIndex(lines, source)
        if (index < 0) return "Untitled"

        val line = lines[index].trim()
        return if (isHeadingLine(line)) {
            line.replace(HEADING_MARKER, "").trim().take(80).ifEmpty { "Untitled" }
        } else {
            line.take(80)
        }
    }

    /**
     * Generate an excerpt from markdown content (first [maxLength] chars),
     * skipping the line used as the title so a list row never repeats the
     * title in its preview.
     */
    fun generateExcerpt(
        content: String,
        source: NoteTitleSource = NoteTitleSource.FIRST_HEADING,
        maxLength: Int = 100
    ): String {
        if (content.isBlank()) return ""

        val lines = content.lines()
        val titleIndex = titleLineIndex(lines, source)
        val bodyLines = if (titleIndex >= 0) {
            lines.filterIndexed { index, _ -> index != titleIndex }
        } else {
            lines
        }

        val clean = bodyLines.joinToString("\n")
            .replace(HEADING_MARKER, "") // Remove headings
            .replace(INLINE_MARKERS, "") // Remove bold, italic, strikethrough, code
            .trim()

        return if (clean.length <= maxLength) clean else clean.take(maxLength) + "..."
    }
}
