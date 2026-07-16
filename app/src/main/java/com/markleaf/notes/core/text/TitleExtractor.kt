package com.markleaf.notes.core.text

object TitleExtractor {

    private val HEADING_MARKER = Regex("#+\\s*")
    private val INLINE_MARKERS = Regex("\\*+|__|~~|`+")

    private fun isHeadingLine(trimmed: String): Boolean =
        trimmed.startsWith("#") && trimmed.length > 1 && (trimmed[1] == ' ' || trimmed[1] == '#')

    /**
     * Index of the line that title extraction consumes — the first Markdown
     * heading, otherwise the first non-empty line — or -1 when there is none.
     * Shared by [extractTitle] and [generateExcerpt] so the excerpt never
     * repeats the line already shown as the note title.
     */
    private fun titleLineIndex(lines: List<String>): Int {
        val headingIndex = lines.indexOfFirst { isHeadingLine(it.trim()) }
        if (headingIndex >= 0) return headingIndex
        return lines.indexOfFirst { it.trim().isNotEmpty() }
    }

    /**
     * Extract title from markdown content.
     * Priority:
     * 1. First Markdown heading
     * 2. First non-empty line
     * 3. Fallback: "Untitled"
     */
    fun extractTitle(content: String): String {
        if (content.isBlank()) return "Untitled"

        val lines = content.lines()
        val index = titleLineIndex(lines)
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
    fun generateExcerpt(content: String, maxLength: Int = 100): String {
        if (content.isBlank()) return ""

        val lines = content.lines()
        val titleIndex = titleLineIndex(lines)
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
